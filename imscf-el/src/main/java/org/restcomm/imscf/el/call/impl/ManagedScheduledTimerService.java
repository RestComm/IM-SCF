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
package org.restcomm.imscf.el.call.impl;

import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.call.TimerListener;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.sip.servlets.TimerInfo;
import org.restcomm.imscf.el.sip.servlets.TimerInfo.TimerType;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;
import org.restcomm.imscf.util.JNDIHelper;
import org.restcomm.imscf.util.NamedScheduledExecutorService;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Managed scheduled execution for non-SIP call timers.
 */
public class ManagedScheduledTimerService {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedScheduledTimerService.class);

    private static ManagedScheduledTimerService instance;

    private final ConfigBean configBean;
    private final CallStore callStore;
    private final CallFactoryBean callFactory;
    private final NamedScheduledExecutorService executorService;

    public ManagedScheduledTimerService() {
        callStore = Objects.requireNonNull(JNDIHelper.getCallStore(), "CallStore not found");
        configBean = Objects.requireNonNull(JNDIHelper.getConfigBean(), "ConfigBean not found");
        callFactory = Objects.requireNonNull(JNDIHelper.getCallFactory(), "CallFactory not found");
        executorService = new NamedScheduledExecutorService(JNDIHelper.getManagedScheduledExecutorService());
        LOG.trace("ScheduledTimerListenerImpl created");
    }

    public static void initialize() {
        instance = new ManagedScheduledTimerService();
    }

    public static ManagedScheduledTimerService getInstance() {
        if (instance == null)
            throw new IllegalStateException("ScheduledTimerListenerImpl must be initialized before use!");
        return instance;
    }

    public String createTimer(String imscfCallId, String timerId, long delay, TimerInfo info) {
        executorService.scheduleNamedTask(timerId, () -> {
            getInstance().timeout(imscfCallId, timerId, info);
        }, delay, TimeUnit.MILLISECONDS);
        return timerId;
    }

    public boolean cancelTimer(String timerId) {
        return executorService.cancelNamedTask(timerId);
    }

    private void timeout(String imscfCallId, String timerId, TimerInfo info) {
        LOG.trace("BEGIN ScheduledTimer timeout for call {}, timer: {}, info: {}", imscfCallId, timerId, info);
        try (ContextLayer cl = CallContext.with(callStore, configBean, callFactory)) {
            if (TimerType.CALL.equals(info.getType())) {
                try (IMSCFCall call = callStore.getCallByImscfCallId(imscfCallId)) {
                    if (call == null) {
                        LOG.warn("Cannot find call {} for timeout of {} ({})", imscfCallId, info.getName(), timerId);
                        LOG.trace("END   ScheduledTimer timeout");
                        return;
                    }
                    LOG.debug("Timeout of {} ({})", info.getName(), timerId);
                    TimerListener listener = call.removeTimerListener(timerId);
                    if (listener == null) {
                        LOG.warn("Cannot find TimerListener for timeout of {} in {}", timerId, call);
                        LOG.trace("END   ScheduledTimer timeout");
                        return;
                    }
                    listener.timeout(info.getUserInfo());
                }
            } else {
                throw new AssertionError();
            }

        } catch (Exception e) {
            LOG.warn("Exception in processing ScheduledTimer {} in call {} (info {}): {}", timerId, imscfCallId, info,
                    e.getMessage(), e);
        }
        LOG.trace("END   ScheduledTimer timeout");
    }
}
