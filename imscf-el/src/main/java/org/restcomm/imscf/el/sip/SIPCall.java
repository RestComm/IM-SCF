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
package org.restcomm.imscf.el.sip;

import org.restcomm.imscf.common.config.SipApplicationServerGroupType;
import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.sip.failover.SipAsLoadBalancer.FailoverContext;
import org.restcomm.imscf.el.sip.servlets.AppSessionHelper;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

/**
 * SIP related common interface for calls.
 */
public interface SIPCall extends IMSCFCall {

    void setSipModule(SipModule sipModule);

    SipModule getSipModule();

    /** Non-null live list. */
    List<Scenario> getSipScenarios();

    default void add(Scenario s) {
        getSipScenarios().add(s);
    }

    default void remove(Scenario s) {
        getSipScenarios().remove(s);
    }

    default void removeIf(Class<? extends Scenario> c) {
        getSipScenarios().removeIf(s -> c.isInstance(s));
    }

    default void removeIf(Predicate<Scenario> p) {
        getSipScenarios().removeIf(p);
    }

    /**
     * Enqueues a request for sending in its SipSession. The message will be sent when
     * at least a provisional (not 100) response has been received for all prior requests
     * in the same SipSession. Note that responses need not be queued.
     */
    void queueMessage(SipServletRequest msg);

    /**
     * Returns an iterator over the SIP sessions of this call.
     */
    default Iterator<SipSession> getSipSessions() {
        return AppSessionHelper.getSipSessions(getAppSession());
    }

    /** Live list: after the call was routed through the first group, it is removed from the list. */
    List<SipApplicationServerGroupType> getAppChain();

    void setFailoverContext(FailoverContext ctx);

    FailoverContext getFailoverContext();

}
