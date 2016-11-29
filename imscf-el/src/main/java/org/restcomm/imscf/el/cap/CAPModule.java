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
package org.restcomm.imscf.el.cap;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.CapModuleType;
import org.restcomm.imscf.el.modules.Module;
import org.restcomm.imscf.el.modules.ModuleInitializationException;

import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

/**
 * CAP module interface.
 */
public interface CAPModule extends GsmScf, Module {

    default void initialize(CapModuleType configuration) throws ModuleInitializationException {
        // NOOP
    }

    void setModuleConfiguration(CapModuleType configuration);

    CapModuleType getModuleConfiguration();

    void setCAPProvider(CAPProvider provider);

    CAPProvider getCAPProvider();

    void setSccpProvider(SccpProvider provider);

    SccpProvider getSccpProvider();

    @Override
    default void initialize(ImscfConfigType configuration) throws ModuleInitializationException {
        Module.super.initialize(configuration);
        CapModuleType capConfig = configuration.getCapModules().stream().filter(m -> m.getName().equals(getName()))
                .findFirst().orElseThrow(() -> new ModuleInitializationException("No CAP module named " + getName()));
        setModuleConfiguration(capConfig);
        initialize(capConfig);
    }

    long getTcapIdleTimeoutMillis();

    long getAsReactionTimeoutMillis();

    /** Called after a dialog has been released due to an activityTest failure. */
    void onDialogFailure(CAPDialog dialog);

    SccpAddress getLocalSccpAddress();

    ISDNAddressString getGsmScfAddress();

    void onAsInitiatedCall(String imscfCallId);
}
