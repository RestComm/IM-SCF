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
package org.restcomm.imscf.el.sip.routing;

import org.restcomm.imscf.common.config.SipApplicationServerGroupType;
import org.restcomm.imscf.common.config.SipApplicationServerGroupWrapperType;
import org.restcomm.imscf.common.config.SipAsRoutingConfigType;
import org.restcomm.imscf.el.modules.routing.RoutingCriteriaMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;

/** Class for matching a SIP AS routing entry. */
public class SipAsGroupMatcher {
    private List<SipApplicationServerGroupType> sipAsGroups;
    private RoutingCriteriaMatcher[] routingCriteria;

    public SipAsGroupMatcher(SipAsRoutingConfigType config) {
        try {
            sipAsGroups = Collections.unmodifiableList(config.getSipApplicationServerGroups().stream()
                    .map(SipApplicationServerGroupWrapperType::getSipApplicationServerGroup)
                    .collect(Collectors.toList()));
            if (sipAsGroups.isEmpty())
                throw new IllegalArgumentException("At least one SIP AS group must be specified for a routing entry!");

            routingCriteria = config.getRoutingCriterias().stream().map(RoutingCriteriaMatcher::new)
                    .toArray(RoutingCriteriaMatcher[]::new);
        } catch (Throwable e) {
            throw new IllegalArgumentException("Failed to parse SIP AS routing entry: " + config, e);
        }
    }

    public List<SipApplicationServerGroupType> getSipAsGroups() {
        return sipAsGroups;
    }

    public boolean matches(CAPApplicationContext appCtx, int serviceKey) {
        // no criteria means unconditional match
        if (routingCriteria.length == 0
                || Arrays.stream(routingCriteria).anyMatch(rc -> rc.matches(appCtx, serviceKey)))
            return true;
        return false;
    }

    public boolean matches(MAPApplicationContext appCtx, int serviceKey) {
        // no criteria means unconditional match
        if (routingCriteria.length == 0
                || Arrays.stream(routingCriteria).anyMatch(rc -> rc.matches(appCtx, serviceKey)))
            return true;
        return false;
    }

}
