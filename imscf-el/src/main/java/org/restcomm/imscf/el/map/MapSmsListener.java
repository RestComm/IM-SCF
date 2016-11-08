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

import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSmsListener;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;

/**
 * Restricted MAP SMS listener interface for operations supported by IMSCF. Unsupported operations throw an exception.
 */
public interface MapSmsListener extends MAPServiceSmsListener {

    @Override
    default void onAlertServiceCentreRequest(AlertServiceCentreRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onAlertServiceCentreResponse(AlertServiceCentreResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onForwardShortMessageRequest(ForwardShortMessageRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onForwardShortMessageResponse(ForwardShortMessageResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onInformServiceCentreRequest(InformServiceCentreRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onMoForwardShortMessageRequest(MoForwardShortMessageRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onMoForwardShortMessageResponse(MoForwardShortMessageResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onMtForwardShortMessageRequest(MtForwardShortMessageRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onMtForwardShortMessageResponse(MtForwardShortMessageResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onReportSMDeliveryStatusRequest(ReportSMDeliveryStatusRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onReportSMDeliveryStatusResponse(ReportSMDeliveryStatusResponse arg0) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    default void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest arg0) {
        throw new RuntimeException("Operation not supported");
    }

}
