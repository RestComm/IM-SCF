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
package org.restcomm.imscf.el.sip.servlets;

import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.call.TimerListener;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.sip.adapters.ServletTimerAdapter;
import org.restcomm.imscf.el.sip.servlets.TimerInfo.TimerType;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;
import org.restcomm.imscf.util.JNDIHelper;

import java.util.Objects;

import javax.ejb.EJB;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.annotation.SipListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared listener for SIP TimerService ServletTimer.
 */
@SipListener
public class SipTimerListenerImpl implements javax.servlet.sip.TimerListener {

    private static final Logger LOG = LoggerFactory.getLogger(SipTimerListenerImpl.class);

    @EJB
    ConfigBean configBean;

    @EJB
    CallStore callStore;

    @EJB
    CallFactoryBean callFactory;

    public SipTimerListenerImpl() {
        if (callStore == null)
            callStore = Objects.requireNonNull(JNDIHelper.getCallStore(), "CallStore not found");
        if (configBean == null)
            configBean = Objects.requireNonNull(JNDIHelper.getConfigBean(), "ConfigBean not found");
        if (callFactory == null)
            callFactory = Objects.requireNonNull(JNDIHelper.getCallFactory(), "CallFactory not found");
        LOG.trace("SipTimerListenerImpl created");
    }

    @Override
    public void timeout(ServletTimer origTimer) {
        LOG.trace("BEGIN ServletTimer timeout SAS: {}, timer: {}, info: {}", origTimer.getApplicationSession().getId(),
                origTimer.getId(), origTimer.getInfo());
        try (ContextLayer cl = CallContext.with(callStore, configBean, callFactory)) {
            ServletTimer timer = ServletTimerAdapter.getAdapter(origTimer);
            SipApplicationSession sas = timer.getApplicationSession();
            String appSessionId = sas.getId();
            String timerId = timer.getId();
            TimerInfo info = (TimerInfo) timer.getInfo();
            if (TimerType.CALL.equals(info.getType())) {
                try (IMSCFCall call = callStore.getCallByAppSessionId(appSessionId)) {
                    if (call == null) {
                        LOG.warn("Cannot find call for timeout of {} ({}) in AppSession {}", info.getName(),
                                timer.getId(), appSessionId);
                        LOG.trace("END   ServletTimer timeout");
                        return;
                    }
                    LOG.debug("Timeout of {} ({}) in AppSession {}", info.getName(), timer.getId(), appSessionId);
                    TimerListener listener = call.removeTimerListener(timerId);
                    if (listener == null) {
                        LOG.warn("Cannot find TimerListener for timeout of {} in AppSession {}", timerId, appSessionId);
                        LOG.trace("END   ServletTimer timeout");
                        return;
                    }
                    listener.timeout(info.getUserInfo());
                }
            } else if (TimerType.APP.equals(info.getType())) {
                LOG.debug("App timer {} ({}) timeout.", info.getName(), timer.getId());
                TimerListener listener;
                // workaround, getTimeRemaining() doesn't seem to work well
                if ("periodic_app_timer".equals(info.getName())) { // periodic
                    listener = SipServletResources.getAppTimerListener(sas, timerId);
                } else { // one-time
                    listener = SipServletResources.removeTimerListener(sas, timerId);
                }
                if (listener == null) {
                    LOG.warn("Cannot find TimerListener for app timer {} ({}) in AppSession {}", info.getName(),
                            timerId, appSessionId);
                    LOG.trace("END   ServletTimer timeout");
                    return;
                }

                listener.timeout(info.getUserInfo());
            }

        } catch (Exception e) {
            LOG.warn("Exception in processing ServletTimer {} in SAS {} (info {}): {}", origTimer.getId(), origTimer
                    .getApplicationSession().getId(), origTimer.getInfo(), e.getMessage(), e);
        }
        LOG.trace("END   ServletTimer timeout");
    }
}
