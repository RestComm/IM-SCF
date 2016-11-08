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

import org.restcomm.imscf.el.cap.CAPModuleBase;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.Sip1xxWithSdpDetector;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageBodyUtil;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for handling 183 with MRF SDP. */
public final class SipScenarioWaitingFor183WithMRFSdp extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioWaitingFor183WithMRFSdp.class);

    public static SipScenarioWaitingFor183WithMRFSdp start(SipServletRequest initialInvite, SipServletResponse resp) {
        return new SipScenarioWaitingFor183WithMRFSdp(initialInvite, resp);
    }

    private SipScenarioWaitingFor183WithMRFSdp(SipServletRequest initialInvite, SipServletResponse resp) {
        super("Waiting for 183 with MRF SDP", new Sip1xxWithSdpDetector(initialInvite, resp), new SipMessageHandler() {

            @Override
            public void handleMessage(Scenario scenario, SipServletMessage msg) {
                // 183 with proper sdp arrived
                // we have to handle the PRACK scenario
                // requestReportBCSMEvent (just for MRF) and connectToResource can be sent out
                // immediately

                // TODO: this is a copy-paste from SipScenarioPrackHandshake - some refactor?
                try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getSipCall(msg)) {
                    LOG.debug("1xxRel with proper SDP arrived, NOT sending PRACK in this scenario.");

                    scenario.setFinished();

                    int networkLegIdFromSdpId = SipUtil.networkLegIdFromSdpId(SipSessionAttributes.LEG_ID.get(
                            msg.getSession(), String.class));
                    CallSegment cs = call.getCallSegmentAssociation().getCallSegmentOfLeg(networkLegIdFromSdpId);
                    ConnectToResourceUtil.connectToMrf(call, cs.getId(),
                            ((CAPModuleBase) call.getCapModule()).getEDPsForMrf(call, cs.getLegs()),
                            SipMessageBodyUtil.getBodyAsString(msg));
                }

            }
        });
    }
}
