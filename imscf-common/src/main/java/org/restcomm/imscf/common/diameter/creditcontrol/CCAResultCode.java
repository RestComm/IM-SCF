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
package org.restcomm.imscf.common .diameter.creditcontrol;

/**
 * Class for storing Diameter CCA result codes.
 */
public enum CCAResultCode {
    SUCCESS("DIAMETER_SUCCESS"), END_USER_SERVICE_DENIED("DIAMETER_END_USER_SERVICE_DENIED"), TECHNICAL_ERROR(
            "DIAMETER_RATING_FAILED");

    private final Long code;
    private final String diameterString;

    CCAResultCode(String diameterString) {
        this.diameterString = diameterString;
        if ("DIAMETER_SUCCESS".equals(diameterString)) {
            this.code = 2001L;
        } else if ("DIAMETER_END_USER_SERVICE_DENIED".equals(diameterString)) {
            this.code = 4010L;
        } else if ("DIAMETER_RATING_FAILED".equals(diameterString)) {
            this.code = 5031L;
        } else {
            this.code = null;
        }
    }

    public static CCAResultCode getCCRequestTypeByString(String str) {
        if ("SUCCESS".equals(str)) {
            return CCAResultCode.SUCCESS;
        } else if ("END_USER_SERVICE_DENIED".equals(str)) {
            return CCAResultCode.END_USER_SERVICE_DENIED;
        } else if ("TECHNICAL_ERROR".equals(str)) {
            return CCAResultCode.TECHNICAL_ERROR;
        }
        return null;
    }

    public static CCAResultCode getCCRequestTypeByCode(Long code) {
        if (code == CCAResultCode.SUCCESS.code()) {
            return CCAResultCode.SUCCESS;
        } else if (code == CCAResultCode.END_USER_SERVICE_DENIED.code()) {
            return CCAResultCode.END_USER_SERVICE_DENIED;
        } else if (code == CCAResultCode.TECHNICAL_ERROR.code()) {
            return CCAResultCode.TECHNICAL_ERROR;
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
