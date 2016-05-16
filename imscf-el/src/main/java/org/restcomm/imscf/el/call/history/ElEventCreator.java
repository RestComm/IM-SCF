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
package org.restcomm.imscf.el.call.history;

import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.diameter.call.DiameterHttpCall;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterSLELCreditControlRequest;

import java.net.HttpURLConnection;
import java.util.Objects;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

/**
 * Helper class for auditlog event strings.
 * @author Miklos Pocsaji
 *
 */
public final class ElEventCreator {

    private ElEventCreator() {
        // This class cannot be instantiated.
    }

    public static String createOutgoingSipEvent(SipServletMessage msg) {
        if (msg instanceof SipServletRequest) {
            return createOutgoingSipEvent((SipServletRequest) msg);
        } else if (msg instanceof SipServletResponse) {
            return createOutgoingSipEvent((SipServletResponse) msg);
        } else {
            return "[null sip msg]->";
        }
    }

    public static String createOutgoingSipEvent(SipServletRequest req) {
        return req.getMethod() + "->";
    }

    public static String createOutgoingSipEvent(SipServletResponse resp) {
        return String.valueOf(resp.getStatus()) + "(" + resp.getMethod() + ")" + "->";
    }

    public static String createIncomingSipEvent(SipServletMessage msg) {
        if (msg instanceof SipServletRequest) {
            return createIncomingSipEvent((SipServletRequest) msg);
        } else if (msg instanceof SipServletResponse) {
            return createIncomingSipEvent((SipServletResponse) msg);
        } else {
            return "[null sip msg]<-";
        }
    }

    public static String createIncomingSipEvent(SipServletRequest req) {
        return req.getMethod() + "<-";
    }

    public static String createIncomingSipEvent(SipServletResponse resp) {
        return String.valueOf(resp.getStatus()) + "(" + resp.getMethod() + ")<-";
    }

    public static String createIncomingHttpEvent(DiameterSLELCreditControlRequest request, int responseCode) {
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return "HTTP" + responseCode + " (" + request.getRequestTypeObject() + ")<-";
        } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return "HTTP" + responseCode + " (" + request.getRequestTypeObject() + ")<-";
        } else {
            return "HTTP" + responseCode + " (" + request.getRequestTypeObject() + ")<-";
        }
    }

    public static void addIncomingSipEvent(String appSessionId, SipServletMessage msg) {
        String event = createIncomingSipEvent(msg);
        addEventByAppSessionId(appSessionId, event);
    }

    public static void addOutgoingSipEvent(String appSessionId, SipServletMessage msg) {
        String event = createOutgoingSipEvent(msg);
        addEventByAppSessionId(appSessionId, event);
    }

    public static void addEventByAppSessionId(String appSessionId, String event) {
        CallStore cs = Objects.requireNonNull((CallStore) CallContext.get(CallContext.CALLSTORE),
                "CallStore from context is null");
        try (SIPCall call = cs.getCallByAppSessionId(appSessionId)) {
            if (call != null) {
                call.getCallHistory().addEvent(event);
            }
        }
    }

    public static void addIncomingHttpEvent(DiameterSLELCreditControlRequest request, int responseCode) {
        String event = createIncomingHttpEvent(request, responseCode);
        addEventByDiameterSessionId(request.getSessionId(), event);
    }

    public static void addIncomingHttpTErrorEvent(DiameterSLELCreditControlRequest request) {
        String event = "HTTP Technical Error (" + request.getRequestTypeObject() + ")<-";
        addEventByDiameterSessionId(request.getSessionId(), event);
    }

    public static void addEventByDiameterSessionId(String diameterSessionId, String event) {
        CallStore cs = Objects.requireNonNull((CallStore) CallContext.get(CallContext.CALLSTORE),
                "CallStore from context is null");
        try (DiameterHttpCall call = cs.getHttpCallByDiameterSessionId(diameterSessionId)) {
            if (call != null) {
                call.getCallHistory().addEvent(event);
            }
        }
    }

    public static void addEventByImscfCallId(String imscfCallId, String event) {
        CallStore cs = Objects.requireNonNull((CallStore) CallContext.get(CallContext.CALLSTORE),
                "CallStore from context is null");
        try (IMSCFCall call = cs.getCallByImscfCallId(imscfCallId)) {
            if (call != null) {
                call.getCallHistory().addEvent(event);
            }
        }
    }

}
