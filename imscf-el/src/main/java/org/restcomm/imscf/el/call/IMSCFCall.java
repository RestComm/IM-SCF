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
package org.restcomm.imscf.el.call;

import org.restcomm.imscf.el.sip.servlets.SipServletResources;
import org.restcomm.imscf.common.util.history.CallHistory;

import java.io.Serializable;
import java.util.List;
import javax.servlet.sip.SipApplicationSession;

/**
 * Common call interface for all call types, i.e. CAP, MAP and Diameter.
 */
public interface IMSCFCall extends AutoCloseable {

    String getImscfCallId();

    void setImscfCallId(String imscfCallId);

    String getAppSessionId();

    default SipApplicationSession getAppSession() {
        String id = getAppSessionId();
        return id == null ? null : SipServletResources.getSipSessionsUtil().getApplicationSessionById(id);
    }

    void setAppSessionId(String appSessionId);

    @Override
    void close();

    String setupTimer(long timeout, TimerListener listener);

    String setupTimer(long timeout, TimerListener listener, Serializable info, String name);

    TimerListener removeTimerListener(String timerId);

    void cancelTimer(String timerId);

    void cancelAllTimers();

    List<Object> getEventQueue();

    long getCreationTime();

    default long getAge() {
        return System.currentTimeMillis() - getCreationTime();
    }

    /**
     * Maximum lifetime of the call in milliseconds.
     * @return A value in milliseconds.
     */
    long getMaxAge();

    void setImscfState(ImscfCallLifeCycleState state);

    ImscfCallLifeCycleState getImscfState();

    void addImscfLifeCycleListener(ImscfCallLifeCycleListener listener);

    void removeImscfLifeCycleListener(ImscfCallLifeCycleListener listener);

    CallHistory getCallHistory();

    /**
     * This method should return a string identifying the service of this call. The result is used as a category in the MBean statistics.
     * <p>
     * <li>For network initiated CAP CS/SMS calls, this could be the value of the serviceKey from the initialDP.
     * <li>For AS initiated calls, it could be some other identifier received from the AS.
     * <li>For diameter calls, this could be the value of the serviceContextId.
     */
    String getServiceIdentifier();
}
