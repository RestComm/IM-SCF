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
package org.restcomm.imscf.el.cap.sip;

import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.sip.AlternativesScenario;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.ScenarioFinishingSipMessageHandler;
import org.restcomm.imscf.el.sip.SipMessageDetector;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.sip.SipResponseClass;
import org.restcomm.imscf.el.sip.SipResponseDetector;
import org.restcomm.imscf.el.stack.CallContext;

import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for performing a PRACK handshake. */
public final class SipScenarioPrackHandshake {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioPrackHandshake.class);

    private SipScenarioPrackHandshake() {
        // no instance
    }

    public static Scenario start(SipServletRequest invite) {
        SipMessageHandler handlerFor1xxRel = (s, inviteRelProvResp) -> {
            // don't finish to allow multiple provisional responses
            LOG.debug("1xxRel arrived, sending PRACK and adding new scenario.");
            try {
                try (SIPCall call = ((CallStore) CallContext.get(CallContext.CALLSTORE)).getSipCall(inviteRelProvResp)) {
                    SipServletRequest prack = ((SipServletResponse) inviteRelProvResp).createPrack();
                    call.queueMessage(prack);
                    call.getSipScenarios().add(
                            new Scenario("Waiting for 200 OK PRACK", new SipResponseDetector(prack),
                                    ScenarioFinishingSipMessageHandler.SHARED_INSTANCE));
                }

            } catch (Rel100Exception e) {
                LOG.warn("Failed to send PRACK to reliable 1xx response", e);
            }
        };

        SipMessageHandler handlerForFinal = ScenarioFinishingSipMessageHandler.SHARED_INSTANCE;

        return AlternativesScenario.create("PRACK handshake", new SipMessageDetector[] { new Sip1xxRelDetector(invite),
                new SipResponseDetector(invite, SipResponseClass.FINAL) }, new SipMessageHandler[] { handlerFor1xxRel,
                handlerForFinal });
    }

}
