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

import org.restcomm.imscf.el.cap.CAPModuleBase;
import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CallSegment.CallSegmentState;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipDetectorLegManipulation;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.ScenarioFinishingSipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for detecting and handling a leg manipulation operation from the AS. */
public final class SipScenarioLegManipulation extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioLegManipulation.class);

    public static SipScenarioLegManipulation start() {
        return new SipScenarioLegManipulation(new SipDetectorLegManipulation());
    }

    // scenario parameter is not used in lambda
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private SipScenarioLegManipulation(SipDetectorLegManipulation detector) {
        super("Waiting for leg manipulation SDP", detector, (scenario, reInvite) -> {
            try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getSipCall(reInvite)) {
                SipServletRequest invite = (SipServletRequest) reInvite;

                // check that the CAP dialog is usable beforehand instead of failing later
                if (!CapUtil.canSendPrimitives(call.getCapDialog())) {
                    SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                            ((SipServletRequest) reInvite).createResponse(SipServletResponse.SC_BAD_REQUEST),
                            "Received leg manipulation request in dialog state: " + call.getCapDialog().getState()),
                            "Failed to send error response to leg manipulation reINVITE");
                    return;
                }

                String reinviteSdp = detector.getSdp();
                String legID = SipSessionAttributes.LEG_ID.get(reInvite.getSession(), String.class);
                CallSegment cs = call.getCallSegmentAssociation().getCallSegmentOfLeg(
                        SipUtil.networkLegIdFromSdpId(legID));

                if (cs == null) {
                    /*
                     * e.g. race condition: a late reINVITE arrives before the ERBCSM notification gets to the AS. A BYE
                     * should already be underway on the same leg (maybe just queued but not sent already), so we send a
                     * 491 response. (note: 481 call leg does not exist cannot be sent, as that would terminate the
                     * session prematurely)
                     */
                    SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                            ((SipServletRequest) reInvite).createResponse(SipServletResponse.SC_REQUEST_PENDING),
                            "Call segment for leg is missing"),
                            "Failed to send error response to leg manipulation reINVITE");
                    return;
                }

                // 0. if SDP is a "no connection", perform a disconnectForwardConnection
                // 1. if the list of legs is the same as those already in the call segment, do nothing
                // 2. if a leg is present in the CS, but not in the SDP, split it
                // 3. if a leg is present in the SDP, but not in the CS, move it in

                if (SipUtil.isNoConnectionSdp(reinviteSdp)) {
                    LOG.debug("No connection SDP, checking if disconnectForwardConnection is necessary");

                    SipSession mrfSession = SipUtil.findMrfSessionForCallSegment(call, cs.getId());
                    if (mrfSession == null) {
                        LOG.debug("No MRF in this CallSegment");
                    } else {
                        DisconnectForwardConnectionUtil.sendDisconnectForwardConnection(call, cs.getId());
                        cs.disconnectForwardConnection();
                    }
                    send200OK(call, invite);
                    return;
                }
                Set<String> sdpLegIDs = new HashSet<>(SipUtil.getLegIDListFromSdp(reinviteSdp));

                // same leg is always implicitly contained
                sdpLegIDs.add(legID);

                // TODO: optimize these set creations if possible...
                Set<String> currentLegs = cs.getLegs().stream().map(SipUtil::sdpIdFromNetworkLegId)
                        .collect(Collectors.toSet());
                Set<String> toSplit = new HashSet<>(currentLegs);
                toSplit.removeAll(sdpLegIDs);
                Set<String> toMove = new HashSet<>(sdpLegIDs);
                toMove.removeAll(currentLegs);

                // split/move can only be applied to CS-1, so check that we are in it

                if (!toSplit.isEmpty() || !toMove.isEmpty()) {
                    if (cs.getId() != 1) {
                        LOG.warn("SplitLeg/MoveLeg can only be applied to CS-1! Current leg is in {}", cs.getName());
                        SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                invite.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                "Leg manipulation call segment error, leg is not in CS-1"),
                                "Failed to send 200 OK to reINVITE");
                        return;
                    }

                    boolean containsMrf = toMove.remove("mrf");
                    // must be CS-1, otherwise split/move wouldn't have been sent
                    Runnable onSuccess = createSuccessHandler(call, invite, containsMrf, sdpLegIDs, 1, reinviteSdp);

                    /*
                     * Check again after remove whether it is only the MRF that is being connected without actual
                     * split/move.
                     */
                    if (toSplit.isEmpty() && toMove.isEmpty()) {
                        LOG.debug("No actual split/move required");
                        onSuccess.run();
                        return;
                    } else {
                        /* split some legs */
                        for (String id : toSplit) {
                            LOG.debug("Splitting {} from {}", id, cs.getName());
                            try {
                                call.getCapOutgoingRequestScenarios().add(
                                        CapScenarioSplitLeg.start(call, SipUtil.networkLegIdFromSdpId(id), call
                                                .getCallSegmentAssociation().getLowestAvailableCSID(), null, invite,
                                                onSuccess));
                            } catch (Exception e) {
                                LOG.warn("Error performing splitLeg: {}", e.getMessage(), e);
                                SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                        invite.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                        "Failed to send splitLeg"),
                                        "Failed to send error response to splitLeg reINVITE");
                                return;
                            }
                        }

                        /* move some legs */
                        for (String id : toMove) {
                            if (id.startsWith("L")) { /* actual leg */
                                LOG.debug("Moving {} to {}", id, cs.getName());
                                try {
                                    call.getCapOutgoingRequestScenarios().add(
                                            CapScenarioMoveLeg.start(call, SipUtil.networkLegIdFromSdpId(id), null,
                                                    invite, onSuccess));
                                } catch (Exception e) {
                                    LOG.warn("Error performing moveLeg: {}", e.getMessage(), e);
                                    SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                            invite.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                            "Failed to send moveLeg"),
                                            "Failed to send error response to moveLeg reINVITE");
                                    return;
                                }
                            } else {
                                LOG.warn("Unrecognized leg id received in SDP: {}", id);
                            }
                        }
                    }

                } else {
                    LOG.debug("reINVITE SDP doesn't change call segment.");

                    if (cs.getState() == CallSegmentState.WAITING_FOR_INSTRUCTIONS) {
                        LOG.debug("reINVITE indicates CAP continue request from AS, sending in CS-{}", cs.getId());

                        /*
                         * TODO: if this is in fact a reconnect continue after a DFC in a midcall announcement, we might
                         * have to send continueWithArgument instead due to an MSS error. But how to determine if this
                         * is the case?
                         */

                        try {
                            ContinueUtil.continueCall(call.getCapDialog(), false);
                            cs.continueCS();
                        } catch (Exception e) {
                            LOG.warn("Failed to send CAP continue in CS-{}", cs.getId(), e);
                            SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                    invite.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                    "Failed to send CAP continue"), "Failed to send error response to reINVITE");
                            return;
                        }
                    }

                    send200OK(call, invite);
                }

            }
        });
    }

    private static Runnable createSuccessHandler(CapSipCsCall call, SipServletRequest invite, boolean containsMrf,
            Set<String> sdpLegIDs, int csId, String reinviteSdp) {
        return () -> {
            if (containsMrf) { /* true if mrf was included in the SDP */
                CallSegment cs = call.getCallSegmentAssociation().getCallSegment(csId); // could be null at this point

                /*
                 * Check that
                 *
                 * 1) MRF SIP leg still exists, and
                 *
                 * 2) target leg and call segment still exists
                 *
                 * otherwise it's pointless to perform connectToResource. This can happen if a call party disconnects
                 * during the splitLeg/moveLeg operation that does not prevent the operation from succeeding, or if the
                 * AS released the MRF SIP dialog for some reason.
                 */
                boolean sipMrfMissing = Optional.ofNullable(SipUtil.findSipSessionForLegID(call, "mrf"))
                        .map(SipUtil::isUaTerminated).orElse(true);
                boolean anyLegsMissing = true;
                if (cs != null) {
                    Set<String> requiredNetworkLegsAfterSuccess = new HashSet<>(sdpLegIDs);
                    requiredNetworkLegsAfterSuccess.remove("mrf");
                    for (String s : requiredNetworkLegsAfterSuccess) {
                        if (!cs.containsLeg(SipUtil.networkLegIdFromSdpId(s)))
                            anyLegsMissing = true;
                    }
                    anyLegsMissing = false;
                }

                if (sipMrfMissing || anyLegsMissing) {
                    LOG.debug(
                            "Sending error response to reINVITE and not performing connectToResource after splitLeg/moveLeg success. MRF SIP leg missing: {}, CS-1 network leg(s) missing: {}.",
                            sipMrfMissing, anyLegsMissing);
                    SipServletResponse resp;
                    try {
                        resp = invite.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
                    } catch (Exception e) {
                        LOG.debug("Failed to create error response to INVITE!", e);
                        return;
                    }
                    SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(resp,
                            "Cannot send connectToResource: missing SIP MRF leg or CS-1 network leg(s)"),
                            "Failed to send error response to reINVITE");
                    return;
                }

                // all seems fine and cs is definitely not null

                LOG.debug("MRF included in SDP, performing connectToResource on {}", cs.getName());

                /*
                 * Only send RRBCSM for followon announcement, not midcall.
                 *
                 * followon: single call segment with one leg
                 *
                 * midcall: multiple call segments, or one with multiple legs
                 *
                 * TODO: we should actually store which EDPs are armed for a leg, and send RRBCSM for the missing ones.
                 */
                List<BCSMEvent> edpList = null;
                if (call.getCallSegmentAssociation().getCallSegmentCount() == 1) {
                    Set<Integer> legs = cs.getLegs();
                    edpList = legs.size() == 1 ? ((CAPModuleBase) call.getCapModule()).getEDPsForMrf(call, legs) : null;
                }
                ConnectToResourceUtil.connectToMrf(call, cs.getId(), edpList, reinviteSdp);
            }

            send200OK(call, invite);
        };
    }

    private static void send200OK(SIPCall call, SipServletRequest invite) {
        String legId = SipSessionAttributes.LEG_ID.get(invite.getSession(), String.class);
        final int cseq = SipUtil.getCSeq(invite);
        SipUtil.sendOrWarn(invite.createResponse(SipServletResponse.SC_OK), "Failed to send 200 OK to reINVITE");
        Scenario ack = new Scenario("Waiting for " + legId + " reINVITE ACK (seq " + cseq + ")", msg -> {
            return msg instanceof SipServletRequest && "ACK".equals(msg.getMethod())
                    && msg.getSession().equals(invite.getSession()) && cseq == SipUtil.getCSeq(msg);
        }, ScenarioFinishingSipMessageHandler.SHARED_INSTANCE);
        call.getSipScenarios().add(ack);
    }
}
