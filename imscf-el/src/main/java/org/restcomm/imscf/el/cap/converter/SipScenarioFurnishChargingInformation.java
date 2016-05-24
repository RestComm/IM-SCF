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
package org.restcomm.imscf.el.cap.converter;

import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipFciDetector;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.Jss7ToXml.XmlDecodeException;

import java.io.IOException;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.FurnishChargingInformationRequest;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.FurnishChargingInformationRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***/
public final class SipScenarioFurnishChargingInformation extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioFurnishChargingInformation.class);

    public static SipScenarioFurnishChargingInformation start() {
        return new SipScenarioFurnishChargingInformation();
    }

    private SipScenarioFurnishChargingInformation() {
        super(
                "Waiting for FCI",
                new SipFciDetector(),
                (scenario, msg) -> {
                    CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
                    try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {
                        SipServletRequest req = (SipServletRequest) msg;

                        // check that the CAP dialog is usable beforehand instead of parsing the XML and then failing
                        if (!CapUtil.canSendPrimitives(call.getCapDialog())) {
                            SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                    req.createResponse(SipServletResponse.SC_BAD_REQUEST),
                                    "Cannot send FCI in dialog state: " + call.getCapDialog().getState()),
                                    "Failed to send error response to FurnishChargingInformation INFO");
                            return;
                        }

                        FurnishChargingInformationRequest fci;
                        byte[] body;
                        try {
                            body = msg.getRawContent();
                        } catch (IOException e) {
                            LOG.warn("Failed to read FurnishChargingInformation INFO message content", e);
                            SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                    "Failed to send error response to FurnishChargingInformation INFO");
                            return;
                        }
                        try {
                            fci = Jss7ToXml.decode(body, FurnishChargingInformationRequestImpl.class);
                        } catch (XmlDecodeException e) {
                            LOG.warn("Invalid or missing FurnishChargingInformation XML in the message body.", e);
                            SipServletResponse resp = req.createResponse(SipServletResponse.SC_BAD_REQUEST);
                            SipUtil.createAndSetWarningHeader(resp,
                                    "Invalid or missing FurnishChargingInformation XML in the message body.");
                            SipUtil.sendOrWarn(resp, "Failed to send error response to FurnishChargingInformation INFO");
                            return;
                        }
                        try {
                            call.getCapOutgoingRequestScenarios().add(
                                    CapScenarioFurnishChargingInformation.start(call, fci));
                        } catch (CAPException e) {
                            LOG.warn("Failed to send CAP FurnishChargingInformation", e);
                            SipServletResponse resp = req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
                            SipUtil.createAndSetWarningHeader(resp,
                                    "Failed to send CAP FCI message. (" + e.getMessage() + ")");
                            SipUtil.sendOrWarn(resp, "Failed to send error response to FurnishChargingInformation INFO");
                            return;
                        }

                        SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_OK),
                                "Failed to send success response to FurnishChargingInformation INFO");
                    }
                });
    }
}
