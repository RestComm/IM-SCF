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
package org.restcomm.imscf.el.cap.converter;

import java.util.ArrayList;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.isup.GenericNumberCap;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ContinueWithArgumentRequest;

/** Utility class for sending out a continue request. */
public final class ContinueUtil {
    private ContinueUtil() {
        // NOOP
    }

    public static Long continueCall(CAPDialogCircuitSwitchedCall dialog, ContinueWithArgumentRequest cwa,
            boolean closeDialog) throws CAPException {
        // all parameters are simply copied

        // Long addContinueWithArgumentRequest(AlertingPatternCap alertingPattern,
        // CAPExtensions extensions,
        // ServiceInteractionIndicatorsTwo serviceInteractionIndicatorsTwo,
        // CallingPartysCategoryInap callingPartysCategory,
        // ArrayList<GenericNumberCap> genericNumbers,
        // CUGInterlock cugInterlock,
        // boolean cugOutgoingAccess,
        // LocationNumberCap chargeNumber,
        // Carrier carrier,
        // boolean suppressionOfAnnouncement,
        // NAOliInfo naOliInfo,
        // boolean borInterrogationRequested,
        // boolean suppressOCsi,
        // ContinueWithArgumentArgExtension continueWithArgumentArgExtension)
        // throws CAPException;
        Long invokeId = dialog.addContinueWithArgumentRequest(cwa.getAlertingPattern(), cwa.getExtensions(),
                cwa.getServiceInteractionIndicatorsTwo(), cwa.getCallingPartysCategory(),
                (ArrayList<GenericNumberCap>) cwa.getGenericNumbers(), cwa.getCugInterlock(),
                cwa.getCugOutgoingAccess(), cwa.getChargeNumber(), cwa.getCarrier(),
                cwa.getSuppressionOfAnnouncement(), cwa.getNaOliInfo(), cwa.getBorInterrogationRequested(),
                cwa.getSuppressOCsi(), cwa.getContinueWithArgumentArgExtension());
        if (closeDialog) {
            dialog.close(false);
        } else {
            dialog.send();
        }
        return invokeId;
    }

    public static Long continueCall(CAPDialogCircuitSwitchedCall dialog, boolean closeDialog) throws CAPException {
        Long invokeId = dialog.addContinueRequest();
        if (closeDialog) {
            dialog.close(false);
        } else {
            dialog.send();
        }
        return invokeId;
    }
}
