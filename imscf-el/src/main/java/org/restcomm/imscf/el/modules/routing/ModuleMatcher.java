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
package org.restcomm.imscf.el.modules.routing;

import org.restcomm.imscf.common.config.CapModuleType;
import org.restcomm.imscf.common.config.MapModuleType;
import org.restcomm.imscf.common.config.ModuleRoutingConfigType;
import org.restcomm.imscf.el.modules.Module;
import org.restcomm.imscf.el.modules.ModuleStore;

import java.util.Arrays;

import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;

/** Class for matching a single module. */
public class ModuleMatcher {
    private Module module;
    private RoutingCriteriaMatcher[] routingCriteria;

    public ModuleMatcher(ModuleRoutingConfigType config) {
        try {
            CapModuleType cap = config.getCapModule();
            MapModuleType map = config.getMapModule();
            if (cap != null && map == null) {
                module = ModuleStore.getCapModules().get(cap.getName());
            } else if (map != null && cap == null) {
                module = ModuleStore.getMapModules().get(map.getName());
            } else {
                throw new IllegalArgumentException(
                        "Either a CAP or a MAP module must be specified for module routing entry!");
            }

            routingCriteria = config.getRoutingCriterias().stream().map(RoutingCriteriaMatcher::new)
                    .toArray(RoutingCriteriaMatcher[]::new);
        } catch (Throwable e) {
            throw new IllegalArgumentException("Failed to parse module routing entry: " + config, e);
        }
    }

    public Module getModule() {
        return module;
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
