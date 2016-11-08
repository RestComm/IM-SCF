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
package org.restcomm.imscf.el.cap.call;

import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;

import org.restcomm.imscf.el.call.CapSipCall;

/**
 * Interface for CAP&lt;-&gt;SIP call conversions.
 */
public interface CapSipCsCall extends CAPCSCall, CapSipCall<CAPDialogCircuitSwitchedCall> {

    /**
     * SIP interface "state machine" state list.
     */
    public static enum SIPState {
        IDP_NOTIFIED, STATELESS_CONTINUE_REQUESTED;
    }

    void setSipState(CapSipCsCall.SIPState sipState);

    CapSipCsCall.SIPState getSipState();

}
