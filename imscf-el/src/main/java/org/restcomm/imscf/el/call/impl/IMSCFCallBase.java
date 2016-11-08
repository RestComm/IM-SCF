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
import org.restcomm.imscf.el.call.ImscfCallLifeCycleListener;
import org.restcomm.imscf.el.call.ImscfCallLifeCycleState;
import org.restcomm.imscf.el.call.MDCParameters;
import org.restcomm.imscf.el.call.TimerListener;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;
import org.restcomm.imscf.el.sip.servlets.TimerInfo;
import org.restcomm.imscf.el.sip.servlets.TimerInfo.TimerType;
import org.restcomm.imscf.common.util.history.CallHistory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Abstract base class for IMSCF calls.
 * AutoCloseable is implemented so that Call can be used as a resource, automatically cleaning up threadlocal modifications after itself.
 */
public abstract class IMSCFCallBase implements IMSCFCall {

    private static final Logger LOG = LoggerFactory.getLogger(IMSCFCallBase.class);

    private String imscfCallId;
    private Map<MDCParameters, String> mdcMap = new HashMap<>();
    private List<Object> eventQueue = new ArrayList<Object>();
    private Map<String, TimerListener> timerListeners = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock(false); // no fairness required
    private ImscfCallLifeCycleState imscfState = ImscfCallLifeCycleState.INITIAL;
    private long creationTime = System.currentTimeMillis();
    private Set<ImscfCallLifeCycleListener> imscfLifeCycleListeners = new HashSet<>();
    private CallHistory callHistory;
    int timerIdCounter = 0;

    public void lock() {
        LOG.trace("Trying to lock");
        lock.lock(); // blocking lock
        LOG.trace("Lock acquired, hold count is {}", lock.getHoldCount());
    }

    /**
     * Unlocks the call lock (decrements lock counter).
     * @return true if the lock hold count is now zero and the thread no longer holds a lock to the call.
     */
    public boolean unlock() {
        assert lock.isHeldByCurrentThread();
        boolean last = lock.getHoldCount() == 1;
        if (last) {
            LOG.trace("Releasing last lock");
        } else {
            LOG.trace("Releasing lock");
        }
        lock.unlock();
        return last;
    }

    @Override
    public final String getImscfCallId() {
        return imscfCallId;
    }

    @Override
    public final void setImscfCallId(String imscfCallId) {
        this.imscfCallId = imscfCallId;

        getMdcMap().put(MDCParameters.IMSCF_CALLID, this.imscfCallId);
        this.callHistory = new CallHistory(imscfCallId);
    }

    @Override
    public ImscfCallLifeCycleState getImscfState() {
        return imscfState;
    }

    @Override
    public void setImscfState(ImscfCallLifeCycleState newState) {
        ImscfCallLifeCycleState oldState = imscfState;
        switch (imscfState) {
        case INITIAL:
            if (newState == ImscfCallLifeCycleState.ACTIVE || newState == ImscfCallLifeCycleState.FINISHED)
                break;
            else
                throw new IllegalStateException("Cannot change state from " + oldState + " to " + newState);
        case ACTIVE:
            if (newState == ImscfCallLifeCycleState.RELEASING || newState == ImscfCallLifeCycleState.FINISHED)
                break;
            else
                throw new IllegalStateException("Cannot change state from " + oldState + " to " + newState);
        case RELEASING:
            if (newState == ImscfCallLifeCycleState.FINISHED)
                break;
            else
                throw new IllegalStateException("Cannot change state from " + oldState + " to " + newState);
        case FINISHED:
        default:
            throw new IllegalStateException("Cannot change state from " + oldState + " to " + newState);
        }
        imscfState = newState;
        // new list to avoid concurrent modification in case a listener is added/removed during the callback
        new ArrayList<>(imscfLifeCycleListeners).stream().forEach(al -> {
            try {
                al.imscfCallStateChanged(this, oldState);
            } catch (Exception e) {
                LOG.warn("Error invoking IMSCF call lifecycle listener", e);
            }
        });
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public void addImscfLifeCycleListener(ImscfCallLifeCycleListener listener) {
        imscfLifeCycleListeners.add(listener);
    }

    @Override
    public void removeImscfLifeCycleListener(ImscfCallLifeCycleListener listener) {
        imscfLifeCycleListeners.remove(listener);
    }

    public static final void clearMDC() {
        // remove only the used attributes instead of MDC.clear()
        for (MDCParameters p : MDCParameters.values()) {
            MDC.remove(p.getKey());
        }
    }

    private static void toMDC(MDCParameters s, Object o) {
        MDC.put(s.getKey(), String.valueOf(o));
    }

    public final void populateMDC() {
        clearMDC();
        for (Entry<MDCParameters, String> e : mdcMap.entrySet()) {
            toMDC(e.getKey(), e.getValue());
        }
    }

    protected Map<MDCParameters, String> getMdcMap() {
        return mdcMap;
    }

    @Override
    public final void close() {
        clearMDC();
    }

    @Override
    public List<Object> getEventQueue() {
        return eventQueue;
    }

    @Override
    public TimerListener removeTimerListener(String timerId) {
        return timerListeners.remove(timerId);
    }

    @Override
    public void cancelTimer(String timerId) {
        removeTimerListener(timerId);
        if (getAppSessionId() == null) {
            boolean ok = ManagedScheduledTimerService.getInstance().cancelTimer(timerId);
            if (ok) {
                LOG.trace("Cancelled timer {}", timerId);
            } else {
                LOG.warn("Tried to cancel nonexistent timer {}!", timerId);
            }

        } else {
            SipApplicationSession sas = getAppSession();
            if (sas == null) {
                LOG.trace("SipApplicationSession already destroyed for call {}", imscfCallId);
                return;
            }
            ServletTimer st = sas.getTimer(timerId);
            if (st != null) {
                st.cancel();
                LOG.trace("Cancelled timer {}", timerId);
            } else {
                LOG.warn("Tried to cancel nonexistent timer {}!", timerId);
            }
        }
    }

    @Override
    public void cancelAllTimers() {
        Set<String> keys = new HashSet<>(timerListeners.keySet());
        LOG.trace("Cancelling all timers: {}", keys);
        for (String timer : keys) {
            cancelTimer(timer);
        }
    }

    @Override
    public String setupTimer(long timeout, TimerListener listener) {
        return setupTimer(timeout, listener, null, "callTimer");
    }

    @Override
    public String setupTimer(long timeout, TimerListener listener, Serializable info, String name) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Timer delay must be > 0!");

        TimerInfo timerInfo = new TimerInfo(TimerType.CALL, name, info);
        String timerId;
        if (getAppSessionId() == null) {
            timerId = imscfCallId + "-timer-" + (++timerIdCounter);
            ManagedScheduledTimerService.getInstance().createTimer(imscfCallId, timerId, timeout, timerInfo);
        } else {
            timerId = SipServletResources.getSipTimerService().createTimer(getAppSession(), timeout, false, timerInfo)
                    .getId();
        }

        timerListeners.put(timerId, listener);
        LOG.trace("Created timer {} ({}) with delay {}ms", name, timerId, timeout);
        LOG.trace("Active call timers: {}", timerListeners.keySet());
        return timerId;
    }

    @Override
    public final CallHistory getCallHistory() {
        return callHistory;
    }
}
