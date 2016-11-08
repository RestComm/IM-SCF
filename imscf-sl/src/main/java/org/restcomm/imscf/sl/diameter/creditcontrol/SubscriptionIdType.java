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
package org.restcomm.imscf.sl.diameter.creditcontrol;

/**
 * Class for storing subscription parameters.
 */
public enum SubscriptionIdType {

    END_USER_MSISDN(0L), END_USER_IMSI(1L);

    private final Long code;

    SubscriptionIdType(Long code) {
        this.code = code;
    }

    public Long code() {
        return code;
    }

    public static SubscriptionIdType getCCRequestTypeByString(String str) {
        if ("END_USER_MSISDN".equals(str)) {
            return SubscriptionIdType.END_USER_MSISDN;
        } else if ("END_USER_IMSI".equals(str)) {
            return SubscriptionIdType.END_USER_IMSI;
        }
        return null;
    }

    public static SubscriptionIdType getCCRequestTypeByCode(Long code) {
        if (code == 0L) {
            return SubscriptionIdType.END_USER_MSISDN;
        } else if (code == 1L) {
            return SubscriptionIdType.END_USER_IMSI;
        }
        return null;
    }

    @Override
    public String toString() {
        if (code == 0) {
            return "END_USER_MSISDN";
        } else if (code == 1) {
            return "END_USER_IMSI";
        }
        return "";
    }

}
