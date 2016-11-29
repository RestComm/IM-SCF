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

import org.restcomm.imscf.el.cap.CAPModuleBase;
import org.restcomm.imscf.el.cap.ConnectToResourceArg;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.sip.SipSession;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for common handling of connectToResource operation. */
public final class ConnectToResourceUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectToResourceUtil.class);
    private static final Pattern MRF_ALIAS_PATTERN = Pattern.compile("^s=(.*)$", Pattern.MULTILINE);

    private ConnectToResourceUtil() {
    }

    public static void connectToMrf(CapSipCsCall call, int callSegmentId, List<BCSMEvent> edpsForMrf, String sdp) {
        // requestReportBCSMEvent
        if (edpsForMrf != null && !edpsForMrf.isEmpty()) {
            LOG.debug("Sending RRBCSM");
            try {
                call.getCapOutgoingRequestScenarios().add(CapScenarioRRBCSM.start(call, edpsForMrf));
            } catch (CAPException e) {
                LOG.warn("Failed to create RequestReportBCSM: {}", e.getMessage(), e);
                // TODO ???
                return;
            }
        } else {
            LOG.debug("Not sending RRBCSM");
        }

        // connectToResource
        Matcher m = MRF_ALIAS_PATTERN.matcher(sdp);
        String sParam = m.find() ? m.group(1) : null;
        // we have the S param from the sdp - based on this, we can find the matching
        ConnectToResourceArg ctra = ((CAPModuleBase) call.getCapModule()).getConnectToResourceArgsForMrfAliases().get(
                sParam);

        ctra.setCallSegmentID(callSegmentId);
        try {
            LOG.debug("Sending ConnectToResource");
            call.getCapOutgoingRequestScenarios().add(CapScenarioConnectToResource.start(call, ctra));
        } catch (CAPException e) {
            LOG.warn("Failed to create ConnectToResource: {}", e.getMessage(), e);
            // TODO ???
            return;
        }

        call.getCallSegmentAssociation().getCallSegment(callSegmentId).connectToResource();

        SipSession sipMrf = SipUtil.findSipSessionForLegID(call, "mrf");
        SipSessionAttributes.MRF_CS_ID.set(sipMrf, callSegmentId);

        // make sure that normal (call) and mrf erbcsm handler scenarios are added in the required order:
        // mrf first, normal after
        call.getCapIncomingRequestScenarios().removeIf(
                s -> s instanceof CapScenarioIncomingERBCSM || s instanceof CapScenarioIncomingERBCSMMrf);
        call.getCapIncomingRequestScenarios().add(new CapScenarioIncomingERBCSMMrf());
        call.getCapIncomingRequestScenarios().add(new CapScenarioIncomingERBCSM());

        call.add(SipScenarioMrfDFC.start());
        call.getSipScenarios().add(SipScenarioPlayAnnouncement.start());
        call.getSipScenarios().add(SipScenarioPromptAndCollectUserInformation.start());
    }
}
