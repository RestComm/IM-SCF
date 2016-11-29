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
package org.restcomm.imscf.el.cap;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorCode;

/** Maps the CapErrorCode's to their appropriate name. */
public final class CapErrorCodeMapper {

    private static final Map<Integer, String> CAP_ERROR_CODES = new HashMap<>();
    static {
        CAP_ERROR_CODES.put(CAPErrorCode.canceled, "canceled");
        CAP_ERROR_CODES.put(CAPErrorCode.cancelFailed, "cancelFailed");
        CAP_ERROR_CODES.put(CAPErrorCode.eTCFailed, "eTCFailed");
        CAP_ERROR_CODES.put(CAPErrorCode.improperCallerResponse, "improperCallerResponse");
        CAP_ERROR_CODES.put(CAPErrorCode.missingCustomerRecord, "missingCustomerRecord");
        CAP_ERROR_CODES.put(CAPErrorCode.missingParameter, "missingParameter");
        CAP_ERROR_CODES.put(CAPErrorCode.parameterOutOfRange, "parameterOutOfRange");
        CAP_ERROR_CODES.put(CAPErrorCode.requestedInfoError, "requestedInfoError");
        CAP_ERROR_CODES.put(CAPErrorCode.systemFailure, "systemFailure");
        CAP_ERROR_CODES.put(CAPErrorCode.taskRefused, "taskRefused");
        CAP_ERROR_CODES.put(CAPErrorCode.unavailableResource, "unavailableResource");
        CAP_ERROR_CODES.put(CAPErrorCode.unexpectedComponentSequence, "unexpectedComponentSequence");
        CAP_ERROR_CODES.put(CAPErrorCode.unexpectedDataValue, "unexpectedDataValue");
        CAP_ERROR_CODES.put(CAPErrorCode.unexpectedParameter, "unexpectedParameter");
        CAP_ERROR_CODES.put(CAPErrorCode.unknownCSID, "unknownCSID");
        CAP_ERROR_CODES.put(CAPErrorCode.unknownLegID, "unknownLegID");
        CAP_ERROR_CODES.put(CAPErrorCode.unknownPDPID, "unknownPDPID");
    }

    private CapErrorCodeMapper() {

    }

    public static String errorCodeAsString(Integer i) {
        return CAP_ERROR_CODES.get(i);
    }

}
