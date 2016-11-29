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
import org.restcomm.imscf.el.cap.scenarios.CapIncomingRequestScenario;
import org.restcomm.imscf.el.cap.sip.SipApplyChargingDetector;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.Jss7ToXml.XmlDecodeException;

import java.io.IOException;
import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingRequest;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ApplyChargingRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***/
public final class SipScenarioApplyCharging extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioApplyCharging.class);

    public static SipScenarioApplyCharging start() {
        return new SipScenarioApplyCharging();
    }

    private SipScenarioApplyCharging() {
        super(
                "Waiting for ApplyCharging",
                new SipApplyChargingDetector(),
                (scenario, msg) -> {
                    CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
                    try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {
                        SipServletRequest req = (SipServletRequest) msg;

                        // check that the CAP dialog is usable beforehand instead of parsing the XML and then failing
                        if (!CapUtil.canSendPrimitives(call.getCapDialog())) {
                            SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                    req.createResponse(SipServletResponse.SC_BAD_REQUEST),
                                    "Cannot send AC in dialog state: " + call.getCapDialog().getState()),
                                    "Failed to send error response to ApplyCharging INFO");
                            return;
                        }

                        ApplyChargingRequest ac;
                        byte[] body;
                        try {
                            body = msg.getRawContent();
                        } catch (IOException e) {
                            LOG.warn("Failed to read ApplyCharging INFO message content", e);
                            SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                    "Failed to send error response to ApplyCharging INFO");
                            return;
                        }
                        try {
                            ac = Jss7ToXml.decode(body, ApplyChargingRequestImpl.class);
                        } catch (XmlDecodeException e) {
                            LOG.warn("Invalid or missing ApplyCharging XML in the message body.", e);
                            SipServletResponse resp = req.createResponse(SipServletResponse.SC_BAD_REQUEST);
                            SipUtil.createAndSetWarningHeader(resp,
                                    "Invalid or missing ApplyCharging XML in the message body.");
                            SipUtil.sendOrWarn(resp, "Failed to send error response to ApplyCharging INFO");
                            return;
                        }

                        try {
                            call.getCapOutgoingRequestScenarios().add(CapScenarioApplyCharging.start(call, ac));
                            List<CapIncomingRequestScenario<?>> isList = call.getCapIncomingRequestScenarios();
                            if (!isList.stream().anyMatch(s -> s instanceof CapScenarioApplyChargingReport)) {
                                call.getCapIncomingRequestScenarios().add(new CapScenarioApplyChargingReport());
                                LOG.trace("First applyCharging request, now listening for applyChargingReport operations");
                            }
                        } catch (CAPException e) {
                            LOG.warn("Failed to send CAP ApplyCharging", e);
                            SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                    "Failed to send error response to ApplyCharging INFO");
                            return;
                        }

                        SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_OK),
                                "Failed to send success response to ApplyCharging INFO");
                    }
                });
    }
}
