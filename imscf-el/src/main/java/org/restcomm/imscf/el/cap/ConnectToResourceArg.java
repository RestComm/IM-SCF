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
package org.restcomm.imscf.el.cap;

import org.mobicents.protocols.ss7.cap.api.isup.CalledPartyNumberCap;
import org.mobicents.protocols.ss7.cap.api.primitives.CAPExtensions;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.ServiceInteractionIndicatorsTwo;

/** Class for holding a ConnectToResourceArg message. */
public final class ConnectToResourceArg {

    private CalledPartyNumberCap resourceAddressIPRoutingAddress;
    private boolean resourceAddressNull;
    private CAPExtensions extensions = null;
    private ServiceInteractionIndicatorsTwo serviceInteractionIndicatorsTwo;
    private Integer callSegmentID;

    public CalledPartyNumberCap getResourceAddressIPRoutingAddress() {
        return resourceAddressIPRoutingAddress;
    }

    public void setResourceAddressIPRoutingAddress(CalledPartyNumberCap resourceAddressIPRoutingAddress) {
        this.resourceAddressIPRoutingAddress = resourceAddressIPRoutingAddress;
    }

    public boolean isResourceAddressNull() {
        return resourceAddressNull;
    }

    public void setResourceAddressNull(boolean resourceAddressNull) {
        this.resourceAddressNull = resourceAddressNull;
    }

    public CAPExtensions getExtensions() {
        return extensions;
    }

    public void setExtensions(CAPExtensions extensions) {
        this.extensions = extensions;
    }

    public ServiceInteractionIndicatorsTwo getServiceInteractionIndicatorsTwo() {
        return serviceInteractionIndicatorsTwo;
    }

    public void setServiceInteractionIndicatorsTwo(ServiceInteractionIndicatorsTwo serviceInteractionIndicatorsTwo) {
        this.serviceInteractionIndicatorsTwo = serviceInteractionIndicatorsTwo;
    }

    public Integer getCallSegmentID() {
        return callSegmentID;
    }

    public void setCallSegmentID(Integer callSegmentID) {
        this.callSegmentID = callSegmentID;
    }

}
