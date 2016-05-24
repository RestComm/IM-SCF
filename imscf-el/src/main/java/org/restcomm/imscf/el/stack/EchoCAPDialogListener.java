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
package org.restcomm.imscf.el.stack;

import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPDialogListener;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPGeneralAbortReason;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPGprsReferenceNumber;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPUserAbortReason;
import org.mobicents.protocols.ss7.tcap.asn.comp.PAbortCauseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for logging/debugging callbacks for CAP dialogs.
 */
public class EchoCAPDialogListener implements CAPDialogListener {
    private static Logger logger = LoggerFactory.getLogger(EchoCAPDialogListener.class);

    @Override
    public void onDialogAccept(CAPDialog arg0, CAPGprsReferenceNumber arg1) {
        logger.debug("onDialogAccept: " + arg0 + ", " + arg1);
    }

    @Override
    public void onDialogClose(CAPDialog arg0) {
        logger.debug("onDialogClose: " + arg0);
    }

    @Override
    public void onDialogDelimiter(CAPDialog arg0) {
        logger.debug("onDialogDelimiter: " + arg0);
    }

    @Override
    public void onDialogNotice(CAPDialog arg0, CAPNoticeProblemDiagnostic arg1) {
        logger.debug("onDialogNotice: " + arg0 + ", " + arg1);
    }

    @Override
    public void onDialogProviderAbort(CAPDialog arg0, PAbortCauseType arg1) {
        logger.debug("onDialogProviderAbort: " + arg0 + ", " + arg1);
    }

    @Override
    public void onDialogRelease(CAPDialog arg0) {
        logger.debug("onDialogRelease: " + arg0);
    }

    @Override
    public void onDialogRequest(CAPDialog arg0, CAPGprsReferenceNumber arg1) {
        logger.debug("onDialogRequest: " + arg0 + ", " + arg1);
    }

    @Override
    public void onDialogTimeout(CAPDialog arg0) {
        logger.debug("onDialogTimeout: " + arg0);
    }

    @Override
    public void onDialogUserAbort(CAPDialog arg0, CAPGeneralAbortReason arg1, CAPUserAbortReason arg2) {
        logger.debug("onDialogUserAbort: " + arg0 + ", " + arg1 + ", " + arg2);
    }

}
