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

import org.restcomm.imscf.el.cap.call.CallSegmentAssociation;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;

import org.mobicents.protocols.ss7.cap.api.isup.CauseCap;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;

/** Default delegating interface. */
interface DelegatingCapSipCsCall extends DelegatingCapCall<CAPDialogCircuitSwitchedCall>, DelegatingSIPCall,
        CapSipCsCall {

    @Override
    default CapSipCsCall getDelegate() {
        return (CapSipCsCall) DelegatingCapCall.super.getDelegate();
    }

    @Override
    default CapSipCsCall.CAPState getCsCapState() {
        return getDelegate().getCsCapState();
    }

    @Override
    default void setCsCapState(CapSipCsCall.CAPState capState) {
        getDelegate().setCsCapState(capState);
    }

    @Override
    default CapSipCsCall.SIPState getSipState() {
        return getDelegate().getSipState();
    }

    @Override
    default void setSipState(CapSipCsCall.SIPState sipState) {
        getDelegate().setSipState(sipState);
    }

    @Override
    default void setIdp(InitialDPRequest idp) {
        getDelegate().setIdp(idp);
    }

    @Override
    default InitialDPRequest getIdp() {
        return getDelegate().getIdp();
    }

    @Override
    default List<BCSMEvent> getBCSMEventsForPendingRrbcsm() {
        return getDelegate().getBCSMEventsForPendingRrbcsm();
    }

    @Override
    default CauseCap getPendingReleaseCause() {
        return getDelegate().getPendingReleaseCause();
    }

    @Override
    default void setPendingReleaseCause(CauseCap cause) {
        getDelegate().setPendingReleaseCause(cause);
    }

    @Override
    default CallSegmentAssociation getCallSegmentAssociation() {
        return getDelegate().getCallSegmentAssociation();
    }

    @Override
    default String getServiceIdentifier() {
        return getDelegate().getServiceIdentifier();
    }

    @Override
    default String getAsProvidedServiceIdentifier() {
        return getDelegate().getAsProvidedServiceIdentifier();
    }

    @Override
    default void setAsProvidedServiceIdentifier(String id) {
        getDelegate().setAsProvidedServiceIdentifier(id);
    }

    @Override
    default boolean isAutomaticCallProcessingEnabled() {
        return getDelegate().isAutomaticCallProcessingEnabled();
    }

    @Override
    default void setAutomaticCallProcessingEnabled(boolean enabled) {
        getDelegate().setAutomaticCallProcessingEnabled(enabled);
    }

    @Override
    default BCSMType getBCSMType() {
        return getDelegate().getBCSMType();
    }

    @Override
    default void setBCSMType(BCSMType bcsmType) {
        getDelegate().setBCSMType(bcsmType);
    }
}
