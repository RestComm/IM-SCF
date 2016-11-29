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

import org.restcomm.imscf.el.cap.call.CAPCSCall;
import org.restcomm.imscf.el.cap.call.CallSegment;

import java.util.ArrayList;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPParameterFactory;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.cap.api.isup.CauseCap;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.isup.ISUPParameterFactory;
import org.mobicents.protocols.ss7.isup.message.parameter.CauseIndicators;

/** Utility class for sending out a releaseCall request. */
public final class ReleaseCallUtil {
    private ReleaseCallUtil() {
        // NOOP
    }

    public static Long releaseCall(CAPCSCall call, int causeValue, boolean closeDialog) throws CAPException {
        CAPProvider prov = call.getCapDialog().getService().getCAPProvider();
        CAPParameterFactory cap = prov.getCAPParameterFactory();
        CauseCap cause = null;
        ISUPParameterFactory isup = prov.getISUPParameterFactory();
        CauseIndicators ci = isup.createCauseIndicators();

        ci.setCauseValue(causeValue);
        ci.setCodingStandard(CauseIndicators._CODING_STANDARD_ITUT);
        // note: OCSC uses "USER" location, but that seems incorrect, as it would indicate that the call has reached the
        // end subscriber, whereas this is an application release/error.
        // We use "public network serving local user".
        ci.setLocation(CauseIndicators._LOCATION_PUBLIC_NSLU);
        cause = cap.createCauseCap(ci);

        return releaseCall(call, cause, closeDialog);
    }

    public static Long releaseCall(CAPCSCall call, CauseCap cause, boolean closeDialog) throws CAPException {
        CAPDialogCircuitSwitchedCall dialog = call.getCapDialog();
        Long invokeId = dialog.addReleaseCallRequest(cause);

        if (closeDialog) {
            // send in TCAP end
            dialog.close(false);
        } else {
            // send in TCAP continue
            dialog.send();
        }
        // either way, all call segments are done:
        for (CallSegment cs : new ArrayList<>(call.getCallSegmentAssociation().getCallSegments())) {
            cs.release();
        }
        return invokeId;
    }
}
