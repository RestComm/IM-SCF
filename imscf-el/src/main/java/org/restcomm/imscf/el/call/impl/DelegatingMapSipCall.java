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
package org.restcomm.imscf.el.call.impl;

import java.util.List;

import org.restcomm.imscf.el.map.call.MAPSIPCall;
import org.restcomm.imscf.el.map.scenarios.MapIncomingRequestScenario;
import org.restcomm.imscf.el.map.scenarios.MapOutgoingRequestScenario;

/**
 * Delegate interface for MAPSIPCall.
 * @author Miklos Pocsaji
 *
 */
public interface DelegatingMapSipCall extends DelegatingMapCall, DelegatingSIPCall, MAPSIPCall {

    @Override
    default MAPSIPCall getDelegate() {
        return (MAPSIPCall) DelegatingSIPCall.super.getDelegate();
    }

    @Override
    default List<MapIncomingRequestScenario<?>> getMapIncomingRequestScenarios() {
        return getDelegate().getMapIncomingRequestScenarios();
    }

    @Override
    default List<MapOutgoingRequestScenario> getMapOutgoingRequestScenarios() {
        return getDelegate().getMapOutgoingRequestScenarios();
    }
}
