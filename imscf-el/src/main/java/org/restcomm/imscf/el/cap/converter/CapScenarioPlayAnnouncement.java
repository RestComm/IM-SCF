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

import org.restcomm.imscf.el.cap.CapErrorCodeMapper;
import org.restcomm.imscf.el.cap.call.CAPCSCall;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.ReasonHeader;
import org.restcomm.imscf.el.sip.WaitForFinalAnswerScenario;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorCode;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PlayAnnouncementRequest;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CAP scenario for playing announcement. */
public final class CapScenarioPlayAnnouncement implements
        org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesMrf.CapScenarioPlayAnnouncement {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioPlayAnnouncement.class);
    private Long invokeId;
    private CAPCSCall call;

    public static CapScenarioPlayAnnouncement start(CAPCSCall call, PlayAnnouncementRequest playAnnouncement)
            throws CAPException {
        Long invokeId = call.getCapDialog().addPlayAnnouncementRequest(playAnnouncement.getInformationToSend(),
                playAnnouncement.getDisconnectFromIPForbidden(),
                playAnnouncement.getRequestAnnouncementCompleteNotification(), playAnnouncement.getExtensions(),
                playAnnouncement.getCallSegmentID(), playAnnouncement.getRequestAnnouncementStartedNotification());
        // send this immediately
        call.getCapDialog().send();
        return new CapScenarioPlayAnnouncement(invokeId, call);

    }

    private CapScenarioPlayAnnouncement(Long invokeId, CAPCSCall call) {
        this.invokeId = invokeId;
        this.call = call;
    }

    @Override
    public Long getInvokeId() {
        return invokeId;
    }

    @Override
    public void onSuccessTimeout() {
        LOG.debug("CAP PlayAnnouncement operation success.");
    }

    @Override
    public void onFailureIndicated(CAPErrorMessage error, Problem problem) {
        if (error.getErrorCode() == CAPErrorCode.unavailableResource) {
            LOG.debug("CAP PlayAnnouncement failed, error {}, problem {}", error, problem);
        } else {
            LOG.warn("CAP PlayAnnouncement failed, error {}, problem {}", error, problem);
        }
        SipSession sip = SipUtil.findSipSessionForLegID((CapSipCsCall) call, "mrf");

        if (sip == null || SipUtil.isUaTerminated(sip)) {
            // e.g. race condition where a disconnect arrives and causes the PA to be rejected
            LOG.debug("MRF leg already terminated, cannot send error in INFO message");
            return;
        }

        SipServletRequest msg = sip.createRequest("INFO");

        ReasonHeader rh = new ReasonHeader("SIP", error.getErrorCode().intValue(),
                CapErrorCodeMapper.errorCodeAsString(error.getErrorCode().intValue()));
        rh.insertAsHeader(msg);

        ((CapSipCsCall) call).getSipScenarios().add(WaitForFinalAnswerScenario.start("Wait for PA error INFO OK", msg));
        ((CapSipCsCall) call).queueMessage(msg);
    }

}
