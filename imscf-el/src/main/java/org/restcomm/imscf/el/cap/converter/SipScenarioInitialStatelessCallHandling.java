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

import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CAPCSCall.CAPState;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CallSegment.CallSegmentState;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.Jss7ToXml.XmlDecodeException;

import java.io.IOException;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ConnectRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ContinueWithArgumentRequest;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ConnectRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ContinueWithArgumentRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for detecting and handling an initial stateless connect/continue response from the AS. */
public final class SipScenarioInitialStatelessCallHandling extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioInitialStatelessCallHandling.class);

    public static SipScenarioInitialStatelessCallHandling start(SipServletRequest invite) {
        return new SipScenarioInitialStatelessCallHandling(invite);
    }

    private SipScenarioInitialStatelessCallHandling(SipServletRequest outgoingInvite) {
        super("Waiting for initial stateless connect/continue", new SipStatelessCallHandlingDetector(outgoingInvite),
                new SipMessageHandler() {
                    @Override
                    public void handleMessage(Scenario scenario, SipServletMessage msg) {
                        scenario.setFinished();
                        CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);

                        SipServletResponse resp3xx = (SipServletResponse) msg;
                        try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(resp3xx);) {

                            // check that the CAP dialog is usable beforehand instead of parsing the XML and then
                            // failing
                            if (!CapUtil.canSendPrimitives(call.getCapDialog())) {
                                LOG.warn("Cannot perform stateless call handling in dialog state: {}", call
                                        .getCapDialog().getState());
                                return;
                            }

                            LOG.debug("Received 302");
                            if (call.getSipState() == CapSipCsCall.SIPState.IDP_NOTIFIED
                                    && (call.getCsCapState() == CapSipCsCall.CAPState.IDP_ARRIVED || call
                                            .getCsCapState() == CAPState.FOLLOWON_CALL)) {
                                call.setSipState(CapSipCsCall.SIPState.STATELESS_CONTINUE_REQUESTED);

                                ConnectRequest con = null;
                                ContinueWithArgumentRequest cwa = null;

                                if (msg.getContentType() != null) {
                                    if (SipConstants.CONTENTTYPE_CAP_XML.match(msg.getContentType())) {
                                        byte[] xml = msg.getRawContent();
                                        LOG.trace("Trying to decode CAP XML content...");
                                        try {
                                            Object obj = Jss7ToXml.decodeAnyOf(xml, ConnectRequestImpl.class,
                                                    ContinueWithArgumentRequestImpl.class);
                                            if (obj instanceof ConnectRequest) {
                                                con = (ConnectRequest) obj;
                                            } else if (obj instanceof ContinueWithArgumentRequest) {
                                                cwa = (ContinueWithArgumentRequest) obj;
                                            }
                                            LOG.trace("Body decoded");
                                        } catch (XmlDecodeException e) {
                                            throw new ServletParseException("Invalid message content received.", e);
                                        }
                                    } else {
                                        throw new ServletParseException("Invalid Content-Type received.");
                                    }
                                }

                                CAPDialogCircuitSwitchedCall cd = call.getCapDialog();

                                // Handling DFC, if MRF is connected
                                String legID = SipSessionAttributes.LEG_ID.get(resp3xx.getSession(), String.class);
                                CallSegment callSegment = call.getCallSegmentAssociation().getCallSegmentOfLeg(
                                        SipUtil.networkLegIdFromSdpId(legID));

                                SipSession sipMrf = SipUtil.findMrfSessionForCallSegment(call, callSegment.getId());
                                boolean mrfInCS = sipMrf != null
                                        && callSegment.getState() == CallSegmentState.WAITING_FOR_END_OF_USER_INTERACTION;
                                if (mrfInCS) {
                                    LOG.debug("MRF in the call segment with WAITING_FOR_END_OF_USER_INTERACTION state. Adding DFC to the dialog and removing the corresponding scenario...");
                                    // MRF in the call segment - sending disconnectForwardConnection and shutting down
                                    // the DFC scenario
                                    cd.addDisconnectForwardConnectionRequest();
                                    callSegment.disconnectForwardConnection();
                                    call.getSipScenarios().removeIf(
                                            ss -> ss instanceof SipScenarioMrfDFC
                                                    || ss instanceof SipScenarioPlayAnnouncement
                                                    || ss instanceof SipScenarioPromptAndCollectUserInformation);
                                    call.getCapIncomingRequestScenarios().removeIf(
                                            s -> s instanceof CapScenarioIncomingERBCSMMrf);
                                }

                                if (con != null) {
                                    LOG.debug("CON XML arrived, sending connect");
                                    ConnectUtil.connectCall(cd, con, true);
                                    LOG.trace("Initial stateless connect finished");
                                } else if (cwa != null) {
                                    LOG.debug("CWA XML arrived, sending continueWithArgument");
                                    ContinueUtil.continueCall(cd, cwa, true);
                                    LOG.trace("Initial stateless continueWithArgument finished");
                                } else {
                                    // Only send continue for the first stateless connect attempt if the number has
                                    // not changed. For follow-on calls or changed B number, send connect.
                                    // check for modified B number in Contact header
                                    URI originalBNumber = outgoingInvite.getRequestURI();
                                    URI newBNumber = resp3xx.getAddressHeader("Contact").getURI();
                                    if (call.getCsCapState() == CAPState.FOLLOWON_CALL) {
                                        LOG.debug("Received follow-on call request, performing stateless connect");
                                        ConnectUtil.connectCall(cd, newBNumber, true);
                                        LOG.trace("Follow-on stateless connect finished");
                                    } else if (!newBNumber.equals(originalBNumber)) {
                                        LOG.debug("Received different URI in the Contact header, performing stateless connect");
                                        ConnectUtil.connectCall(cd, newBNumber, true);
                                        LOG.trace("Initial stateless connect finished");
                                    } else {
                                        LOG.debug("Same URI received, sending continue");
                                        ContinueUtil.continueCall(cd, true);
                                        LOG.trace("Initial stateless continue finished");
                                    }
                                }

                            } else {
                                LOG.warn("Call state error, SIP 302 received in wrong state for: {}", call);
                            }
                        } catch (CAPException | ServletParseException | IOException e) {
                            LOG.warn("Failed to send CAP continue", e);
                        }
                    }
                });
    }
}
