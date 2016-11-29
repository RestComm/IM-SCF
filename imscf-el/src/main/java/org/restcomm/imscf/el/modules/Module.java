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
package org.restcomm.imscf.el.modules;

import org.restcomm.imscf.common.config.ImscfConfigType;

/**
 * Common interface for all EL modules.
 */
public interface Module {
    void setName(String name);

    String getName();

    default void setImscfConfiguration(ImscfConfigType configuration) {
        // NOOP
    }

    default ImscfConfigType getImscfConfiguration() {
        return null;
    }

    /**
     * Called after all attributes were set (e.g. name).
     * @param configuration IMSCF configuration
     * @throws ModuleInitializationException if the module failed to initialize with the given configuration.
     */
    default void initialize(ImscfConfigType configuration) throws ModuleInitializationException {
        setImscfConfiguration(configuration);
    }

    default void start() {
        // NOOP
    }

    default void stop() {
        // NOOP
    }
}
