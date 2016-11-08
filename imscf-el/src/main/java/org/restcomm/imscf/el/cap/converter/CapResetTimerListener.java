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
package org.restcomm.imscf.el.cap.converter;

import org.restcomm.imscf.common.config.CapModuleType.GeneralProperties;
import org.restcomm.imscf.el.call.TimerListener;
import org.restcomm.imscf.el.cap.call.CAPCSCall;
import org.restcomm.imscf.el.cap.call.CallSegment;
import org.restcomm.imscf.el.cap.call.CallSegment.CallSegmentState;
import org.restcomm.imscf.el.cap.call.CallSegmentAssociationListener;
import org.restcomm.imscf.el.stack.CallContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPDialogState;
import org.mobicents.protocols.ss7.cap.api.primitives.TimerID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Listener that listens for call segment state change and sends resetTimer if needed. */
public class CapResetTimerListener implements CallSegmentAssociationListener, TimerListener {

    private static final Logger LOG = LoggerFactory.getLogger(CapResetTimerListener.class);

    private String imscfCallId;
    private int resetTimerSec;
    private int resetTimerUiSec;

    private Map<Integer, String> timers = new HashMap<Integer, String>(2, 1.0f);

    public CapResetTimerListener(CAPCSCall call) {
        // never store a reference to the call itself
        this.imscfCallId = call.getImscfCallId();
        GeneralProperties gp = call.getCapModule().getModuleConfiguration().getGeneralProperties();
        this.resetTimerSec = gp.getResetTimerSec();
        this.resetTimerUiSec = gp.getResetTimerUiSec();
    }

    @Override
    public void callSegmentStateChanged(CallSegment cs) {
        try (CAPCSCall call = ((CAPCSCall) CallContext.getCallStore().getCallByImscfCallId(imscfCallId))) {
            LOG.trace("CS changed state: {}", cs);
            // Previous timer needs to be canceled, e.g. when transitioning from WFI to WFEOUI or back, which might
            // change the resetTimer period.
            // Cancel is a NOOP for non-existent timer, so it's safe to always call it.
            cancelTimer(call, cs);
            switch (cs.getState()) {
            case WAITING_FOR_INSTRUCTIONS:
            case WAITING_FOR_END_OF_USER_INTERACTION:
                setupTimer(call, cs);
                break;
            case MONITORING:
            case IDLE:
                break;
            default:
                throw new IllegalStateException("Illegal enum value " + cs.getState());
            }
        }
    }

    @Override
    public void callSegmentDestroyed(CallSegment cs) {
        try (CAPCSCall call = ((CAPCSCall) CallContext.getCallStore().getCallByImscfCallId(imscfCallId))) {
            LOG.trace("CS destroyed: {}", cs);
            cancelTimer(call, cs);
        }
    }

    @Override
    public void timeout(Serializable info) {
        try (CAPCSCall call = ((CAPCSCall) CallContext.getCallStore().getCallByImscfCallId(imscfCallId))) {
            CAPDialogState dialogState = call.getCapDialog().getState();
            switch (dialogState) {
            case InitialSent:
            case Expunged:
            case Idle:
                // don't send this time and don't renew the timer either
                LOG.warn("ResetTimer should not be active in CAP dialog state {}, disabling it.", dialogState);
                return;
            case Active:
            case InitialReceived:
            default:
                break;
            }
            int csid = (Integer) info;
            int delay;
            CallSegment cs = call.getCallSegmentAssociation().getCallSegment(csid);
            CallSegmentState state = cs.getState();
            switch (state) {
            case WAITING_FOR_INSTRUCTIONS:
                delay = resetTimerSec + 1;
                break;
            case WAITING_FOR_END_OF_USER_INTERACTION:
                delay = resetTimerUiSec + 1;
                break;
            default:
                throw new IllegalStateException("Timer should have been cancelled, cannot send resetTimer in state "
                        + state);
            }
            try {
                call.getCapDialog().addResetTimerRequest(1, TimerID.tssf, delay, null, csid);
                call.getCapDialog().send();
            } catch (CAPException e) {
                LOG.warn("Failed to send resetTimer in {}", cs, e);
            }

            // setup next timer
            setupTimer(call, cs);
        }
    }

    private void setupTimer(CAPCSCall call, CallSegment cs) {
        long delay = (cs.getState() == CallSegmentState.WAITING_FOR_INSTRUCTIONS ? resetTimerSec : resetTimerUiSec) * 1000;
        if (delay > 0) {
            LOG.trace("Sending resetTimer in {}ms for {}", delay, cs);
            String timerId = call.setupTimer(delay, this, cs.getId(), "resetTimer");
            timers.put(Integer.valueOf(cs.getId()), timerId);
        } else {
            LOG.trace("resetTimer inactive");
        }
    }

    private void cancelTimer(CAPCSCall call, CallSegment cs) {
        String timerId = timers.remove(cs.getId());
        if (timerId != null) {
            LOG.trace("Canceling resetTimer for {}", cs);
            call.cancelTimer(timerId);
        }
    }
}
