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
import org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesCsCall.CapScenarioRequestReportBCSMEvent;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CAP scenario for sending out RRBCSM. */
public final class CapScenarioRRBCSM implements CapScenarioRequestReportBCSMEvent {

    private static final Logger LOG = LoggerFactory.getLogger(CapScenarioRRBCSM.class);
    private Long invokeId;

    public static CapScenarioRRBCSM start(CAPCSCall call, List<BCSMEvent> defaultEvents) throws CAPException {
        ArrayList<BCSMEvent> edps = new ArrayList<>(defaultEvents);
        List<BCSMEvent> asList = call.getBCSMEventsForPendingRrbcsm();
        if (asList.isEmpty()) {
            LOG.trace("Using default RRBCSME list: {}", edps);
        } else {
            LOG.trace("Using AS provided RRBCSME list to override EDPs: {}", asList);
            edps.removeIf(edp -> asList.stream().anyMatch(e -> e.getEventTypeBCSM().equals(edp.getEventTypeBCSM())));
            edps.addAll(asList);
            LOG.trace("Resulting EDP list: {}", edps);
        }
        Long invokeId = call.getCapDialog().addRequestReportBCSMEventRequest(edps, null);
        return new CapScenarioRRBCSM(invokeId);

    }

    private CapScenarioRRBCSM(Long invokeId) {
        this.invokeId = invokeId;
    }

    @Override
    public Long getInvokeId() {
        return invokeId;
    }

    @Override
    public void onSuccessTimeout() {
        LOG.trace("CAP RRBCSM operation success.");
    }

    @Override
    public void onFailureIndicated(CAPErrorMessage error, Problem problem) {
        LOG.warn("CAP RRBCSM failed, error {}, problem {}", error, problem);
    }

}
