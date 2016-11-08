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

import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.sip.SipResponseClass;
import org.restcomm.imscf.el.sip.SipResponseDetector;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether a message is an error response to the original INVITE sent to the AS. Only messages without bodies are accepted.
 */
public class SipInitialReleaseDetector extends SipResponseDetector {

    private static final Logger LOG = LoggerFactory.getLogger(SipInitialReleaseDetector.class);

    public SipInitialReleaseDetector(SipServletRequest invite) {
        super(invite, SipResponseClass.ERROR);
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        if (msg.getContentType() != null) {
            LOG.trace("Message has body, not accepting");
            return false;
        }

        if (SipSessionAttributes.UAC_DISCONNECTED.get(msg.getSession(), Object.class) != null) {
            LOG.trace("UAC disconnect, not accepting message");
            return false;
        }

        return super.accept(msg);
    }

}
