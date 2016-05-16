/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011­2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.imscf.el.stack;

import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.GsmScfSmsListener;
import org.restcomm.imscf.el.cap.call.CAPCall;
import org.restcomm.imscf.el.cap.call.CapDialogCallData;
import org.restcomm.imscf.el.cap.call.CapSmsCall;
import org.restcomm.imscf.el.modules.routing.ModuleRouter;

import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPMessage;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.service.sms.EventReportSMSRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.InitialDPSMSRequest;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stack listener for SMS. There is a single instance of this which is responsible for distributing
 * the messages among actual CAP modules.
 */
public class CAPSMSListener extends ImscfStackListener implements GsmScfSmsListener {

    private static final Logger LOG = LoggerFactory.getLogger(CAPSMSListener.class);

    @Override
    public void onCAPMessage(CAPMessage arg0) {
        LOG.trace("CAPMessage: {}", arg0);
    }

    @Override
    public void onErrorComponent(CAPDialog arg0, Long arg1, CAPErrorMessage arg2) {
        LOG.warn("Unexpected message: {}", arg0);
    }

    @Override
    public void onInvokeTimeout(CAPDialog arg0, Long arg1) {
        LOG.warn("Unexpected message: {}", arg0);
    }

    @Override
    public void onRejectComponent(CAPDialog arg0, Long arg1, Problem arg2, boolean arg3) {
        LOG.warn("Unexpected message: {}", arg0);
    }

    @Override
    public void onEventReportSMSRequest(EventReportSMSRequest arg0) {
        try (CAPCall<?> call = (CAPCall<?>) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for eventReportSMS: {}", arg0);
                return;
            }
            call.getCapModule().onEventReportSMSRequest(arg0);
        }

    }

    @Override
    public void onInitialDPSMSRequest(InitialDPSMSRequest arg0) {
        String imscfCallId = (String) CallContext.get(CallContext.IMSCFCALLID);
        LOG.debug("New InitialDPSMS received with call id {}:\n{}", imscfCallId, arg0);
        // ElStatistics.createOneShotServiceKeyStatisticsSetter(arg0.getServiceKey()).incInitialDpSmsCount();
        CapDialogCallData data = new CapDialogCallData();
        data.setImscfCallId(imscfCallId);
        arg0.getCAPDialog().setUserObject(data);
        CAPModule module = ModuleRouter.getInstance().route(arg0);
        if (module == null) {
            LOG.warn("Failed to route initialDPSMS to any module, check module routing config! idpSms: {}", arg0);
            return;
        }
        LOG.debug("New sms call will be handled by CAP module {}", module.getName());
        imscfCallId = callFactory.newCall(arg0, module);
        try (CapSmsCall call = (CapSmsCall) callStore.getCallByImscfCallId(imscfCallId)) {
            call.getCapModule().onInitialDPSMSRequest(arg0);
        }
    }

}
