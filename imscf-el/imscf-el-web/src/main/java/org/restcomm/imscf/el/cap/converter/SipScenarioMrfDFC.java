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

import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageDetector;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for handling DFC. */
public final class SipScenarioMrfDFC extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioMrfDFC.class);

    public static SipScenarioMrfDFC start() {
        return new SipScenarioMrfDFC();
    }

    private SipScenarioMrfDFC() {
        super("Waiting for MRF disconnect on SIP leg", new SipMessageDetector() {

            @Override
            public boolean accept(SipServletMessage msg) {
                // accepts only BYE messages from MRF without answering it
                // answering the message still belongs to the disconnect scenario
                // here, we only start the DFC CAP scenario - this scenario finishes immediately
                return "mrf".equals(SipSessionAttributes.LEG_ID.get(msg.getSession(), String.class))
                        && "BYE".equals(msg.getMethod()) && msg instanceof SipServletRequest;
            }
        }, new SipMessageHandler() {

            @Override
            public void handleMessage(Scenario scenario, SipServletMessage msg) {
                scenario.setFinished();
                try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getSipCall(msg)) {

                    // check that the CAP dialog is usable beforehand instead of failing later
                    if (!CapUtil.canSendPrimitives(call.getCapDialog())) {
                        LOG.warn("CAP dialog already finished, cannot perform DFC");
                        return;
                    }

                    // clean-up of unnecessary scenarios
                    call.getSipScenarios().removeIf(
                            ss -> ss instanceof SipScenarioPlayAnnouncement
                                    || ss instanceof SipScenarioPromptAndCollectUserInformation);
                    call.getCapIncomingRequestScenarios().removeIf(s -> s instanceof CapScenarioIncomingERBCSMMrf);

                    Integer csID = SipSessionAttributes.MRF_CS_ID.get(msg.getSession(), Integer.class);
                    CallSegment cs = call.getCallSegmentAssociation().getCallSegment(csID);

                    if (cs != null) {
                        LOG.debug("MRF SIP disconnect received, performing disconnectForwardConnection");
                        DisconnectForwardConnectionUtil.sendDisconnectForwardConnection(call, cs.getId());
                        cs.disconnectForwardConnection();
                    } else {
                        // this can happen if the listening party disconnected, causing a race condition between the
                        // handling of SRR and ERBCSM
                        // e.g.:
                        // 1. INFO(SRR) sent to AS
                        // 2. ERBCSM arrives, leg is disconnected and CS is destroyed
                        // 3. AS responds to SRR with a BYE to the MRF
                        LOG.debug("MRF SIP BYE received for missing CS-{}, not sending DFC", csID);
                        // nothing else to do in this case
                    }
                }
            }
        });

    }

}
