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
package org.restcomm.imscf.el.cap.converter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CallSegment.CallSegmentState;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesCsCall.CapScenarioEventReportBCSM;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.MultipartBuilder;

/** Handles the incoming ERBCSM events. If there is an MRF leg in the callSegment, then sends a BYE on the MRF leg. The content will contain the SDP and ERBCSM. */
public final class CapScenarioIncomingERBCSMMrf implements CapScenarioEventReportBCSM {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioIncomingERBCSMMrf.class);
    private boolean finish = false;

    @Override
    public boolean isFinished() {
        // handles only one erbcsm event in connection with mrf
        return finish;
    }

    @Override
    public void onRequest(EventReportBCSMRequest erbcsm) {
        CallStore store = CallContext.getCallStore();
        try (CapSipCsCall call = (CapSipCsCall) store.getCapCall(erbcsm.getCAPDialog().getLocalDialogId())) {
            LOG.debug("eventReportBCSMRequest arrived: {}", erbcsm);
            int legID = getLegID(erbcsm);
            CallSegment cs = call.getCallSegmentAssociation().getCallSegmentOfLeg(legID);

            SipSession sipMrf = SipUtil.findMrfSessionForCallSegment(call, cs.getId());
            if (sipMrf == null) {
                LOG.debug("No MRF present in {}, ignoring ERBCSM", cs.getName());
                // don't set finish flag, as the ERBCSM was from a different CS
                return;
            }

            switch (erbcsm.getEventTypeBCSM()) {
            case oAbandon:
            case tAbandon:
            case oDisconnect:
            case tDisconnect:
                // handled below
                break;
            default:
                LOG.warn("Event not handled in this scenario: {}", erbcsm.getEventTypeBCSM());
                return;
            }

            int csLegCount = cs.getLegCount();

            if (csLegCount == 1) {
                int csCount = call.getCallSegmentAssociation().getCallSegmentCount();
                if (csCount == 1) {
                    LOG.debug("Single leg announcement, this is the only CallSegment. Sending BYE on MRF leg and CAP continue with TC_END.");
                    finish = true;
                    sendMrfBye(call, sipMrf, erbcsm);
                    try {
                        ContinueUtil.continueCall(call.getCapDialog(), true);
                    } catch (CAPException e) {
                        LOG.warn("Failed to send CAP continue for call {}: {}", call, e.getMessage(), e);
                    }
                } else { // csCount > 1; cannot be 0 as the current leg must be in one...
                    LOG.debug(
                            "Midcall announcement for L{}, there are {} CallSegments. Sending BYE on MRF leg and CAP continueWithArgument({}).",
                            legID, csCount, cs.getName());
                    finish = true;
                    sendMrfBye(call, sipMrf, erbcsm);

                    // If the CTR was sent out already, but not the PA/PACUI, then sending CWA results in UCS error.
                    // We detect this by checking for active CAP scenarios.
                    // note: this does not support multiple parallel announcements in the call, but we don't support
                    // them anyway due to the exclusive usage of the "mrf" leg ID.
                    boolean connectedButNoInteraction = cs.getState() == CallSegmentState.WAITING_FOR_END_OF_USER_INTERACTION
                            && call.getCapIncomingRequestScenarios().stream()
                                    .noneMatch(s -> s instanceof CapScenarioSpecializedResourceReport)
                            && call.getCapOutgoingRequestScenarios()
                                    .stream()
                                    .noneMatch(
                                            s -> s instanceof CapScenarioPlayAnnouncement
                                                    || s instanceof CapScenarioPromptAndCollectUserInformation);
                    if (connectedButNoInteraction) {
                        LOG.debug("Leg disconnect arrived before PA/PACUI. Sending DFCwA before CWA.");
                        // clean up unnecessary scenarios
                        call.getSipScenarios().removeIf(
                                ss -> ss instanceof SipScenarioPlayAnnouncement
                                        || ss instanceof SipScenarioPromptAndCollectUserInformation);
                        DisconnectForwardConnectionUtil.sendDisconnectForwardConnection(call, cs.getId());
                        cs.disconnectForwardConnection();
                    }

                    try {
                        call.getCapOutgoingRequestScenarios().add(
                                CapScenarioContinueWithArgument.start(call, cs.getId()));
                    } catch (CAPException e) {
                        LOG.warn("Failed to send CAP continueWithArgument for call {}: {}", call, e.getMessage(), e);
                    }
                }
            } else { // csLegCount > 1 (cannot be 0, as getCallSegmentOfLeg wouldn't have found it)
                // midcall announcement for both(/all) legs
                LOG.debug("Midcall announcement for {} legs. Waiting for AS decision.", csLegCount);
                // B2BUA in AS should:
                // - forward BYE to the other leg to request continue
                // - send BYE to the other leg to request (DFC+)releaseCall
                // - send INFO on other leg with CWA(this leg) to request continueWithArgument and keep the MRF playing
                // to the other leg(s). TODO: is this OK ?
            }

        }
    }

    private void sendMrfBye(CapSipCsCall call, SipSession sipMrf, EventReportBCSMRequest erbcsm) {
        SipServletRequest msg = sipMrf.createRequest("BYE");
        try {
            setContent(msg, Jss7ToXml.encode(erbcsm, "eventReportBCSM"));
        } catch (IOException | MessagingException e) {
            LOG.warn("Failed to insert eventReport body into MRF BYE request", e);
            // continue, better to send empty message than none
        }
        call.queueMessage(msg);
    }

    private int getLegID(EventReportBCSMRequest erbcsm) {
        return Optional.ofNullable(erbcsm.getLegID()).map(l -> l.getReceivingSideID().getCode()).orElseGet(() -> {
            /* if legID is not present, infer from event type */
            switch (erbcsm.getEventTypeBCSM()) {
            case oAbandon:
            case tAbandon:
                return 1;
            default:
                /* busy, rsf, noanswer, ringing, answer. */
                /* [ot]Disconnect should always contain legID */
                return 2;
            }
        });
    }

    private void setContent(SipServletMessage msg, String xmlContent) throws MessagingException,
            UnsupportedEncodingException {
        // create multipart content with ERBCSM and SDP
        MimeMultipart mm = new MultipartBuilder().addPartBody(SipConstants.CONTENTTYPE_CAP_XML_STRING, xmlContent)
                .addPartBody(SipConstants.CONTENTTYPE_SDP_STRING, SipUtil.createSdpForMrfLegs(msg.getSession()))
                .getResult();
        msg.setContent(mm, mm.getContentType());
    }

}
