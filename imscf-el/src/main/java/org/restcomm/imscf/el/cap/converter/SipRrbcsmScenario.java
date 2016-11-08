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

import java.io.IOException;

import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipRrbcsmDetector;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.Jss7ToXml.XmlDecodeException;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.RequestReportBCSMEventRequest;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.RequestReportBCSMEventRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***/
public final class SipRrbcsmScenario extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipRrbcsmScenario.class);

    public static SipRrbcsmScenario start() {
        return new SipRrbcsmScenario();
    }

    private SipRrbcsmScenario() {
        super("Waiting for RRBCSM", new SipRrbcsmDetector(), new SipMessageHandler() {
            @Override
            public void handleMessage(Scenario scenario, SipServletMessage msg) {
                CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
                try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {

                    RequestReportBCSMEventRequest rrbcsm;
                    try {
                        rrbcsm = Jss7ToXml.decode(msg.getRawContent(), RequestReportBCSMEventRequestImpl.class);
                    } catch (IOException | XmlDecodeException e) {
                        LOG.warn("Failed to parse RRBCSM INFO message", e);
                        SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                ((SipServletRequest) msg).createResponse(SipServletResponse.SC_BAD_REQUEST),
                                "Invalid or missing RRBCSM XML in the message body."),
                                "Failed to send error response to RRBCSM INFO");
                        return;
                    }

                    // overwrite with values requested by the AS
                    call.getBCSMEventsForPendingRrbcsm().clear();
                    call.getBCSMEventsForPendingRrbcsm().addAll(rrbcsm.getBCSMEventList());

                    SipUtil.sendOrWarn(((SipServletRequest) msg).createResponse(SipServletResponse.SC_OK),
                            "Failed to create success response to RRBCSM INFO");
                }
            }
        });
    }
}
