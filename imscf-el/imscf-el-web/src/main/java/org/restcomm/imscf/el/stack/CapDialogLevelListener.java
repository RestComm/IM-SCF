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

import org.restcomm.imscf.el.cap.call.CAPCall;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;

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
 * Class for listening to CAP dialog level events that are not specific to any CAP service.
 * Some of these callbacks should be passed to CAP modules as well, but are not present in
 * the service specific listener interface.
 */
public class CapDialogLevelListener extends ImscfStackListener implements CAPDialogListener {

    private static Logger logger = LoggerFactory.getLogger(CapDialogLevelListener.class);

    @Override
    public void onDialogAccept(CAPDialog arg0, CAPGprsReferenceNumber arg1) {
        logger.debug("onDialogAccept: {}, {}", arg0, arg1);
    }

    @Override
    public void onDialogClose(CAPDialog arg0) {
        logger.debug("onDialogClose: {}", arg0);
        try (CAPCall<?> call = (CAPCall<?>) callStore.getCallByLocalTcapTrId(arg0.getLocalDialogId())) {
            if (call == null) {
                logger.warn("Could not find call for onDialogClose: {}", arg0);
                return;
            }
            call.getCapModule().onDialogClose(arg0);
        }
    }

    @Override
    public void onDialogDelimiter(CAPDialog arg0) {
        logger.debug("onDialogDelimiter: {}", arg0);
    }

    @Override
    public void onDialogNotice(CAPDialog arg0, CAPNoticeProblemDiagnostic arg1) {
        logger.debug("onDialogNotice: {}, {}", arg0, arg1);
    }

    @Override
    public void onDialogProviderAbort(CAPDialog arg0, PAbortCauseType arg1) {
        logger.debug("onDialogProviderAbort: {}, {}", arg0, arg1);
        try (CAPCall<?> call = (CAPCall<?>) callStore.getCallByLocalTcapTrId(arg0.getLocalDialogId())) {
            if (call == null) {
                logger.warn("Could not find call for onDialogProviderAbort: {}", arg0);
                return;
            }
            call.getCapModule().onDialogProviderAbort(arg0, arg1);
        }
    }

    @Override
    public void onDialogRelease(CAPDialog arg0) {
        logger.debug("onDialogRelease: {}", arg0);
        try (ContextLayer cl = CallContext.with(callStore)) {
            try (CAPCall<?> call = (CAPCall<?>) callStore.getCallByLocalTcapTrId(arg0.getLocalDialogId())) {
                if (call == null) { // OK during deleteCall
                    logger.debug("Could not find call for onDialogRelease: {}", arg0);
                    return;
                }
                call.getCapModule().onDialogRelease(arg0);
            }
        }
    }

    @Override
    public void onDialogRequest(CAPDialog arg0, CAPGprsReferenceNumber arg1) {
        logger.debug("onDialogRequest: {}, {}", arg0, arg1);
    }

    @Override
    public void onDialogTimeout(CAPDialog arg0) {
        logger.debug("onDialogTimeout: {}", arg0);
        try (ContextLayer cl = CallContext.with(callStore)) {
            try (CAPCall<?> call = (CAPCall<?>) callStore.getCallByLocalTcapTrId(arg0.getLocalDialogId())) {
                if (call == null) {
                    logger.warn("Could not find call for onDialogTimeout: {}", arg0);
                    return;
                }
                call.getCapModule().onDialogTimeout(arg0);
            }
        }
    }

    @Override
    public void onDialogUserAbort(CAPDialog arg0, CAPGeneralAbortReason arg1, CAPUserAbortReason arg2) {
        logger.debug("onDialogUserAbort: {}, {}, {}", arg0, arg1, arg2);
        try (CAPCall<?> call = (CAPCall<?>) callStore.getCallByLocalTcapTrId(arg0.getLocalDialogId())) {
            if (call == null) {
                logger.warn("Could not find call for onDialogUserAbort: {}", arg0);
                return;
            }
            call.getCapModule().onDialogUserAbort(arg0, arg1, arg2);
        }
    }

}
