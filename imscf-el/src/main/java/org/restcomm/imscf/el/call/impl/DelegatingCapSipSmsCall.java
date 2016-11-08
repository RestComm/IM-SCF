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

import org.restcomm.imscf.el.cap.call.CapSipSmsCall;
import org.restcomm.imscf.el.cap.call.CapSmsCall;

import org.mobicents.protocols.ss7.cap.api.service.sms.CAPDialogSms;
import org.mobicents.protocols.ss7.cap.api.service.sms.InitialDPSMSRequest;

/** Default delegating interface. */
interface DelegatingCapSipSmsCall extends DelegatingCapCall<CAPDialogSms>, DelegatingSIPCall, CapSipSmsCall {

    @Override
    default CapSipSmsCall getDelegate() {
        return (CapSipSmsCall) DelegatingCapCall.super.getDelegate();
    }

    @Override
    default CapSmsCall.CAPState getSmsCapState() {
        return ((CapSmsCall) getDelegate()).getSmsCapState();
    }

    @Override
    default void setSmsCapState(CapSmsCall.CAPState capState) {
        ((CapSmsCall) getDelegate()).setSmsCapState(capState);
    }

    @Override
    default CapSipSmsCall.SIPState getSipState() {
        return getDelegate().getSipState();
    }

    @Override
    default void setSipState(CapSipSmsCall.SIPState sipState) {
        getDelegate().setSipState(sipState);
    }

    @Override
    default void setIdpSMS(InitialDPSMSRequest idpSms) {
        ((CapSmsCall) getDelegate()).setIdpSMS(idpSms);
    }

    @Override
    default InitialDPSMSRequest getIdpSMS() {
        return ((CapSmsCall) getDelegate()).getIdpSMS();
    }

    @Override
    default String getServiceIdentifier() {
        return getDelegate().getServiceIdentifier();
    }
}
