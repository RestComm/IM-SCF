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

import java.util.Arrays;

/** SIP response classes for matching status codes. */
public enum SipResponseClass {
    PROVISIONAL(101, 199), SUCCESS(200, 299), REDIRECT(300, 399), ERROR_REQUEST(400, 499), ERROR_SERVER(500, 599), ERROR_GLOBAL(
            600, 699), ERROR(ERROR_REQUEST, ERROR_SERVER, ERROR_GLOBAL), FINAL(SUCCESS, REDIRECT, ERROR), ALL();

    private final int min, max;
    private final SipResponseClass[] refs;

    private SipResponseClass(int min, int max) {
        this.min = min;
        this.max = max;
        refs = null;
    }

    private SipResponseClass(SipResponseClass... others) {
        this.min = -1;
        this.max = -1;
        this.refs = others;
    }

    public boolean matches(int status) {
        if (refs != null) {
            return refs.length == 0 || Arrays.stream(refs).anyMatch(r -> r.matches(status));
        } else {
            return min <= status && status <= max;
        }
    }
}
