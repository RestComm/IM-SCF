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
package org.restcomm.imscf.el.map.scenarios;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberState;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberStateChoice;

import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.map.call.AtiRequest;
import org.restcomm.imscf.el.map.call.MAPSIPCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageDetector;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.util.Jss7ToXml;

/**
 * Scenario which handles outgoing NOTIFY requests.
 * @author Miklos Pocsaji
 *
 */
public final class SipMapNotifyScenario extends Scenario {
    private static final String SUBSCRIPTION_STATE_REASON_OK = "timeout";

    private static final NotifyResponseDetector NOTIFY_RESPONSE_DETECTOR = new NotifyResponseDetector();

    public static SipMapNotifyScenario startAtiSuccess(AtiRequest atiRequest, MAPSIPCall call,
            AnyTimeInterrogationResponse atiResp) throws MessagingException, ServletParseException,
            UnsupportedEncodingException, IOException {

        String atiResponse = Jss7ToXml.encode(atiResp, "anyTimeInterrogationResult");

        byte[] atiResponseBytes = atiResponse.getBytes(StandardCharsets.UTF_8);
        Object content = atiResponseBytes;
        String contentType = SipConstants.CONTENTTYPE_MAP_XML_STRING;

        if (atiResp.getSubscriberInfo() != null && atiResp.getSubscriberInfo().getSubscriberState() != null) {
            InternetHeaders ih;
            MimeMultipart mm = new MimeMultipart("mixed");

            ih = new InternetHeaders();
            ih.addHeaderLine("Content-Type: " + SipConstants.CONTENTTYPE_MAP_XML_STRING);
            MimeBodyPart mapXml = new MimeBodyPart(ih, atiResponseBytes);
            mm.addBodyPart(mapXml);

            ih = new InternetHeaders();
            ih.addHeaderLine("Content-Type: " + SipConstants.CONTENTTYPE_PIDF_XML_STRING);
            String pidf = subscriberInfoToPidfXml(atiRequest, atiResp.getSubscriberInfo().getSubscriberState());
            mm.addBodyPart(new MimeBodyPart(ih, pidf.getBytes(StandardCharsets.UTF_8)));

            content = mm;
            contentType = mm.getContentType();
        }

        SipSession atiSession = call.getAppSession().getSipSession(atiRequest.getSipSessionId());
        SipServletRequest notify = atiSession.createRequest("NOTIFY");

        notify.setHeader("Subscription-State", "terminated;reason=" + SUBSCRIPTION_STATE_REASON_OK);
        notify.setHeader("Event", "presence");

        notify.setContent(content, contentType);
        notify.send();
        return new SipMapNotifyScenario();
    }

    public static SipMapNotifyScenario startAtiError(AtiRequest atiRequest, MAPSIPCall call, String reason)
            throws IOException {
        SipApplicationSession appSession = call.getAppSession();
        SipSession atiSession = appSession.getSipSession(atiRequest.getSipSessionId());
        SipServletRequest notify = atiSession.createRequest("NOTIFY");
        notify.setHeader("Subscription-State", "terminated;reason=" + reason);
        notify.send();
        return new SipMapNotifyScenario();
    }

    private SipMapNotifyScenario() {
        super("Detects end of MAP query", NOTIFY_RESPONSE_DETECTOR, (scenario, msg) -> {
            SipServletResponse resp = (SipServletResponse) msg;
            if (resp.getStatus() >= 200) {
                try (IMSCFCall call = CallContext.getCallStore().getCallByAppSessionId(
                        resp.getApplicationSession().getId())) {
                    CallContext.getCallFactory().deleteCall(call);
                }
                scenario.setFinished();
            }
        });
    }

    private static String subscriberInfoToPidfXml(AtiRequest atiRequest, SubscriberState subscriberState) {
        StringBuilder ret = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        ret.append("<presence xmlns=\"urn:ietf:params:xml:ns:pidf\" entity=\"pres:")
                .append(atiRequest.getTargetNumber()).append("@imscf.restcomm.org\">\n");
        ret.append("  <tuple id=\"1\">\n");
        ret.append("    <status>\n");
        String basic = "open";
        String state = "reachable";
        if (subscriberState.getSubscriberStateChoice() != SubscriberStateChoice.assumedIdle) {
            basic = "closed";
            switch (subscriberState.getSubscriberStateChoice()) {
            case camelBusy:
                state = "busy";
                break;
            case netDetNotReachable:
                state = "unreachable";
                break;
            default:
                state = "unknown";
                break;
            }
        }
        ret.append("      <basic>").append(basic).append("</basic>\n");
        ret.append("      <state>").append(state).append("</state>\n");
        ret.append("    </status>\n");
        ret.append("  </tuple>\n");
        ret.append("</presence>\n");
        return ret.toString();
    }

    /**
     * SIP message detector which accepts NOTIFY responses.
     * @author Miklos Pocsaji
     *
     */
    private static class NotifyResponseDetector implements SipMessageDetector {

        @Override
        public boolean accept(SipServletMessage msg) {
            if (!"NOTIFY".equals(msg.getMethod()))
                return false;
            return (msg instanceof SipServletResponse);

        }

    }
}
