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

import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.modules.Module;
import org.restcomm.imscf.el.sip.servlets.AppSessionHelper;
import org.restcomm.imscf.util.IteratorStream;

import javax.servlet.sip.SipApplicationSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for SIP modules.
 */
public interface SipModule extends Module {
    Logger LOG = LoggerFactory.getLogger(SipModule.class);

    int getAppSessionTimeoutMin();

    void scenarioFinished(SIPCall call, Scenario s);

    void sessionExpired(SipApplicationSession sas);

    void sessionDestroyed(SipApplicationSession sas);

    void handleAsUnavailableError(SIPCall call);

    void handleAsReactionTimeout(SIPCall call);

    /**
     * Determines whether the SIP side of a call is finished.
     * This method should only return true when no more call handling is possible
     * for this call via incoming/outgoing messages in either existing or future
     * SIP dialogs.
     * @implSpec The default implementation returns true if the SipApplicationSession is already invalid or
     * all SipSessions are effectively terminated.
     *
     * @return true if the SIP side view of the call is finished. */
    default boolean isSipCallFinished(SIPCall call) {
        SipApplicationSession sas = call.getAppSession();
        if (sas == null || !sas.isValid())
            return true;
        boolean ret = IteratorStream.of(AppSessionHelper.getSipSessions(sas)).allMatch(SipUtil::isUaTerminated);
        LOG.trace("SIP side of the call is {}", ret ? "finished" : "alive");
        return ret;
    }

    /**
     * Callback to handle the other side (e.g. CAP, MAP) of the call for which
     * the AS control relationship doesn't exist anymore.
     * <p>
     * This method is called after {@link #isSipCallFinished(SIPCall)} first returns
     * true for the specified SIPCall. At this point, the module MUST NOT attempt to
     * create any further SIP dialogs for the call.
     *  */
    void handleSipCallFinished(SIPCall call);
}
