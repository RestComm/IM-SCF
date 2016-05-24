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
package org.restcomm.imscf.el.sip.servlets;

import org.restcomm.imscf.el.call.ImscfCallLifeCycleState;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.call.history.ElEventCreator;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipApplicationSessionAttributes;
import org.restcomm.imscf.el.sip.adapters.SipServletMessageAdapter;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;
import org.restcomm.imscf.common.util.overload.OverloadProtector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SIP servlet base class for scenario handling.
 */
public class ScenarioBasedServlet extends SipServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ScenarioBasedServlet.class);

    private static final long serialVersionUID = 1L;

    @EJB
    protected transient CallStore callStore;

    @EJB
    protected transient ConfigBean configBean;

    @EJB
    protected transient CallFactoryBean callFactory;

    @Override
    public void init() throws ServletException {
        super.init();
    }

    protected String shortDescription(SipServletMessage msg) {
        if (msg instanceof SipServletRequest)
            return msg.getMethod() + " request";
        else
            return ((SipServletResponse) msg).getStatus() + " " + msg.getMethod() + " response";
    }

    @Override
    public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        SipServletMessage msg = req instanceof SipServletRequest ? (SipServletRequest) req
                : resp instanceof SipServletResponse ? (SipServletResponse) resp : null;
        if (msg == null) {
            LOG.error("Unhandled ServletRequest type in service: {}, {}", req.getClass(), resp.getClass());
            return;
        }

        try (ContextLayer cl = CallContext.with(callStore, configBean, callFactory)) {

            // scenarios should receive a wrapped message
            SipServletMessage msgA = SipServletMessageAdapter.getAdapter(msg);

            LOG.trace("Trying to find call with appsession id {}", msgA.getApplicationSession().getId());
            try (SIPCall call = callStore.getCallByAppSessionId(msgA.getApplicationSession().getId());) {
                LOG.debug("Incoming message:\n{}", msgA);

                // SIP message should never arrive after the application session was invalidated.
                // Whether or not a call exists for this SAS, the message is simply dropped. However, we only do that
                // here to enable call information logging if it's available.
                if (!msgA.getApplicationSession().isValid()) {
                    LOG.warn("Incoming {} with Call-ID {} in already invalidated appsession {} for call {}.",
                            shortDescription(msgA), msgA.getCallId(), msgA.getApplicationSession().getId(),
                            call == null ? "N/A" : call.getImscfCallId());
                    return;
                }

                if (call == null) {
                    if (req != null && OverloadProtector.getInstance().getCurrentState().isCpuOrHeapOverloaded()) {
                        // If the incoming message is a request and the system is overloaded
                        // then send back 503
                        LOG.debug("System is overloaded. Send back 503.");
                        SipServletResponse unavailable = ((SipServletRequest) req)
                                .createResponse(SipServletResponse.SC_SERVICE_UNAVAILABLE);
                        SipUtil.createAndSetWarningHeader(unavailable, "Server is overloaded.");
                        unavailable.send();
                    } else {
                        super.service(req, resp); // super gets the original unwrapped parameters
                    }
                    // The call is not yet created. In this case the event is added by CallFactoryBean
                    return;
                }

                call.getCallHistory().addEvent(ElEventCreator.createIncomingSipEvent(msgA));
                // if the call already exists, scenarios should handle the message
                runScenarios(call, msgA);
            }
        }
    }

    // this method is extracted so that subclasses can also call it, e.g.:
    // 1.) this.service()
    //
    // 2.a) -> this.runScenarios()
    //
    // 2.b) -> this.super.service()
    // 2.b.1) -> subclass.doInvite() /* creates the call, adds scenarios, calls this method */
    // 2.b.2) -> this.runScenarios()

    /**
     * @param call Current call that is already locked by the current thread
     * @param msgA SipServletMessage already wrapped in an adapter
     */
    protected final void runScenarios(SIPCall call, SipServletMessage msgA) {
        // we must already have a ContextLayer
        Objects.requireNonNull(CallContext.getCallFactory(), "ContextLayer must be present!");
        // we must already be locked on the call
        // - no check available for that yet...
        Objects.requireNonNull(call, "Call cannot be null!");

        LOG.trace("Message belongs to call: {}", call);
        AppSessionHelper.renewAppSessionTimeout(call, msgA.getApplicationSession());

        boolean handledByAny = false;
        List<Scenario> refList = call.getSipScenarios();
        LOG.trace("Originally active scenarios: {}", new ScenarioListPrinter(refList));
        // A scenario might add/remove other scenarios while handling a message, thus modifying the refList.
        // the "iteratorList" is used to:
        // 1) prevent ConcurrentModificationException on the original list
        // 2) only call handleMessage on the scenarios that where originally present, but not on those added as
        // a result of processing
        List<Scenario> iteratorList = new ArrayList<>(refList);
        boolean handledByCurrent;
        for (Scenario s : iteratorList) {
            String sname = s.getName();
            handledByCurrent = s.handleMessage(msgA);
            handledByAny |= handledByCurrent;
            LOG.trace("Scenario {} {} the message.", sname, handledByCurrent ? "accepted" : "ignored");
            if (call.getImscfState() == ImscfCallLifeCycleState.FINISHED) {
                LOG.trace("Scenario deleted the call, skipping remaining scenarios.");
                return;
            }
            if (s.isFinished()) {
                if (refList.remove(s)) {
                    LOG.trace("Removed finished scenario {}", sname);
                } else {
                    LOG.trace("Scenario {} was already removed.", sname);
                }
                // notify module
                call.getSipModule().scenarioFinished(call, s);
                if (call.getImscfState() == ImscfCallLifeCycleState.FINISHED) {
                    LOG.trace("SIP module deleted the call, skipping remaining scenarios.");
                    return;
                }
            }
        }
        if (handledByAny) {
            LOG.trace("Message handled");
        } else if (SipUtil.isCommitted(msgA)) {
            LOG.trace("Message not handled, but already committed.");
        } else if (msgA instanceof SipServletRequest && "BYE".equals(msgA.getMethod())
                && SipUtil.isUaTerminated(msgA.getSession())) {
            // The race condition of glare BYE messages is handled here instead of in a dedicated scenario, as this is
            // not a call control event.
            LOG.debug("Incoming BYE for an already disconnected SIP leg, sending back 200 OK.");
            SipUtil.sendOrWarn(
                    SipUtil.createAndSetWarningHeader(
                            ((SipServletRequest) msgA).createResponse(SipServletResponse.SC_OK), "Glare BYE condition"),
                    "Failed to send 200 BYE on already finished SIP leg for:\n{}", msgA);
        } else {
            LOG.warn("Message is not committed and was not handled by any SIP scenario!");
            // answer unintelligible requests with 400 instead of just ignoring them
            if (msgA instanceof SipServletRequest) {
                SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                        ((SipServletRequest) msgA).createResponse(SipServletResponse.SC_BAD_REQUEST),
                        "Request not understood"), "Failed to send error response to unhandled request");
            }
        }

        LOG.trace("Newly active scenarios: {}", new ScenarioListPrinter(refList));

        // Call was not deleted by any scenarios, but this may be due to failures or an unexpected call flow. Here we
        // give a chance to the module to detect a one-sided call and handle it appropriately.
        // Only call once: a finished call may still receive a few reply messages to disconnect requests
        boolean markedFinished = Boolean.TRUE.equals(SipApplicationSessionAttributes.SIP_CALL_FINISHED.get(call
                .getAppSession()));
        if (!markedFinished && call.getSipModule().isSipCallFinished(call)) {
            LOG.debug("SIP module indicated that the SIP side of the call is finished, calling handler");
            SipApplicationSessionAttributes.SIP_CALL_FINISHED.set(call.getAppSession(), Boolean.TRUE);
            call.getSipModule().handleSipCallFinished(call);
        }

    }

    /** Logging utility class for printing the names of a Scenario list. */
    private static class ScenarioListPrinter {
        private final List<Scenario> list;

        ScenarioListPrinter(List<Scenario> list) {
            this.list = list;
        }

        @Override
        public String toString() {
            return list.stream().map(Scenario::getName).collect(Collectors.joining(", "));
        }
    }
}
