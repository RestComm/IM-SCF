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

import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.map.MAPModule;
import org.restcomm.imscf.el.sccp.SccpModule;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Module instances can be retrieved from this class.
 */
public final class ModuleStore {
    private static final ConcurrentHashMap<String, SccpModule> SCCP_MODULES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CAPModule> CAP_MODULES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MAPModule> MAP_MODULES = new ConcurrentHashMap<>();

    public static final String SCCP_MODULE_NAME = "ElSccpModule";

    private ModuleStore() {
    }

    public static ConcurrentHashMap<String, SccpModule> getSccpModules() {
        return SCCP_MODULES;
    }

    public static SccpModule getSccpModule() {
        return getSccpModules().get(SCCP_MODULE_NAME);
    }

    public static ConcurrentHashMap<String, CAPModule> getCapModules() {
        return CAP_MODULES;
    }

    public static ConcurrentHashMap<String, MAPModule> getMapModules() {
        return MAP_MODULES;
    }

}
