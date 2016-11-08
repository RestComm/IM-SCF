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
import org.restcomm.imscf.el.call.TimerListener;
import org.restcomm.imscf.common.util.history.CallHistory;

import java.io.Serializable;
import java.util.List;

/** Default delegating interface. */
interface DelegatingIMSCFCall extends IMSCFCall {

    /**
     * Default implementation allows subinterfaces to override this method.
     * Implementors MUST implement this properly however.
     */
    default IMSCFCall getDelegate() {
        throw new UnsupportedOperationException();
    }

    @Override
    default String getImscfCallId() {
        return getDelegate().getImscfCallId();
    }

    @Override
    default void setImscfCallId(String imscfCallId) {
        getDelegate().setImscfCallId(imscfCallId);
    }

    @Override
    default String getAppSessionId() {
        return getDelegate().getAppSessionId();
    }

    @Override
    default void setAppSessionId(String sipAppSessionId) {
        getDelegate().setAppSessionId(sipAppSessionId);
    }

    @Override
    default void cancelTimer(String timerId) {
        getDelegate().cancelTimer(timerId);
    }

    @Override
    default void cancelAllTimers() {
        getDelegate().cancelAllTimers();
    }

    @Override
    default String setupTimer(long timeout, TimerListener listener) {
        return getDelegate().setupTimer(timeout, listener);
    }

    @Override
    default String setupTimer(long timeout, TimerListener listener, Serializable info, String name) {
        return getDelegate().setupTimer(timeout, listener, info, name);
    }

    @Override
    default TimerListener removeTimerListener(String timerId) {
        return getDelegate().removeTimerListener(timerId);
    }

    @Override
    default List<Object> getEventQueue() {
        return getDelegate().getEventQueue();
    }

    @Override
    default long getCreationTime() {
        return getDelegate().getCreationTime();
    }

    @Override
    default long getMaxAge() {
        return getDelegate().getMaxAge();
    }

    @Override
    default void setImscfState(ImscfCallLifeCycleState state) {
        getDelegate().setImscfState(state);
    }

    @Override
    default ImscfCallLifeCycleState getImscfState() {
        return getDelegate().getImscfState();
    }

    @Override
    default void addImscfLifeCycleListener(ImscfCallLifeCycleListener listener) {
        getDelegate().addImscfLifeCycleListener(listener);
    }

    @Override
    default void removeImscfLifeCycleListener(ImscfCallLifeCycleListener listener) {
        getDelegate().removeImscfLifeCycleListener(listener);
    }

    @Override
    default CallHistory getCallHistory() {
        return getDelegate().getCallHistory();
    }

    @Override
    default String getServiceIdentifier() {
        return getDelegate().getServiceIdentifier();
    }
}
