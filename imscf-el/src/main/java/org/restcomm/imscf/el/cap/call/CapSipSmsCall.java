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
package org.restcomm.imscf.el.cap.call;

import org.mobicents.protocols.ss7.cap.api.service.sms.CAPDialogSms;

import org.restcomm.imscf.el.call.CapSipCall;

/**
 * Interface for CAP SMS &lt;-&gt; SIP conversions.
 */
public interface CapSipSmsCall extends CapSmsCall, CapSipCall<CAPDialogSms> {

    /**
     * SIP interface "state machine" state list.
     */
    public static enum SIPState {
        IDPSMS_NOTIFIED, STATELESS_CONTINUE_REQUESTED, STATEFUL_CONTINUE_REQUESTED, ERSMS_NOTIFIED, TERMINATED;
    }

    void setSipState(CapSipSmsCall.SIPState sipState);

    CapSipSmsCall.SIPState getSipState();
}
