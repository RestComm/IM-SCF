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

import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPDialogListener;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPGprsReferenceNumber;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPNoticeProblemDiagnostic;

/**
 * Interface for a CAP module capable of handling CS calls and SMS.
 *
 */
public interface GsmScf extends GsmScfCsCallListener, GsmScfSmsListener, CAPDialogListener {

    @Override
    default void onDialogAccept(CAPDialog arg0, CAPGprsReferenceNumber arg1) {
        // NOOP
    }

    @Override
    default void onDialogClose(CAPDialog arg0) {
        // NOOP
    }

    @Override
    default void onDialogDelimiter(CAPDialog arg0) {
        // NOOP
    }

    @Override
    default void onDialogNotice(CAPDialog arg0, CAPNoticeProblemDiagnostic arg1) {
        // NOOP
    }

    @Override
    default void onDialogRequest(CAPDialog arg0, CAPGprsReferenceNumber arg1) {
        // NOOP
    }

}
