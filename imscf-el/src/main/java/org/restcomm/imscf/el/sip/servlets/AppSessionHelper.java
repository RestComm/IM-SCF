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
package org.restcomm.imscf.el.sip.servlets;

import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.stack.CallContext;

import java.util.Iterator;
import java.util.Objects;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSession.Protocol;
import javax.servlet.sip.SipSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for SipApplicationSession manipulation. */
public final class AppSessionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AppSessionHelper.class);

    private AppSessionHelper() {
    }

    public static void renewAppSessionTimeout(SIPCall call, SipApplicationSession sas) {
        Objects.requireNonNull(sas, "SipApplicationSession is null");
        Objects.requireNonNull(call, "Call is null");
        if (!call.getAppSessionId().equals(sas.getId()))
            throw new IllegalArgumentException("SAS " + sas.getId() + " does not belong to call "
                    + call.getAppSessionId());

        int maxIdle = call.getSipModule().getAppSessionTimeoutMin();

        int age = (int) ((System.currentTimeMillis() - sas.getCreationTime()) / 60000); // in minutes
        int maxAge = Integer.MAX_VALUE; // TODO don't restrict until there is a config value for sip calls
        if (age < maxAge) {
            int addition = Math.min(maxAge - age, maxIdle);
            int granted = sas.setExpires(addition);
            if (granted < addition) {
                LOG.warn(
                        "Tried to renew appsession expiry for {} minutes, but container only granted {} minutes. Appsession is {} minutes old.",
                        addition, granted, age);
            } else {
                LOG.trace("AppSession expiry renewed for {} minutes. AppSession is {} minutes old", addition, age);
            }
        } else {
            LOG.warn("Cannot renew appsession expiry for call {}, appSession is already {} minutes old (max is {})",
                    call.getImscfCallId(), age, maxAge);
        }
    }

    public static void renewAppSessionTimeout(SipApplicationSession sas) {
        Objects.requireNonNull(sas, "SipApplicationSession is null");
        CallStore cs = Objects.requireNonNull((CallStore) CallContext.get(CallContext.CALLSTORE),
                "CallStore from context is null");
        try (SIPCall call = cs.getCallByAppSessionId(sas.getId())) {
            if (call != null) {
                AppSessionHelper.renewAppSessionTimeout(call, sas);
            } else {
                LOG.trace("No call exists for appsession {}", sas.getId());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Iterator<SipSession> getSipSessions(SipApplicationSession sas) {
        return (Iterator<SipSession>) sas.getSessions(Protocol.SIP.name());
    }
}
