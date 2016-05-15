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
package org.restcomm.imscf.sl.overload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.message.SccpDataMessage;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for overload protection handling in Signaling Layer server.
 * @author Miklos Pocsaji
 *
 */
public final class SlOverloadUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlOverloadUtil.class);
    /** Byte sequence for TCAP U-ABORT.
     * The bytes at index 4-7 should be filled with the destination transaction id.
     */
    // private static final byte[] TCAP_PABORT_BYTES = { 103, 9, 73, 4, 0, 0, 0, 0, 74, 1, 4 };
    private static final byte[] TCAP_UABORT_BYTES = { 0x67, 0x06, 0x49, 0x04, 0x00, 0x00, 0x00, 0x00 };
    private static SccpProvider sccpProvider;

    private SlOverloadUtil() {
        // This class cannot be instantiated from the outside.
    }

    public static void configure(SccpProvider sccpProvider) {
        SlOverloadUtil.sccpProvider = sccpProvider;
    }

    public static void destroy() {
        sccpProvider = null;
    }

    public static void rejectBeginFromNetworkWithPAbort(SccpDataMessage tcapBegin, Long tcapId) {
        byte[] data = Arrays.copyOf(TCAP_UABORT_BYTES, TCAP_UABORT_BYTES.length);
        ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
        bb.putLong(tcapId);
        byte[] tcapIdBytes = bb.array();
        LOGGER.trace("destination transaction ID for abort: {}, as bytes: {}", tcapId, Arrays.toString(tcapIdBytes));
        // copy destination transaction id to its place
        System.arraycopy(tcapIdBytes, 4, data, 4, 4);
        LOGGER.trace("TCAP abort as bytes: {}", Arrays.toString(data));

        SccpAddress calling = tcapBegin.getCalledPartyAddress();
        SccpAddress called = tcapBegin.getCallingPartyAddress();

        // If our side was addressed with a called party address which contained a GT but the
        // routing indicator was not "route on GT" then we change the routing indicator.
        if (calling.getGlobalTitle() != null
                && calling.getAddressIndicator().getRoutingIndicator() != RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE) {
            calling = sccpProvider.getParameterFactory().createSccpAddress(
                    RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, calling.getSignalingPointCode(),
                    calling.getGlobalTitle(), calling.getSubsystemNumber());
        }
        SccpDataMessage uabort = sccpProvider.getMessageFactory().createDataMessageClass1(called, calling, data, 0,
                calling.getSubsystemNumber(), false, null, null);
        try {
            sccpProvider.send(uabort);
        } catch (IOException e) {
            LOGGER.warn("Error sending out overload protection P-ABORT.", e);
        }
    }
}
