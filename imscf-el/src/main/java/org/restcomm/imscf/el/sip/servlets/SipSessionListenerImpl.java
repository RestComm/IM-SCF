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

import static org.restcomm.imscf.el.sip.SipApplicationSessionAttributes.TIMER_KEEPS_APPSESSION_ALIVE;
import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;
import org.restcomm.imscf.util.IteratorStream;

import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.annotation.SipListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** App level SipSession listener. */
@SipListener
public class SipSessionListenerImpl implements SipSessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(SipSessionListenerImpl.class);

    @EJB
    private transient CallStore callStore;
    @EJB
    private transient CallFactoryBean callFactory;

    @EJB
    private transient ConfigBean config;

    @PostConstruct
    private void init() {
        Objects.requireNonNull(callStore, "CallStore not found");
        Objects.requireNonNull(config, "ConfigBean not found");
        Objects.requireNonNull(callFactory, "CallFactory not found");
    }

    private String describeNoCall(SipApplicationSession sas) {
        return sas == null ? "null" : ("(no call, sas: " + sas.getId() + ")");
    }

    @Override
    public void sessionCreated(SipSessionEvent arg0) {
        try (ContextLayer cl = CallContext.with(callStore, config, callFactory)) {
            try (IMSCFCall call = CallContext.getCallStore().getCallByAppSessionId(
                    arg0.getSession().getApplicationSession().getId())) {
                LOG.trace("SipSession created: {} in {}", arg0.getSession().getId(), call != null ? call
                        : describeNoCall(arg0.getSession().getApplicationSession()));
            }
        }

    }

    @Override
    public void sessionDestroyed(SipSessionEvent arg0) {
        try (ContextLayer cl = CallContext.with(callStore, config, callFactory)) {
            SipApplicationSession sas = arg0.getSession().getApplicationSession();
            try (IMSCFCall call = CallContext.getCallStore().getCallByAppSessionId(sas.getId())) {
                LOG.trace("SipSession destroyed: {} in {}", arg0.getSession().getId(), call != null ? call
                        : describeNoCall(sas));

                if (sas.isValid()) {
                    // destroy appsession if all sessions are terminated, except if asked not to
                    if (Boolean.TRUE.equals(TIMER_KEEPS_APPSESSION_ALIVE.get(sas))) {
                        LOG.trace("TIMER_KEEPS_APPSESSION_ALIVE set, leaving SipApplicationSession alive");
                    } else if (IteratorStream.of(SipSessions.of(sas)).allMatch(SipUtil::isTerminated)) {
                        LOG.trace("No more SipSessions, invalidating SipApplicationSession");
                        // calling sas.invalidate(); would result in a recursive call to this method and an exception on
                        // the second invalidation attempt...
                        // so instead we cancel all the timers, and allow the appsession to invalidate itself.
                        sas.getTimers().forEach(ServletTimer::cancel);
                        // TODO: check that the above mentioned invalidate call should be allowed from here or not
                    } else {
                        LOG.trace("More SipSessions present in appsession.");
                    }
                }
            }
        }
    }

    @Override
    public void sessionReadyToInvalidate(SipSessionEvent arg0) {
        try (ContextLayer cl = CallContext.with(callStore, config, callFactory)) {
            try (IMSCFCall call = CallContext.getCallStore().getCallByAppSessionId(
                    arg0.getSession().getApplicationSession().getId())) {
                LOG.trace("SipSession ready to invalidate: {} in {}", arg0.getSession().getId(), call != null ? call
                        : describeNoCall(arg0.getSession().getApplicationSession()));
            }
        }
    }

}
