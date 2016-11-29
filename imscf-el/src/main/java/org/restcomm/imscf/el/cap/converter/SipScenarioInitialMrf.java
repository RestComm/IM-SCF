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

import org.restcomm.imscf.common.config.MediaResourceType;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.ScenarioFinishingSipMessageHandler;
import org.restcomm.imscf.el.sip.SipMessageDetector;
import org.restcomm.imscf.el.stack.CallContext;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSession.State;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario to handle connection requests to the MRF. */
public final class SipScenarioInitialMrf extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioInitialMrf.class);

    public static SipScenarioInitialMrf start() {
        return new SipScenarioInitialMrf();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private SipScenarioInitialMrf() {
        super(
                "Waiting for MRF Invite",
                new SipMessageDetector() {

                    @Override
                    public boolean accept(SipServletMessage msg) {
                        URI toUri = msg.getTo().getURI();
                        if (!(toUri instanceof SipURI)) {
                            return false;
                        }
                        String userPart = ((SipURI) msg.getTo().getURI()).getUser(); // nullcheck???
                        CallStore cs = CallContext.getCallStore();

                        try (SIPCall sipCall1 = cs.getSipCall(msg)) {
                            // here, later we have to account for the different types of mediaresources (mrf or mrf1 -
                            // the connectToResource is different for these two
                            return "INVITE".equals(msg.getMethod())
                                    && msg instanceof SipServletRequest
                                    && sipCall1.getSipModule().getImscfConfiguration().getMediaResources().stream()
                                            .anyMatch(mr -> mr.getAlias().equals(userPart));
                        }
                    }
                },
                (scenario, msg) -> {
                    CallStore cs = CallContext.getCallStore();
                    try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {

                        String userPart = ((SipURI) msg.getTo().getURI()).getUser(); // nullcheck???
                        Optional<MediaResourceType> mediaResource = call.getSipModule().getImscfConfiguration()
                                .getMediaResources().stream().filter(mr -> mr.getAlias().equals(userPart)).findFirst();
                        SipSessionAttributes.MRF_ALIAS.set(msg.getSession(), mediaResource.get().getAlias());

                        SipSessionAttributes.LEG_ID.set(msg.getSession(), "mrf");
                        SipServletRequest req = (SipServletRequest) msg;
                        SipServletResponse resp = req.createResponse(SipServletResponse.SC_OK);
                        try {
                            resp.setContent(SipUtil.createSdpForMrfLegs(resp.getSession()),
                                    SipConstants.CONTENTTYPE_SDP_STRING);
                        } catch (UnsupportedEncodingException e) {
                            LOG.warn("Failed to set SDP content", e);
                        }
                        SipUtil.sendOrWarn(resp, "Failed to send 200 OK for MRF Invite");

                        call.getSipScenarios().add(new Scenario("Waiting for MRF ACK", new SipMessageDetector() {
                            @Override
                            public boolean accept(SipServletMessage m) {
                                return m instanceof SipServletRequest && "ACK".equals(m.getMethod())
                                        && m.getSession().equals(msg.getSession());
                            }
                        }, ScenarioFinishingSipMessageHandler.SHARED_INSTANCE));

                        // for not yet confirmed L1 sessions, wait for 183 with MRF SDP
                        SipSession ssL1 = SipUtil.findSipSessionForLegID(call, "L1");
                        if (ssL1 != null && (ssL1.getState() == State.INITIAL || ssL1.getState() == State.EARLY)) {
                            // here we start a new scenario which triggers if a 183 with the proper SDP arrives
                            // here we need the leg1 invite
                            SipServletRequest initialInvite = SipSessionAttributes.INITIAL_REQUEST.get(ssL1,
                                    SipServletRequest.class);
                            call.getSipScenarios().add(SipScenarioWaitingFor183WithMRFSdp.start(initialInvite, resp));
                        }
                        // else for a confirmed leg, SipScenarioLegManipulation will handle the reINVITE with SDP
                    }
                });
    }
}
