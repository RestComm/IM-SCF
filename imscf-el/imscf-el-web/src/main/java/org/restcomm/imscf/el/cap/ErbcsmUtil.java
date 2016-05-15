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
package org.restcomm.imscf.el.cap;

import java.util.Optional;

import org.mobicents.protocols.ss7.cap.api.EsiBcsm.OCalledPartyBusySpecificInfo;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.ODisconnectSpecificInfo;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.RouteSelectFailureSpecificInfo;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.TBusySpecificInfo;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.TDisconnectSpecificInfo;
import org.mobicents.protocols.ss7.cap.api.isup.CauseCap;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest;
import org.mobicents.protocols.ss7.inap.api.primitives.MiscCallInfo;
import org.mobicents.protocols.ss7.inap.api.primitives.MiscCallInfoMessageType;

/** Utility class for ERBCSM manipulation. */
public final class ErbcsmUtil {
    private ErbcsmUtil() {
    }

    public static MiscCallInfoMessageType getMessageType(EventReportBCSMRequest erbcsm) {
        return Optional.ofNullable(erbcsm.getMiscCallInfo()).map(MiscCallInfo::getMessageType)
                .orElse(MiscCallInfoMessageType.request);
    }

    public static Optional<CauseCap> getCause(EventReportBCSMRequest erbcsm) {
        return Optional.ofNullable(erbcsm.getEventSpecificInformationBCSM()).map(
                esi -> {
                    switch (erbcsm.getEventTypeBCSM()) {
                    case oCalledPartyBusy:
                        return Optional.ofNullable(esi.getOCalledPartyBusySpecificInfo())
                                .map(OCalledPartyBusySpecificInfo::getBusyCause).orElse(null);
                    case tBusy:
                        return Optional.ofNullable(esi.getTBusySpecificInfo()).map(TBusySpecificInfo::getBusyCause)
                                .orElse(null);
                    case oDisconnect:
                        return Optional.ofNullable(esi.getODisconnectSpecificInfo())
                                .map(ODisconnectSpecificInfo::getReleaseCause).orElse(null);
                    case tDisconnect:
                        return Optional.ofNullable(esi.getTDisconnectSpecificInfo())
                                .map(TDisconnectSpecificInfo::getReleaseCause).orElse(null);
                    case routeSelectFailure:
                        return Optional.ofNullable(esi.getRouteSelectFailureSpecificInfo())
                                .map(RouteSelectFailureSpecificInfo::getFailureCause).orElse(null);
                    default:
                        return null;
                    }
                });
    }
}
