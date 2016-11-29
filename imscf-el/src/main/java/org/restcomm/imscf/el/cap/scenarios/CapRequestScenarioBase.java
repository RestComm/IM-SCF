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
package org.restcomm.imscf.el.cap.scenarios;

import org.mobicents.protocols.ss7.cap.api.CAPMessage;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.tcap.api.tc.component.InvokeClass;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;

/**
 * Scenario primitive for CAP messages, base implementation with additional methods to help implementors.
 */
public interface CapRequestScenarioBase extends CapOutgoingRequestScenario {

    InvokeClass getInvokeClass();

    void onSuccessIndicated(CAPMessage response);

    void onSuccessTimeout();

    void onFailureIndicated(CAPErrorMessage error, Problem problem);

    void onFailureTimeout();

    /**
     * Called when a returnResult(Last) has arrived for this invoke.
     */
    @Override
    default void onReturnResult(CAPMessage response) {
        switch (getInvokeClass()) {
        case Class1:
        case Class3:
            onSuccessIndicated(response);
            return;
        default:
            throw new IllegalStateException("returnResult cannot arrive for " + getInvokeClass() + " operation.");
        }
    }

    /**
     * Called when a returnError arrives for this invoke.
     */
    @Override
    default void onErrorComponent(CAPErrorMessage error) {
        switch (getInvokeClass()) {
        case Class1:
        case Class2:
            onFailureIndicated(error, null);
            return;
        default:
            throw new IllegalStateException("errorComponent cannot arrive for " + getInvokeClass() + " operation.");
        }
    }

    /**
     * Only called when a reject component arrives from the remote peer for this class 1/2 operation,
     * not for locally generated rejects.
     */
    @Override
    default void onRejectComponent(Problem problem) {
        switch (getInvokeClass()) {
        case Class1:
        case Class2:
            onFailureIndicated(null, problem);
            return;
        default:
            throw new IllegalStateException("rejectComponent cannot arrive for " + getInvokeClass() + " operation.");
        }
    }

    /**
     * Called when this invoke operation times out.
     * <li>Class 1 – Both success and failure are reported, invoke timeout is an abnormal error case.
     * <li>Class 2 – Only failure is reported, invoke timeout means operation success.
     * <li>Class 3 – Only success is reported, invoke timeout means operation failure.
     * <li>Class 4 – Neither success, nor failure is reported, invoke timeout is ignored.</li>
     * <p>The default implementation simply calls success/failure based on the above logic.
     */
    @Override
    default void onInvokeTimeout() {
        switch (getInvokeClass()) {
        case Class1:
        case Class3:
            onFailureTimeout();
            return;
        case Class2:
            onSuccessTimeout();
            return;
        case Class4:
            return;
        default:
            throw new IllegalArgumentException("Invalid enum value: " + getInvokeClass());
        }
    }

    /***/
    interface CapRequestScenarioFailureIndicated extends CapRequestScenarioBase {

    }

    /***/
    interface CapRequestScenarioFailureNotIndicated extends CapRequestScenarioBase {
        @Override
        default void onFailureIndicated(CAPErrorMessage error, Problem problem) {
            throw new IllegalStateException();
        }
    }

    /**
     *
     * @param <Response> response type.
     */
    interface CapRequestScenarioSuccessIndicated<Response extends CAPMessage> extends CapRequestScenarioBase {

        void onSuccess(Response response);

        @Override
        @SuppressWarnings("unchecked")
        default void onSuccessIndicated(CAPMessage response) {
            onSuccess((Response) response);
        }

        @Override
        default void onSuccessTimeout() {
            throw new IllegalStateException();
        }

    }

    /***/
    interface CapRequestScenarioSuccessNotIndicated extends CapRequestScenarioBase {

        @Override
        default void onSuccessIndicated(CAPMessage response) {
            throw new IllegalStateException();
        }

    }

}
