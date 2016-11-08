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

import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.MultipartBuilder;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitiateCallAttemptResponse;
import org.mobicents.protocols.ss7.cap.primitives.BCSMEventImpl;
import org.mobicents.protocols.ss7.inap.api.primitives.LegID;
import org.mobicents.protocols.ss7.inap.api.primitives.LegType;
import org.mobicents.protocols.ss7.inap.primitives.LegIDImpl;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CAP scenario for sending out RRBCSM. */
public final class CapScenarioInitiateCallAttempt implements
        org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesCsCall.CapScenarioInitiateCallAttempt {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioInitiateCallAttempt.class);
    private CapSipCsCall call;
    private SipServletRequest invite;
    private Long invokeId;
    private List<BCSMEvent> defaultEvents;
    private int csID;

    public static CapScenarioInitiateCallAttempt start(CapSipCsCall call, SipServletRequest invite, Long invokeId,
            List<BCSMEvent> defaultEvents, int csID) {
        return new CapScenarioInitiateCallAttempt(call, invite, invokeId, defaultEvents, csID);
    }

    private CapScenarioInitiateCallAttempt(CapSipCsCall call, SipServletRequest invite, Long invokeId,
            List<BCSMEvent> defaultEvents, int csID) {
        this.call = call;
        this.invite = invite;
        this.invokeId = invokeId;
        this.defaultEvents = defaultEvents;
        this.csID = csID;
    }

    @Override
    public Long getInvokeId() {
        return invokeId;
    }

    @Override
    public void onSuccess(InitiateCallAttemptResponse response) {
        // we must already be locked on the call if the scenario receives control

        int legid = SipUtil.networkLegIdFromSdpId(SipSessionAttributes.LEG_ID.get(invite.getSession(), String.class));
        // create new call segment on success only
        call.getCallSegmentAssociation().initiateCallAttempt(legid, csID);

        // respond with 183 + SDP
        SipServletResponse resp = invite.createResponse(SipServletResponse.SC_SESSION_PROGRESS);
        SipUtil.prepareIcaResponseToAS(resp, invite.getRemoteHost());

        String xmlContent = Jss7ToXml.encode(response, "initiateCallAttemptResponse");
        String sdpContent = SipUtil.createSdpForLegs(invite.getSession());

        try {
            MimeMultipart mm;
            mm = new MultipartBuilder().addPartBody(SipConstants.CONTENTTYPE_CAP_XML_STRING, xmlContent)
                    .addPartBody(SipConstants.CONTENTTYPE_SDP_STRING, sdpContent).getResult();
            resp.setContent(mm, mm.getContentType());
        } catch (MessagingException | UnsupportedEncodingException e) {
            LOG.warn("Failed to create message body: {}", e.getMessage(), e);
        }

        String log = "Failed to send progress to ICA INVITE";
        if (SipUtil.supports100Rel(invite) && SipUtil.sendReliablyOrWarn(resp, log)) {
            call.getSipScenarios().add(
                    new Scenario("200 OK for PRACK on ICA leg", m -> m instanceof SipServletRequest
                            && "PRACK".equals(m.getMethod()) && m.getSession().equals(invite.getSession())
                            && !m.isCommitted(), (scen, message) -> {
                        scen.setFinished();
                        SipUtil.sendOrWarn(((SipServletRequest) message).createResponse(SipServletResponse.SC_OK),
                                "Failed to send 200 OK for PRACK");
                    }));
        } else {
            SipUtil.sendOrWarn(resp, log);
        }

        // TODO: check for something like "x-wcs-cps: start" that could set this to true
        // and allow the AS to send RRBCSM, ApplyCharging or else. For now it's fixed
        boolean waitForFurtherInstructions = false;
        if (waitForFurtherInstructions) {
            return;
        }

        LegID legID = new LegIDImpl(true, LegType.getInstance(legid));

        // send RRBCSM
        LOG.debug("Sending RRBCSM for ICA");
        try {
            // copy defaults, but override leg parameter, which is mandatory in this case
            List<BCSMEvent> events = defaultEvents
                    .stream()
                    .map(e -> new BCSMEventImpl(e.getEventTypeBCSM(), e.getMonitorMode(), legID, e
                            .getDpSpecificCriteria(), e.getAutomaticRearm())).collect(Collectors.toList());
            call.getCapOutgoingRequestScenarios().add(CapScenarioRRBCSM.start(call, events));

            // optimistically add ERBCSM scenario even before success timeout occurs, but only if not present already
            if (!call.getCapIncomingRequestScenarios().stream().anyMatch(s -> s instanceof CapScenarioIncomingERBCSM)) {
                call.getCapIncomingRequestScenarios().add(new CapScenarioIncomingERBCSM());
            }

        } catch (CAPException e) {
            LOG.warn("Failed to send CAP RRBCSM: ", e.getMessage(), e);
        }

        // send continueWithArgument for this leg
        LOG.debug("Sending CWA");
        try {
            call.getCapOutgoingRequestScenarios().add(CapScenarioContinueWithArgument.start(call, legID));
            call.getCallSegmentAssociation().getCallSegmentOfLeg(legid).continueCS();
        } catch (CAPException e) {
            LOG.warn("Failed to send CAP CWA: ", e.getMessage(), e);
        }
    }

    @Override
    public void onFailureIndicated(CAPErrorMessage error, Problem problem) {
        // we must already be locked on the call if the scenario receives control
        LOG.warn("ICA error for {}: {} / {}", call, error, problem);

        // respond with 4xx
        SipServletResponse resp = invite.createResponse(SipServletResponse.SC_BAD_REQUEST);
        SipUtil.createAndSetWarningHeader(resp, "Error response for initiateCallAttempt");
        SipUtil.sendOrWarn(resp, "Failed to send response to ICA INVITE");
    }

    @Override
    public void onFailureTimeout() {
        // we must already be locked on the call if the scenario receives control
        LOG.warn("ICA timeout for {}", call);

        // respond with 408 timeout
        SipServletResponse resp = invite.createResponse(SipServletResponse.SC_REQUEST_TIMEOUT);
        SipUtil.createAndSetWarningHeader(resp, "Invoke timeout for initiateCallAttempt");
        SipUtil.sendOrWarn(resp, "Failed to send response to ICA INVITE");
    }
}
