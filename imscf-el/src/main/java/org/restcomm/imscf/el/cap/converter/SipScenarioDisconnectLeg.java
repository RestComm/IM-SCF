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
import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipDisconnectLegDetector;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.Jss7ToXml.XmlDecodeException;

import java.io.IOException;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectLegRequest;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.DisconnectLegRequestImpl;
import org.mobicents.protocols.ss7.isup.message.parameter.CauseIndicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scenario for handling disconnectLeg requests from the AS. */
public final class SipScenarioDisconnectLeg extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioDisconnectLeg.class);

    private static final String LOG_MESSAGE_RLC = "AS requested disconnectLeg, but there is only a single leg in a single call segment."
            + " Sending releaseCall instead using {}.";

    public static SipScenarioDisconnectLeg start() {
        return new SipScenarioDisconnectLeg();
    }

    private SipScenarioDisconnectLeg() {
        super("Waiting for disconnectLeg", SipDisconnectLegDetector.SHARED_INSTANCE, (scenario, msg) -> {
            CallStore cs = CallContext.getCallStore();
            try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {
                SipServletRequest req = null;
                if (msg instanceof SipServletRequest)
                    req = (SipServletRequest) msg;

                // check that the CAP dialog is usable beforehand instead of parsing the XML and then failing
                if (!CapUtil.canSendPrimitives(call.getCapDialog()) && req != null && !req.isCommitted()) {
                    SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                            req.createResponse(SipServletResponse.SC_BAD_REQUEST),
                            "Cannot send disconnectLeg in dialog state: " + call.getCapDialog().getState()),
                            "Failed to send error response to disconnectLeg req");
                    return;
                }

                DisconnectLegRequest dl;
                byte[] body;
                try {
                    body = msg.getRawContent();
                } catch (IOException e) {
                    LOG.warn("Failed to read disconnectLeg message content", e);
                    if (req != null) {
                        SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                "Failed to send error response to disconnectLeg request");
                    }
                    return;
                }
                try {
                    dl = Jss7ToXml.decode(body, DisconnectLegRequestImpl.class);
                } catch (XmlDecodeException e) {
                    LOG.warn("Invalid or missing disconnectLeg XML in the message body.", e);
                    if (req != null) {
                        SipServletResponse resp = req.createResponse(SipServletResponse.SC_BAD_REQUEST);
                        SipUtil.createAndSetWarningHeader(resp,
                                "Invalid or missing disconnectLeg XML in the message body.");
                        SipUtil.sendOrWarn(resp, "Failed to send error response to disconnectLeg request");
                    }
                    return;
                }

                boolean sendReleaseCallForLastDisconnectLeg = true; // TODO maybe make this parameterable
                if (sendReleaseCallForLastDisconnectLeg && call.getCallSegmentAssociation().getCallSegmentCount() == 1
                        && call.getCallSegmentAssociation().getOnlyCallSegment().getLegCount() == 1) {
                    /*
                     * There is only one call segment in the call with a single leg in it, and the AS requested a
                     * disconnectLeg. In this case, we send releaseCall to avoid unexpectedComponentSequence from the
                     * SSF.
                     */
                    try {
                        if (dl.getReleaseCause() != null) {
                            LOG.debug(LOG_MESSAGE_RLC, "the same release cause");
                            ReleaseCallUtil.releaseCall(call, dl.getReleaseCause(), true);
                        } else {
                            LOG.debug(LOG_MESSAGE_RLC, "NORMAL_UNSPECIFIED release cause");
                            ReleaseCallUtil.releaseCall(call, CauseIndicators._CV_NORMAL_UNSPECIFIED, true);
                        }
                        /* If a releaseCall succeeded, no more disconnectLeg requests can arrive. */
                        scenario.setFinished();

                        if (req != null) {
                            /* respond with 200 OK */
                            SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_OK),
                                    "Failed to send success response to disconnectLeg request");
                        }

                    } catch (CAPException e) {
                        LOG.warn("Failed to send CAP releaseCall: {}", e.getMessage(), e);
                        if (req != null) {
                            SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                    "Failed to send error response to disconnectLeg request");
                        }
                    }
                    return;
                } else {
                    LOG.debug("Sending CAP disconnectLeg");
                    try {
                        /* in this case, req will be answered when DLEG result arrives from SSF */
                        call.getCapOutgoingRequestScenarios().add(CapScenarioDisconnectLeg.start(msg, dl));
                    } catch (CAPException e) {
                        LOG.warn("Failed to send CAP DisconnectLeg", e);
                        if (req != null) {
                            SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                    "Failed to send error response to disconnectLeg request");
                        }
                    }
                    return;
                }
            }
        });
    }
}
