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
package org.restcomm.imscf.el.cap.sip;

import org.restcomm.imscf.el.call.CapSipCall;
import org.restcomm.imscf.el.sip.NoopSipMessageHandler;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageDetector;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;

import javax.servlet.sip.SipSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for detecting an initial reaction from a SIP AS or handling an AS not responding timeout. */
public final class SipScenarioAsReaction extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioAsReaction.class);

    private SipScenarioAsReaction(SipMessageDetector detector, SipMessageHandler handler) {
        super("No reaction from AS", detector, handler);
    }

    public static Scenario start(CapSipCall<?> call, SipSession inviteSession) {

        String noReactionTimerId = call.setupTimer(call.getCapModule().getAsReactionTimeoutMillis(), imscfCallId -> {
            try (CapSipCall<?> c = (CapSipCall<?>) CallContext.getCallStore().getCallByImscfCallId((String) imscfCallId)) {
                LOG.debug("No reaction received from AS!");
                // No answer could be selective for certain IDP/INVITE content, therefore we should not ban the AS for a
                // single failed call.
                // String group = SipSessionAttributes.SIP_AS_GROUP.get(inviteSession, String.class);
                // String instance = SipSessionAttributes.SIP_AS_NAME.get(inviteSession, String.class);
                // SipAsLoadBalancer.getInstance().setAsUnavailable(group, instance);
                c.getSipModule().handleAsReactionTimeout(c);
            }
        }, call.getImscfCallId(), "NoReactionFromAS");

        SipMessageDetector detector = msg -> {
            LOG.debug("Message received from AS, canceling no answer timer");
            // don't accept any messages, as that would toggle the "handled" flag for them. However, any message
            // received should trigger a side-effect canceling the no answer timer.
            call.cancelTimer(noReactionTimerId);
            call.removeIf(SipScenarioAsReaction.class);
            return false;
        };

        SipScenarioAsReaction ret = new SipScenarioAsReaction(detector, NoopSipMessageHandler.SHARED_INSTANCE);
        call.add(ret);
        return ret;
    }

}
