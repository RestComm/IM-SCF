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
package org.restcomm.imscf.el.sip.failover;

import org.restcomm.imscf.el.call.TimerListener;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;
import org.restcomm.imscf.el.sip.servlets.SipSessions;
import org.restcomm.imscf.util.IteratorStream;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Timer listener for sending heartbeat OPTIONS messages. */
class HeartbeatSenderTimerListener implements TimerListener {

    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatSenderTimerListener.class);
    private static final String HB_TYPE_ATTRIBUTE = HeartbeatSenderTimerListener.class.getName() + ".HB_TYPE";
    private final String appSessionId;

    public HeartbeatSenderTimerListener(String appSessionId) {
        this.appSessionId = appSessionId;
    }

    /** Timeout type. */
    static enum TimeoutType {
        /** Time to ping available nodes. */
        ACTIVE_PING,
        /** Timeout for available nodes. */
        ACTIVE_TIMEOUT,
        /** Time to ping unavailable nodes. */
        INACTIVE_PING,
        /** Timeout for unavailable nodes. */
        INACTIVE_TIMEOUT
    }

    @Override
    public void timeout(Serializable info) {
        HeartbeatSenderTimerListener.TimeoutType t = (HeartbeatSenderTimerListener.TimeoutType) info;
        boolean active = TimeoutType.ACTIVE_PING == t || TimeoutType.ACTIVE_TIMEOUT == t;
        switch (t) {
        case ACTIVE_PING:
        case INACTIVE_PING:
            LOG.debug("Pinging {} SIP AS instances", active ? "available" : "unavailable");
            for (AsAvailability ava : SipAsLoadBalancer.getInstance().getAllHeartbeatEnabledEndpoints()) {
                // check the appropriate servers for this timeout
                if (active == ava.isAvailable()) {
                    try {
                        LOG.debug("Checking availability of {}/{}", ava.getAsGroupName(), ava.getServer().getName());
                        SipServletRequest req = SipAsLoadBalancer.getInstance().createHeartbeatMessage(ava);
                        req.getSession().setAttribute(HB_TYPE_ATTRIBUTE,
                                active ? TimeoutType.ACTIVE_TIMEOUT : TimeoutType.INACTIVE_TIMEOUT);
                        InetSocketAddress outboundInterface = new InetSocketAddress(InetAddress.getByName(ava
                                .getSipAsRouteAndInterface().getOutboundInterfaceHost()), ava
                                .getSipAsRouteAndInterface().getOutboundInterfacePort());
                        req.getSession().setOutboundInterface(outboundInterface);
                        req.send();
                    } catch (ServletException e) {
                        LOG.warn("Failed to create OPTIONS heartbeat message to {}/{}!", ava.getAsGroupName(), ava
                                .getServer().getName(), e);
                    } catch (IOException e) {
                        LOG.debug("Failed to send OPTIONS heartbeat message to {}/{}!", ava.getAsGroupName(), ava
                                .getServer().getName(), e);
                        ava.setAvailable(false);
                    }
                }
            }
            break;
        case ACTIVE_TIMEOUT:
        case INACTIVE_TIMEOUT:
            LOG.debug("Timeout for {} SIP AS heartbeat responses", active ? "available" : "unavailable");
            // timeouts: find still active sessions (waiting for response) and kill them + mark AS as unavailable
            SipApplicationSession sas = SipServletResources.getSipSessionsUtil()
                    .getApplicationSessionById(appSessionId);
            IteratorStream.of(SipSessions.of(sas))
                    .filter(ss -> !SipUtil.isUaTerminated(ss) && t == ss.getAttribute(HB_TYPE_ATTRIBUTE))
                    .forEach(ss -> {
                        LOG.trace("Invalidating SipSession {}", ss.getId());
                        SipAsLoadBalancer.getInstance().getAsAvailability(ss).setAvailable(false);
                        ss.invalidate();
                    });
            break;
        default:
            throw new AssertionError(t);
        }

    }
}
