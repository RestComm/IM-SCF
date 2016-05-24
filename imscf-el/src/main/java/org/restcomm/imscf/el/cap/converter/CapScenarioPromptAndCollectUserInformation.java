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
package org.restcomm.imscf.el.cap.converter;

import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.CapErrorCodeMapper;
import org.restcomm.imscf.el.cap.call.CAPCSCall;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.ReasonHeader;
import org.restcomm.imscf.el.sip.WaitForFinalAnswerScenario;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;

import java.io.IOException;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorCode;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationResponse;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CAP scenario for PromptAndCollect. */
public final class CapScenarioPromptAndCollectUserInformation implements
        org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesMrf.CapScenarioPromptAndCollectUserInformation {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioPromptAndCollectUserInformation.class);
    private Long invokeId;
    private CAPCSCall call;

    public static CapScenarioPromptAndCollectUserInformation start(CAPCSCall call,
            PromptAndCollectUserInformationRequest promptAndCollect) throws CAPException {
        Long invokeId = call.getCapDialog().addPromptAndCollectUserInformationRequest(
                promptAndCollect.getCollectedInfo(), promptAndCollect.getDisconnectFromIPForbidden(),
                promptAndCollect.getInformationToSend(), promptAndCollect.getExtensions(),
                promptAndCollect.getCallSegmentID(), promptAndCollect.getRequestAnnouncementStartedNotification());
        // send this immediately
        call.getCapDialog().send();
        return new CapScenarioPromptAndCollectUserInformation(invokeId, call);

    }

    private CapScenarioPromptAndCollectUserInformation(Long invokeId, CAPCSCall call) {
        this.invokeId = invokeId;
        this.call = call;

    }

    @Override
    public Long getInvokeId() {
        return invokeId;
    }

    @Override
    public void onFailureIndicated(CAPErrorMessage error, Problem problem) {
        if (error.getErrorCode() == CAPErrorCode.improperCallerResponse
                || error.getErrorCode() == CAPErrorCode.unavailableResource) {
            LOG.debug("CAP PromptAndCollect failed, error {}, problem {}", error, problem);
        } else {
            LOG.warn("CAP PromptAndCollect failed, error {}, problem {}", error, problem);
        }

        // clean-up of unnecessary scenarios
        boolean automaticDisconnection = !((CapSipCsCall) call).getSipScenarios().stream()
                .anyMatch(s -> s instanceof SipScenarioMrfDFC);
        if (automaticDisconnection) {
            /** Automatic disconnection is in effect (disconnectFromIPForbidden FALSE) */
            /** In this case, we have to remove the PA and PACUI scenarios */
            ((CapSipCsCall) call).getSipScenarios().removeIf(
                    ss -> ss instanceof SipScenarioPlayAnnouncement
                            || ss instanceof SipScenarioPromptAndCollectUserInformation);
            call.getCapIncomingRequestScenarios().removeIf(caps -> caps instanceof CapScenarioIncomingERBCSMMrf);
        }

        SipSession sip = SipUtil.findSipSessionForLegID((CapSipCsCall) call, "mrf");

        if (sip == null || SipUtil.isUaTerminated(sip)) {
            // e.g. race condition where a disconnect arrives and causes the PACUI to be rejected
            LOG.debug("MRF leg already terminated, cannot send error in INFO message");
            return;
        }

        SipServletRequest msg = sip.createRequest("INFO");

        ReasonHeader rh = new ReasonHeader("SIP", error.getErrorCode().intValue(),
                CapErrorCodeMapper.errorCodeAsString(error.getErrorCode().intValue()));
        rh.insertAsHeader(msg);

        ((CapSipCsCall) call).getSipScenarios().add(
                WaitForFinalAnswerScenario.start("Wait for PACUI error INFO OK", msg));
        ((CapSipCsCall) call).queueMessage(msg);

        // TODO: Do we need to request a BYE on the MRF leg, just like in the case of SRR??
    }

    @Override
    public void onSuccess(PromptAndCollectUserInformationResponse response) {
        CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (CapSipCsCall call = (CapSipCsCall) cs.getCapCall(response.getCAPDialog().getLocalDialogId())) {
            LOG.debug("promptAndcollect result arrived: {}", response);

            // clean-up of unnecessary scenarios
            boolean automaticDisconnection = !call.getSipScenarios().stream()
                    .anyMatch(s -> s instanceof SipScenarioMrfDFC);
            if (automaticDisconnection) {
                /** Automatic disconnection is in effect (disconnectFromIPForbidden FALSE) */
                /** In this case, we have to remove the PA and PACUI scenarios */
                call.getSipScenarios().removeIf(
                        ss -> ss instanceof SipScenarioPlayAnnouncement
                                || ss instanceof SipScenarioPromptAndCollectUserInformation);
                call.getCapIncomingRequestScenarios().removeIf(caps -> caps instanceof CapScenarioIncomingERBCSMMrf);
            }

            SipSession sip = SipUtil.findSipSessionForLegID(call, "mrf");
            SipServletRequest msg = sip.createRequest("INFO");

            try {
                msg.setContent(Jss7ToXml.encode(response, "promptAndCollectUserInformationResult"),
                        SipConstants.CONTENTTYPE_CAP_XML_STRING);

                call.getSipScenarios().add(WaitForFinalAnswerScenario.start("Wait for PACUI result INFO OK", msg));
                call.queueMessage(msg);

                if (automaticDisconnection) {
                    // The IMSCF should initiate a BYE request on the MRF leg, when automatic disconnection is requested
                    SipServletRequest msgBye = sip.createRequest("BYE");
                    call.getSipScenarios().add(WaitForFinalAnswerScenario.start("Wait for MRF BYE OK", msgBye));
                    call.queueMessage(msgBye);
                }

            } catch (IOException e) {
                LOG.warn("Couldn't construct/send message to AS", e);
            }

        }

    }

    @Override
    public void onFailureTimeout() {
        LOG.warn("CAP PromptAndCollect operation failed.");
    }

}
