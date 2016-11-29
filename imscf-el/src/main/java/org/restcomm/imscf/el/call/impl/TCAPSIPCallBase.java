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
package org.restcomm.imscf.el.call.impl;

import org.restcomm.imscf.common.config.SipApplicationServerGroupType;
import org.restcomm.imscf.el.call.TcapSipCall;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipResponseDetector;
import org.restcomm.imscf.el.sip.failover.SipAsLoadBalancer.FailoverContext;
import org.restcomm.imscf.el.sip.servlets.AppSessionHelper;
import org.restcomm.imscf.el.tcap.call.impl.TCAPCallBase;
import org.restcomm.imscf.util.IteratorStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.servlet.sip.SipServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for TCAP&lt;-&gt;SIP conversions, used both for CAP and MAP calls.
 */
public abstract class TCAPSIPCallBase extends TCAPCallBase implements TcapSipCall {

    private static final Logger LOG = LoggerFactory.getLogger(TCAPSIPCallBase.class);

    protected String sipAppSessionId;
    protected final List<Scenario> sipScenarios = new ArrayList<>();
    protected final Map<String, Queue<SipServletRequest>> pendingOutgoingRequests = new HashMap<>();
    protected FailoverContext failoverContext;
    protected final List<SipApplicationServerGroupType> appChain = new ArrayList<SipApplicationServerGroupType>();
    private boolean sipDialogCreationDisabled = false;

    @Override
    public String getAppSessionId() {
        return sipAppSessionId;
    }

    @Override
    public void setAppSessionId(String sipAppSessionId) {
        this.sipAppSessionId = sipAppSessionId;
    }

    @Override
    public List<Scenario> getSipScenarios() {
        return sipScenarios;
    }

    @Override
    public void setFailoverContext(FailoverContext failoverContext) {
        this.failoverContext = failoverContext;
    }

    @Override
    public FailoverContext getFailoverContext() {
        return failoverContext;
    }

    @Override
    public List<SipApplicationServerGroupType> getAppChain() {
        return appChain;
    }

    @Override
    public void disableSipDialogCreation() {
        this.sipDialogCreationDisabled = true;
    }

    @Override
    public boolean isSipDialogCreationDisabled() {
        return sipDialogCreationDisabled;
    }

    @Override
    public void queueMessage(SipServletRequest req) {
        Queue<SipServletRequest> queue = getPendingOutgoingRequests(req.getSession().getId());
        // requests stay in the queue until a response arrives. Empty queue means no outstanding transactions.
        boolean send = queue.isEmpty();
        queue.add(req);
        LOG.debug("Queued {} request for leg {}", req.getMethod(),
                SipSessionAttributes.LEG_ID.get(req.getSession(), String.class));
        if (send) {
            // no messages waiting for response, we can send the message right away
            sendNextMessageInQueue(queue);
        }
    }

    private Queue<SipServletRequest> getPendingOutgoingRequests(String sessionId) {
        return pendingOutgoingRequests.computeIfAbsent(sessionId, id -> {
            if (!IteratorStream.of(AppSessionHelper.getSipSessions(getAppSession()))
                    .anyMatch(s -> s.getId().equals(id)))
                throw new IllegalArgumentException("SipSession does not belong to this call! " + id);

            return new LinkedList<>();
        });
    }

    private void sendNextMessageInQueue(Queue<SipServletRequest> queue) {
        SipServletRequest req = queue.peek();
        if (req == null) {
            LOG.trace("No pending requests to send.");
            return;
        }
        if (SipUtil.isUaTerminated(req.getSession(false))) {
            LOG.trace("SipSession is terminated at the UA level, emptying request queue.");
            queue.clear();
            return;
        }
        LOG.trace("Trying to send {} request now", req.getMethod());
        if (SipUtil.sendOrWarn(req, "Failed to send queued {} request", req.getMethod())) {
            getSipScenarios().add(
                    new Scenario("Request Queue Processor", new SipResponseDetector(req), (scenario, response) -> {
                        scenario.setFinished();
                        queue.remove(); // remove answered request
                            sendNextMessageInQueue(queue);
                        }));
            if ("CANCEL".equals(req.getMethod()) || "BYE".equals(req.getMethod())) {
                SipSessionAttributes.UAC_DISCONNECTED.set(req.getSession(), true);
            }
        } else {
            // remove failed request, send next
            queue.remove();
            sendNextMessageInQueue(queue);
        }
    }
}
