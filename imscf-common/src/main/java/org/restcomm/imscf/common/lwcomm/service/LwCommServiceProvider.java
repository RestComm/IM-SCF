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
package org.restcomm.imscf.common.lwcomm.service;

import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.service.impl.LwCommServiceImpl;

/**
 * Use this class to initialize and get an instance of LwCommService.
 * Current implementation uses static fields for storing configuration and singleton
 * references so can be used more than once in a JVM when the classes are loaded by
 * different and not related classloaders (e.g. by the classloaders of different
 * EAR deployments).
 * @author Miklos Pocsaji
 *
 */
public final class LwCommServiceProvider {

    private LwCommServiceProvider() {

    }

    /**
     * Initialize the service.
     * @param config Configuration to use. You can extend the abstract Configuration class or
     * use the defaults provided.
     * @return true if the initialization was successful.
     */
    public static boolean init(Configuration config) {
        return LwCommServiceImpl.init(config);
    }

    /**
     * Get the LwComm service instance.
     * Should be called before initialization. Before calling init() this method returns null.
     * @return The LwCommService instance.
     */
    public static LwCommService getService() {
        return LwCommServiceImpl.getService();
    }

    /**
     * Checks if the LwComm service is initialized.
     * @return Simple getService() != null
     */
    public static boolean isServiceInitialized() {
        return LwCommServiceImpl.getService() != null;
    }
}
