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
package org.restcomm.imscf.el.sip.routing;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.SipApplicationServerGroupType;

import java.util.Collections;
import java.util.List;
import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.cap.api.CAPMessage;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.InitialDPSMSRequest;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPMessage;

/**
 * Class for routing a dialog-starting SIP message to the appropriate Application Server groups.
 * This class should be reinitialized after a configuration change.
 */
public final class SipAsRouter {

    private static volatile SipAsRouter instance;

    // instance fields
    private SipAsGroupMatcher[] entries;

    public static SipAsRouter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SipAsRouter should be initialized first");
        }
        return instance;
    }

    // there's no synchronization here, as getInstance does not initialize automatically, and this method is only called
    // when a configuration change occurs
    public static void initialize(ImscfConfigType config) {
        // if the new config fails to parse, instance will simply remain the same
        instance = new SipAsRouter(config);
    }

    private SipAsRouter(ImscfConfigType config) {
        entries = config.getSipAsRouting().stream().map(SipAsGroupMatcher::new).toArray(SipAsGroupMatcher[]::new);
    }

    public List<SipApplicationServerGroupType> route(InitialDPRequest idp) {
        return route(idp, idp.getServiceKey());
    }

    public List<SipApplicationServerGroupType> route(InitialDPSMSRequest idpSms) {
        return route(idpSms, idpSms.getServiceKey());
    }

    public List<SipApplicationServerGroupType> route(CAPMessage message, int serviceKey) {
        CAPApplicationContext appCtx = message.getCAPDialog().getApplicationContext();
        // return the first module that matches
        for (SipAsGroupMatcher mm : entries) {
            if (mm.matches(appCtx, serviceKey))
                return mm.getSipAsGroups();
        }
        return Collections.emptyList();
    }

    public List<SipApplicationServerGroupType> route(MAPMessage message) {
        // serviceKey doesn't really make sense for MAP routing...
        return route(message, 0);
    }

    public List<SipApplicationServerGroupType> route(MAPMessage message, int serviceKey) {
        MAPApplicationContext appCtx = message.getMAPDialog().getApplicationContext();
        // return the first module that matches
        for (SipAsGroupMatcher mm : entries) {
            if (mm.matches(appCtx, serviceKey))
                return mm.getSipAsGroups();
        }
        return Collections.emptyList();
    }

}
