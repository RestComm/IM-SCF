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

import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.call.CAPCSCall.CAPState;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.ScenarioFinishingSipMessageHandler;
import org.restcomm.imscf.el.sip.SipMessageBodyUtil;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.Jss7ToXml.XmlDecodeException;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.URI;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ConnectRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ContinueWithArgumentRequest;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ConnectRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ContinueWithArgumentRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.InitialDPRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for detecting and handling an initial stateless connect/continue response from the AS. */
public final class SipScenarioInitialStatefulCallHandling extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioInitialStatefulCallHandling.class);

    public static SipScenarioInitialStatefulCallHandling start(SipServletRequest invite, List<BCSMEvent> defaultEvents) {
        return new SipScenarioInitialStatefulCallHandling(invite, defaultEvents);
    }

    private SipScenarioInitialStatefulCallHandling(SipServletRequest outgoingInvite, List<BCSMEvent> defaultEvents) {
        super("Waiting for stateful call handling", new SipForwardedMessageDetector(outgoingInvite),
                new SipMessageHandler() {
                    @Override
                    public void handleMessage(Scenario scenario, SipServletMessage incomingInvite) {
                        try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getSipCall(incomingInvite)) {
                            String legID = SipSessionAttributes.LEG_ID.get(incomingInvite.getSession(), String.class);
                            SipServletRequest inviteReq = (SipServletRequest) incomingInvite;

                            // check that the CAP dialog is usable beforehand instead of failing later
                            if (!CapUtil.canSendPrimitives(call.getCapDialog())) {
                                SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                        inviteReq.createResponse(SipServletResponse.SC_BAD_REQUEST),
                                        "Cannot connect call in dialog state: " + call.getCapDialog().getState()),
                                        "Failed to send error response to connect/continue INVITE");
                                return;
                            }

                            CallSegment cs = call.getCallSegmentAssociation().getCallSegmentOfLeg(1); // L1
                            if (cs == null) {
                                LOG.warn(
                                        "Stateful connect/continue requested, but L1 is not present in any call segment! {}",
                                        call.getCallSegmentAssociation());
                                SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                        inviteReq.createResponse(SipServletResponse.SC_BAD_REQUEST),
                                        "Invalid call state for stateful connect/continue"),
                                        "Failed to send error response to INVITE in {}", call);
                                return;
                            }
                            if (cs.getLegCount() != 1) {
                                LOG.warn(
                                        "Stateful connect/continue requested, but L1 is already connected to another leg in {}",
                                        cs);
                                SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                        inviteReq.createResponse(SipServletResponse.SC_BAD_REQUEST),
                                        "Invalid call state for stateful connect/continue"),
                                        "Failed to send error response to INVITE in {}", call);
                                return;
                            }

                            // TODO: check if L2 is already present, but split into some other CS? This shouldn't happen
                            // probably...

                            if (legID != null) {
                                LOG.warn("Call state error, SIP INVITE received for leg {} in {}", legID, call);
                                SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                        inviteReq.createResponse(SipServletResponse.SC_BAD_REQUEST),
                                        "Invalid call state for stateful connect/continue"),
                                        "Failed to send error response to INVITE in {}", call);
                                return;
                            }

                            LOG.trace("Trying to decode CAP XML content...");
                            ConnectRequest con = null;
                            ContinueWithArgumentRequest cwa = null;

                            List<Object> capBodyParts = SipMessageBodyUtil.findContent(incomingInvite,
                                    SipConstants.CONTENTTYPE_CAP_XML);
                            for (Object o : capBodyParts) {
                                try {
                                    Object arg = Jss7ToXml.decodeAnyOf(o, InitialDPRequestImpl.class,
                                            ConnectRequestImpl.class, ContinueWithArgumentRequestImpl.class);
                                    if (arg instanceof InitialDPRequestImpl) {
                                        continue; // should be the same as sent out, detector checked this already
                                    } else if (arg instanceof ConnectRequest) {
                                        con = (ConnectRequest) arg;
                                        break;
                                    } else if (arg instanceof ContinueWithArgumentRequest) {
                                        cwa = (ContinueWithArgumentRequest) arg;
                                        break;
                                    }
                                } catch (XmlDecodeException e) {
                                    LOG.warn("Invalid INVITE XML body received", e);
                                    SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                            inviteReq.createResponse(SipServletResponse.SC_BAD_REQUEST),
                                            "Invalid XML body"), "Failed to send error response to INVITE in {}", call);
                                    return;
                                }
                            }
                            LOG.trace("Body decoded");

                            LOG.debug("Starting stateful call flow.");
                            SipSessionAttributes.LEG_ID.set(incomingInvite.getSession(), "L2");
                            SipSessionAttributes.INITIAL_REQUEST.set(incomingInvite.getSession(), incomingInvite);
                            CAPDialogCircuitSwitchedCall cap = call.getCapDialog();

                            try {
                                LOG.debug("Sending RRBCSM");
                                call.getCapOutgoingRequestScenarios().add(CapScenarioRRBCSM.start(call, defaultEvents));
                            } catch (CAPException e) {
                                LOG.warn("Failed to create RequestReportBCSM", e);
                                // TODO allow the call to continue without RRBCSM? check for already armed EDPs?
                            }

                            try {
                                if (con != null) {
                                    LOG.debug("CON XML arrived, sending connect");
                                    ConnectUtil.connectCall(cap, con, false);
                                    LOG.trace("Initial stateful connect finished");
                                } else if (cwa != null) {
                                    LOG.debug("CWA XML arrived, sending continueWithArgument");
                                    ContinueUtil.continueCall(cap, cwa, false);
                                    LOG.trace("Initial stateful continueWithArgument finished");
                                } else {
                                    // Only send continue for the first stateful connect attempt if the number has
                                    // not changed. For follow-on calls or changed B number, send connect.
                                    // check for modified B number in Request-URI
                                    URI originalBNumber = outgoingInvite.getRequestURI();
                                    URI newBNumber = ((SipServletRequest) incomingInvite).getRequestURI();
                                    if (call.getCsCapState() == CAPState.FOLLOWON_CALL) {
                                        LOG.debug("Received follow-on call request, performing stateful connect");
                                        ConnectUtil.connectCall(cap, newBNumber, false);
                                        LOG.trace("Follow-on stateful connect finished");
                                    } else if (!newBNumber.equals(originalBNumber)) {
                                        LOG.debug("Received different Request-URI, performing stateful connect");
                                        ConnectUtil.connectCall(cap, newBNumber, false);
                                        LOG.trace("Initial stateful connect finished");
                                    } else {
                                        LOG.debug("Same Request-URI received, performing stateful continue");
                                        ContinueUtil.continueCall(cap, false);
                                        LOG.trace("Initial stateful continue finished");
                                    }
                                }
                                // in all cases: set leg2 and go to monitoring
                                call.getCallSegmentAssociation().getOnlyCallSegment().connect();
                            } catch (CAPException e) {
                                LOG.warn("Failed to connect/continue call", e);
                                SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                        inviteReq.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                        "Failed to perform connect/continue operation"),
                                        "Failed to send error response to INVITE in {}", call);
                                return;
                            }

                            // any subsequent connect/continue request should be treated as follow-on
                            call.setCsCapState(CapSipCsCall.CAPState.FOLLOWON_CALL);

                            // now we should listen for incoming ERBCSM requests
                            if (!call.getCapIncomingRequestScenarios().stream()
                                    .anyMatch(s -> s instanceof CapScenarioIncomingERBCSM)) {
                                call.getCapIncomingRequestScenarios().add(new CapScenarioIncomingERBCSM());
                            }

                            // TODO make this handshake optional/configurable?
                            sendHandshake(call, (SipServletRequest) incomingInvite);

                        }
                    }
                });
    }

    private static boolean sendHandshake(CapSipCsCall call, SipServletRequest incomingInvite) {
        SipServletResponse handshakeResponse = incomingInvite.createResponse(SipServletResponse.SC_SESSION_PROGRESS);
        try {
            handshakeResponse.setContent(SipUtil.createSdpForLegs(handshakeResponse.getSession()),
                    SipConstants.CONTENTTYPE_SDP_STRING);
        } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to set SDP content", e);
            return false;
        }
        String log = "Failed to send handshake 183 on L2";
        if (SipUtil.supports100Rel(incomingInvite) && SipUtil.sendReliablyOrWarn(handshakeResponse, log)) {
            call.getSipScenarios().add(
                    new Scenario("200 OK for PRACK L2", m -> m instanceof SipServletRequest
                            && "PRACK".equals(m.getMethod()) && m.getSession().equals(incomingInvite.getSession()), (
                            scen, message) -> {
                        scen.setFinished();
                        SipUtil.sendOrWarn(((SipServletRequest) message).createResponse(SipServletResponse.SC_OK),
                                "Failed to send 200 OK for PRACK");
                    }));
            return true;
        } else if (SipUtil.sendOrWarn(handshakeResponse, log)) {
            call.getSipScenarios().add(
                    new Scenario("Forwarded handshake 183", new SipForwardedMessageDetector(handshakeResponse),
                            ScenarioFinishingSipMessageHandler.SHARED_INSTANCE));
            return true;
        }
        return false;
    }
}
