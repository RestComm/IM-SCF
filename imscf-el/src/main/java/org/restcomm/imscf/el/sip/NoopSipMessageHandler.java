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

/** Simple message handler that does nothing. */
public final class NoopSipMessageHandler implements SipMessageHandler {

    /** Since this implementation is completely stateless, it is inherently thread-safe and can be shared among threads
     * or even among calls. Thus this public instance can be used instead of creating new instances every time.
     * Note: this property might not hold for any subclasses.
     */
    public static final NoopSipMessageHandler SHARED_INSTANCE = new NoopSipMessageHandler();

    private NoopSipMessageHandler() {
        // protected, so that this class can only be used through the SHARED_INSTANCE
    }

    @Override
    public void handleMessage(Scenario scenario, SipServletMessage msg) {
        // NOOP
    }

}
