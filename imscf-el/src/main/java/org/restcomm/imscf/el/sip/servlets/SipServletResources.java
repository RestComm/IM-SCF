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

import org.restcomm.imscf.el.call.TimerListener;
import org.restcomm.imscf.el.sip.adapters.SipFactoryAdapter;
import org.restcomm.imscf.el.sip.adapters.SipSessionsUtilAdapter;
import org.restcomm.imscf.el.sip.adapters.TimerServiceAdapter;
import org.restcomm.imscf.el.sip.servlets.TimerInfo.TimerType;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipSessionsUtil;
import javax.servlet.sip.TimerService;

/**
 * Utility class to allow unmanaged classes to retrieve SIP ServletContext resources.
 * Initialized by {@link org.restcomm.imscf.el.sip.servlets.MainServlet MainServlet} on startup.
 */
public final class SipServletResources {

    private static SipFactory sipFactory;
    private static SipSessionsUtil sipSessionsUtil;
    private static TimerService sipTimerService;
    private static boolean initialized = false;

    /** (AppSessionId,timerId) -> Listener mapping. timerId is prefixed with appSessionId to preserve uniqueness. */
    private static final Map<String, TimerListener> APP_TIMER_LISTENERS = new ConcurrentHashMap<String, TimerListener>();

    private SipServletResources() {
    }

    public static void init(ServletContext servletContext) {
        init((SipFactory) servletContext.getAttribute(SipServlet.SIP_FACTORY),
                (SipSessionsUtil) servletContext.getAttribute(SipServlet.SIP_SESSIONS_UTIL),
                (TimerService) servletContext.getAttribute(SipServlet.TIMER_SERVICE));
    }

    public static void init(SipFactory sf, SipSessionsUtil ssu, TimerService ts) {
        sipFactory = Objects.requireNonNull(SipFactoryAdapter.getAdapter(sf), "SipFactory cannot be null");
        sipSessionsUtil = Objects.requireNonNull(SipSessionsUtilAdapter.getAdapter(ssu),
                "SipSessionsUtil cannot be null");
        sipTimerService = Objects.requireNonNull(TimerServiceAdapter.getAdapter(ts), "TimerService cannot be null");
        initialized = true;
    }

    public static SipFactory getSipFactory() {
        if (!initialized)
            throw new IllegalStateException("SipFactory requested before initialization completed!");
        return sipFactory;
    }

    public static SipSessionsUtil getSipSessionsUtil() {
        if (!initialized)
            throw new IllegalStateException("SipSessionsUtil requested before initialization completed!");
        return sipSessionsUtil;
    }

    public static TimerService getSipTimerService() {
        if (!initialized)
            throw new IllegalStateException("TimerService requested before initialization completed!");
        return sipTimerService;
    }

    public static String getTimerKey(SipApplicationSession sas, String timerId) {
        return sas.getId() + timerId;
    }

    public static String createAppTimer(long delay, Serializable info, TimerListener listener) {
        return createAppTimer(getSipFactory().createApplicationSession(), delay, info, listener);
    }

    public static String createAppTimer(SipApplicationSession appSession, long delay, Serializable info,
            TimerListener listener) {
        String timerId = getSipTimerService().createTimer(appSession, delay, false,
                new TimerInfo(TimerType.APP, "app_timer", info)).getId();
        APP_TIMER_LISTENERS.put(getTimerKey(appSession, timerId), listener);
        return timerId;
    }

    public static String createAppTimer(long delay, long period, boolean fixedDelay, Serializable info,
            TimerListener listener) {
        return createAppTimer(getSipFactory().createApplicationSession(), delay, period, fixedDelay, info, listener);
    }

    public static String createAppTimer(SipApplicationSession appSession, long delay, long period, boolean fixedDelay,
            Serializable info, TimerListener listener) {
        String timerId = getSipTimerService().createTimer(appSession, delay, period, fixedDelay, false,
                new TimerInfo(TimerType.APP, "periodic_app_timer", info)).getId();
        APP_TIMER_LISTENERS.put(getTimerKey(appSession, timerId), listener);
        return timerId;
    }

    static TimerListener getAppTimerListener(SipApplicationSession sas, String timerId) {
        return APP_TIMER_LISTENERS.get(getTimerKey(sas, timerId));
    }

    static TimerListener removeTimerListener(SipApplicationSession sas, String timerId) {
        return APP_TIMER_LISTENERS.remove(getTimerKey(sas, timerId));
    }

    public static void cancelTimer(SipApplicationSession sas, String timerId) {
        APP_TIMER_LISTENERS.remove(getTimerKey(sas, timerId));
        ServletTimer timer = sas.getTimer(timerId);
        if (timer != null) {
            timer.cancel();
        }
    }

}
