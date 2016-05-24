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
package org.restcomm.imscf.el.cap.converter;

import org.restcomm.imscf.el.cap.call.CAPCSCall;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
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
        CAPDialogCircuitSwitchedCall dialog = call.getCapDialog();
        Long invokeId = dialog.addContinueWithArgumentRequest(null, null, null, null, null, null, false, null, null,
                false, null, false, false, new ContinueWithArgumentArgExtensionImpl(false, false, false,
                        new LegOrCallSegmentImpl(legID)));
        // send this immediately
        dialog.send();
        return new CapScenarioContinueWithArgument(invokeId);

    }

    public static CapScenarioContinueWithArgument start(CAPCSCall call, int callSegmentID) throws CAPException {
        CAPDialogCircuitSwitchedCall dialog = call.getCapDialog();
        Long invokeId = dialog.addContinueWithArgumentRequest(null, null, null, null, null, null, false, null, null,
                false, null, false, false, new ContinueWithArgumentArgExtensionImpl(false, false, false,
                        new LegOrCallSegmentImpl(callSegmentID)));
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
