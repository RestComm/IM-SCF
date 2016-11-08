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

import org.restcomm.imscf.common.config.InviteErrorActionType;
import org.restcomm.imscf.common.config.ReleaseCauseType;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.ReleaseCauseMapper;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.sip.Q850ReasonHeader;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.sip.failover.SipAsLoadBalancer;
import org.restcomm.imscf.el.stack.CallContext;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.isup.message.parameter.CauseIndicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for detecting an initial INVITE error response from the AS and continuing/releasing the call accordingly. */
public final class SipScenarioInitialRelease extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioInitialRelease.class);

    public static SipScenarioInitialRelease start(SipServletRequest invite, InviteErrorMatcher[] handlers) {
        return new SipScenarioInitialRelease(invite, handlers);
    }

    private SipScenarioInitialRelease(SipServletRequest invite, InviteErrorMatcher[] handlers) {
        super("Waiting for initial release", new SipInitialReleaseDetector(invite), new SipMessageHandler() {
            @Override
            public void handleMessage(Scenario scenario, SipServletMessage msg) {
                scenario.setFinished();
                CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
                try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {
                    int statusCode = ((SipServletResponse) msg).getStatus();
                    int serviceKey = call.getIdp().getServiceKey();
                    Q850ReasonHeader reason = null;
                    try {
                        reason = Q850ReasonHeader.parse(msg);
                        if (reason != null) {
                            LOG.debug("Reason header received, releasing with requested release cause {}",
                                    reason.getCause());
                            ReleaseCallUtil.releaseCall(call, reason.getCause(), true);
                            LOG.trace("Initial call release finished");
                            return;
                        } else {
                            LOG.trace("No Reason header received from AS, treating as INVITE error");
                        }
                    } catch (ServletParseException e) {
                        LOG.warn("Failed to parse Reason header, treating as INVITE error", e);
                    }

                    LOG.trace("Processing INVITE error handlers for response {}, sk {}...", statusCode, serviceKey);

                    InviteErrorActionType action = null;
                    for (InviteErrorMatcher handler : handlers) {
                        if (handler.matches(statusCode, serviceKey)) {
                            LOG.trace("match: {}", handler);
                            action = handler.getAction();
                            break;
                        }
                    }
                    if (action == null) {
                        LOG.warn(
                                "Couldn't find matching inviteErrorHandler for response {} with sk {}, configuration is incomplete!",
                                statusCode, serviceKey);
                        // this is a configuration error that should have not have passed validation, not handled here
                        return;
                    }

                    switch (action.getAction()) {
                    case CONTINUE:
                        LOG.debug("Action to perform: continue");
                        continueCall(call);
                        break;
                    case RELEASE:
                        LOG.debug("Action to perform: release with cause {}", action.getReleaseCause());
                        releaseCall(call, action.getReleaseCause());
                        break;
                    case FAILOVER:
                        LOG.debug("Action to perform: failover to next AS. Marking AS as unavailable.");
                        SipAsLoadBalancer.getInstance().setAsUnavailable(
                                SipSessionAttributes.SIP_AS_GROUP.get(msg.getSession(), String.class),
                                SipSessionAttributes.SIP_AS_NAME.get(msg.getSession(), String.class));
                        call.getSipModule().handleAsUnavailableError(call);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Invalid configuration: invalid initialErrorHandler action type: " + action.getAction());
                    }

                    LOG.trace("Initial invite error handling finished");

                } catch (CAPException e) {
                    LOG.warn("Failed to execute initial invite error handling, deleting call.", e);
                    // SIP side should be finished (final response to only SIP leg)
                    // TODO: anything left to do with CAP dialog before delete?
                }
            }
        });
    }

    private static void releaseCall(CapSipCsCall call, ReleaseCauseType cause) throws CAPException {
        Integer causeValue = ReleaseCauseMapper.releaseCauseToCauseValue(cause);
        if (causeValue == null) {
            LOG.warn(
                    "Requested cause value {} cannot be mapped to jss7 release cause. Using NORMAL_UNSPECIFIED instead.",
                    cause);
            causeValue = CauseIndicators._CV_NORMAL_UNSPECIFIED;
        }
        ReleaseCallUtil.releaseCall(call, causeValue, true);
    }

    private static void continueCall(CapSipCsCall call) throws CAPException {
        CAPDialogCircuitSwitchedCall dialog = call.getCapDialog();
        ContinueUtil.continueCall(dialog, true);
    }
}
