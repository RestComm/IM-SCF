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

import org.restcomm.imscf.common.config.ReleaseCauseType;

import java.util.EnumMap;
import java.util.Map;

import org.mobicents.protocols.ss7.isup.message.parameter.CauseIndicators;

/** Utility class for mapping release cause values between IMSCF config type and mobicents type. */
public final class ReleaseCauseMapper {

    // value used in: CauseIndicators.setCauseValue(int)
    private static final Map<ReleaseCauseType, Integer> CAP_CAUSES = new EnumMap<>(ReleaseCauseType.class);
    static {
        // FIXME: no IMSCF value to be mapped to these jss7 constants:
        // CauseIndicators._CV_REJECTED_DUE_TO_ACR_SUPP_SERVICES;
        // CauseIndicators._CV_USER_INFORMATION_DISCARDED;

        CAP_CAUSES.put(ReleaseCauseType.A_SUSPENDED_CALL_EXISTS_BUT_THIS_CALL_IDENTITY_DOES_NOT, null);
        CAP_CAUSES.put(ReleaseCauseType.ACCESS_INFORMATION_DISCARDED, null);
        CAP_CAUSES.put(ReleaseCauseType.ANSI_CALL_BLOCKED_DUE_TO_GROUP_RESTRICTIONS, null);
        CAP_CAUSES.put(ReleaseCauseType.BEARER_CAPABILITY_NOT_AUTHORIZED,
                CauseIndicators._CV_BEARER_CAPABILITY_NOT_AUTHORIZED);
        CAP_CAUSES.put(ReleaseCauseType.BEARER_CAPABILITY_NOT_IMPLEMENTED,
                CauseIndicators._CV_BEARER_CAPABILITY_NOT_IMPLEMENTED);
        CAP_CAUSES.put(ReleaseCauseType.BEARER_CAPABILITY_NOT_PRESENTLY_AVAILABLE,
                CauseIndicators._CV_BEARER_CAPABILITY_NOT_AVAILABLE);
        CAP_CAUSES.put(ReleaseCauseType.CALL_AWARDED_AND_BEING_DELIVERED_IN_AN_ESTABLISHED_CHANNEL, null);
        CAP_CAUSES.put(ReleaseCauseType.CALL_HAVING_REQUESTED_CALL_IDENTITY_HAS_BEEN_CLEARED, null);
        CAP_CAUSES.put(ReleaseCauseType.CALL_IDENTITY_IN_USE, null);
        CAP_CAUSES.put(ReleaseCauseType.CALL_TYPE_INCOMPATIBLE_WITH_SERVICE_REQUEST, null);
        CAP_CAUSES.put(ReleaseCauseType.CALLED_REJECTED, CauseIndicators._CV_CALL_REJECTED);
        CAP_CAUSES.put(ReleaseCauseType.CHANNEL_TYPE_NOT_IMPLEMENTED, null);
        CAP_CAUSES.put(ReleaseCauseType.CHANNEL_UNACCEPTABLE, null);
        CAP_CAUSES.put(ReleaseCauseType.DESTINATION_OUT_OF_ORDER, CauseIndicators._CV_DESTINATION_OUT_OF_ORDER);
        CAP_CAUSES.put(ReleaseCauseType.EXCHANGE_ROUTING_ERROR, CauseIndicators._CV_EXCHANGE_ROUTING_ERROR);
        CAP_CAUSES.put(ReleaseCauseType.FACILITY_REJECTED, CauseIndicators._CV_FACILITY_REJECTED);
        CAP_CAUSES.put(ReleaseCauseType.IDENTIFIED_CHANNEL_DOES_NOT_EXIST, null);
        CAP_CAUSES.put(ReleaseCauseType.INCOMING_CALLS_BARRED_WITH_CUG,
                CauseIndicators._CV_INCOMING_CALL_BARRED_WITHIN_CUG);
        CAP_CAUSES.put(ReleaseCauseType.INCOMPATIBLE_DESTINATION, CauseIndicators._CV_INCOMPATIBLE_DESTINATION);
        CAP_CAUSES.put(ReleaseCauseType.INCONSISTENCY_IN_DESIGNATED_OUT_GOING, null);
        CAP_CAUSES.put(ReleaseCauseType.INFORMATION_ELEMENT_PARAMETER_NON_EXISTENT_OR_NOT_IMPLEMENTED,
                CauseIndicators._CV_PARAMETER_NON_EXISTENT_DISCARD);
        CAP_CAUSES.put(ReleaseCauseType.INTERWORKING, CauseIndicators._CV_INTERNETWORKING_UNSPECIFIED);
        CAP_CAUSES.put(ReleaseCauseType.INVALID_CALL_REFERENCE_VALUE, CauseIndicators._CV_INVALID_CALL_REFERENCE);
        CAP_CAUSES.put(ReleaseCauseType.INVALID_INFORMATION_ELEMENT_CONTENTS,
                CauseIndicators._CV_INVALID_PARAMETER_CONTENT);
        CAP_CAUSES.put(ReleaseCauseType.INVALID_MESSAGE, CauseIndicators._CV_INVALID_MESSAGE_UNSPECIFIED);
        CAP_CAUSES.put(ReleaseCauseType.INVALID_NUMBER_FORMAT_ADDRESS_INCOMPLETE,
                CauseIndicators._CV_ADDRESS_INCOMPLETE);
        CAP_CAUSES.put(ReleaseCauseType.INVALID_TRANSIT_NETWORK_SELECTION,
                CauseIndicators._CV_INVALID_TRANSIT_NETWORK_SELECTION);
        CAP_CAUSES.put(ReleaseCauseType.MANDATORY_INFORMATION_ELEMENT_IS_MISSING,
                CauseIndicators._CV_MANDATORY_ELEMENT_MISSING);
        CAP_CAUSES.put(ReleaseCauseType.MESSAGE_NOT_COMPATIBLE_WITH_CALL_STATE, null);
        CAP_CAUSES.put(ReleaseCauseType.MESSAGE_NOT_COMPATIBLE_WITH_CALL_STATE_OR, null);
        CAP_CAUSES.put(ReleaseCauseType.MESSAGE_TYPE_NON_EXISTENT_OR_NOT_IMPLEMENTED,
                CauseIndicators._CV_MESSAGE_TYPE_NON_EXISTENT);
        CAP_CAUSES.put(ReleaseCauseType.MESSAGE_WITH_UNRECOGNIZED_PARAMETER_DISCARDED,
                CauseIndicators._CV_MESSAGE_WITH_UNRECOGNIZED_PARAMETER_DISCARDED);
        CAP_CAUSES.put(ReleaseCauseType.MISDIALLED_TRUNK_PREFIX, CauseIndicators._CV_MISDIALED_TRUNK_PREFIX);
        CAP_CAUSES.put(ReleaseCauseType.NETWORK_OUT_OF_ORDER, CauseIndicators._CV_NETWORK_OUT_OF_ORDER);
        CAP_CAUSES.put(ReleaseCauseType.NO_ANSWER_FROM_USER, CauseIndicators._CV_NO_ANSWER);
        CAP_CAUSES.put(ReleaseCauseType.NO_CALL_SUSPENDED, null);
        CAP_CAUSES.put(ReleaseCauseType.NO_CIRCUIT_CHANNEL_AVAILABLE, CauseIndicators._CV_NO_CIRCUIT_AVAILABLE);
        CAP_CAUSES.put(ReleaseCauseType.NO_ROUTE_TO_DESTINATION, CauseIndicators._CV_NO_ROUTE_TO_DEST);
        CAP_CAUSES.put(ReleaseCauseType.NO_ROUTE_TO_SPECIFIED_TRANSIT_NETWORK,
                CauseIndicators._CV_NO_ROUTE_TO_TRANSIT_NET);
        CAP_CAUSES.put(ReleaseCauseType.NO_USER_RESPONDING, CauseIndicators._CV_NO_USER_RESPONSE);
        CAP_CAUSES.put(ReleaseCauseType.NON_EXISTENT_CUG, null);
        CAP_CAUSES.put(ReleaseCauseType.NON_SELECTED_USER_CLEARING, null);
        CAP_CAUSES.put(ReleaseCauseType.NORMAL_CALL_CLEARING, CauseIndicators._CV_ALL_CLEAR);
        CAP_CAUSES.put(ReleaseCauseType.NORMAL_OR_UNSPECIFIED, CauseIndicators._CV_NORMAL_UNSPECIFIED);
        CAP_CAUSES.put(ReleaseCauseType.NUMBER_CHANGED, CauseIndicators._CV_NUMBER_CHANGED);
        CAP_CAUSES.put(ReleaseCauseType.ONLY_RESTRICTED_DIGITAL_INFORMATION_BEARER_CAPABILITY_IS_AVAILABLE,
                CauseIndicators._CV_RESTRICTED_DIGITAL_BEARED_AVAILABLE);
        CAP_CAUSES.put(ReleaseCauseType.OUT_GOING_CALLS_BARRED_WITH_CUG, null);
        CAP_CAUSES.put(ReleaseCauseType.PARAMETER_NON_EXISTENT_OR_NOT_IMPLEMENTED_PASSED_ON,
                CauseIndicators._CV_PARAMETER_NON_EXISTENT_PASS_ALONG);
        CAP_CAUSES.put(ReleaseCauseType.PERMANENT_FRAME_MODE_CONNECTION_OPERATIONAL, null);
        CAP_CAUSES.put(ReleaseCauseType.PERMANENT_FRAME_MODE_CONNECTION_OUT_OF_SERVICE, null);
        CAP_CAUSES.put(ReleaseCauseType.PRECEDENCE_CALL_BLOCKED, null);
        CAP_CAUSES.put(ReleaseCauseType.PREEMPTION, CauseIndicators._CV_PREEMPTION);
        CAP_CAUSES.put(ReleaseCauseType.PREEMPTION_CIRCUIT_RESERVED_FOR_REUSE, null);
        CAP_CAUSES.put(ReleaseCauseType.PROTOCOL_ERROR, CauseIndicators._CV_PROTOCOL_ERROR);
        CAP_CAUSES.put(ReleaseCauseType.QUAILITY_OF_SERVICE_NOT_AVAILABLE, null);
        CAP_CAUSES.put(ReleaseCauseType.RECOVERY_ON_TIMER_EXPIRY, CauseIndicators._CV_TIMEOUT_RECOVERY);
        CAP_CAUSES.put(ReleaseCauseType.REDIRECTION_TO_NEW_DESTINATION, null);
        CAP_CAUSES.put(ReleaseCauseType.REQUESTED_CIRCUIT_CHANNEL_NOT_AVAILABLE,
                CauseIndicators._CV_REQUESTED_CIRCUIT_UNAVAILABLE);
        CAP_CAUSES.put(ReleaseCauseType.REQUESTED_FACILITY_NOT_IMPLEMENTED,
                CauseIndicators._CV_FACILITY_NOT_IMPLEMENTED);
        CAP_CAUSES.put(ReleaseCauseType.REQUESTED_FACILITY_NOT_SUBSCRIBED, CauseIndicators._CV_FACILITY_NOT_SUBSCRIBED);
        CAP_CAUSES.put(ReleaseCauseType.RESOURCE_UN_AVAILABLE, CauseIndicators._CV_RESOURCE_UNAVAILABLE);
        CAP_CAUSES.put(ReleaseCauseType.RESPONSE_TO_STATUS_ENQUIRY, null);
        CAP_CAUSES.put(ReleaseCauseType.SEND_SPECIAL_INFORMATION_TONE, CauseIndicators._CV_SEND_SPECIAL_TONE);
        CAP_CAUSES.put(ReleaseCauseType.SERVICE_OR_OPTION_NOT_AVAILABLE,
                CauseIndicators._CV_SERVICE_OR_OPTION_NOT_AVAILABLE);
        CAP_CAUSES.put(ReleaseCauseType.SERVICE_OR_OPTION_NOT_IMPLEMENTED,
                CauseIndicators._CV_SERVICE_OR_OPTION_NOT_IMPLEMENTED);
        CAP_CAUSES.put(ReleaseCauseType.SUBSCRIBER_ABSENT, CauseIndicators._CV_SUBSCRIBER_ABSENT);
        CAP_CAUSES
                .put(ReleaseCauseType.SWITCHING_EQUIPMENT_CONGESTION, CauseIndicators._CV_SWITCH_EQUIPMENT_CONGESTION);
        CAP_CAUSES.put(ReleaseCauseType.TEMPORARY_FAILURE, CauseIndicators._CV_TEMPORARY_FAILURE);
        CAP_CAUSES.put(ReleaseCauseType.UN_ALLOCATED_NUMBER, CauseIndicators._CV_UNALLOCATED);
        CAP_CAUSES.put(ReleaseCauseType.USER_BUSY, CauseIndicators._CV_USER_BUSY);
        CAP_CAUSES.put(ReleaseCauseType.USER_NOT_MEMBER_OF_CUG, CauseIndicators._CV_CALLED_USER_NOT_MEMBER_OF_CUG);
    }

    private ReleaseCauseMapper() {
        // no instance
    }

    public static Integer releaseCauseToCauseValue(ReleaseCauseType rc) {
        return CAP_CAUSES.get(rc);
    }
}
