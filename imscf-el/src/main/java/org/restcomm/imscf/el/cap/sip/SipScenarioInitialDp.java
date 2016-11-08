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
package org.restcomm.imscf.el.cap.sip;

import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipResponseClass;
import org.restcomm.imscf.el.sip.SipResponseDetector;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;
import org.restcomm.imscf.el.sip.routing.SipAsRouteAndInterface;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.MultipartBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.isup.CalledPartyNumberCap;
import org.mobicents.protocols.ss7.cap.api.isup.CallingPartyNumberCap;
import org.mobicents.protocols.ss7.cap.api.primitives.CalledPartyBCDNumber;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.isup.message.parameter.CalledPartyNumber;
import org.mobicents.protocols.ss7.isup.message.parameter.CallingPartyNumber;
import org.mobicents.protocols.ss7.isup.message.parameter.NAINumber;

/** SIP scenario for disconnecting a single SIP leg. */
public final class SipScenarioInitialDp {
    // visual separators not included, as they cannot appear in an IDP...
    private static final Pattern GLOBAL_NUM_PATTERN = Pattern.compile("[0-9]+");

    private SipScenarioInitialDp() {
        // no instance, pseudo-scenario
        throw new UnsupportedOperationException();
    }

    public static SipServletRequest start(CapSipCsCall call, SipAsRouteAndInterface asRoute) throws CAPException,
            IOException, ServletException, MessagingException {
        InitialDPRequest idp = call.getIdp();
        SipApplicationSession sas = call.getAppSession();
        SipServletRequest invite;
        String from = null;
        String to = null;
        String privacy = null;

        Optional<CallingPartyNumberCap> cgpnOpt = Optional.ofNullable(idp.getCallingPartyNumber());
        if (cgpnOpt.isPresent()) {
            CallingPartyNumber cgpn = cgpnOpt.get().getCallingPartyNumber();
            String prefix = "", postfix = ";phone-context=local", noa = null, address = cgpn.getAddress();
            switch (cgpn.getNatureOfAddressIndicator()) {
            case NAINumber._NAI_INTERNATIONAL_NUMBER:
                prefix = "+";
                postfix = "";
                break;
            case NAINumber._NAI_NATIONAL_SN:
                noa = "national";
                break;
            case NAINumber._NAI_SUBSCRIBER_NUMBER:
                noa = "subscriber";
                break;
            case NAINumber._NAI_UNKNOWN:
                noa = "unknown";
                break;
            default:
                break;
            }
            if (address != null && !address.isEmpty()) { // otherwise the tel uri is invalid
                from = "<tel:" + prefix + address + postfix + (noa == null ? "" : ";noa=" + noa) + ";npi="
                        + cgpn.getNumberingPlanIndicator() + ">";
            }
            if (cgpn.getAddressRepresentationRestrictedIndicator() == CallingPartyNumber._APRI_RESTRICTED) {
                privacy = "id";
            }
        }

        Optional<CalledPartyNumberCap> cdpnOpt = Optional.ofNullable(idp.getCalledPartyNumber());
        if (cdpnOpt.isPresent()) {
            CalledPartyNumber cdpn = cdpnOpt.get().getCalledPartyNumber();
            String prefix = "", postfix = ";phone-context=local", noa = null, address = cdpn.getAddress();
            switch (cdpn.getNatureOfAddressIndicator()) {
            case NAINumber._NAI_INTERNATIONAL_NUMBER:
                // Check whether the number is actually safe to be used in a global tel: URI
                // (i.e. not containing *, #, or anything other than numbers)
                if (address != null && GLOBAL_NUM_PATTERN.matcher(address).matches()) {
                    prefix = "+";
                    postfix = "";
                } else {
                    // pass to the AS to interpret the invalid number as it wishes...
                    noa = "international";
                }
                break;
            case NAINumber._NAI_NATIONAL_SN:
                noa = "national";
                break;
            case NAINumber._NAI_SUBSCRIBER_NUMBER:
                noa = "subscriber";
                break;
            case NAINumber._NAI_UNKNOWN:
                noa = "unknown";
                break;
            default:
                break;
            }
            if (address != null && !address.isEmpty()) { // otherwise the tel uri is invalid
                to = "<tel:" + prefix + address + postfix + (noa == null ? "" : ";noa=" + noa) + ";npi="
                        + cdpn.getNumberingPlanIndicator() + ">";
            }
        }
        Optional<CalledPartyBCDNumber> cdbnOpt = Optional.ofNullable(idp.getCalledPartyBCDNumber());
        if (to == null && cdbnOpt.isPresent()) {
            CalledPartyBCDNumber cdbn = cdbnOpt.get();
            String prefix = "", postfix = ";phone-context=local", noa = null, address = cdbn.getAddress();
            switch (cdbn.getAddressNature()) {
            case international_number:
                // Check whether the number is actually safe to be used in a global tel: URI
                // (i.e. not containing *, #, or anything other than numbers)
                if (address != null && GLOBAL_NUM_PATTERN.matcher(address).matches()) {
                    prefix = "+";
                    postfix = "";
                } else {
                    // pass to the AS to interpret the invalid number as it wishes...
                    noa = "international";
                }
                break;
            case national_significant_number:
                noa = "national";
                break;
            case subscriber_number:
                noa = "subscriber";
                break;
            case unknown:
                noa = "unknown";
                break;
            default:
                break;
            }
            if (address != null && !address.isEmpty()) { // otherwise the tel uri is invalid
                to = "<tel:" + prefix + address + postfix + (noa == null ? "" : ";noa=" + noa) + ";npi="
                        + cdbn.getNumberingPlan().getIndicator() + ">";
            }
        }

        if (from == null)
            from = "<sip:anonymous@anonymous.invalid>";
        if (to == null)
            to = "<sip:anonymous@anonymous.invalid>";

        invite = SipServletResources.getSipFactory().createRequest(sas, "INVITE", from, to);
        SipSession session = invite.getSession();
        session.setHandler("CAPServlet");
        SipSessionAttributes.LEG_ID.set(session, "L1");
        SipSessionAttributes.INITIAL_REQUEST.set(session, invite);

        if (privacy != null) {
            invite.setHeader("Privacy", privacy);
        }

        MimeMultipart mm = new MultipartBuilder()
                .addPartBody(SipConstants.CONTENTTYPE_CAP_XML_STRING, Jss7ToXml.encode(idp, "initialDP"))
                .addPartBody(SipConstants.CONTENTTYPE_SDP_STRING, SipUtil.createSdpForLegs(session)).getResult();
        invite.setContent(mm, mm.getContentType());

        // TODO: make this configurable?
        invite.addHeader("Supported", SipConstants.SUPPORTED_100REL);

        // sets up encodeuri and return route
        InetSocketAddress outboundInterface = new InetSocketAddress(InetAddress.getByName(asRoute
                .getOutboundInterfaceHost()), asRoute.getOutboundInterfacePort());
        SipUtil.prepareInitialInviteToAS(invite, asRoute.getAsRoute(), outboundInterface);

        // setting the appropriate outbound interface
        session.setOutboundInterface(outboundInterface);

        // technical scenario: store final response for INVITE if it arrives
        // this must be added BEFORE the Request Queue Processor for the INVITE added by queueMessage()
        // TODO: ugly solution, find something better
        call.getSipScenarios().add(
                new Scenario("Final answer for INVITE", new SipResponseDetector(invite, SipResponseClass.FINAL), (scen,
                        msg) -> {
                    scen.setFinished();
                    SipSessionAttributes.INITIAL_RESPONSE.set(msg.getSession(), msg);
                }));

        call.queueMessage(invite);
        SipScenarioAsReaction.start(call, invite.getSession());
        return invite;
    }

}
