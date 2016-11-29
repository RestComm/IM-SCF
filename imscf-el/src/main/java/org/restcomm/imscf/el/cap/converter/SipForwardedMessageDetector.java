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

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether a SIP message is just a replay of the original message on another leg, meaning a continue request.
 */
public class SipForwardedMessageDetector extends SipForwardedContentDetector {

    private static final Logger LOG = LoggerFactory.getLogger(SipForwardedMessageDetector.class);

    public SipForwardedMessageDetector(SipServletMessage original) {
        super(original);
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        if (originalMessage instanceof SipServletResponse) {
            if (!(msg instanceof SipServletResponse)) {
                LOG.trace("original is response, msg is request");
                return false;
            }
            SipServletResponse oresp = (SipServletResponse) originalMessage;
            SipServletResponse nresp = (SipServletResponse) msg;
            if (oresp.getStatus() != nresp.getStatus()) {
                LOG.trace("different status code");
                return false;
            }
        } else if (!(msg instanceof SipServletRequest)) {
            LOG.trace("original is request, msg is response");
            return false;
        }
        if (!originalMessage.getMethod().equals(msg.getMethod())) {
            LOG.trace("different method");
            return false;
        }

        return super.accept(msg);
    }
}
