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
package org.restcomm.imscf.el.cap;

import org.mobicents.protocols.ss7.cap.api.service.sms.CAPServiceSmsListener;
import org.mobicents.protocols.ss7.cap.api.service.sms.ConnectSMSRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.ContinueSMSRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.FurnishChargingInformationSMSRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.ReleaseSMSRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.RequestReportSMSEventRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.ResetTimerSMSRequest;

/**
 * Interface for CAP gsmSCF modules (phase 3-4) listening for SMS events.
 * This is a restriction of the {@link CAPServiceSmsListener} interface that by default
 * throws RuntimeException if a message is received that cannot be targeted to a gsmSCF instance.
 * Implementors need only implement the methods for messages that can actually be received.
 */
// note: this could be an abstract base class, but being an interface provides more flexibility
// for the implementing module
public interface GsmScfSmsListener extends CAPServiceSmsListener {

    @Override
    default void onConnectSMSRequest(ConnectSMSRequest arg0) {
        throw new RuntimeException("Unexpected ConnectSMS message received in gsmSCF!");
    }

    @Override
    default void onContinueSMSRequest(ContinueSMSRequest arg0) {
        throw new RuntimeException("Unexpected ContinueSMS message received in gsmSCF!");
    }

    @Override
    default void onFurnishChargingInformationSMSRequest(FurnishChargingInformationSMSRequest arg0) {
        throw new RuntimeException("Unexpected FurnishChargingInformationSMS message received in gsmSCF!");
    }

    @Override
    default void onReleaseSMSRequest(ReleaseSMSRequest arg0) {
        throw new RuntimeException("Unexpected ReleaseSMS message received in gsmSCF!");
    }

    @Override
    default void onRequestReportSMSEventRequest(RequestReportSMSEventRequest arg0) {
        throw new RuntimeException("Unexpected RequestReportSMSEvent message received in gsmSCF!");
    }

    @Override
    default void onResetTimerSMSRequest(ResetTimerSMSRequest arg0) {
        throw new RuntimeException("Unexpected ResetTimerSMS message received in gsmSCF!");
    }

}
