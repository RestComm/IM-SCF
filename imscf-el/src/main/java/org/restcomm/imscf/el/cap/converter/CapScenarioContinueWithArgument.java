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

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.LegOrCallSegment;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.primitive.ContinueWithArgumentArgExtensionImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.primitive.LegOrCallSegmentImpl;
import org.mobicents.protocols.ss7.inap.api.primitives.LegID;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CAP scenario for sending out ContinueWithArgument. */
public final class CapScenarioContinueWithArgument implements
        org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesCsCall.CapScenarioContinueWithArgument {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioContinueWithArgument.class);
    private Long invokeId;

    public static CapScenarioContinueWithArgument start(CAPCSCall call, LegID legID) throws CAPException {
        return start(call, new LegOrCallSegmentImpl(legID));
    }

    public static CapScenarioContinueWithArgument start(CAPCSCall call, int callSegmentID) throws CAPException {
        return start(call, new LegOrCallSegmentImpl(callSegmentID));
    }

    private static CapScenarioContinueWithArgument start(CAPCSCall call, LegOrCallSegment locs) throws CAPException {
        CAPDialogCircuitSwitchedCall dialog = call.getCapDialog();

        if (dialog.getApplicationContext().getVersion().getVersion() < 4) {
            LOG.warn("Cannot send CWA for a leg/CS in dialog ctx {}", dialog.getApplicationContext());
            return null;
        }

        Long invokeId = dialog.addContinueWithArgumentRequest(null, null, null, null, null, null, false, null, null,
                false, null, false, false, new ContinueWithArgumentArgExtensionImpl(false, false, false, locs));

        // send this immediately
        dialog.send();
        return new CapScenarioContinueWithArgument(invokeId);
    }

    private CapScenarioContinueWithArgument(Long invokeId) {
        this.invokeId = invokeId;
    }

    @Override
    public Long getInvokeId() {
        return invokeId;
    }

    @Override
    public void onSuccessTimeout() {
        LOG.trace("CAP ContinueWithArgument operation success.");
    }

    @Override
    public void onFailureIndicated(CAPErrorMessage error, Problem problem) {
        LOG.warn("CAP ContinueWithArgument failed, error {}, problem {}", error, problem);
    }

}
