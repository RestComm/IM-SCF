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
package org.restcomm.imscf.el.cap.converter;

import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipInitiateCallAttemptDetector;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.modules.ModuleStore;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageBodyUtil;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.common.util.ImscfCallId;
import org.restcomm.imscf.util.Jss7ToXml;
import org.restcomm.imscf.util.Jss7ToXml.XmlDecodeException;

import java.util.List;
import java.util.Optional;

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.cap.api.isup.CallingPartyNumberCap;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitiateCallAttemptRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.DestinationRoutingAddress;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.InitiateCallAttemptRequestImpl;
import org.mobicents.protocols.ss7.inap.api.primitives.LegID;
import org.mobicents.protocols.ss7.inap.api.primitives.LegType;
import org.mobicents.protocols.ss7.inap.primitives.LegIDImpl;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.callhandling.CallReferenceNumber;
import org.mobicents.protocols.ss7.map.service.callhandling.CallReferenceNumberImpl;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SIP scenario for detecting and handling an incoming initiateCallAttempt INVITE. */
public final class SipScenarioInitiateCallAttempt extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioInitiateCallAttempt.class);

    public static SipScenarioInitiateCallAttempt start(List<BCSMEvent> defaultEvents) {
        return new SipScenarioInitiateCallAttempt(defaultEvents);
    }

    private SipScenarioInitiateCallAttempt(List<BCSMEvent> defaultEvents) {
        super("Waiting for initiateCallAttempt INVITE", new SipInitiateCallAttemptDetector(), new SipMessageHandler() {
            @Override
            public void handleMessage(Scenario scenario, SipServletMessage incomingInvite) {
                SipServletRequest invite = (SipServletRequest) incomingInvite;
                CallStore store = (CallStore) CallContext.get(CallContext.CALLSTORE);
                try (final CapSipCsCall call = (CapSipCsCall) store.getSipCall(invite)) {
                    LOG.debug("ICA request arrived");

                    SipSessionAttributes.INITIAL_REQUEST.set(invite.getSession(), invite);

                    // check that the CAP dialog is usable beforehand instead of parsing the XML and failing during send
                    if (!CapUtil.canSendPrimitives(call.getCapDialog())) {
                        SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                                invite.createResponse(SipServletResponse.SC_BAD_REQUEST),
                                "Cannot send ICA in dialog state: " + call.getCapDialog().getState()),
                                "Failed to send error response to ICA INVITE");
                        return;
                    }

                    // first check if we received an ica arg
                    LOG.trace("Trying to decode CAP XML content...");
                    InitiateCallAttemptRequest ica = null;

                    List<Object> capBodyParts = SipMessageBodyUtil
                            .findContent(invite, SipConstants.CONTENTTYPE_CAP_XML);
                    for (Object o : capBodyParts) {
                        // if such a body part is present, it must be a valid ICA arg
                        try {
                            ica = Jss7ToXml.decode(o, InitiateCallAttemptRequestImpl.class);
                            LOG.debug("ICA XML received and decoded successfully");
                        } catch (XmlDecodeException e) {
                            LOG.warn("Invalid or missing initiateCallAttempt XML", e);
                            SipServletResponse resp = invite.createResponse(SipServletResponse.SC_BAD_REQUEST);
                            SipUtil.createAndSetWarningHeader(resp, "Invalid or missing initiateCallAttempt XML");
                            SipUtil.sendOrWarn(resp, "Failed to send error response to ICA INVITE");
                            return;
                        }
                    }
                    if (ica == null) {
                        LOG.trace("No ICA XML body received");
                    }

                    // Sending Side LegID is always used in operations sent from the gsmSCF to the gsmSSF, and Receiving
                    // Side LegID is always used in operations sent from the gsmSSF to the gsmSCF.
                    int networkLegID = Optional.ofNullable(ica).map(InitiateCallAttemptRequest::getLegToBeCreated)
                            .map(LegID::getSendingSideID).map(LegType::getCode)
                            .orElse(call.getCallSegmentAssociation().getLowestAvailableIcaLegID());
                    if (networkLegID < 0 || call.getCallSegmentAssociation().getCallSegmentOfLeg(networkLegID) != null) {
                        LOG.warn("LegID {} is invalid or already used", networkLegID);
                        SipServletResponse resp = invite.createResponse(SipServletResponse.SC_BAD_REQUEST);
                        SipUtil.createAndSetWarningHeader(resp, "LegID " + networkLegID + " is invalid or already used");
                        SipUtil.sendOrWarn(resp, "Failed to send error response to ICA INVITE");
                        return;
                    }

                    SipSessionAttributes.LEG_ID.set(invite.getSession(), SipUtil.sdpIdFromNetworkLegId(networkLegID));

                    int csID = Optional.ofNullable(ica).map(InitiateCallAttemptRequest::getNewCallSegment)
                            .orElse(call.getCallSegmentAssociation().getLowestAvailableCSID());
                    if (csID < 0 || call.getCallSegmentAssociation().getCallSegment(csID) != null) {
                        LOG.warn("CallSegmentID {} is invalid or already used", csID);
                        SipServletResponse resp = invite.createResponse(SipServletResponse.SC_BAD_REQUEST);
                        SipUtil.createAndSetWarningHeader(resp, "CallSegmentID " + csID + " is invalid or already used");
                        SipUtil.sendOrWarn(resp, "Failed to send error response to ICA INVITE");
                        return;
                    }
                    LOG.debug("New legID {}, callSegmentID {}", networkLegID, csID);

                    // update dialog: set remote address based on the alias, if not already set
                    if (call.getCapDialog().getRemoteAddress() == null) {
                        String remoteAlias = getRemoteAliasFromInvite(invite);
                        SccpAddress remoteAddress = ModuleStore.getSccpModule().getRemoteAddress(remoteAlias);
                        if (remoteAddress == null) {
                            LOG.warn("Remote alias '{}' is invalid", remoteAlias);
                            SipServletResponse resp = invite.createResponse(SipServletResponse.SC_BAD_REQUEST);
                            SipUtil.createAndSetWarningHeader(resp, "Remote alias '" + remoteAlias + "' is invalid");
                            SipUtil.sendOrWarn(resp, "Failed to send error response to ICA INVITE");
                            return;
                        }
                        LOG.debug("CAP dialog has no remote address, setting to {} -> {}", remoteAlias, remoteAddress);
                        call.getCapDialog().setRemoteAddress(remoteAddress);
                    }

                    long invokeId = 0;
                    // update ICA arg to contain legid and csid, then send it out
                    try {
                        if (ica != null) {
                            // send as requested by the AS (but include missing and necessary parameters)
                            LOG.debug("Sending ICA with provided ICAArg");
                            invokeId = sendInitiateCallAttempt(call.getCapDialog(), ica, networkLegID, csID, call
                                    .getCapModule().getGsmScfAddress());
                        } else if (call.getCallSegmentAssociation().getCallSegments().isEmpty()) {
                            // new call started by this ICA request, include all available parameters
                            LOG.debug("Call is being created by ICA, using full parameter set");
                            invokeId = sendInitiateCallAttempt(call.getCapDialog(),
                                    getCallingPartyNumberFromInvite(invite),
                                    getDestinationRoutingAddressFromInvite(invite), networkLegID, csID,
                                    createCallReferenceNumber(call), call.getCapModule().getGsmScfAddress());
                        } else {
                            // ICA in existing call, send with minimal parameters
                            LOG.debug("ICA in existing call, using minimal parameter set");
                            invokeId = sendInitiateCallAttempt(call.getCapDialog(),
                                    getDestinationRoutingAddressFromInvite(invite), networkLegID, csID);
                        }
                    } catch (CAPException e) {
                        LOG.warn("Failed to send initiateCallAttempt: {}", e.getMessage(), e);
                        SipServletResponse resp = invite.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
                        SipUtil.createAndSetWarningHeader(resp, "Failed to send initiateCallAttempt");
                        SipUtil.sendOrWarn(resp, "Failed to send error response to ICA INVITE");
                        return;
                    }

                    call.getCapOutgoingRequestScenarios().add(
                            CapScenarioInitiateCallAttempt.start(call, invite, invokeId, defaultEvents, csID));

                }
            }
        });
    }

    protected static String getRemoteAliasFromInvite(SipServletRequest invite) {
        URI uri = getDestinationRoutingAddressFromInvite(invite);
        if (uri instanceof SipURI) {
            return ((SipURI) uri).getHost();
        }
        return null;
    }

    protected static URI getCallingPartyNumberFromInvite(SipServletRequest invite) {
        return invite.getFrom().getURI();
    }

    protected static URI getDestinationRoutingAddressFromInvite(SipServletRequest invite) {
        return invite.getRequestURI();
    }

    private static long sendInitiateCallAttempt(CAPDialogCircuitSwitchedCall dialog, InitiateCallAttemptRequest ica,
            int newLegID, int newCsID, ISDNAddressString gsmScfAddress) throws CAPException {

        LegID legToBeCreated = Optional.ofNullable(ica.getLegToBeCreated()).orElseGet(() -> {
            return new LegIDImpl(true, LegType.getInstance(newLegID));
        });
        Integer csID = Optional.ofNullable(ica.getNewCallSegment()).orElseGet(() -> {
            return Integer.valueOf(newCsID);
        });
        ISDNAddressString gsmScf = Optional.ofNullable(ica.getGsmSCFAddress()).orElse(gsmScfAddress);

        Long invokeId = dialog.addInitiateCallAttemptRequest(ica.getDestinationRoutingAddress(), ica.getExtensions(),
                legToBeCreated, csID, ica.getCallingPartyNumber(), ica.getCallReferenceNumber(), gsmScf,
                ica.getSuppressTCsi());
        dialog.send();
        return invokeId;
    }

    /** Send an ICA in an existing call with only minimal parameters. */
    private static long sendInitiateCallAttempt(CAPDialogCircuitSwitchedCall dialog, URI destination, int newLegID,
            int newCsID) throws CAPException {
        return sendInitiateCallAttempt(dialog, null, destination, newLegID, newCsID, null, null);
    }

    /** Send an ICA as a new call with all the required parameters. */
    private static long sendInitiateCallAttempt(CAPDialogCircuitSwitchedCall dialog, URI callingParty, URI destination,
            int newLegID, int newCsID, CallReferenceNumber callReferenceNumber, ISDNAddressString gsmScfAddress)
            throws CAPException {

        CAPProvider p = dialog.getService().getCAPProvider();
        DestinationRoutingAddress dra = UriAddressParser.parseDestinationRoutingAddress(destination, p);
        CallingPartyNumberCap cgpn = UriAddressParser.parseCallingPartyNumberCap(callingParty, p);
        LegID legToBeCreated = new LegIDImpl(true, LegType.getInstance(newLegID));
        Integer csID = Integer.valueOf(newCsID);

        Long invokeId = dialog.addInitiateCallAttemptRequest(dra, null, legToBeCreated, csID, cgpn,
                callReferenceNumber, gsmScfAddress, false);
        dialog.send();
        return invokeId;
    }

    private static CallReferenceNumber createCallReferenceNumber(IMSCFCall call) {
        ImscfCallId imscfcallid = ImscfCallId.parse(call.getImscfCallId());
        long ref = (imscfcallid.getTimestamp() << 16);
        ref |= ((imscfcallid.getSequence() & 0xFF) << 8);
        ref |= CallContext.getConfigBean().getServerIndex() & 0xFF;

        return new CallReferenceNumberImpl(longToBytes(ref));
    }

    private static byte[] longToBytes(long val) {
        byte[] ret = new byte[Long.BYTES];
        for (int i = 0, sh = (Long.SIZE - Byte.SIZE); i < Long.BYTES; i++, sh -= Byte.SIZE) {
            ret[i] = (byte) ((val >>> sh) & 0xFF);
        }
        return ret;
    }

}
