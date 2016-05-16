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
import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.call.CallSegment.CallSegmentState;
import org.restcomm.imscf.el.cap.sip.SipPromptAndCollectUserInformationDetector;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.Jss7ToXml.XmlDecodeException;

import java.io.IOException;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationRequest;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.PromptAndCollectUserInformationRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scenario for PromptAndCollect. */
public final class SipScenarioPromptAndCollectUserInformation extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioPromptAndCollectUserInformation.class);

    public static SipScenarioPromptAndCollectUserInformation start() {
        return new SipScenarioPromptAndCollectUserInformation();
    }

    private SipScenarioPromptAndCollectUserInformation() {
        super("Waiting for PromptAndCollect", new SipPromptAndCollectUserInformationDetector(), (scenario, msg) -> {
            CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
            try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {
                SipServletRequest req = (SipServletRequest) msg;

                // check that the CAP dialog is usable beforehand instead of failing later
                if (!CapUtil.canSendPrimitives(call.getCapDialog())) {
                    SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                            req.createResponse(SipServletResponse.SC_BAD_REQUEST),
                            "Cannot send PACUI in dialog state: " + call.getCapDialog().getState()),
                            "Failed to send error response to PACUI INFO");
                    return;
                }

                PromptAndCollectUserInformationRequest pacuiReq;
                byte[] body;
                try {
                    body = req.getRawContent();
                } catch (IOException e) {
                    LOG.warn("Failed to read PromptAndCollect INFO message content", e);
                    SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                            "Failed to send error response to PromptAndCollect INFO");
                    return;
                }
                try {
                    pacuiReq = Jss7ToXml.decode(body, PromptAndCollectUserInformationRequestImpl.class);
                } catch (XmlDecodeException e) {
                    LOG.warn("Invalid or missing PromptAndCollect XML in the message body.", e);
                    SipServletResponse resp = req.createResponse(SipServletResponse.SC_BAD_REQUEST);
                    SipUtil.createAndSetWarningHeader(resp,
                            "Invalid or missing PromptAndCollect XML in the message body.");
                    SipUtil.sendOrWarn(resp, "Failed to send error response to PlayAnnouncement INFO");
                    return;
                }

                Integer csid = pacuiReq.getCallSegmentID();
                if (csid != null) {
                    CallSegment callseg = call.getCallSegmentAssociation().getCallSegment(csid);
                    if (callseg == null || callseg.getState() != CallSegmentState.WAITING_FOR_END_OF_USER_INTERACTION) {
                        LOG.debug("PACUI request arrived for bad call segment, sending back error response");
                        SipServletResponse resp = req.createResponse(SipServletResponse.SC_BAD_REQUEST);
                        SipUtil.createAndSetWarningHeader(resp,
                                "Invalid call segment: CS-" + pacuiReq.getCallSegmentID());
                        SipUtil.sendOrWarn(resp, "Failed to send error response to PACUI INFO");
                        return;
                    }
                }

                try {
                    if (pacuiReq.getDisconnectFromIPForbidden() != null) {
                        if (pacuiReq.getDisconnectFromIPForbidden()) {
                            /** Manual disconnection */
                            /** Here, we should add the DFC scenario to the sip scenarios again because it could have been removed */
                            if (!call.getSipScenarios().stream().anyMatch(s -> s instanceof SipScenarioMrfDFC)) {
                                call.getSipScenarios().add(SipScenarioMrfDFC.start());
                            }
                        } else {
                            /** Automatic disconnection - no need for the DFC scenario */
                            call.getSipScenarios().removeIf(ss -> ss instanceof SipScenarioMrfDFC);
                        }
                    }

                    call.getCapOutgoingRequestScenarios().add(
                            CapScenarioPromptAndCollectUserInformation.start(call, pacuiReq));

                    if (pacuiReq.getRequestAnnouncementStartedNotification() != null
                            && pacuiReq.getRequestAnnouncementStartedNotification()) {
                        call.getCapIncomingRequestScenarios().add(new CapScenarioSpecializedResourceReport());
                    }

                } catch (CAPException e) {
                    LOG.warn("Failed to send CAP PromptAndCollect", e);
                    SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                            "Failed to send error response to PromptAndCollect INFO");
                    return;
                }

                SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_OK),
                        "Failed to send success response to PromptAndCollect INFO");
            }
        });
    }

}
