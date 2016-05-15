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

import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CallSegment.CallSegmentState;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.stack.CallContext;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectLegRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectLegResponse;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CAP scenario for sending out disconnectLeg. */
public final class CapScenarioDisconnectLeg implements
        org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesCsCall.CapScenarioDisconnectLeg {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioDisconnectLeg.class);
    private SipServletRequest req;
    private int legId;
    private Long invokeId;

    public static CapScenarioDisconnectLeg start(SipServletMessage msg, DisconnectLegRequest dl) throws CAPException {
        try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getSipCall(msg)) {
            long invokeId;
            int legId = dl.getLegToBeReleased().getSendingSideID().getCode();
            CAPDialogCircuitSwitchedCall dialog = call.getCapDialog();
            invokeId = dialog
                    .addDisconnectLegRequest(dl.getLegToBeReleased(), dl.getReleaseCause(), dl.getExtensions());
            dialog.send();
            return new CapScenarioDisconnectLeg(msg, invokeId, legId);
        }
    }

    private CapScenarioDisconnectLeg(SipServletMessage msg, Long invokeId, int legId) {
        if (msg instanceof SipServletRequest) {
            this.req = (SipServletRequest) msg;
        }
        this.invokeId = invokeId;
        this.legId = legId;
    }

    @Override
    public Long getInvokeId() {
        return invokeId;
    }

    @Override
    public void onSuccess(DisconnectLegResponse response) {
        try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getCallByLocalTcapTrId(
                response.getCAPDialog().getLocalDialogId())) {
            // we must already be locked on the call if the scenario receives control
            LOG.debug("disconnectLeg success");
            CallSegment cs = call.getCallSegmentAssociation().getCallSegmentOfLeg(legId);
            cs.disconnectLeg(legId);

            // send continueWithArgument for this CS if enabled and necessary (other legs are still present)
            if (call.isAutomaticCallProcessingEnabled() && cs.getState() == CallSegmentState.WAITING_FOR_INSTRUCTIONS) {
                LOG.debug("Sending CWA after disconnectLeg");
                try {
                    call.getCapOutgoingRequestScenarios().add(CapScenarioContinueWithArgument.start(call, cs.getId()));
                    cs.continueCS();
                } catch (CAPException e) {
                    LOG.warn("Failed to send CAP CWA: {}", e.getMessage(), e);
                    if (req != null) {
                        SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR),
                                "Failed to send error response to disconnectLeg request");
                    }
                    return;
                }

            }

            if (req != null) {
                // respond with 200
                SipUtil.sendOrWarn(req.createResponse(SipServletResponse.SC_OK),
                        "Failed to send success response to disconnectLeg request");
            }
        }
    }

    @Override
    public void onFailureIndicated(CAPErrorMessage error, Problem problem) {
        // we must already be locked on the call if the scenario receives control
        LOG.warn("disconnectLeg error: {} / {}", error, problem);

        if (req != null) {
            // respond with 4xx
            SipServletResponse resp = req.createResponse(SipServletResponse.SC_BAD_REQUEST);
            SipUtil.createAndSetWarningHeader(resp, "Error response for disconnectLeg");
            SipUtil.sendOrWarn(resp, "Failed to send response to disconnectLeg request");
        }
    }

    @Override
    public void onFailureTimeout() {
        // we must already be locked on the call if the scenario receives control
        LOG.warn("disconnectLeg timeout");

        if (req != null) {
            // respond with 408 timeout
            SipServletResponse resp = req.createResponse(SipServletResponse.SC_REQUEST_TIMEOUT);
            SipUtil.createAndSetWarningHeader(resp, "Invoke timeout for disconnectLeg");
            SipUtil.sendOrWarn(resp, "Failed to send response to disconnectLeg request");
        }
    }
}
