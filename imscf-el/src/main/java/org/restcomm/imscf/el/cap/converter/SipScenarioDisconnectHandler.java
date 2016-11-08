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

import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipCapCancelDetector;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Q850ReasonHeader;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageDetector;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.isup.message.parameter.CauseIndicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for detecting an incoming disconnect message and possibly answering 200 OK to it. */
public final class SipScenarioDisconnectHandler extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioDisconnectHandler.class);

    // all scenario attributes are immutable: detector, handler, name and finished state (this scenario never finishes,
    // only gets added to a call).
    private static final SipScenarioDisconnectHandler SHARED_INSTANCE = new SipScenarioDisconnectHandler();

    public static SipScenarioDisconnectHandler start() {
        return SHARED_INSTANCE;
    }

    private SipScenarioDisconnectHandler() {
        super("Waiting for disconnect on SIP leg", Detector.SHARED_INSTANCE, Handler.SHARED_INSTANCE);
    }

    /** Message detector. */
    private static class Detector implements SipMessageDetector {
        // stateless class
        private static final Detector SHARED_INSTANCE = new Detector();

        @Override
        public boolean accept(SipServletMessage msg) {
            // accept BYE or CANCEL request that terminated the leg, or a final answer to the initial INVITE;
            // but only handle empty messages, not forwarded ERBCSM messages
            // also, don't accept CAP cancel messages.
            if (msg.getContentType() != null || SipCapCancelDetector.SHARED_INSTANCE.accept(msg))
                return false;

            if (msg instanceof SipServletRequest) {
                return "BYE".equals(msg.getMethod()) || "CANCEL".equals(msg.getMethod());
            } else {
                if (SipSessionAttributes.UAC_DISCONNECTED.get(msg.getSession(), Object.class) != null) {
                    LOG.trace("UAC disconnect, not accepting message");
                    return false;
                }
                return SipUtil.isErrorResponseForInitialInvite(msg);
            }
        }
    }

    /** Message handler. */
    private static class Handler implements SipMessageHandler {
        // stateless class
        private static final Handler SHARED_INSTANCE = new Handler();

        @Override
        public void handleMessage(Scenario scenario, SipServletMessage msg) {
            CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
            try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {
                if (msg instanceof SipServletRequest) {
                    if (!msg.isCommitted()) {
                        SipUtil.sendOrWarn(((SipServletRequest) msg).createResponse(SipServletResponse.SC_OK),
                                "Failed to send 200 OK to " + msg.getMethod() + " request");
                    } else {
                        LOG.trace("Incoming {} already answered", msg.getMethod());
                    }
                }

                if (call.getSipModule().isSipCallFinished(call)) {

                    if (CapSipConverterImpl.isCapCallFinished(call)) {
                        LOG.debug("Last SIP disconnect received in call, but CAP call is already terminated. Deleting call.");
                        CallContext.getCallFactory().deleteCall(call);
                        return;
                    }

                    LOG.debug("Last SIP disconnect received in call, sending CAP releaseCall and deleting call");
                    int causeValue = CauseIndicators._CV_NORMAL_UNSPECIFIED;
                    try {
                        Q850ReasonHeader reason = Q850ReasonHeader.parse(msg);
                        if (reason != null) {
                            causeValue = reason.getCause();
                            LOG.debug("Reason header received, releasing with requested release cause {}", causeValue);
                        } else {
                            LOG.debug("No Reason header received from AS, releasing with default release cause {}",
                                    causeValue);
                        }
                    } catch (ServletParseException e) {
                        LOG.warn("Failed to parse Reason header, releasing with default release cause {}", causeValue,
                                e);
                    }
                    // last disconnect, release call
                    ReleaseCallUtil.releaseCall(call, causeValue, true);
                    // call end, delete
                    CallContext.getCallFactory().deleteCall(call);
                }
            } catch (CAPException e) {
                LOG.warn("Failed to release call", e);
            }
        }
    }

}
