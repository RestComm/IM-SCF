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
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.WaitForFinalAnswerScenario;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.mobicents.protocols.ss7.cap.api.primitives.AChChargingAddress;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingReportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Incoming CAP request scenario for handling applyChargingReport requests. */
public class CapScenarioApplyChargingReport implements
        org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesCsCall.CapScenarioApplyChargingReport {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioApplyChargingReport.class);

    @Override
    public boolean isFinished() {
        // a single scenario is used throughout the call
        return false;
    }

    @Override
    public void onRequest(ApplyChargingReportRequest acr) {
        CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (CapSipCsCall call = (CapSipCsCall) cs.getCapCall(acr.getCAPDialog().getLocalDialogId())) {
            LOG.debug("applyChargingReport arrived: {}", acr);
            String legID;
            switch (call.getCapDialog().getApplicationContext().getVersion()) {
            case version4:
                AChChargingAddress aca = acr.getTimeDurationChargingResult().getAChChargingAddress();
                if (aca == null) {
                    LOG.debug("CAP4 but AChChargingAddress is not present, assuming default value of L1");
                    // default is legid:receivingsideid:leg1
                    legID = "L1";
                } else if (aca.getLegID() != null) {
                    int code = Optional.ofNullable(aca.getLegID().getReceivingSideID())
                            .orElse(aca.getLegID().getSendingSideID()).getCode();
                    legID = "L" + code;
                    LOG.debug("CAP4 AChChargingAddress contained legID: {}", legID);
                } else {
                    LOG.warn("AChChargingAddress.srfConnection not supported, sending on L1");
                    // send on L1
                    legID = "L1";
                }
                break;
            case version3:
            case version2:
                LOG.debug("CAP2/3, using leg from partyToCharge");
                // no ACA, use PTC
                legID = "L" + acr.getTimeDurationChargingResult().getPartyToCharge().getReceivingSideID().getCode();
                break;
            default:
                throw new AssertionError();
            }

            SipSession sip = SipUtil.findSipSessionForLegID(call, legID);
            if (sip == null) {
                LOG.debug("SIP leg {} terminated or disconnecting already, cannot send ACR INFO.", legID);
                return;
            }

            SipServletRequest msg = sip.createRequest("INFO");

            try {
                msg.setContent(Jss7ToXml.encode(acr, "applyChargingReport"), SipConstants.CONTENTTYPE_CAP_XML_STRING);

                call.getSipScenarios().add(WaitForFinalAnswerScenario.start("Wait for ACR INFO OK", msg));
                call.queueMessage(msg);

            } catch (IOException e) {
                LOG.warn("Couldn't construct/send message to AS", e);
            }

        }
    }
}
