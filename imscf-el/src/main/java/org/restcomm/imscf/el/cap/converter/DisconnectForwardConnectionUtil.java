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
import org.restcomm.imscf.el.cap.call.CapSipCsCall;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for sending DFC or DFCwA message. */
public final class DisconnectForwardConnectionUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DisconnectForwardConnectionUtil.class);

    private DisconnectForwardConnectionUtil() {
    }

    public static void sendDisconnectForwardConnection(CapSipCsCall call, int callSegmentId) {
        if (call.getCallSegmentAssociation().getCallSegmentCount() > 1) {
            CallSegment cs = call.getCallSegmentAssociation().getCallSegment(callSegmentId);
            LOG.debug("Multiple call segments present, sending disconnectForwardConnectionWithArgument for {}",
                    cs.getName());
            try {
                call.getCapOutgoingRequestScenarios().add(
                        CapScenarioDisconnectForwardConnectionWithArgument.start(call, cs.getId(), null));
            } catch (CAPException e) {
                LOG.warn("Failed to create DisconnectForwardConnectionWithArgument: {}", e.getMessage(), e);
                return;
            }
        } else {
            LOG.debug("Only 1 call segment present, sending disconnectForwardConnection");
            try {
                call.getCapOutgoingRequestScenarios().add(CapScenarioDisconnectForwardConnection.start(call));
            } catch (CAPException e) {
                LOG.warn("Failed to create DisconnectForwardConnection: {}", e.getMessage(), e);
                return;
            }
        }
    }
}
