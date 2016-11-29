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
import org.restcomm.imscf.el.call.ImscfCallLifeCycleState;
import org.restcomm.imscf.el.call.TimerListener;
import org.restcomm.imscf.el.stack.CallContext;

import java.io.Serializable;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Listener for calls reaching max allowed age. */
public class ImscfCallMaxAgeTimerListener implements TimerListener {

    private static final Logger LOG = LoggerFactory.getLogger(ImscfCallMaxAgeTimerListener.class);

    private IMSCFCall call;

    public ImscfCallMaxAgeTimerListener(IMSCFCall call) {
        this.call = call;
    }

    @Override
    public void timeout(Serializable info) {
        LOG.debug("Max age timeout after {} for {}", Duration.ofMillis(call.getAge()), call);
        call.getCallHistory().addEvent("MAX_AGE");
        // if modules fail to release the call within a minute, we forcefully delete it.
        // if modules did release and delete the call, this timer will never fire.
        call.setupTimer(60000, new TimerListener() {
            @Override
            public void timeout(Serializable info) {
                LOG.warn("Call release failed, deleting call.");
                CallContext.getCallFactory().deleteCall(call);
            }
        }, null, "maxAgeDeleteAfterRelease");
        if (call.getImscfState() == ImscfCallLifeCycleState.ACTIVE) {
            LOG.debug("Forcing call to RELEASING state");
            // module is triggered to release in the life cycle callback method
            call.setImscfState(ImscfCallLifeCycleState.RELEASING);
        }
    }

}
