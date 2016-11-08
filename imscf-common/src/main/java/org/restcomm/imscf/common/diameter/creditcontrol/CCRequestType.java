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
package org.restcomm.imscf.common .diameter.creditcontrol;

/**
 * Class for storing Diameter CCR types.
 */
public enum CCRequestType {
    BALANCE("INITIAL_REQUEST"), DEBIT("TERMINATION_REQUEST");

    private final Long code;
    private final String diameterString;

    private CCRequestType(String diameterString) {
        if ("INITIAL_REQUEST".equals(diameterString)) {
            this.code = 1L;
            this.diameterString = "BALANCE";
        } else if ("TERMINATION_REQUEST".equals(diameterString)) {
            this.diameterString = "DEBIT";
            this.code = 3L;
        } else {
            this.diameterString = null;
            this.code = null;
        }
    }

    CCRequestType(Long code) {
        this.code = code;
        if (code == 1L) {
            diameterString = "BALANCE";
        } else if (code == 3L) {
            diameterString = "DEBIT";
        } else {
            this.diameterString = null;
        }
    }

    public static CCRequestType getCCRequestTypeByString(String str) {
        if ("BALANCE".equals(str) || "INITIAL_REQUEST".equals(str)) {
            return CCRequestType.BALANCE;
        } else if ("DEBIT".equals(str) || "TERMINATION_REQUEST".equals(str)) {
            return CCRequestType.DEBIT;
        }
        return null;
    }

    public static CCRequestType getCCRequestTypeByCode(Long code) {
        if (code == 1L) {
            return CCRequestType.BALANCE;
        } else if (code == 3L) {
            return CCRequestType.DEBIT;
        }
        return null;
    }

    public long code() {
        return code;
    }

    @Override
    public String toString() {
        return diameterString;
    }

}
