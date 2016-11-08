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

import org.restcomm.imscf.el.sip.SipMessageBodyUtil;
import org.restcomm.imscf.el.sip.SipMessageDetector;

import java.io.IOException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether a message is an incoming reINVITE that has an SDP indicating a leg manipulation operation (containing i= line).
 */
public class SipDetectorLegManipulation implements SipMessageDetector {

    private static final Logger LOG = LoggerFactory.getLogger(SipDetectorLegManipulation.class);

    /** Stores the SDP after a successful detection, so that the message handler can access it without parsing the message again. */
    private String sdp;

    public String getSdp() {
        return sdp;
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        // Accept incoming reINVITE requests that contain an SDP, but nothing more

        sdp = null;

        if (!msg.getMethod().equals("INVITE") || !(msg instanceof SipServletRequest)
                || ((SipServletRequest) msg).isInitial())
            return false;

        if (!SipConstants.CONTENTTYPE_SDP.match(msg.getContentType()))
            return false;

        try {
            sdp = SipMessageBodyUtil.convertToString(msg.getContent());
        } catch (IOException e) {
            LOG.warn("Failed to read SDP body");
        }
        return sdp != null;
    }
}
