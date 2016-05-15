/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011�2016, Telestax Inc and individual contributors
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
package org.restcomm.imscf.el.map.scenarios;

import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

/**
 * Scenario abstraction for outgoing MAP operations.
 * @author Miklos Pocsaji
 *
 */
public interface MapOutgoingRequestScenario {
    Long getInvokeId();

    /**
     * Called when a returnResult(Last) arrives for this invoke.
     */
    void onReturnResult(MAPMessage response);

    /**
     * Called when a returnError arrives for this invoke.
     */
    void onErrorComponent(MAPErrorMessage error);

    /**
     * Only called when a reject component arrives from the remote peer for this class 1/2 operation,
     * not for locally generated rejects.
     */
    void onRejectComponent(Problem problem);

    /**
     * Called when this invoke operation times out.
     * <li>Class 1 – Both success and failure are reported, invoke timeout is an abnormal error case.
     * <li>Class 2 – Only failure is reported, invoke timeout means operation success.
     * <li>Class 3 – Only success is reported, invoke timeout means operation failure.
     * <li>Class 4 – Neither success, nor failure is reported, invoke timeout should be ignored.</li>
     */
    void onInvokeTimeout();

}
