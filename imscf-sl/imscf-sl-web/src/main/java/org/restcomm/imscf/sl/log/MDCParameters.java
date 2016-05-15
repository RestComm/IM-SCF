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
package org.restcomm.imscf.sl.log;

import org.slf4j.MDC;

/**
 * Handles SL's log MDC.
 */
public final class MDCParameters {

    private MDCParameters() {
        // This class cannot be instantiated.
    }

    /**
     * Possible variables in SL's MDC.
     * @author Miklos Pocsaji
     *
     */
    public enum Parameter {
        IMSCF_CALLID("imscf.callid");

        private final String key;

        private Parameter(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static void toMDC(Parameter p, String value) {
        MDC.put(p.getKey(), value);
    }

    public static void clearMDC() {
        for (Parameter p : Parameter.values()) {
            MDC.remove(p.getKey());
        }
    }
}
