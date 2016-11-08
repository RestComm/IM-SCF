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

import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ActivityTestRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.AssistRequestInstructionsRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPServiceCircuitSwitchedCallListener;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CallInformationRequestRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CancelRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ConnectRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ConnectToResourceRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ContinueRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ContinueWithArgumentRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectForwardConnectionRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectForwardConnectionWithArgumentRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectLegRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EstablishTemporaryConnectionRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.FurnishChargingInformationRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitiateCallAttemptRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.MoveLegRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PlayAnnouncementRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ReleaseCallRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.RequestReportBCSMEventRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ResetTimerRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SendChargingInformationRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SplitLegRequest;

/**
 * Interface for CAP gsmSCF modules (phase 2-4) listening for CS call events.
 * This is a restriction of the {@link CAPServiceCircuitSwitchedCallListener} interface that by default
 * throws RuntimeException if a message is received that cannot be targeted to a gsmSCF instance.
 * Implementors need only implement the methods for messages that can actually be received.
 */
// note: this could be an abstract base class, but being an interface provides more flexibility
// for the implementing module
public interface GsmScfCsCallListener extends CAPServiceCircuitSwitchedCallListener {

    @Override
    default void onActivityTestRequest(ActivityTestRequest arg0) {
        throw new RuntimeException("Unexpected onActivityTestRequest message received in gsmSCF!");
    }

    @Override
    default void onApplyChargingRequest(ApplyChargingRequest arg0) {
        throw new RuntimeException("Unexpected ApplyChargingRequest message received in gsmSCF!");
    }

    @Override
    default void onAssistRequestInstructionsRequest(AssistRequestInstructionsRequest arg0) {
        throw new RuntimeException("Unexpected AssistRequestInstructions message received in gsmSCF!");
    }

    @Override
    default void onCallInformationRequestRequest(CallInformationRequestRequest arg0) {
        throw new RuntimeException("Unexpected CallInformationRequest message received in gsmSCF!");
    }

    @Override
    default void onCancelRequest(CancelRequest arg0) {
        throw new RuntimeException("Unexpected Cancel message received in gsmSCF!");
    }

    @Override
    default void onConnectRequest(ConnectRequest arg0) {
        throw new RuntimeException("Unexpected Connect message received in gsmSCF!");
    }

    @Override
    default void onConnectToResourceRequest(ConnectToResourceRequest arg0) {
        throw new RuntimeException("Unexpected ConnectToResource message received in gsmSCF!");
    }

    @Override
    default void onContinueRequest(ContinueRequest arg0) {
        throw new RuntimeException("Unexpected Continue message received in gsmSCF!");
    }

    @Override
    default void onContinueWithArgumentRequest(ContinueWithArgumentRequest arg0) {
        throw new RuntimeException("Unexpected ContinueWithArgument message received in gsmSCF!");
    }

    @Override
    default void onDisconnectForwardConnectionRequest(DisconnectForwardConnectionRequest arg0) {
        throw new RuntimeException("Unexpected DisconnectForwardConnection message received in gsmSCF!");
    }

    @Override
    default void onDisconnectForwardConnectionWithArgumentRequest(DisconnectForwardConnectionWithArgumentRequest arg0) {
        throw new RuntimeException("Unexpected DisconnectForwardConnectionWithArgument message received in gsmSCF!");
    }

    @Override
    default void onDisconnectLegRequest(DisconnectLegRequest arg0) {
        throw new RuntimeException("Unexpected DisconnectLeg message received in gsmSCF!");
    }

    @Override
    default void onEstablishTemporaryConnectionRequest(EstablishTemporaryConnectionRequest arg0) {
        throw new RuntimeException("Unexpected EstablishTemporaryConnection message received in gsmSCF!");
    }

    @Override
    default void onFurnishChargingInformationRequest(FurnishChargingInformationRequest arg0) {
        throw new RuntimeException("Unexpected FurnishChargingInformation message received in gsmSCF!");
    }

    @Override
    default void onInitiateCallAttemptRequest(InitiateCallAttemptRequest arg0) {
        throw new RuntimeException("Unexpected InitiateCallAttempt message received in gsmSCF!");
    }

    @Override
    default void onMoveLegRequest(MoveLegRequest arg0) {
        throw new RuntimeException("Unexpected MoveLeg message received in gsmSCF!");
    }

    @Override
    default void onPlayAnnouncementRequest(PlayAnnouncementRequest arg0) {
        throw new RuntimeException("Unexpected PlayAnnouncement message received in gsmSCF!");
    }

    @Override
    default void onPromptAndCollectUserInformationRequest(PromptAndCollectUserInformationRequest arg0) {
        throw new RuntimeException("Unexpected PromptAndCollectUserInformation message received in gsmSCF!");
    }

    @Override
    default void onReleaseCallRequest(ReleaseCallRequest arg0) {
        throw new RuntimeException("Unexpected ReleaseCall message received in gsmSCF!");
    }

    @Override
    default void onRequestReportBCSMEventRequest(RequestReportBCSMEventRequest arg0) {
        throw new RuntimeException("Unexpected RequestReportBCSMEvent message received in gsmSCF!");
    }

    @Override
    default void onResetTimerRequest(ResetTimerRequest arg0) {
        throw new RuntimeException("Unexpected ResetTimer message received in gsmSCF!");
    }

    @Override
    default void onSendChargingInformationRequest(SendChargingInformationRequest arg0) {
        throw new RuntimeException("Unexpected SendChargingInformation message received in gsmSCF!");
    }

    @Override
    default void onSplitLegRequest(SplitLegRequest arg0) {
        throw new RuntimeException("Unexpected SplitLegRequest message received in gsmSCF!");
    }

}
