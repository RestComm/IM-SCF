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
package org.restcomm.imscf.el.map;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.MapModuleType;
import org.restcomm.imscf.el.modules.Module;
import org.restcomm.imscf.el.modules.ModuleInitializationException;

import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

/**
 * MAP module interface.
 */
public interface MAPModule extends MapListener, Module {

    default void initialize(MapModuleType configuration) throws ModuleInitializationException {
        // NOOP
    }

    void setModuleConfiguration(MapModuleType configuration);

    MapModuleType getModuleConfiguration();

    void setMAPProvider(MAPProvider provider);

    MAPProvider getMAPProvider();

    void setSccpProvider(SccpProvider provider);

    SccpProvider getSccpProvider();

    SccpAddress getLocalSccpAddress();

    @Override
    default void initialize(ImscfConfigType configuration) throws ModuleInitializationException {
        Module.super.initialize(configuration);
        MapModuleType mapConfig = configuration.getMapModules().stream().filter(m -> m.getName().equals(getName()))
                .findFirst().orElseThrow(() -> new ModuleInitializationException("No MAP module named " + getName()));
        setModuleConfiguration(mapConfig);
        initialize(mapConfig);
    }
}
