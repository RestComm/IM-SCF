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

import org.restcomm.imscf.el.sip.ReasonHeader;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.ScenarioFinishingSipMessageHandler;
import org.restcomm.imscf.el.sip.SipResponseClass;
import org.restcomm.imscf.el.sip.SipResponseDetector;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for disconnecting a single SIP leg. */
public final class SipDisconnectLegScenario extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipDisconnectLegScenario.class);

    public static SipDisconnectLegScenario start(SIPCall call, SipSession session, String imscfReason, ReasonHeader reason) {
        String legid = SipSessionAttributes.LEG_ID.get(session, String.class);
        SipServletMessage msgToSend = null;
        SipServletRequest reqToAnswer = null;
        LOG.trace("SipSession state for {}: {}", legid, session.getState());
        switch (session.getState()) {
        case INITIAL:
        case EARLY:
            // only L1 is an outgoing INVITE, all others (including ICA) are incoming from the AS
            boolean client = "L1".equals(legid);
            SipServletRequest invite = SipSessionAttributes.INITIAL_REQUEST.get(session, SipServletRequest.class);
            if (client) {
                msgToSend = invite.createCancel();
                // wait for 487 for the INVITE, not the CANCEL
                reqToAnswer = invite;
            } else {
                // TODO what status code should we use here?
                msgToSend = invite.createResponse(SipServletResponse.SC_GONE);
            }
            break;
        case CONFIRMED:
            reqToAnswer = session.createRequest("BYE");
            msgToSend = reqToAnswer;
            break;
        case TERMINATED:
        default:
            throw new IllegalArgumentException("Invalid SIP session state: " + session.getState());
        }
        if (imscfReason != null) {
            msgToSend.addHeader("x-asc-reason", imscfReason);
        }
        if (reason != null) {
            reason.insertAsHeader(msgToSend);
        }
        if (msgToSend instanceof SipServletRequest) {
            call.queueMessage((SipServletRequest) msgToSend);
        } else {
            SipUtil.sendOrWarn(msgToSend, "Failed to send error response for " + legid);
        }
        if (reqToAnswer != null) {
            SipDisconnectLegScenario scenario = new SipDisconnectLegScenario(legid, reqToAnswer);
            call.getSipScenarios().add(scenario);
            return scenario;
        } else {
            return null;
        }
    }

    private SipDisconnectLegScenario(String legid, SipServletRequest bye) {
        super("SipDisconnectLegScenario-" + legid, new SipResponseDetector(bye, SipResponseClass.FINAL),
                ScenarioFinishingSipMessageHandler.SHARED_INSTANCE);
    }
}
