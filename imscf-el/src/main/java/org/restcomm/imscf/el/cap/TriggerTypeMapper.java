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

import java.util.Optional;

import org.restcomm.imscf.common.config.EventType;
import org.restcomm.imscf.common.config.NotificationType;
import org.restcomm.imscf.common.config.TriggerType;

import org.mobicents.protocols.ss7.cap.api.CAPParameterFactory;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.cap.api.primitives.EventTypeBCSM;
import org.mobicents.protocols.ss7.cap.api.primitives.MonitorMode;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.DpSpecificCriteria;
import org.mobicents.protocols.ss7.cap.api.service.sms.primitive.EventTypeSMS;
import org.mobicents.protocols.ss7.cap.api.service.sms.primitive.SMSEvent;
import org.mobicents.protocols.ss7.inap.api.INAPParameterFactory;
import org.mobicents.protocols.ss7.inap.api.primitives.LegID;
import org.mobicents.protocols.ss7.inap.api.primitives.LegType;

/** Utility class for mapping IMSCF config types to jss7 EventType. */
public final class TriggerTypeMapper {

    private TriggerTypeMapper() {
        // no instances
    }

    public static EventTypeBCSM getAsCapEventTypeBCSM(EventType event) {
        switch (event) {
        case O_ABANDON:
            return EventTypeBCSM.oAbandon;
        case O_ANSWER:
            return EventTypeBCSM.oAnswer;
        case O_NO_ANSWER:
            return EventTypeBCSM.oNoAnswer;
        case O_CALLED_PARTY_BUSY:
            return EventTypeBCSM.oCalledPartyBusy;
        case O_DISCONNECT:
        case O_DISCONNECT_LEG_1:
        case O_DISCONNECT_LEG_2:
            return EventTypeBCSM.oDisconnect;
        case O_MID_CALL:
            return EventTypeBCSM.oMidCall;
        case O_TERM_SEIZED:
            return EventTypeBCSM.oTermSeized;
        case ROUTE_SELECT_FAILURE:
            return EventTypeBCSM.routeSelectFailure;
        case CALL_ACCEPTED:
            return EventTypeBCSM.callAccepted;
        case T_ABANDON:
            return EventTypeBCSM.tAbandon;
        case T_ANSWER:
            return EventTypeBCSM.tAnswer;
        case T_BUSY:
            return EventTypeBCSM.tBusy;
        case T_DISCONNECT_LEG_1:
        case T_DISCONNECT_LEG_2:
            return EventTypeBCSM.tDisconnect;
        case T_MID_CALL:
            return EventTypeBCSM.tMidCall;
        case T_NO_ANSWER:
            return EventTypeBCSM.tNoAnswer;

        default:
            throw new IllegalArgumentException("SMS event " + event + " cannot be converted to BCSM event");
        }
    }

    public static EventTypeSMS getAsCapEventTypeSMS(EventType event) {
        switch (event) {
        case O_SMS_FAILURE:
            return EventTypeSMS.oSmsFailure;
        case O_SMS_SUBMISSION:
            return EventTypeSMS.oSmsSubmission;
        case T_SMS_FAILURE:
            return EventTypeSMS.tSmsFailure;
        case T_SMS_SUBMISSION:
            return EventTypeSMS.tSmsDelivery;
        default:
            throw new IllegalArgumentException("BCSM event " + event + " cannot be converted to SMS event");
        }
    }

    public static MonitorMode getAsCapMonitorMode(NotificationType notification) {
        switch (notification) {
        case INTERRUPTED:
            return MonitorMode.interrupted;
        case NOTIFY_AND_CONTINUE:
            return MonitorMode.notifyAndContinue;
        default:
            return MonitorMode.transparent;
        }
    }

    public static BCSMEvent getAsCapBCSMEvent(TriggerType edp, CAPParameterFactory cpf, INAPParameterFactory ipf) {
        LegID legId = null;
        DpSpecificCriteria crit = null;

        switch (edp.getEvent()) {
        case O_DISCONNECT_LEG_1:
        case T_DISCONNECT_LEG_1:
            legId = ipf.createLegID(true, LegType.leg1);
            break;
        case O_DISCONNECT_LEG_2:
        case T_DISCONNECT_LEG_2:
            legId = ipf.createLegID(true, LegType.leg2);
            break;
        case O_NO_ANSWER:
        case T_NO_ANSWER:
            crit = Optional.<Integer> ofNullable(edp.getNoAnswerTimeoutSec()).map(i -> cpf.createDpSpecificCriteria(i))
                    .orElse(null);
            break;
        default:
            break;
        }

        return cpf.createBCSMEvent(getAsCapEventTypeBCSM(edp.getEvent()), getAsCapMonitorMode(edp.getTriggerType()),
                legId, crit, false);
    }

    public static SMSEvent getAsCapSMSEvent(TriggerType edp, CAPParameterFactory cpf) {
        return cpf.createSMSEvent(getAsCapEventTypeSMS(edp.getEvent()), getAsCapMonitorMode(edp.getTriggerType()));
    }
}
