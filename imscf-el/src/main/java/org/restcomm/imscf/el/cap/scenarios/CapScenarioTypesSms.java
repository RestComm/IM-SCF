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
package org.restcomm.imscf.el.cap.scenarios;

import org.mobicents.protocols.ss7.cap.api.service.sms.EventReportSMSRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.InitialDPSMSRequest;

/** Collection of scenario interfaces for SMS call operations. */
// this class is not intended to be used in itself, only as a collection of interfaces to avoid having many
// separate 2 line files...
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class CapScenarioTypesSms {
    private CapScenarioTypesSms() {
        // no instances
    }

    // outgoing operation scenarios

    /***/
    public interface CapScenarioConnectSMS extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioContinueSMS extends CapRequestScenarioClass4 {

    }

    /***/
    public interface CapScenarioFurnishChargingInformationSMS extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioReleaseSMS extends CapRequestScenarioClass4 {

    }

    /***/
    public interface CapScenarioRequestReportSMSEvent extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioResetTimerSMS extends CapRequestScenarioClass2 {

    }

    // incoming operation scenarios

    /***/
    public interface CapScenarioInitialDPSMS extends CapIncomingRequestScenario<InitialDPSMSRequest> {
        @Override
        default Class<InitialDPSMSRequest> getRequestClass() {
            return InitialDPSMSRequest.class;
        }
    }

    /***/
    public interface CapScenarioEventReportSMS extends CapIncomingRequestScenario<EventReportSMSRequest> {
        @Override
        default Class<EventReportSMSRequest> getRequestClass() {
            return EventReportSMSRequest.class;
        }
    }
}
