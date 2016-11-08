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

import java.util.Objects;

import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.adapters.SipApplicationSessionAdapter;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;
import org.restcomm.imscf.util.JNDIHelper;

import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.annotation.SipListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** App level appsession listener. */
@SipListener
public class SipApplicationSessionListenerImpl implements SipApplicationSessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(SipApplicationSessionListenerImpl.class);

    // @EJB
    private transient CallStore callStore;
    // @EJB
    private transient CallFactoryBean callFactory;

    // @EJB
    private transient ConfigBean config;

    public SipApplicationSessionListenerImpl() {
        // injection does not work at the moment, so we use lookup
        init();
    }

    // @PostConstruct
    private void init() {
        if (callStore == null)
            callStore = Objects.requireNonNull(JNDIHelper.getCallStore(), "CallStore not found");
        if (config == null)
            config = Objects.requireNonNull(JNDIHelper.getConfigBean(), "ConfigBean not found");
        if (callFactory == null)
            callFactory = Objects.requireNonNull(JNDIHelper.getCallFactory(), "CallFactory not found");
    }

    @Override
    public void sessionCreated(SipApplicationSessionEvent ev) {
        LOG.trace("AppSession created: {}", ev.getApplicationSession().getId());
    }

    @Override
    public void sessionDestroyed(SipApplicationSessionEvent ev) {
        String id = ev.getApplicationSession().getId();
        LOG.trace("AppSession destroyed: {}", id);
        try (ContextLayer cl = CallContext.with(callStore, callFactory, config)) {
            try (SIPCall call = callStore.getCallByAppSessionId(id)) {
                if (call != null) {
                    call.getSipModule().sessionDestroyed(
                            SipApplicationSessionAdapter.getAdapter(ev.getApplicationSession()));
                }
            }
        }
    }

    @Override
    public void sessionExpired(SipApplicationSessionEvent ev) {
        String id = ev.getApplicationSession().getId();
        if (!ev.getApplicationSession().isReadyToInvalidate()) {
            LOG.trace("AppSession {} expired before isReadyToInvalidate!", id);
        } else {
            LOG.trace("AppSession expired: {}", id);
        }
        try (ContextLayer cl = CallContext.with(callStore, callFactory, config)) {
            try (SIPCall call = callStore.getCallByAppSessionId(id)) {
                if (call != null) {
                    call.getSipModule().sessionExpired(
                            SipApplicationSessionAdapter.getAdapter(ev.getApplicationSession()));
                }
            }
        }
    }

    @Override
    public void sessionReadyToInvalidate(SipApplicationSessionEvent ev) {
        LOG.trace("AppSession ready to invalidate: {}", ev.getApplicationSession().getId());
    }

}
