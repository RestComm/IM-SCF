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
package org.restcomm.imscf.el.sccp;

import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.ImscfConfigType.Sccp;
import org.restcomm.imscf.el.modules.Module;
import org.restcomm.imscf.el.modules.ModuleInitializationException;

/**
 * SCCP module for parsing the SCCP configuration and providing lookup/cache functions.
 */
public interface SccpModule extends Module {
    default void initialize(Sccp configuration) throws ModuleInitializationException {
        // NOOP
    }

    void setModuleConfiguration(Sccp configuration);

    Sccp getModuleConfiguration();

    void setSccpProvider(SccpProvider provider);

    SccpProvider getSccpProvider();

    @Override
    default void initialize(ImscfConfigType configuration) throws ModuleInitializationException {
        Module.super.initialize(configuration);
        Sccp sccpConfig = configuration.getSccp();
        setModuleConfiguration(sccpConfig);
        initialize(sccpConfig);
    }

    SccpAddress getLocalAddress(String alias);

    SccpAddress getRemoteAddress(String alias);
}
