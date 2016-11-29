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

import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ActivityTestResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CallInformationReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectLegResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitiateCallAttemptResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.MoveLegResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SplitLegResponse;

/** Collection of scenario interfaces for CS call operations. */
// this class is not intended to be used in itself, only as a collection of interfaces to avoid having many
// separate 2 line files...
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class CapScenarioTypesCsCall {
    private CapScenarioTypesCsCall() {
        // no instances
    }

    // outgoing operation scenarios

    /***/
    public interface CapScenarioActivityTest extends CapRequestScenarioClass3<ActivityTestResponse> {

    }

    /***/
    public interface CapScenarioApplyCharging extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioCallInformationRequest extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioCancel extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioConnect extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioConnectToResource extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioContinue extends CapRequestScenarioClass4 {

    }

    /***/
    public interface CapScenarioContinueWithArgument extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioDisconnectForwardConnection extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioDisconnectForwardConnectionWithArgument extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioDisconnectLeg extends CapRequestScenarioClass1<DisconnectLegResponse> {

    }

    /***/
    public interface CapScenarioFurnishChargingInformation extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioInitiateCallAttempt extends CapRequestScenarioClass1<InitiateCallAttemptResponse> {

    }

    /***/
    public interface CapScenarioMoveLeg extends CapRequestScenarioClass1<MoveLegResponse> {

    }

    /***/
    public interface CapScenarioPlayTone extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioReleaseCall extends CapRequestScenarioClass4 {

    }

    /***/
    public interface CapScenarioRequestReportBCSMEvent extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioResetTimer extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioSendChargingInformation extends CapRequestScenarioClass2 {

    }

    /***/
    public interface CapScenarioSplitLeg extends CapRequestScenarioClass1<SplitLegResponse> {

    }

    // incoming operation scenarios

    /***/
    public interface CapScenarioApplyChargingReport extends CapIncomingRequestScenario<ApplyChargingReportRequest> {
        @Override
        default Class<ApplyChargingReportRequest> getRequestClass() {
            return ApplyChargingReportRequest.class;
        }
    }

    /***/
    public interface CapScenarioCallInformationReport extends CapIncomingRequestScenario<CallInformationReportRequest> {
        @Override
        default Class<CallInformationReportRequest> getRequestClass() {
            return CallInformationReportRequest.class;
        }
    }

    /***/
    public interface CapScenarioEventReportBCSM extends CapIncomingRequestScenario<EventReportBCSMRequest> {
        @Override
        default Class<EventReportBCSMRequest> getRequestClass() {
            return EventReportBCSMRequest.class;
        }
    }

    /***/
    public interface CapScenarioInitialDP extends CapIncomingRequestScenario<InitialDPRequest> {
        @Override
        default Class<InitialDPRequest> getRequestClass() {
            return InitialDPRequest.class;
        }
    }

}
