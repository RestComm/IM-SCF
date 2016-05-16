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
package org.restcomm.imscf.el.diameter;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.DiameterGatewayModuleType;
import org.restcomm.imscf.el.call.ImscfCallLifeCycleListener;
import org.restcomm.imscf.el.modules.Module;
import org.restcomm.imscf.el.modules.ModuleInitializationException;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterSLELCreditControlRequest;

/**
 * Diameter module interface.
 */
public interface DiameterModule extends Module, ImscfCallLifeCycleListener {

    default void initialize(DiameterGatewayModuleType configuration) throws ModuleInitializationException {
        // NOOP
    }

    default void setModuleConfiguration(DiameterGatewayModuleType configuration) {
        // NOOP
    }

    default DiameterGatewayModuleType getModuleConfiguration() {
        return null;
    }

    @Override
    default void initialize(ImscfConfigType configuration) throws ModuleInitializationException {
        Module.super.initialize(configuration);
        DiameterGatewayModuleType diameterConfig = configuration.getDiameterGatewayModules().stream()
                .filter(m -> m.getName().equals(getName())).findFirst()
                .orElseThrow(() -> new ModuleInitializationException("No DiameterGW module named " + getName()));
        setModuleConfiguration(diameterConfig);
        initialize(diameterConfig);
    }

    void doCreditControlRequest(DiameterSLELCreditControlRequest request, String imscfCallId, String lwcommRouteName,
            String appServerGroupName);
}
