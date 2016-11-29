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
package org.restcomm.imscf.el.cap.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import javax.servlet.sip.SipURI;
import javax.servlet.sip.TelURL;
import javax.servlet.sip.URI;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPParameterFactory;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.cap.api.isup.CallingPartyNumberCap;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.DestinationRoutingAddress;
import org.mobicents.protocols.ss7.isup.ISUPParameterFactory;
import org.mobicents.protocols.ss7.isup.message.parameter.CalledPartyNumber;
import org.mobicents.protocols.ss7.isup.message.parameter.CallingPartyNumber;

/** Utility class for converting SIP/Tel URI to various SS7 addresses. */
public final class UriAddressParser {
    private UriAddressParser() {
    }

    public static String getNumber(URI uri) {
        if (uri == null)
            return null;
        else if (uri instanceof SipURI) {
            String num = ((SipURI) uri).getUser();
            if (num.startsWith("+"))
                num = num.substring(1);
            return num;
        } else if (uri instanceof TelURL) {
            return ((TelURL) uri).getPhoneNumber(); // return value never contains the +
        } else {
            throw new AssertionError("Invalid URI type");
        }
    }

    public static CalledPartyNumber parseCalledPartyNumber(URI uri, CAPProvider capProvider) {
        if (uri == null)
            return null;
        ISUPParameterFactory isup = capProvider.getISUPParameterFactory();
        CalledPartyNumber cdpn = isup.createCalledPartyNumber();
        cdpn.setAddress(getNumber(uri));

        cdpn.setNumberingPlanIndicator(CalledPartyNumber._NPI_ISDN);
        Optional.ofNullable(uri.getParameter("npi")).ifPresent(npi -> {
            cdpn.setNumberingPlanIndicator(Integer.parseInt(npi));
        });

        cdpn.setInternalNetworkNumberIndicator(CalledPartyNumber._INN_ROUTING_NOT_ALLOWED);
        Optional.ofNullable(uri.getParameter("inn")).ifPresent(inn -> {
            cdpn.setInternalNetworkNumberIndicator(Integer.parseInt(inn));
        });
        cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_INTERNATIONAL_NUMBER);
        Optional.ofNullable(uri.getParameter("noa")).ifPresent(noa -> {
            switch (noa) {
            case "international":
                cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_INTERNATIONAL_NUMBER);
                break;
            case "national":
                cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_NATIONAL_SN);
                break;
            case "unknown":
                cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_UNKNOWN);
                break;
            case "subscriber":
                cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_SUBSCRIBER_NUMBER);
                break;
            default:
                cdpn.setNatureOfAddresIndicator(Integer.parseInt(noa));
                break;
            }
        });
        return cdpn;
    }

    public static DestinationRoutingAddress parseDestinationRoutingAddress(URI uri, CAPProvider capProvider)
            throws CAPException {
        if (uri == null)
            return null;
        CalledPartyNumber cdpn = parseCalledPartyNumber(uri, capProvider);
        CAPParameterFactory cap = capProvider.getCAPParameterFactory();
        return cap.createDestinationRoutingAddress(new ArrayList<>(Collections.singletonList(cap
                .createCalledPartyNumberCap(cdpn))));
    }

    public static CallingPartyNumber parseCallingPartyNumber(URI uri, CAPProvider capProvider) {
        if (uri == null)
            return null;
        ISUPParameterFactory isup = capProvider.getISUPParameterFactory();
        CallingPartyNumber cgpn = isup.createCallingPartyNumber();
        cgpn.setAddress(getNumber(uri));

        // no parameter for these, use defaults
        cgpn.setAddressRepresentationREstrictedIndicator(CallingPartyNumber._APRI_ALLOWED);
        cgpn.setNumberIncompleteIndicator(CallingPartyNumber._NI_COMPLETE);
        cgpn.setScreeningIndicator(CallingPartyNumber._SI_NETWORK_PROVIDED);

        cgpn.setNumberingPlanIndicator(CallingPartyNumber._NPI_ISDN);
        Optional.ofNullable(uri.getParameter("npi")).ifPresent(npi -> {
            cgpn.setNumberingPlanIndicator(Integer.parseInt(npi));
        });

        cgpn.setNatureOfAddresIndicator(CallingPartyNumber._NAI_INTERNATIONAL_NUMBER);
        Optional.ofNullable(uri.getParameter("noa")).ifPresent(noa -> {
            switch (noa) {
            case "international":
                cgpn.setNatureOfAddresIndicator(CallingPartyNumber._NAI_INTERNATIONAL_NUMBER);
                break;
            case "national":
                cgpn.setNatureOfAddresIndicator(CallingPartyNumber._NAI_NATIONAL_SN);
                break;
            case "unknown":
                cgpn.setNatureOfAddresIndicator(CallingPartyNumber._NAI_UNKNOWN);
                break;
            case "subscriber":
                cgpn.setNatureOfAddresIndicator(CallingPartyNumber._NAI_SUBSCRIBER_NUMBER);
                break;
            default:
                cgpn.setNatureOfAddresIndicator(Integer.parseInt(noa));
                break;
            }
        });
        return cgpn;
    }

    public static CallingPartyNumberCap parseCallingPartyNumberCap(URI uri, CAPProvider capProvider)
            throws CAPException {
        if (uri == null)
            return null;
        CallingPartyNumber cgpn = parseCallingPartyNumber(uri, capProvider);
        return capProvider.getCAPParameterFactory().createCallingPartyNumberCap(cgpn);
    }
}
