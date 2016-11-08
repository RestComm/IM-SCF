/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

import org.restcomm.imscf.el.map.MapSmsListener;
import org.restcomm.imscf.el.map.call.MAPCall;

import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SS7 stack listener for MAP SMS operations, used only for SendRoutingInfoForSM.
 */
public class MAPSmsListener extends ImscfStackListener implements MapSmsListener {

    private static final Logger LOG = LoggerFactory.getLogger(MAPSmsListener.class);

    @Override
    public void onErrorComponent(MAPDialog arg0, Long arg1, MAPErrorMessage arg2) {
        LOG.warn("Unexpected message: {}, {}, {}", arg0, arg1, arg2);
    }

    @Override
    public void onInvokeTimeout(MAPDialog arg0, Long arg1) {
        LOG.warn("Unexpected invokeTimeout: {}, {}", arg0, arg1);

    }

    @Override
    public void onMAPMessage(MAPMessage arg0) {
        LOG.trace("MAP message {}", arg0);
    }

    @Override
    public void onRejectComponent(MAPDialog arg0, Long arg1, Problem arg2, boolean arg3) {
        LOG.warn("Unexpected reject: {}, {}, {}, {}", arg0, arg1, arg2, arg3);
    }

    @Override
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse arg0) {
        try (MAPCall call = (MAPCall) getCallStore().getCallByLocalTcapTrId(arg0.getMAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for SendRoutingInfoForSMResponse: {}", arg0);
                return;
            }
            call.getMapModule().onSendRoutingInfoForSMResponse(arg0);
        }
    }

}
