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
package org.restcomm.imscf.common.diameter.creditcontrol;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for storing http application server group - diameter service and diameter module - diameter service pairs.
 */
public final class DiameterAsRoutes {
    private static final ConcurrentHashMap<String, DiameterAppServerData> HTTP_SERVER_INFO = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> SERVICE_HTTP_SERVER_PAIRS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> SERVICE_DIAMETER_MODULE_PAIRS = new ConcurrentHashMap<>();

    private DiameterAsRoutes() {

    }

    public static ConcurrentHashMap<String, String> getServiceHttpServerPairs() {
        return SERVICE_HTTP_SERVER_PAIRS;
    }

    public static ConcurrentHashMap<String, DiameterAppServerData> getServerInfo() {
        return HTTP_SERVER_INFO;
    }

    public static ConcurrentHashMap<String, String> getServiceDiameterModulePairs() {
        return SERVICE_DIAMETER_MODULE_PAIRS;
    }

    public static void reset() {
        HTTP_SERVER_INFO.clear();
        SERVICE_HTTP_SERVER_PAIRS.clear();
        SERVICE_DIAMETER_MODULE_PAIRS.clear();
    }
}
