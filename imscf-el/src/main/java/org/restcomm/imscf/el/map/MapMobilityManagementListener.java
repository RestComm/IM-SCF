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
package org.restcomm.imscf.el.map;

import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobilityListener;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.SendAuthenticationInfoResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.CancelLocationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.CancelLocationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.PurgeMSRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.PurgeMSResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.SendIdentificationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.SendIdentificationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateGprsLocationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.UpdateLocationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.InsertSubscriberDataRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.InsertSubscriberDataResponse;

/**
 * Restricted MAP Mobility Management listener interface for operations supported by IMSCF. Unsupported operations throw an exception.
 */
public interface MapMobilityManagementListener extends MAPServiceMobilityListener {

    @Override
    default void onAnyTimeInterrogationRequest(AnyTimeInterrogationRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onCancelLocationRequest(CancelLocationRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onCancelLocationResponse(CancelLocationResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onCheckImeiRequest(CheckImeiRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onCheckImeiResponse(CheckImeiResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onInsertSubscriberDataRequest(InsertSubscriberDataRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onInsertSubscriberDataResponse(InsertSubscriberDataResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onPurgeMSRequest(PurgeMSRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onPurgeMSResponse(PurgeMSResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onSendAuthenticationInfoRequest(SendAuthenticationInfoRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onSendAuthenticationInfoResponse(SendAuthenticationInfoResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onSendIdentificationRequest(SendIdentificationRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onSendIdentificationResponse(SendIdentificationResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onUpdateGprsLocationRequest(UpdateGprsLocationRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onUpdateGprsLocationResponse(UpdateGprsLocationResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onUpdateLocationRequest(UpdateLocationRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onUpdateLocationResponse(UpdateLocationResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }
}
