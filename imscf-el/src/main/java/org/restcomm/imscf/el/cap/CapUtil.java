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

import java.util.Objects;

import org.mobicents.protocols.ss7.cap.api.CAPDialog;

/** Utility class for CAP. */
public final class CapUtil {

    private CapUtil() {
    }

    public static boolean canSendPrimitives(CAPDialog dialog) {
        Objects.requireNonNull(dialog, "CAPDialog cannot be null");
        switch (dialog.getState()) {
        case Idle: // dialog just created locally (ICA)
        case InitialReceived: // TC_BEGIN arrived (IDP)
        case Active: // established after at least one TC_CONTINUE
            return true;
        case InitialSent: // TC_BEGIN sent, waiting for TC_CONTINUE response (ICA)
        case Expunged: // TC_END/TC_ABORT sent/arrived
            return false;
        default:
            throw new AssertionError("Invalid dialog state: " + dialog.getState());
        }
    }
}
