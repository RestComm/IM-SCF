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

import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.sip.SipMessageBodyUtil;
import org.restcomm.imscf.el.sip.SipMessageDetector;

import java.io.IOException;
import javax.servlet.sip.SipServletMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether a SIP message contains the same body as the original message and is received on a different leg.
 */
public class SipForwardedContentDetector implements SipMessageDetector {

    private static final Logger LOG = LoggerFactory.getLogger(SipForwardedContentDetector.class);
    protected final SipServletMessage originalMessage;

    public SipForwardedContentDetector(SipServletMessage original) {
        if (original == null)
            throw new IllegalArgumentException("original SipServletMessage is null");
        this.originalMessage = original;
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        String oLegID = SipSessionAttributes.LEG_ID.get(originalMessage.getSession(), String.class);
        String nLegID = SipSessionAttributes.LEG_ID.get(msg.getSession(), String.class);
        if (oLegID != null && oLegID.equals(nLegID)) {
            LOG.trace("same leg (should be different)");
            return false;
        }
        Object obody, nbody;
        try {
            obody = originalMessage.getContent();
            nbody = msg.getContent();
        } catch (IOException e) {
            LOG.trace("error comparing bodies");
            return false;
        }

        // strictOrder==false to allow SDP/CAP content to switch and additional content to be added.
        boolean bodiesMatch = SipMessageBodyUtil.compareContents(obody, nbody, false, true);
        LOG.trace("Bodies{}match.", bodiesMatch ? " " : " don't ");
        return bodiesMatch;
    }
}
