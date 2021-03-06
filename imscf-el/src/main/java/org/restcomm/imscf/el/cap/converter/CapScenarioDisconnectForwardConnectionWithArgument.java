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
import org.mobicents.protocols.ss7.cap.api.primitives.CAPExtensions;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CAP scenario for DFCwA. */
public final class CapScenarioDisconnectForwardConnectionWithArgument
        implements
        org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesCsCall.CapScenarioDisconnectForwardConnectionWithArgument {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioDisconnectForwardConnectionWithArgument.class);
    private Long invokeId;

    public static CapScenarioDisconnectForwardConnectionWithArgument start(CAPCSCall call, Integer callSegmentId,
            CAPExtensions extensions) throws CAPException {
        Long invokeId = call.getCapDialog()
                .addDisconnectForwardConnectionWithArgumentRequest(callSegmentId, extensions);
        // send this immediately
        call.getCapDialog().send();
        return new CapScenarioDisconnectForwardConnectionWithArgument(invokeId);
    }

    private CapScenarioDisconnectForwardConnectionWithArgument(Long invokeId) {
        this.invokeId = invokeId;
    }

    @Override
    public Long getInvokeId() {
        return invokeId;
    }

    @Override
    public void onSuccessTimeout() {
        LOG.debug("CAP DisconnectForwardConnectionWithArgument operation success.");
    }

    @Override
    public void onFailureIndicated(CAPErrorMessage error, Problem problem) {
        LOG.warn("CAP DisconnectForwardConnectionWithArgument failed, error {}, problem {}", error, problem);
    }

}
