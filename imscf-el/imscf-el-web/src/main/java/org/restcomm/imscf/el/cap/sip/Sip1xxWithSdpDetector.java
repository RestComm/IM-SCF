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
package org.restcomm.imscf.el.cap.sip;

import org.restcomm.imscf.el.sip.SipMessageBodyUtil;

import java.io.IOException;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Detector for reliable provisional responses for a request which checks whether the contents are equal. */
public class Sip1xxWithSdpDetector extends Sip1xxRelDetector {

    private static final Logger LOG = LoggerFactory.getLogger(Sip1xxWithSdpDetector.class);

    /** The message which holds the sdp which should be compared to the incoming message's sdp. **/
    private SipServletMessage originalMessage;

    public Sip1xxWithSdpDetector(SipServletRequest originalRequest, SipServletMessage originalMessage) {
        super(originalRequest);
        this.originalMessage = originalMessage;
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        Object originalMsgContent, incomingMsgContent;
        try {
            originalMsgContent = originalMessage.getContent();
            incomingMsgContent = msg.getContent();
        } catch (IOException e) {
            LOG.trace("error comparing bodies");
            return false;
        }
        boolean bodiesMatch = SipMessageBodyUtil.compareContents(originalMsgContent, incomingMsgContent, false, true);
        LOG.trace("Bodies{}match.", bodiesMatch ? " " : " don't ");
        return bodiesMatch && super.accept(msg);
    }

}
