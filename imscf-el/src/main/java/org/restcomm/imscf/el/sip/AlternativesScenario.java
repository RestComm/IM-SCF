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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scenario implementation that runs the appropriate message handler based on which message
 * detector has accepted the message.
 */
public final class AlternativesScenario extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(AlternativesScenario.class);

    public static AlternativesScenario create(String name, SipMessageDetector[] detectors, SipMessageHandler[] handlers) {
        Objects.requireNonNull(detectors, "detectors[] cannot be null");
        Objects.requireNonNull(handlers, "handlers[] cannot be null");
        if (detectors.length != handlers.length)
            throw new IllegalArgumentException("detectors[] and handlers[] length must be the same");

        SipAlternativeMessageDetector detector = new SipAlternativeMessageDetector(detectors);
        SipMessageHandler handler = (s, msg) -> {
            int h = detector.getSavedIndex();
            LOG.trace("Giving message to handler #{}", h);
            handlers[h].handleMessage(s, msg);
        };
        return new AlternativesScenario(name, detector, handler);
    }

    private AlternativesScenario(String name, SipMessageDetector detector, SipMessageHandler handler) {
        super(name, detector, handler);
    }

}
