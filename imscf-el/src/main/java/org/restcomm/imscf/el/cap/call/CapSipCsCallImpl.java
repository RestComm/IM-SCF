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

import java.util.ArrayList;
import java.util.List;

import org.restcomm.imscf.el.call.MDCParameters;
import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.converter.CapSipConverter;
import org.restcomm.imscf.el.sip.SipModule;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.isup.CauseCap;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;

/**
 * Storage for information and state related to a CAP call.
 */
public class CapSipCsCallImpl extends CapSipCallBase implements CapSipCsCall {

    private CAPState capState;
    private CapSipCsCall.SIPState sipState;
    private CAPDialogCircuitSwitchedCall capDialog;
    private InitialDPRequest idp;
    private CapSipConverter converterModule;
    private List<BCSMEvent> bcsmEventsForPendingRrbcsm;
    private CauseCap pendingReleaseCause;
    private final CallSegmentAssociation callSegmentAssociation = new CallSegmentAssociation();
    private String asProvidedServiceIdentifier;
    private boolean automaticCallProcessingEnabled = true;
    private BCSMType bcsmType;

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
    public CAPState getCsCapState() {
        return capState;
    }

    @Override
    public void setCsCapState(CAPState capState) {
        this.capState = capState;
    }

    @Override
    public CapSipCsCall.SIPState getSipState() {
        return sipState;
    }

    @Override
    public void setSipState(CapSipCsCall.SIPState sipState) {
        this.sipState = sipState;
    }

    @Override
    public void setCapDialog(CAPDialogCircuitSwitchedCall capDialog) {
        this.capDialog = capDialog;
        setLocalTcapTrId(this.capDialog.getLocalDialogId());
        setRemoteTcapTrId(this.capDialog.getRemoteDialogId());

        getMdcMap().put(MDCParameters.SCCP_LOCAL, String.valueOf(this.capDialog.getLocalAddress()));
        getMdcMap().put(MDCParameters.SCCP_REMOTE, String.valueOf(this.capDialog.getRemoteAddress()));

        getMdcMap().put(MDCParameters.TCAP_LOCAL_DIALOG_ID, String.valueOf(this.capDialog.getLocalDialogId()));
        getMdcMap().put(MDCParameters.TCAP_REMOTE_DIALOG_ID, String.valueOf(this.capDialog.getRemoteDialogId()));
    }

    @Override
    public CAPDialogCircuitSwitchedCall getCapDialog() {
        return capDialog;
    }

    @Override
    public void setBCSMType(BCSMType bcsmType) {
        this.bcsmType = bcsmType;
    }

    @Override
    public BCSMType getBCSMType() {
        return bcsmType;
    }

    @Override
    public void setIdp(InitialDPRequest idp) {
        this.idp = idp;

        String calling = null, called = "", calledBCD = "";
        if (idp.getCallingPartyNumber() != null) {
            try {
                calling = idp.getCallingPartyNumber().getCallingPartyNumber().getAddress();
            } catch (CAPException e) {
                calling = "";
            }
        }
        if (idp.getCalledPartyNumber() != null) {
            try {
                called = idp.getCalledPartyNumber().getCalledPartyNumber().getAddress();
            } catch (CAPException e) {
                called = "";
            }
        }
        if (idp.getCalledPartyBCDNumber() != null) {
            calledBCD = idp.getCalledPartyBCDNumber().getAddress();
        }

        String msisdns = calling + "|" + called + "|" + calledBCD;

        getMdcMap().put(MDCParameters.CAP_CALLING, calling);
        getMdcMap().put(MDCParameters.CAP_CALLED, called);
        getMdcMap().put(MDCParameters.CAP_CALLEDBCD, calledBCD);
        getMdcMap().put(MDCParameters.CAP_MSISDN, msisdns);
    }

    @Override
    public InitialDPRequest getIdp() {
        return idp;
    }

    @Override
    public List<BCSMEvent> getBCSMEventsForPendingRrbcsm() {
        if (bcsmEventsForPendingRrbcsm == null) {
            bcsmEventsForPendingRrbcsm = new ArrayList<BCSMEvent>();
        }
        return bcsmEventsForPendingRrbcsm;
    }

    @Override
    public CauseCap getPendingReleaseCause() {
        return pendingReleaseCause;
    }

    @Override
    public void setPendingReleaseCause(CauseCap pendingReleaseCause) {
        this.pendingReleaseCause = pendingReleaseCause;
    }

    @Override
    public CallSegmentAssociation getCallSegmentAssociation() {
        return callSegmentAssociation;
    }

    @Override
    public void setAsProvidedServiceIdentifier(String asProvidedServiceIdentifier) {
        this.asProvidedServiceIdentifier = asProvidedServiceIdentifier;
    }

    @Override
    public String getAsProvidedServiceIdentifier() {
        return asProvidedServiceIdentifier;
    }

    @Override
    public boolean isAutomaticCallProcessingEnabled() {
        return automaticCallProcessingEnabled;
    }

    @Override
    public void setAutomaticCallProcessingEnabled(boolean automaticCallProcessingEnabled) {
        this.automaticCallProcessingEnabled = automaticCallProcessingEnabled;
    }

    @Override
    public String toString() {
        return "Call [imscfCallId=" + getImscfCallId() + ", serviceIdentifier=" + getServiceIdentifier()
                + ", sipAppSessionId=" + sipAppSessionId + ", remoteTcapTrId=" + getRemoteTcapTrId()
                + ", localTcapTrId=" + getLocalTcapTrId() + ", capState=" + capState + ", sipState=" + sipState
                + ", sipScenarioCount=" + sipScenarios.size() + ", capScenarioCount=" + capOutgoingScenarios.size()
                + ", csa=" + callSegmentAssociation + "]";
    }

}
