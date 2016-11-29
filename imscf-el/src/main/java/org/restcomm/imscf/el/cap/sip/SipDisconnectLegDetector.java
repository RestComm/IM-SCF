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

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

/**
 * Checks whether a message is a request for disconnectLeg.
 */
public class SipDisconnectLegDetector extends SipBodyContentDetector {

    /** This implementation is stateless and thread safe. This instance can be shared among threads/calls. */
    public static final SipDisconnectLegDetector SHARED_INSTANCE = new SipDisconnectLegDetector();

    protected SipDisconnectLegDetector() {
        super("<disconnectLeg");
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        // Accept incoming INFO/BYE/CANCEL requests or INVITE answers that contain a disconnectLeg XML body

        if (msg instanceof SipServletRequest) {
            switch (msg.getMethod()) {
            case "INFO":
            case "BYE":
            case "CANCEL":
                return super.accept(msg);
            default:
                return false;
            }
        } else if (msg instanceof SipServletResponse && SipUtil.isErrorResponseForInitialInvite(msg)) {
            return super.accept(msg);
        }

        return false;
    }
}
