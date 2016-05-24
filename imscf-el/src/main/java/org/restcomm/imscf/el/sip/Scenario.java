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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario base. */
public class Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(Scenario.class);

    private final String name;
    private final SipMessageDetector detector;
    private final SipMessageHandler handler;
    private boolean finished;

    public Scenario(String name, SipMessageDetector detector, SipMessageHandler handler) {
        this.name = name;
        this.detector = detector;
        this.handler = handler;
    }

    public final String getName() {
        return name;
    }

    public final boolean handleMessage(SipServletMessage msg) {
        if (detector.accept(msg)) {
            LOG.debug("Handling message in {}", name);
            handler.handleMessage(this, msg);
            return true;
        } else {
            LOG.debug("Not handling message in {}", name);
            return false;
        }
    }

    public final void setFinished() {
        finished = true;
    }

    public final boolean isFinished() {
        return finished;
    }

}
