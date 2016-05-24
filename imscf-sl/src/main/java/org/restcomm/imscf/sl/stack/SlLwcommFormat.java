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
package org.restcomm.imscf.sl.stack;

import org.restcomm.imscf.common.SccpSerializer;

import java.util.regex.Pattern;

import org.mobicents.protocols.ss7.sccp.message.SccpDataMessage;

/** Utility class for formatting LWCOMM messages sent by the SL. */
public final class SlLwcommFormat {
    private static final String SCCP_TO_EL = "" + //
            "Target: SUA\r\n" + //
            "Content: SccpDataMessage\r\n" + //
            "\r\n" + //
            "{sccp}";

    private static final String ELROUTER_QUERY = "" + //
            // lwcomm group-id is a newly generated ImscfCallId
            "Target: ELRouter/query\r\n" + //
            "Content: SccpDataMessage\r\n" + //
            "\r\n" + //
            "{sccp}";

    public static final Pattern SUCCESS_STATUS_LINE_PATTERN = Pattern
            .compile("Status: success,imscfCallId=(?<imscfCallId>[^,]+),node=(?<node>[^,]+)");
    private static final String ELROUTER_SUCCESS_RESPONSE = "" + //
            // lwcomm group-id is the ImscfCallId of the query, which is unrelated to the value in the response itself
            "Target: ELRouter/response\r\n" + //
            "Status: success,imscfCallId={imscfcallid},node={node}\r\n" + //
            "Content: SccpDataMessage\r\n" + //
            "\r\n" + //
            "{sccp}";

    private static final String ELROUTER_NOTFOUND_RESPONSE = "" + //
            // lwcomm group-id is the ImscfCallId of the query
            "Target: ELRouter/response\r\n" + //
            "Status: notfound\r\n" + //
            "Content: SccpDataMessage\r\n" + //
            "\r\n" + //
            "{sccp}";

    private SlLwcommFormat() {
    }

    public static String formatSccpToEL(SccpDataMessage msg) {
        return SCCP_TO_EL.replace("{sccp}", SccpSerializer.serialize(msg));
    }

    public static String formatELRouterQuery(SccpDataMessage msg) {
        return ELROUTER_QUERY.replace("{sccp}", SccpSerializer.serialize(msg));
    }

    public static String formatELRouterSuccessResponse(String imscfCallId, String nodeName, String sccpData) {
        return ELROUTER_SUCCESS_RESPONSE.replace("{imscfcallid}", imscfCallId).replace("{node}", nodeName)
                .replace("{sccp}", sccpData);
    }

    public static String formatELRouterNotfoundResponse(String sccpData) {
        return ELROUTER_NOTFOUND_RESPONSE.replace("{sccp}", sccpData);
    }

}
