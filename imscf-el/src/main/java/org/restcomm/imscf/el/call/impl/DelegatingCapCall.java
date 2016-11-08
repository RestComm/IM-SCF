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

import java.util.List;

import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.call.CAPCall;
import org.restcomm.imscf.el.cap.scenarios.CapIncomingRequestScenario;
import org.restcomm.imscf.el.cap.scenarios.CapOutgoingRequestScenario;

import org.mobicents.protocols.ss7.cap.api.CAPDialog;

/** Default delegating interface.
 * @param <CapType> the cap dialog type of the specific call.
 * */
interface DelegatingCapCall<CapType extends CAPDialog> extends DelegatingTcapCall, CAPCall<CapType> {

    @SuppressWarnings("unchecked")
    @Override
    default CAPCall<CapType> getDelegate() {
        return (CAPCall<CapType>) DelegatingTcapCall.super.getDelegate();
    }

    @Override
    default void setCapDialog(CapType capDialog) {
        getDelegate().setCapDialog(capDialog);
    }

    @Override
    default CapType getCapDialog() {
        return getDelegate().getCapDialog();
    }

    @Override
    default CAPModule getCapModule() {
        return getDelegate().getCapModule();
    }

    @Override
    default void setCapModule(CAPModule capModule) {
        getDelegate().setCapModule(capModule);
    }

    @Override
    default List<CapOutgoingRequestScenario> getCapOutgoingRequestScenarios() {
        return getDelegate().getCapOutgoingRequestScenarios();
    };

    @Override
    default List<CapIncomingRequestScenario<?>> getCapIncomingRequestScenarios() {
        return getDelegate().getCapIncomingRequestScenarios();
    };

    @Override
    default long getMaxAge() {
        return DelegatingTcapCall.super.getMaxAge();
    }
}
