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

import org.restcomm.imscf.el.cap.scenarios.CapRequestScenarioBase.CapRequestScenarioFailureNotIndicated;
import org.restcomm.imscf.el.cap.scenarios.CapRequestScenarioBase.CapRequestScenarioSuccessNotIndicated;

import org.mobicents.protocols.ss7.tcap.api.tc.component.InvokeClass;

/**
 * Class 4 operation: Neither success, nor failure are indicated.
 * */
interface CapRequestScenarioClass4 extends CapRequestScenarioSuccessNotIndicated, CapRequestScenarioFailureNotIndicated {
    @Override
    default InvokeClass getInvokeClass() {
        return InvokeClass.Class4;
    }

    @Override
    default void onSuccessTimeout() {
        throw new IllegalStateException();
    }

    @Override
    default void onFailureTimeout() {
        throw new IllegalArgumentException();
    }

}
