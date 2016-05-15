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

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.map.MAPModule;

import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.cap.api.CAPMessage;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.InitialDPSMSRequest;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPMessage;

/**
 * Class for routing a dialog-starting message to the appropriate module.
 * This class should be reinitialized when all modules have been
 * initialized already after a configuration change, as module references
 * are only retrieved once from the ModuleStore.
 */
public final class ModuleRouter {

    private static volatile ModuleRouter instance;

    // instance fields
    private ModuleMatcher[] entries;

    public static ModuleRouter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ModuleRouter should be initialized first");
        }
        return instance;
    }

    // there's no synchronization here, as getInstance does not initialize automatically, and this method is only called
    // when a configuration change occurs
    public static void initialize(ImscfConfigType config) {
        // if the new config fails to parse, instance will simply remain the same
        instance = new ModuleRouter(config);
    }

    private ModuleRouter(ImscfConfigType config) {
        entries = config.getModuleRouting().stream().map(ModuleMatcher::new).toArray(ModuleMatcher[]::new);
    }

    public CAPModule route(InitialDPRequest idp) {
        return route(idp, idp.getServiceKey());
    }

    public CAPModule route(InitialDPSMSRequest idpSms) {
        return route(idpSms, idpSms.getServiceKey());
    }

    public CAPModule route(CAPMessage message, int serviceKey) {
        return route(message.getCAPDialog().getApplicationContext(), serviceKey);
    }

    public CAPModule route(CAPApplicationContext appCtx, int serviceKey) {
        // return the first module that matches
        for (ModuleMatcher mm : entries) {
            if (mm.matches(appCtx, serviceKey))
                return (CAPModule) mm.getModule();
        }
        return null;
    }

    public MAPModule route(MAPMessage message) {
        // serviceKey doesn't really make sense for MAP routing...
        return route(message, 0);
    }

    public MAPModule route(MAPMessage message, int serviceKey) {
        MAPApplicationContext appCtx = message.getMAPDialog().getApplicationContext();
        // return the first module that matches
        for (ModuleMatcher mm : entries) {
            if (mm.matches(appCtx, serviceKey))
                return (MAPModule) mm.getModule();
        }
        return null;
    }

}
