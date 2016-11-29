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
import org.restcomm.imscf.el.cap.ErbcsmUtil;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesCsCall.CapScenarioEventReportBCSM;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.AlternativesScenario;
import org.restcomm.imscf.el.sip.Q850ReasonHeader;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.ScenarioFinishingSipMessageHandler;
import org.restcomm.imscf.el.sip.SipMessageDetector;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.sip.SipResponseClass;
import org.restcomm.imscf.el.sip.SipResponseDetector;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.MultipartBuilder;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSession.State;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest;
import org.mobicents.protocols.ss7.inap.api.primitives.LegType;
import org.mobicents.protocols.ss7.inap.api.primitives.MiscCallInfoMessageType;
import org.mobicents.protocols.ss7.inap.primitives.LegIDImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Incoming CAP request scenario for handling ERBCSM requests. */
@SuppressWarnings("PMD.GodClass")
public class CapScenarioIncomingERBCSM implements CapScenarioEventReportBCSM {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioIncomingERBCSM.class);

    @Override
    public boolean isFinished() {
        return false; // wait for multiple ERBCSM requests
    }

    @Override
    public void onRequest(EventReportBCSMRequest erbcsm) {
        CallStore store = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (CapSipCsCall call = (CapSipCsCall) store.getCapCall(erbcsm.getCAPDialog().getLocalDialogId())) {
            LOG.debug("eventReportBCSMRequest arrived: {}", erbcsm);
            int legID = getLegID(erbcsm);
            CallSegment cs = call.getCallSegmentAssociation().getCallSegmentOfLeg(legID);
            SipSession sip = SipUtil.findSipSessionForLegID(call, SipUtil.sdpIdFromNetworkLegId(legID));
            SipServletMessage msg;
            final boolean insertSdp;
            final boolean closeOnContinue;
            boolean cwaSent = false;

            if (cs == null || sip == null) {
                LOG.warn("There is no CS ({}) or SIP leg ({}) for L{}, dropping ERBCSM {}", cs, sip, legID, erbcsm);
                return;
            }

            MiscCallInfoMessageType messageType = ErbcsmUtil.getMessageType(erbcsm);
            if (messageType == MiscCallInfoMessageType.request) {
                cs.eventReportInterrupted();

                // For legs that are alone in their call segments (e.g. ICA), the AS cannot issue continue, as there is
                // no
                // other SIP leg. In this case, we send CUE/CWA automatically.
                // but: if there is an mrf leg, then we should avoid this

                SipSession sipMrf = SipUtil.findMrfSessionForCallSegment(call, cs.getId());
                boolean mrfInCS = sipMrf != null;
                int legCount = cs.getLegCount();
                int capVersion = call.getCapDialog().getApplicationContext().getVersion().getVersion();
                LOG.debug("MRF in callSegment {} : {}, leg count: {}, CAP version: {}", cs.getId(), mrfInCS ? "Yes"
                        : "No", legCount, capVersion);
                if (legCount == 1 && !mrfInCS && capVersion >= 4) { // leg/cs CWA only available on CAP4 and later
                    LOG.debug("Sending automatic CWA for standalone leg in {} in {}", cs, call);
                    try {
                        call.add(CapScenarioContinueWithArgument.start(call,
                                new LegIDImpl(true, LegType.getInstance(legID))));
                        cs.continueCS();
                        cwaSent = true;
                    } catch (CAPException e) {
                        LOG.warn("Failed to send CWA: \"{}\" for {}", e.getMessage(), call, e);
                        return;
                    }
                }

            }

            switch (erbcsm.getEventTypeBCSM()) {
            case oTermSeized:
            case callAccepted:
                // ringing
                msg = SipSessionAttributes.INITIAL_REQUEST.get(sip, SipServletRequest.class).createResponse(
                        SipServletResponse.SC_RINGING);
                closeOnContinue = false;
                insertSdp = true;
                break;
            case oAnswer:
            case tAnswer:
                msg = SipSessionAttributes.INITIAL_REQUEST.get(sip, SipServletRequest.class).createResponse(
                        SipServletResponse.SC_OK);
                // forwarded 200 OK, only if L1 is not yet confirmed
                SipSession sessionL1 = SipUtil.findSipSessionForLegID(call, "L1");
                if (sessionL1 != null && sessionL1.getState() == State.EARLY) {
                    call.getSipScenarios().add(
                            new Scenario("Forwarded 200 OK on answer", new SipForwardedMessageDetector(msg),
                            /*
                             * Simply send ACK, then remove stateless handling scenarios, as they don't make sense
                             * anymore on a confirmed leg.
                             *
                             * TODO: the latter should be done somewhere else, as it also applies to e.g. MRF
                             * connection, where the leg gets confirmed
                             */
                            (scenario, m) -> {
                                scenario.setFinished();
                                /* don't send ACK if the leg has already disconnecting before the AS could react */
                                if (call.getCallSegmentAssociation().getCallSegmentOfLeg(1) != null) {
                                    SipUtil.sendOrWarn(((SipServletResponse) m).createAck(),
                                            "Cannot send ACK to 200 OK INVITE");
                                } else {
                                    LOG.debug("Cannot find L1, not sending ACK for forwarded 200 OK.");
                                }
                                call.getSipScenarios().removeIf(
                                        s -> s instanceof SipScenarioInitialStatelessCallHandling
                                                || s instanceof SipScenarioInitialRelease);
                            }));
                } else {
                    LOG.trace("L1 SipSession is not in early state, not waiting for forwarded 200 OK");
                }
                // ACK for this 200 OK
                // lambda only keeps reference to these 2 variables, not the entire SipServletMessage object
                int cseq200OK = SipUtil.getCSeq(msg);
                String sessionId200OK = msg.getSession().getId();
                call.getSipScenarios().add(new Scenario("ACK for 200 OK answer", (m) -> {
                    return "ACK".equals(m.getMethod()) /* ACK */
                            && m.getSession().getId().equals(sessionId200OK) /* on this leg */
                            && SipUtil.getCSeq(m) == cseq200OK; /* for this 200 OK */
                }, ScenarioFinishingSipMessageHandler.SHARED_INSTANCE));
                closeOnContinue = false;
                insertSdp = true;
                break;
            case oCalledPartyBusy:
            case tBusy:
                cs.disconnectLeg(legID);
                msg = SipSessionAttributes.INITIAL_REQUEST.get(sip, SipServletRequest.class).createResponse(
                        SipServletResponse.SC_BUSY_HERE);
                closeOnContinue = true;
                insertSdp = false;
                break;
            case oAbandon:
            case tAbandon:
                closeOnContinue = true;
                cs.disconnectLeg(legID);
                if (sip.getState() == State.EARLY) {
                    // normal case, abandon is indicated by a CANCEL
                    SipServletRequest initialInvite = SipSessionAttributes.INITIAL_REQUEST.get(sip,
                            SipServletRequest.class);
                    msg = initialInvite.createCancel();
                    call.add(createL1AbandonHandler(initialInvite, (SipServletRequest) msg, !cwaSent, closeOnContinue,
                            erbcsm, cs.getId()));
                } else if (sip.getState() == State.CONFIRMED) {
                    // race condition case: abandon arrived after the answer, but before the continue for it. send BYE
                    msg = sip.createRequest("BYE");
                    call.add(new Scenario("Final answer for abandon BYE", new SipResponseDetector(
                            (SipServletRequest) msg, SipResponseClass.FINAL),
                            ScenarioFinishingSipMessageHandler.SHARED_INSTANCE));
                } else {
                    LOG.warn("Cannot send abandon ERBCSM in L1 SIP dialog state {}", sip.getState());
                    return;
                }
                insertSdp = false;
                break;
            case oDisconnect:
            case tDisconnect:
                cs.disconnectLeg(legID);
                msg = sip.createRequest("BYE");
                call.getSipScenarios().add(
                        new Scenario("Response for Disconnect BYE", new SipResponseDetector((SipServletRequest) msg),
                                ScenarioFinishingSipMessageHandler.SHARED_INSTANCE));
                closeOnContinue = true;
                insertSdp = false;
                break;
            case oNoAnswer:
            case tNoAnswer:
                cs.disconnectLeg(legID);
                msg = SipSessionAttributes.INITIAL_REQUEST.get(sip, SipServletRequest.class).createResponse(
                        SipServletResponse.SC_TEMPORARILY_UNAVAILABLE);
                closeOnContinue = true;
                insertSdp = false;
                break;
            case routeSelectFailure:
                cs.disconnectLeg(legID);
                msg = SipSessionAttributes.INITIAL_REQUEST.get(sip, SipServletRequest.class).createResponse(
                        SipServletResponse.SC_NOT_FOUND);
                closeOnContinue = true;
                insertSdp = false;
                break;
            default:
                LOG.warn("Unhandled event type {}", erbcsm.getEventTypeBCSM());
                return;
            }

            ErbcsmUtil.getCause(erbcsm).ifPresent(cause -> {
                try {
                    new Q850ReasonHeader(cause.getCauseIndicators().getCauseValue(), null).insertAsHeader(msg);
                } catch (CAPException e1) {
                    LOG.warn("Failed to parse CAP cause indicator value", e1);
                }
                LOG.trace("Storing pending release cause value: {}", cause);
                call.setPendingReleaseCause(cause);
            });

            String xml = Jss7ToXml.encode(erbcsm, "eventReportBCSM");
            if (xml == null) { // serialization error
                xml = "";
            }
            // try to insert content even if empty, the status code and SDP may still be relevant to the AS
            try {
                setContent(msg, xml, insertSdp);
            } catch (UnsupportedEncodingException | MessagingException e) {
                LOG.warn("Couldn't create body for ERBCSM message to AS, sending without content", e);
            }

            boolean sipMsgSentOrQueued;
            if (msg instanceof SipServletRequest) {
                call.queueMessage((SipServletRequest) msg);
                sipMsgSentOrQueued = true;
            } else {
                sipMsgSentOrQueued = SipUtil.sendOrWarn(msg, "Couldn't send ERBCSM response to AS");
            }

            if (!cwaSent && sipMsgSentOrQueued) {
                // only send continue for requests, but still check if we need to send TCAP end for notifications
                call.getSipScenarios()
                        .add(SipScenarioContinueForERBCSM.start(msg, closeOnContinue, erbcsm, cs.getId()));
            }

            if (closeOnContinue && call.getCallSegmentAssociation().getCallSegmentCount() == 0) {
                // last leg disconnected, close when SIP side finishes
                call.setCsCapState(CapSipCsCall.CAPState.TERMINATED);
            }
        }
    }

    private int getLegID(EventReportBCSMRequest erbcsm) {
        return Optional.ofNullable(erbcsm.getLegID()).map(l -> l.getReceivingSideID().getCode()).orElseGet(() -> {
            /* if legID is not present, infer from event type */
            switch (erbcsm.getEventTypeBCSM()) {
            case oAbandon:
            case tAbandon:
                return 1;
            default:
                /* busy, rsf, noanswer, ringing, answer. */
                /* [ot]Disconnect should always contain legID */
                return 2;
            }
        });
    }

    private void setContent(SipServletMessage msg, String xmlContent, boolean insertSdp) throws MessagingException,
            UnsupportedEncodingException {
        if (insertSdp) {
            // create multipart content with ERBCSM and SDP
            MimeMultipart mm = new MultipartBuilder().addPartBody(SipConstants.CONTENTTYPE_CAP_XML_STRING, xmlContent)
                    .addPartBody(SipConstants.CONTENTTYPE_SDP_STRING, SipUtil.createSdpForLegs(msg.getSession()))
                    .getResult();
            msg.setContent(mm, mm.getContentType());
        } else {
            // insert only ERBCSM
            msg.setContent(xmlContent, SipConstants.CONTENTTYPE_CAP_XML_STRING);
        }
    }

    private Scenario createL1AbandonHandler(SipServletRequest initialInvite, SipServletRequest cancel,
            boolean expectForwardedMessage, boolean closeTcap, EventReportBCSMRequest erbcsm, int csID) {

        SipMessageHandler cancelFailedHandler = (s, m) -> {
            // SipServletResponse inviteSuccess = (SipServletResponse) m;
            s.setFinished();
            /*
             * 481 for CANCEL, 200 INV arrived in the meantime. Retry the disconnect with a BYE, apply same logic as
             * before: Reason header, ERBCSM body, TCAP continue on forwarded message.
             */
            LOG.debug("INVITE success after attempted CANCEL, sending abandon BYE");
            try (SIPCall call = CallContext.getCallStore().getSipCall(initialInvite)) {
                SipServletRequest bye = initialInvite.getSession().createRequest("BYE");
                bye.setContent(cancel.getRawContent(), cancel.getContentType());
                if (cancel.getHeader("Reason") != null) {
                    bye.setHeader("Reason", cancel.getHeader("Reason"));
                }
                bye.send();
                call.add(new Scenario("Final answer for BYE", new SipResponseDetector(bye, SipResponseClass.FINAL),
                        ScenarioFinishingSipMessageHandler.SHARED_INSTANCE));
                if (expectForwardedMessage) {
                    call.add(SipScenarioContinueForERBCSM.start(bye, closeTcap, erbcsm, csID));
                }
            } catch (Exception e) {
                LOG.warn("Failed to send BYE request after CANCEL error", e);
            }
        };

        // 1) CANCEL succeeds and we receive an error response (e.g. 487 terminated) for the INVITE
        // 2) CANCEL is rejected and we receive a success response to the INVITE -> we have to disconnect with a BYE
        return AlternativesScenario.create("L1 abandon handler", new SipMessageDetector[] {
                new SipResponseDetector(initialInvite, SipResponseClass.ERROR_REQUEST),
                new SipResponseDetector(initialInvite, SipResponseClass.SUCCESS) }, new SipMessageHandler[] {
                ScenarioFinishingSipMessageHandler.SHARED_INSTANCE, cancelFailedHandler });

    }
}
