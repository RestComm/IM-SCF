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

import org.restcomm.imscf.el.sip.SipMessageDetector;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;

/** Detector for empty bodied reliable provisional responses for a request. */
public class SipEmpty1xxRelDetector extends Sip1xxRelDetector implements SipMessageDetector {

    public SipEmpty1xxRelDetector(SipServletRequest req) {
        super(req);
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        return super.accept(msg) && msg.getContentType() == null;
    }

}
