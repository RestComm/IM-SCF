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
package org.restcomm.imscf.el.sip.servlets;

import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.failover.SipAsLoadBalancer;
import org.restcomm.imscf.common.util.overload.OverloadProtector;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SIP servlet responding to SIP OPTIONS heartbeat messages.
 */
@javax.servlet.sip.annotation.SipServlet(name = "HeartbeatServlet")
public class HeartbeatServlet extends SipServlet {

    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatServlet.class);
    private static final long serialVersionUID = 1L;

    /**
     * @see SipServlet#SipServlet()
     */
    public HeartbeatServlet() {
        super();
    }

    @Override
    protected void doOptions(SipServletRequest req) throws ServletException, IOException {
        LOG.trace("OPTIONS heartbeat arrived from {} [via: {}]", req.getInitialRemoteAddr(), req.getHeader("Via"));
        if (OverloadProtector.getInstance().getCurrentState().isCpuOrHeapOverloaded()) {
            // If the system is overloaded send back 503
            LOG.debug("System is overloaded. Send back 503 for heartbeat.");
            SipServletResponse unavailable = req.createResponse(SipServletResponse.SC_SERVICE_UNAVAILABLE);
            SipUtil.createAndSetWarningHeader(unavailable, "Server is overloaded.");
            unavailable.send();
        } else {
            req.createResponse(SipServletResponse.SC_OK).send();
        }
    }

    @Override
    protected void doResponse(SipServletResponse resp) throws ServletException, IOException {
        if ("OPTIONS".equals(resp.getMethod())) {
            LOG.trace("OPTIONS heartbeat response arrived from {}", resp.getInitialRemoteAddr());
            SipAsLoadBalancer.getInstance().processHeartbeatResponse(resp);
        } else {
            LOG.warn("unexpected message:\n{}", resp);
        }
    }
}
