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
import org.restcomm.imscf.el.cap.sip.SipCapCancelDetector;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.SIPReasonHeader;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.stack.CallContext;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SIP scenario for detecting SIP message with Reason: SIP;cause=901 (;text=cancel) and sending CAP cancel.
 */
public final class SipScenarioCapCancel extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioCapCancel.class);

    public static SipScenarioCapCancel start() {
        return new SipScenarioCapCancel();
    }

    private SipScenarioCapCancel() {
        super("CAP cancel", SipCapCancelDetector.SHARED_INSTANCE, (scenario, msg) -> {
            CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
            try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {
                scenario.setFinished();

                // check that the CAP dialog is usable beforehand instead of trying and failing
                if (!CapUtil.canSendPrimitives(call.getCapDialog()) && !msg.isCommitted()) {
                    SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                            ((SipServletRequest) msg).createResponse(SipServletResponse.SC_BAD_REQUEST),
                            "Cannot send cancel in dialog state: " + call.getCapDialog().getState()),
                            "Failed to send error response to CAP cancel req");
                    return;
                }

                LOG.debug("Sending CAP cancel(allRequests)");
                try {
                    CAPDialogCircuitSwitchedCall dialog = call.getCapDialog();
                    dialog.addCancelRequest_AllRequests();
                    /* close() to end dialog on cancel */
                    dialog.close(false);

                    if (!msg.isCommitted()) {
                        SipUtil.sendOrWarn(((SipServletRequest) msg).createResponse(SipServletResponse.SC_OK),
                                "Failed to send 200 OK to CAP cancel request");
                    }

                    ((CapSipConverterImpl) call.getSipModule()).releaseSip(call, "CAP cancel",
                            SIPReasonHeader.INSTANCE_CAP_CANCEL);

                } catch (CAPException e) {
                    LOG.warn("Failed to send CAP cancel", e);
                    if (!msg.isCommitted()) {
                        SipUtil.sendOrWarn(
                                ((SipServletRequest) msg).createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                "Failed to send 500 Server internal error to CAP cancel request");
                    }
                }
            }
        });
    }
}
