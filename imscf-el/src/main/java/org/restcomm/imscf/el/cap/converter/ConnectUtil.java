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
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ConnectRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.DestinationRoutingAddress;
import org.mobicents.protocols.ss7.isup.ISUPParameterFactory;
import org.mobicents.protocols.ss7.isup.message.parameter.CalledPartyNumber;

/** Utility class for sending out a connect request. */
public final class ConnectUtil {
    private ConnectUtil() {
        // NOOP
    }

    public static Long connectCall(CAPDialogCircuitSwitchedCall dialog, URI newBNumber, boolean closeDialog)
            throws CAPException {

        ISUPParameterFactory isup = dialog.getService().getCAPProvider().getISUPParameterFactory();
        CalledPartyNumber cdpn = isup.createCalledPartyNumber();
        if (newBNumber instanceof SipURI) {
            String num = ((SipURI) newBNumber).getUser();
            if (num.startsWith("+"))
                num = num.substring(1);
            cdpn.setAddress(num);
        } else if (newBNumber instanceof TelURL) {
            cdpn.setAddress(((TelURL) newBNumber).getPhoneNumber()); // return value never contains the +
        } else {
            throw new CAPException("No destinationRoutingAddress available for the connect");
        }

        cdpn.setNumberingPlanIndicator(CalledPartyNumber._NPI_ISDN);
        Optional.ofNullable(newBNumber.getParameter("npi")).ifPresent(npi -> {
            cdpn.setNumberingPlanIndicator(Integer.parseInt(npi));
        });

        cdpn.setInternalNetworkNumberIndicator(CalledPartyNumber._INN_ROUTING_NOT_ALLOWED);
        Optional.ofNullable(newBNumber.getParameter("inn")).ifPresent(inn -> {
            cdpn.setInternalNetworkNumberIndicator(Integer.parseInt(inn));
        });
        cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_INTERNATIONAL_NUMBER);
        Optional.ofNullable(newBNumber.getParameter("noa")).ifPresent(noa -> {
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

        CAPParameterFactory cap = dialog.getService().getCAPProvider().getCAPParameterFactory();

        DestinationRoutingAddress dra = cap.createDestinationRoutingAddress(new ArrayList<>(Collections
                .singletonList(cap.createCalledPartyNumberCap(cdpn))));

        // Long addConnectRequest(DestinationRoutingAddress destinationRoutingAddress,
        // AlertingPatternCap alertingPattern,
        // OriginalCalledNumberCap originalCalledPartyID,
        // CAPExtensions extensions,
        // Carrier carrier,
        // CallingPartysCategoryInap callingPartysCategory,
        // RedirectingPartyIDCap redirectingPartyID,
        // RedirectionInformationInap redirectionInformation,
        // ArrayList<GenericNumberCap> genericNumbers,
        // ServiceInteractionIndicatorsTwo serviceInteractionIndicatorsTwo,
        // LocationNumberCap chargeNumber,
        // LegID legToBeConnected,
        // CUGInterlock cugInterlock,
        // boolean cugOutgoingAccess,
        // boolean suppressionOfAnnouncement,
        // boolean ocsIApplicable,
        // NAOliInfo naoliInfo,
        // boolean borInterrogationRequested)
        // throws CAPException;
        Long invokeId = dialog.addConnectRequest(dra, null, null, null, null, null, null, null, null, null, null, null,
                null, false, false, false, null, false, false);
        if (closeDialog) {
            dialog.close(false);
        } else {
            dialog.send();
        }
        return invokeId;
    }

    public static Long connectCall(CAPDialogCircuitSwitchedCall dialog, ConnectRequest con, boolean closeDialog)
            throws CAPException {
        // all parameters are simply copied
        Long invokeId = dialog.addConnectRequest(con.getDestinationRoutingAddress(), con.getAlertingPattern(),
                con.getOriginalCalledPartyID(), con.getExtensions(), con.getCarrier(), con.getCallingPartysCategory(),
                con.getRedirectingPartyID(), con.getRedirectionInformation(), con.getGenericNumbers(),
                con.getServiceInteractionIndicatorsTwo(), con.getChargeNumber(), con.getLegToBeConnected(),
                con.getCUGInterlock(), con.getCugOutgoingAccess(), con.getSuppressionOfAnnouncement(),
                con.getOCSIApplicable(), con.getNAOliInfo(), con.getBorInterrogationRequested(), con.getSuppressNCSI());
        if (closeDialog) {
            dialog.close(false);
        } else {
            dialog.send();
        }
        return invokeId;
    }
}
