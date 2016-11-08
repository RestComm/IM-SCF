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
package org.restcomm.imscf.el.cap.sip;

import org.restcomm.imscf.el.sip.SipBodyContentDetector;
import org.restcomm.imscf.el.sip.SipMessageBodyUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;

/**
 * Checks whether a message is an ICA INVITE.
 */
public class SipInitiateCallAttemptDetector extends SipBodyContentDetector {

    public SipInitiateCallAttemptDetector() {
        super("initiateCallAttempt");
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        // Accept incoming INVITE requests if they are initial and:
        // - they contain only a no connection SDP; or
        // - they contain a no connection SDP + an ICA XML
        // Don't accept reINVITEs, as a reINVITE with a no connection SDP means a DFC operation

        if (!msg.getMethod().equals("INVITE") || !(msg instanceof SipServletRequest)
                || !((SipServletRequest) msg).isInitial())
            return false;

        // ignore SDP if an ICA XML is present
        if (super.accept(msg))
            return true;

        List<Object> sdps = SipMessageBodyUtil.findContent(msg, SipConstants.CONTENTTYPE_SDP);
        if (sdps.isEmpty() || sdps.size() > 1)
            return false;
        Object sdp = sdps.get(0);
        if (sdp instanceof String) {
            return SipUtil.isNoConnectionSdp((String) sdp);
        } else if (sdp instanceof byte[]) {
            return SipUtil.isNoConnectionSdp(new String((byte[]) sdp, StandardCharsets.US_ASCII));
        }

        return false;
    }
}
