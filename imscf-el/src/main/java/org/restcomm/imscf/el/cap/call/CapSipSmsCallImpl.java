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
package org.restcomm.imscf.el.cap.call;

import org.restcomm.imscf.el.call.MDCParameters;
import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.converter.CapSipConverter;
import org.restcomm.imscf.el.sip.SipModule;

import org.mobicents.protocols.ss7.cap.api.service.sms.CAPDialogSms;
import org.mobicents.protocols.ss7.cap.api.service.sms.InitialDPSMSRequest;

/**
 * Storage for information and state related to a CAP SMS call.
 */
public class CapSipSmsCallImpl extends CapSipCallBase implements CapSipSmsCall {

    private CAPState capState;
    private CAPDialogSms capDialog;
    private InitialDPSMSRequest idpSms;
    private CapSipConverter converterModule;
    private CapSipSmsCall.SIPState sipState;

    @Override
    public void setCapModule(CAPModule capModule) {
        this.converterModule = (CapSipConverter) capModule;
    }

    @Override
    public CAPModule getCapModule() {
        return converterModule;
    }

    @Override
    public void setSipModule(SipModule sipModule) {
        this.converterModule = (CapSipConverter) sipModule;
    }

    @Override
    public SipModule getSipModule() {
        return converterModule;
    }

    @Override
    public CAPState getSmsCapState() {
        return capState;
    }

    @Override
    public void setSmsCapState(CAPState capState) {
        this.capState = capState;
    }

    @Override
    public void setCapDialog(CAPDialogSms capDialog) {
        this.capDialog = capDialog;

        getMdcMap().put(MDCParameters.SCCP_LOCAL, this.capDialog.getLocalAddress().toString());
        getMdcMap().put(MDCParameters.SCCP_REMOTE, this.capDialog.getRemoteAddress().toString());

        getMdcMap().put(MDCParameters.TCAP_LOCAL_DIALOG_ID, String.valueOf(this.capDialog.getLocalDialogId()));
        getMdcMap().put(MDCParameters.TCAP_REMOTE_DIALOG_ID, String.valueOf(this.capDialog.getRemoteDialogId()));
    }

    @Override
    public CAPDialogSms getCapDialog() {
        return capDialog;
    }

    @Override
    public void setIdpSMS(InitialDPSMSRequest idpSms) {
        this.idpSms = idpSms;

        String calling = null, called = "", calledDestSub = "";
        if (idpSms.getCallingPartyNumber() != null) {
            calling = idpSms.getCallingPartyNumber().getAddress();
        }
        if (idpSms.getCalledPartyNumber() != null) {
            called = idpSms.getCalledPartyNumber().getAddress();
        }
        if (idpSms.getDestinationSubscriberNumber() != null) {
            calledDestSub = idpSms.getDestinationSubscriberNumber().getAddress();
        }

        String msisdns = calling + "|" + called + "|" + calledDestSub;

        getMdcMap().put(MDCParameters.CAP_CALLING, calling);
        getMdcMap().put(MDCParameters.CAP_CALLED, called);
        getMdcMap().put(MDCParameters.CAP_DESTINATION_SUBSCRIBER, calledDestSub);
        getMdcMap().put(MDCParameters.CAP_MSISDN, msisdns);
    }

    @Override
    public InitialDPSMSRequest getIdpSMS() {
        return idpSms;
    }

    @Override
    public void setSipState(SIPState sipState) {
        this.sipState = sipState;

    }

    @Override
    public SIPState getSipState() {
        return sipState;
    }

    @Override
    public String toString() {
        return "SmsCall [imscfCallId=" + getImscfCallId() + ", sipAppSessionId=" + sipAppSessionId + ", remoteTcapTrId="
                + getRemoteTcapTrId() + ", localTcapTrId=" + getLocalTcapTrId() + ", capState=" + capState
                + ", scenarioCount=" + sipScenarios.size() + "]";
    }

}
