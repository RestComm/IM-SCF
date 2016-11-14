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

import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.GsmScfCsCallListener;
import org.restcomm.imscf.el.cap.call.CAPCSCall;
import org.restcomm.imscf.el.cap.call.CapDialogCallData;
import org.restcomm.imscf.el.modules.routing.ModuleRouter;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;
import org.restcomm.imscf.el.statistics.ElStatistics;

import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPMessage;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ActivityTestResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CallInformationReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectLegResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitiateCallAttemptResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.MoveLegResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SpecializedResourceReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SplitLegResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CollectInformationRequest;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stack listener for CS calls. There is a single instance of this which is responsible for distributing
 * the messages among actual CAP modules.
 */
public class CAPCSCallListener extends ImscfStackListener implements GsmScfCsCallListener {

    private static final Logger LOG = LoggerFactory.getLogger(CAPCSCallListener.class);

    @Override
    public void onActivityTestResponse(ActivityTestResponse arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onActivityTestResponse: {}", arg0);
                return;
            }
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier())
                    .incActivityTestResponseCount();
            try {
                call.getCapModule().onActivityTestResponse(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onApplyChargingReportRequest(ApplyChargingReportRequest arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onApplyChargingReportRequest: {}", arg0);
                return;
            }
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier())
                    .incApplyChargingReportCount();
            try {
                call.getCapModule().onApplyChargingReportRequest(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onCallInformationReportRequest(CallInformationReportRequest arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onCallInformationReportRequest: {}", arg0);
                return;
            }
            try {
                call.getCapModule().onCallInformationReportRequest(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onDisconnectLegResponse(DisconnectLegResponse arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onDisconnectLegResponse: {}", arg0);
                return;
            }
            try {
                call.getCapModule().onDisconnectLegResponse(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onEventReportBCSMRequest(EventReportBCSMRequest arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onEventReportBCSMRequest: {}", arg0);
                return;
            }
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier()).incEventReportBcsmCount();
            try {
                call.getCapModule().onEventReportBCSMRequest(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onInitialDPRequest(InitialDPRequest arg0) {
        String imscfCallId = (String) CallContext.get(CallContext.IMSCFCALLID);
        LOG.debug("New InitialDP received with call id {}:\n{}", imscfCallId, arg0);

        // TODO: maybe increment counters here, before anything call related could happen

        CapDialogCallData data = new CapDialogCallData();
        data.setImscfCallId(imscfCallId);
        arg0.getCAPDialog().setUserObject(data);
        CAPModule module = ModuleRouter.getInstance().route(arg0);
        if (module == null) {
            LOG.warn("Failed to route initialDP to any module, check module routing config! idp: {}", arg0);
            return;
        }
        LOG.debug("New call will be handled by CAP module {}", module.getName());
        imscfCallId = callFactory.newCall(arg0, module); // should return the same though...
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByImscfCallId(imscfCallId)) {

            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier()).incInitialDpCount();
            // Since when TcapStatisticsListener is notified no call existed it cannot know the service key.
            // So TCAP-level counters could not have been increased.
            // Increase here tcapBeginReceivedCount and tcapInvokeReceivedCount.
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier())
                    .incTcapBeginReceivedCount();
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier())
                    .incTcapInvokeReceivedCount();
            try {
                call.getCapModule().onInitialDPRequest(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onInitiateCallAttemptResponse(InitiateCallAttemptResponse arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onInitiateCallAttemptResponse: {}", arg0);
                return;
            }
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier())
                    .incInitiateCallAttemptResponseCount();
            try {
                call.getCapModule().onInitiateCallAttemptResponse(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onMoveLegResponse(MoveLegResponse arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onMoveLegResponse: {}", arg0);
                return;
            }
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier()).incMoveLegResponseCount();
            try {
                call.getCapModule().onMoveLegResponse(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onPromptAndCollectUserInformationResponse(PromptAndCollectUserInformationResponse arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onPromptAndCollectUserInformationResponse: {}", arg0);
                return;
            }
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier())
                    .incPromptAndCollectUserInformationResultCount();
            try {
                call.getCapModule().onPromptAndCollectUserInformationResponse(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onSpecializedResourceReportRequest(SpecializedResourceReportRequest arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onSpecializedResourceReportRequest: {}", arg0);
                return;
            }
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier())
                    .incSpecializedResourceReportCount();
            try {
                call.getCapModule().onSpecializedResourceReportRequest(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onSplitLegResponse(SplitLegResponse arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onSplitLegResponse: {}", arg0);
                return;
            }
            ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier())
                    .incSplitLegResponseCount();
            try {
                call.getCapModule().onSplitLegResponse(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onCAPMessage(CAPMessage arg0) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getCAPDialog().getLocalDialogId())) {
            if (call == null) {
                // don't warn, this is normal for TC_BEGIN components
                LOG.trace("Could not find call for onCAPMessage: {}", arg0);
                return;
            }
            try {
                call.getCapModule().onCAPMessage(arg0);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onErrorComponent(CAPDialog arg0, Long arg1, CAPErrorMessage arg2) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onErrorComponent: {}", arg0);
                return;
            }
            try {
                call.getCapModule().onErrorComponent(arg0, arg1, arg2);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onInvokeTimeout(CAPDialog arg0, Long arg1) {
        try (ContextLayer cl = CallContext.with(callStore)) {
            try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getLocalDialogId())) {
                if (call == null) {
                    LOG.warn("Could not find call for onInvokeTimeout: {}", arg0);
                    return;
                }
                try {
                    call.getCapModule().onInvokeTimeout(arg0, arg1);
                } catch (RuntimeException ex) {
                    LOG.error("Exception on CAMEL processing", ex);
                    throw ex;
                }
            }
        }
    }

    @Override
    public void onRejectComponent(CAPDialog arg0, Long arg1, Problem arg2, boolean arg3) {
        try (CAPCSCall call = (CAPCSCall) callStore.getCallByLocalTcapTrId(arg0.getLocalDialogId())) {
            if (call == null) {
                LOG.warn("Could not find call for onRejectComponent: {}", arg0);
                return;
            }
            try {
                call.getCapModule().onRejectComponent(arg0, arg1, arg2, arg3);
            } catch (RuntimeException ex) {
                LOG.error("Exception on CAMEL processing", ex);
                throw ex;
            }
        }
    }

    @Override
    public void onCollectInformationRequest(CollectInformationRequest ind) {
        LOG.debug("Called onCollectInformationRequest {}", ind);
	}

}
