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

import java.util.List;

import org.mobicents.protocols.ss7.cap.api.isup.CauseCap;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;

/**
 * CAP CS call interface.
 */
public interface CAPCSCall extends CAPCall<CAPDialogCircuitSwitchedCall> {

    /**
     * CAP "state machine" state list.
     */
    public static enum CAPState {
        /** IDP has arrived and no connect attempt was made yet.*/
        IDP_ARRIVED,
        /** At least one connect attempt was made already. This state is used even if that attempt was not successful at establishing a call. */
        FOLLOWON_CALL,

        TERMINATED;
    }

    /**
     *  BCSM type of the call.
     */
    public static enum BCSMType {
        // TODO: what if an ICA leg with oBCSM is connected to an IDP leg with tBCSM? What is the BCSM type of the call
        // then? Should this be stored on a leg by leg basis?
        oBCSM, tBCSM
    }

    void setBCSMType(BCSMType bcsmType);

    BCSMType getBCSMType();

    CAPState getCsCapState();

    void setCsCapState(CAPState capState);

    void setIdp(InitialDPRequest idp);

    InitialDPRequest getIdp();

    /** Must return a writable live list. */
    List<BCSMEvent> getBCSMEventsForPendingRrbcsm();

    void setPendingReleaseCause(CauseCap cause);

    CauseCap getPendingReleaseCause();

    CallSegmentAssociation getCallSegmentAssociation();

    @Override
    default String getServiceIdentifier() {
        if (getIdp() != null) {
            return "SK_" + String.format("%05d", getIdp().getServiceKey());
        } else if (getAsProvidedServiceIdentifier() != null) {
            return getAsProvidedServiceIdentifier();
        } else {
            return "UNDEFINED";
        }
    }

    String getAsProvidedServiceIdentifier();

    void setAsProvidedServiceIdentifier(String id);

    boolean isAutomaticCallProcessingEnabled();

    void setAutomaticCallProcessingEnabled(boolean enabled);
}
