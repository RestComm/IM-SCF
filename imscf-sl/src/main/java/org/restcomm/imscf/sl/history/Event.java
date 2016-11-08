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
package org.restcomm.imscf.sl.history;

import org.restcomm.imscf.common.diameter.creditcontrol.CCAResultCode;
import org.restcomm.imscf.common.diameter.creditcontrol.CCRequestType;
import org.restcomm.imscf.common.util.TCAPMessageInfo;

import java.util.Arrays;

/**
 * Lists possible events for an IMSCF call in view of the Signaling Layer.
 * <p>The event string is the following format:<br/>
 * <pre>PREFIX[(optional parameters...)]POSTFIX</pre>
 * PREFIX and POSTFIX both may be empty, they are
 * @author Miklos Pocsaji
 *
 */
public enum Event {

    TC_BEGIN_IN("->TC_BEGIN", ""), TC_CONT_IN("->TC_CONT", ""), TC_END_IN("->TC_END", ""), TC_ABRT_IN("->TC_ABORT", ""), TC_BEGIN_OUT(
            "<-TC_BEGIN", ""), TC_CONT_OUT("<-TC_CONT", ""), TC_END_OUT("<-TC_END", ""), TC_ABRT_OUT("<-TC_ABRT", ""), LWC_OUT_OK(
            "LWC_OK", "->"), LWC_OUT_ERR("LWC_ERR", "->"), LWC_IN("LWC", "<-"),

    EL_ROUTER_QUERY_OUT("ROUTE_QUERY", "××>"), EL_ROUTER_QUERY_IN("××>ROUTE_QUERY", ""), EL_ROUTER_QUERY_ANSWER_IN(
            "ROUTE_ANSWER", "<××"), EL_ROUTER_QUERY_ANSWER_OUT("<××ROUTE_ANSWER", ""), EL_ROUTER_QUERY_NOTFOUND_IN(
            "ROUTE_ERROR", "<××"), EL_ROUTER_QUERY_NOTFOUND_OUT("<××ROUTE_ERROR", ""), EL_ROUTER_QUERY_TIMEOUT(
            "ROUTE_TIMEOUT", "<××"),

    D_BALANCE_IN("->D_BALANCE", ""), D_BALANCE_SUCCES_IN("D_BALANCE_SUCCES<-", ""), D_BALANCE_SERVICE_DENIED_IN(
            "D_BALANCE_SERVICE_DENIED<-", ""), D_BALANCE_TECHNICAL_ERROR_IN("D_BALANCE_TECHNICAL_ERROR<-", ""), D_BALANCE_OUT(
            "D_BALANCE->", ""), D_BALANCE_SUCCES_OUT("<-D_BALANCE_SUCCES", ""), D_BALANCE_SERVICE_DENIED_OUT(
            "<-D_BALANCE_SERVICE_DENIED", ""), D_BALANCE_TECHNICAL_ERROR_OUT("<-D_BALANCE_TECHNICAL_ERROR", ""),

    D_DEBIT_IN("->D_DEBIT", ""), D_DEBIT_SUCCES_IN("D_DEBIT_SUCCES<-", ""), D_DEBIT_SERVICE_DENIED_IN(
            "D_DEBIT_SERVICE_DENIED<-", ""), D_DEBIT_TECHNICAL_ERROR_IN("D_DEBIT_TECHNICAL_ERROR<-", ""), D_DEBIT_OUT(
            "D_DEBIT->", ""), D_DEBIT_SUCCES_OUT("<-D_DEBIT_SUCCES", ""), D_DEBIT_SERVICE_DENIED_OUT(
            "<-D_DEBIT_SERVICE_DENIED", ""), D_DEBIT_TECHNICAL_ERROR_OUT("<-D_DEBIT_TECHNICAL_ERROR", "");

    private final String prefix;
    private final String postfix;

    private Event(String prefix, String postfix) {
        this.prefix = prefix;
        this.postfix = postfix;
    }

    @Override
    public String toString() {
        return toEventString();
    }

    public String toEventString(String... parameters) {
        if (parameters == null || parameters.length == 0) {
            return prefix + postfix;
        } else if (parameters.length == 1) {
            return prefix + "(" + parameters[0] + ")" + postfix;
        }
        StringBuilder sb = new StringBuilder(prefix).append("(");
        sb.append(parameters[0]);
        Arrays.stream(parameters, 1, parameters.length).forEachOrdered(p -> sb.append(", ").append(p));
        sb.append(")").append(postfix);
        return sb.toString();
    }

    public static Event fromTcap(TCAPMessageInfo info, boolean fromMobileNetwork) {
        switch (info.getMessageType()) {
        case TC_ABORT:
            return fromMobileNetwork ? TC_ABRT_IN : TC_ABRT_OUT;
        case TC_BEGIN:
            return fromMobileNetwork ? TC_BEGIN_IN : TC_BEGIN_OUT;
        case TC_CONTINUE:
            return fromMobileNetwork ? TC_CONT_IN : TC_CONT_OUT;
        case TC_END:
            return fromMobileNetwork ? TC_END_IN : TC_END_OUT;
        default:
            throw new IllegalArgumentException("Unexpected TCAPMessageInfo type: " + info.getMessageType());
        }
    }

    public static Event fromDiameter(CCRequestType reqType, CCAResultCode resType, boolean fromMobileNetwork) {
        switch (reqType) {

        case BALANCE:
            if (resType != null) {
                switch (resType) {
                case SUCCESS:
                    return fromMobileNetwork ? D_BALANCE_SUCCES_IN : D_BALANCE_SUCCES_OUT;
                case END_USER_SERVICE_DENIED:
                    return fromMobileNetwork ? D_BALANCE_SERVICE_DENIED_IN : D_BALANCE_SERVICE_DENIED_OUT;
                case TECHNICAL_ERROR:
                    return fromMobileNetwork ? D_BALANCE_TECHNICAL_ERROR_IN : D_BALANCE_TECHNICAL_ERROR_OUT;
                default:
                    throw new IllegalArgumentException("Unexpected Diameter Response type!");
                }
            } else {
                return fromMobileNetwork ? D_BALANCE_IN : D_BALANCE_OUT;
            }
        case DEBIT:
            if (resType != null) {
                switch (resType) {
                case SUCCESS:
                    return fromMobileNetwork ? D_DEBIT_SUCCES_IN : D_DEBIT_SUCCES_OUT;
                case END_USER_SERVICE_DENIED:
                    return fromMobileNetwork ? D_DEBIT_SERVICE_DENIED_IN : D_DEBIT_SERVICE_DENIED_OUT;
                case TECHNICAL_ERROR:
                    return fromMobileNetwork ? D_DEBIT_TECHNICAL_ERROR_IN : D_DEBIT_TECHNICAL_ERROR_OUT;
                default:
                    throw new IllegalArgumentException("Unexpected Diameter Response type!");
                }
            } else {
                return fromMobileNetwork ? D_DEBIT_IN : D_DEBIT_OUT;
            }
        default:
            throw new IllegalArgumentException("Unexpected Diameter Request type!");
        }
    }
}
