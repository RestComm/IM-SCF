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

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SpecializedResourceReportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CAP scenario for handling incoming specializedResourceReport messages. */
public class CapScenarioSpecializedResourceReport implements
        org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesMrf.CapScenarioSpecializedResourceReport {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioSpecializedResourceReport.class);

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public void onRequest(SpecializedResourceReportRequest srr) {
        CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (CapSipCsCall call = (CapSipCsCall) cs.getCapCall(srr.getCAPDialog().getLocalDialogId())) {
            // clean-up of unnecessary scenarios
            boolean automaticDisconnection = !call.getSipScenarios().stream()
                    .anyMatch(s -> s instanceof SipScenarioMrfDFC);
            if (automaticDisconnection) {
                /** Automatic disconnection is in effect (disconnectFromIPForbidden FALSE) */
                /** In this case, we have to remove the PA and PACUI scenarios */
                call.getSipScenarios().removeIf(
                        ss -> ss instanceof SipScenarioPlayAnnouncement
                                || ss instanceof SipScenarioPromptAndCollectUserInformation);
                call.getCapIncomingRequestScenarios().removeIf(caps -> caps instanceof CapScenarioIncomingERBCSMMrf);
            }

            LOG.debug("specializedResourceReport arrived: {}", srr);

            SipSession sip = SipUtil.findSipSessionForLegID(call, "mrf");
            SipServletRequest msgInfo = sip.createRequest("INFO");

            try {
                msgInfo.setContent(Jss7ToXml.encode(srr, "specializedResourceReport"),
                        SipConstants.CONTENTTYPE_CAP_XML_STRING);

                call.getSipScenarios().add(WaitForFinalAnswerScenario.start("Wait for SRR INFO OK", msgInfo));
                call.queueMessage(msgInfo);

                if (automaticDisconnection) {
                    // The IMSCF should initiate a BYE request on the MRF leg, when automatic disconnection is requested
                    SipServletRequest msgBye = sip.createRequest("BYE");
                    call.getSipScenarios().add(WaitForFinalAnswerScenario.start("Wait for MRF BYE OK", msgBye));
                    call.queueMessage(msgBye);
                }

            } catch (IOException e) {
                LOG.warn("Couldn't construct/send message to AS", e);
            }

        }

    }

}
