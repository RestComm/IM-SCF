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
import org.restcomm.imscf.el.cap.ErbcsmUtil;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CallSegment.CallSegmentState;
import org.restcomm.imscf.el.cap.call.CallSegmentAssociationListener;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest;
import org.mobicents.protocols.ss7.inap.api.primitives.MiscCallInfoMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for detecting and handling a SIP message that contains
 *  an ERBCSM body forwarded to the other leg, meaning a CAP continue. */
public final class SipScenarioContinueForERBCSM extends Scenario implements CallSegmentAssociationListener {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioContinueForERBCSM.class);

    private int callSegmentId;
    private String imscfCallId;
    private MiscCallInfoMessageType messageType;

    public static SipScenarioContinueForERBCSM start(SipServletMessage msg, boolean closeTcap,
            EventReportBCSMRequest erbcsm, int callSegmentId) {
        MiscCallInfoMessageType messageType = ErbcsmUtil.getMessageType(erbcsm);
        String name = "Waiting for continue for ERBCSM(" + erbcsm.getEventTypeBCSM() + "/" + messageType + ")";
        SipScenarioContinueForERBCSM ret = new SipScenarioContinueForERBCSM(name, msg, closeTcap, messageType);
        ret.callSegmentId = callSegmentId;
        try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getSipCall(msg)) {
            ret.imscfCallId = call.getImscfCallId();
            call.getCallSegmentAssociation().registerListener(ret);
        }
        return ret;
    }

    private SipScenarioContinueForERBCSM(String name, SipServletMessage outgoingMessage, boolean closeTcap,
            MiscCallInfoMessageType messageType) {
        super(name, new SipForwardedContentDetector(outgoingMessage), new SipMessageHandler() {
            @Override
            public void handleMessage(Scenario scenario, SipServletMessage incomingMessage) {
                scenario.setFinished();
                CallStore store = (CallStore) CallContext.get(CallContext.CALLSTORE);
                try (CapSipCsCall call = (CapSipCsCall) store.getSipCall(incomingMessage)) {
                    // message handling and CS listening are alternatives - if a continue message arrives, unregister
                    call.getCallSegmentAssociation().unregisterListener((SipScenarioContinueForERBCSM) scenario);

                    // send SIP answer if necessary
                    if (incomingMessage instanceof SipServletRequest && !incomingMessage.isCommitted()) {
                        LOG.trace("Answering forwarded request");
                        SipUtil.sendOrWarn(
                                ((SipServletRequest) incomingMessage).createResponse(SipServletResponse.SC_OK),
                                "Failed to answer forwarded request");
                    }

                    if (messageType == MiscCallInfoMessageType.request) {

                        int legID = SipUtil.networkLegIdFromSdpId(SipSessionAttributes.LEG_ID.get(
                                incomingMessage.getSession(), String.class));
                        CallSegment cs = call.getCallSegmentAssociation().getCallSegmentOfLeg(legID);
                        if (cs == null) {
                            // target leg of the CS could have disconnected in the meantime
                            LOG.warn("Cannot find CS of L{}, dropping ERBCSM continue request from AS", legID);
                            return;
                        }
                        CAPDialogCircuitSwitchedCall cd = call.getCapDialog();
                        if (!CapUtil.canSendPrimitives(cd)) {
                            LOG.warn("Cannot send continue to ERBCSM in CAP dialog state {}", call.getCapDialog()
                                    .getState());
                            return;
                        }

                        // find CAMEL dialogue
                        LOG.debug("Sending continue for ERBCSM on CAMEL dialogue with local/remote tid: {}/{}",
                                call.getLocalTcapTrId(), call.getRemoteTcapTrId());

                        // TODO: before calling continueCS(), additional legs should be removed in case of
                        // disconnect, busy, etc.

                        // send continue
                        try {
                            cd.addContinueRequest();
                            if (closeTcap) {
                                LOG.debug("Terminating TCAP dialog.");
                                cd.close(false);
                                cs.continueCS();
                                call.setCsCapState(CapSipCsCall.CAPState.TERMINATED);
                            } else {
                                LOG.debug("Continuing TCAP dialog.");
                                cd.send();
                                cs.continueCS();
                            }
                        } catch (CAPException e) {
                            LOG.warn("Failed to send CAP continue", e);
                            return;
                        }
                    } else if (messageType == MiscCallInfoMessageType.notification) {
                        LOG.trace("Continue received from AS for notifyAndContinue ERBCSM");
                        // AS continue received for ERBCSM in notifyAndContinue mode,
                        // e.g. cap2 abandon or cap4 ringing

                        // basically don't do anything, except if the call is terminated.
                        // In that case, send TC-END

                        if (closeTcap && call.getSipModule().isSipCallFinished(call)) {
                            LOG.debug("Terminating TCAP dialog for finished call local/remote tid: {}/{}",
                                    call.getLocalTcapTrId(), call.getRemoteTcapTrId());
                            try {
                                CAPDialogCircuitSwitchedCall cd = call.getCapDialog();
                                cd.close(true);
                            } catch (CAPException e) {
                                LOG.warn("Failed to send TCAP end", e);
                                return;
                            }
                        } else {
                            LOG.trace("Nothing to do, call is still active");
                        }

                    }

                }

            }
        });
        this.messageType = messageType;
    }

    @Override
    public void callSegmentStateChanged(CallSegment cs) {
        try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getCallByImscfCallId(imscfCallId)) {
            if (cs.getId() == callSegmentId) {
                if (messageType == MiscCallInfoMessageType.request
                        && cs.getState() != CallSegmentState.WAITING_FOR_INSTRUCTIONS) {
                    LOG.debug("CS-{} is not in WAITING_FOR_INSTRUCTIONS state any more, terminating scenario {}",
                            callSegmentId, getName());
                    setFinished();
                    call.getSipScenarios().remove(this);
                    call.getCallSegmentAssociation().unregisterListener(this);
                } else if (messageType == MiscCallInfoMessageType.notification
                        && cs.getState() != CallSegmentState.MONITORING) {
                    LOG.debug("CS-{} is not in MONITORING state any more, terminating scenario {}", callSegmentId,
                            getName());
                    setFinished();
                    call.getSipScenarios().remove(this);
                    call.getCallSegmentAssociation().unregisterListener(this);
                }
            }
        }
    }

}
