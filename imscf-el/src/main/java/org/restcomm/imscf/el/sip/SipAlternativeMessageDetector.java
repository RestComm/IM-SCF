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
package org.restcomm.imscf.el.sip;

import javax.servlet.sip.SipServletMessage;

/** SipMessageDetector implementation that selects a match from multiple detectors.
 *  During {@link #accept(SipServletMessage)}, detectors are tested in order and
 *  the first accepting detector is saved. This detector is thus stateful between
 *  subsequent calls to accept. The saved detector can be queried in the scenario
 *  message handler after a successful accept.
 *  Detectors should ideally detect mutually exclusive messages. */
public class SipAlternativeMessageDetector implements SipMessageDetector {

    SipMessageDetector[] detectors;
    int savedIndex;

    public SipAlternativeMessageDetector(SipMessageDetector... detectors) {
        this.detectors = detectors;
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        savedIndex = -1; // make sure no false result is stored
        for (int i = 0; i < detectors.length; i++) {
            if (detectors[i].accept(msg)) {
                savedIndex = i;
                return true;
            }
        }
        return false;
    }

    public int getSavedIndex() {
        return savedIndex;
    }

    public SipMessageDetector getSavedDetector() {
        if (savedIndex < 0)
            throw new IllegalStateException("getSavedDetector may only be called after a successful match");
        else
            return detectors[savedIndex];
    }
}
