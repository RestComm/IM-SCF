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
package org.restcomm.imscf.sl.stack;

import org.restcomm.imscf.common.config.SignalingLayerServerType;
import org.restcomm.imscf.sl.diameter.DiameterDialogId;
import org.restcomm.imscf.sl.diameter.DiameterGW;
import org.restcomm.imscf.sl.diameter.ELDiameterRouterBean;
import org.restcomm.imscf.sl.diameter.listener.SLDiameterCCASessionListener;
import org.restcomm.imscf.sl.history.Event;
import org.restcomm.imscf.sl.history.SlCallHistoryStore;
import org.restcomm.imscf.sl.log.MDCParameters;
import org.restcomm.imscf.sl.log.MDCParameters.Parameter;
import org.restcomm.imscf.sl.statistics.SlStatistics;
import org.restcomm.imscf.common.DiameterSerializer;
import org.restcomm.imscf.common.SLELRouter;
import org.restcomm.imscf.common.SccpDialogId;
import org.restcomm.imscf.common.SccpSerializer;
import org.restcomm.imscf.common.TcapDialogId;
import org.restcomm.imscf.common.diameter.creditcontrol.CCRequestType;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterSLELCreditControlResponse;
import org.restcomm.imscf.common.messages.SccpManagementMessage;
import org.restcomm.imscf.common.util.TCAPMessageInfo;
import org.restcomm.imscf.common.util.TCAPMessageInfo.MessageType;
import org.restcomm.imscf.common.util.ImscfCallId;
import org.restcomm.imscf.common.util.overload.OverloadProtector;
import org.restcomm.imscf.common.lwcomm.service.IncomingTextMessage;
import org.restcomm.imscf.common.lwcomm.service.LwCommService;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.message.SccpDataMessage;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageReceiver implementation for LWCOMM. Forwards received messages to the SCCP stack.
 */
@SuppressWarnings("PMD.GodClass")
public class LwcommMessageReceiver implements MessageReceiver {
    private static Logger logger = LoggerFactory.getLogger(LwcommMessageReceiver.class);
    private static final Pattern LWC_MESSAGE_PATTERN = Pattern
            .compile("^Target: (?<target>.+)\r\n(?<extraHeaders>(?:[^: ]+: .+\r\n)+)?(?:Content: (?<contentType>.+)\r\n\r\n(?<contentData>.+))?$");
    private static final int PC_FILLED_BY_SL = 0xC000;
    private SccpProvider sccpProvider;
    private SLELRouter<SlElMappingData> slElRouter;
    private SignalingLayerServerType server;
    private SlCallHistoryStore callHistoryStore;
    private ELDiameterRouterBean elDiameterRouterBean;
    private transient LwCommService lwc;

    private transient SLSccpListener slSccpListener;

    public LwcommMessageReceiver(SlCallHistoryStore callHistoryStore) {
        this.callHistoryStore = Objects.requireNonNull(callHistoryStore, "CallHistoryStore cannot be null");
    }

    public void setupForSigtran(SccpProvider sccpProvider, SignalingLayerServerType server,
            SLELRouter<SlElMappingData> slElRouter, LwCommService lwc, SLSccpListener slSccpListener) {
        this.sccpProvider = Objects.requireNonNull(sccpProvider, "SCCP provider cannot be null");
        this.server = Objects.requireNonNull(server, "SL server cannot be null");
        this.slElRouter = Objects.requireNonNull(slElRouter, "SL-EL router cannot be null");
        this.lwc = Objects.requireNonNull(lwc, "LwcommService cannot be null");
        this.slSccpListener = Objects.requireNonNull(slSccpListener, "SLSccpListener cannot be null");
    }

    public void setupForDiameter(ELDiameterRouterBean elDiameterRouterBean) {
        this.elDiameterRouterBean = Objects.requireNonNull(elDiameterRouterBean, "ELDiameterRouter cannot be null");
    }

    private void handleSccpDataMessageContent(IncomingTextMessage msg, String sccpData) {
        String elNodeName = msg.getFrom().getName();
        try {
            MDCParameters.toMDC(MDCParameters.Parameter.IMSCF_CALLID, msg.getGroupId());
            SccpDataMessage sdm = SccpSerializer.deserialize(sccpData);
            logger.trace("SccpDataMessage got: {}", sdm);
            if (sdm.getCallingPartyAddress().getSignalingPointCode() == PC_FILLED_BY_SL) {
                logger.debug("Calling party's point code is special: 0xC0000 -- this means that it should be replaced by the point code of this SL.");
                SccpAddress origCalling = sdm.getCallingPartyAddress();
                SccpAddress calling = new SccpAddressImpl(origCalling.getAddressIndicator().getRoutingIndicator(),
                        origCalling.getGlobalTitle(), server.getPointCode(), origCalling.getSubsystemNumber());
                sdm.setCallingPartyAddress(calling);
                logger.debug("Calling party address set to {}", calling);
            }
            if (1 == sdm.getProtocolClass().getProtocolClass()) {
                // create new message in the actual protocol stack
                sdm = sccpProvider.getMessageFactory().createDataMessageClass1(sdm.getCalledPartyAddress(),
                        sdm.getCallingPartyAddress(), sdm.getData(), sdm.getSls(), sdm.getOriginLocalSsn(),
                        sdm.getProtocolClass().getReturnMessageOnError(), sdm.getHopCounter(), sdm.getImportance());
            }

            TCAPMessageInfo info = TCAPMessageInfo.parse(sdm.getData());
            logger.debug("TCAP info: {}", info.toString());
            if (info.getMessageType() == MessageType.TC_BEGIN
                    && OverloadProtector.getInstance().getCurrentState().isCpuOrHeapOverloaded()) {
                // System is overloaded. Do not handle incoming TCAP BEGIN messages, simply ignore them
                logger.warn("System is overloaded, dropping TC_BEGIN from EL for call {}", msg.getGroupId());
                return;
            }
            SccpDialogId sdid = SccpDialogId.extractFromSccpMessage(sdm, false);
            TcapDialogId tdid = TcapDialogId.extractFromTCAPMessageInfo(info, false);
            ImscfCallId imscfCallId = ImscfCallId.parse(msg.getGroupId());
            // TC_BEGIN: ICA, MAP, etc.; dialogue started on EL side
            // further TCAP messages in SL->EL direction with
            // this DTID should be sent to the same node
            // TC_CONTINUE: dialogue started on remote side, mapping only stored here

            // Update the mapping here, regardless of what (BEGIN, CONT, END, ABORT) has
            // been arrived.
            // This is due to that network-initiated calls' mapping are not stored when
            // receiving the BEGIN from the network. The reasons for that are the following:
            // - If the call is a BEGIN-END (e.g. IDP-releaseCall), no mapping has to be stored
            // at all
            // - If the call is a BEGIN-CONTINUE-...-END, the mapping can be stored only at
            // the first continue from EL.
            //
            // The drawback of setting the mapping here regardless of TCAP type
            // is that if it is an END or ABORT, the next part will null it out.
            // The reason for that setting even at END or ABORT is that if a clearing
            // tries to clear a non-existent mapping, a WARN log is generated. We do
            // not want to lower the level of that log because in some
            // circumstances it can indicate malicious behavior.
            SlElMappingData mappingData = new SlElMappingData();
            mappingData.setNodeName(elNodeName);
            mappingData.setImscfCallId(imscfCallId);
            SlElMappingData previousData = slElRouter.setMappingData(sdid, tdid, mappingData);
            String storedElNodeName = previousData != null ? previousData.getNodeName() : null;
            if (storedElNodeName != null && !storedElNodeName.equals(elNodeName)) {
                logger.warn("Unexpected EL mapping change: {} -> {}!", storedElNodeName, elNodeName);
            }
            if (info.getMessageType() == MessageType.TC_END || info.getMessageType() == MessageType.TC_ABORT) {
                // no further messages will arrive from EL, remove mapping
                logger.debug("dialog end for {} / {} due to {}, removing EL mapping", sdid, tdid, info.getMessageType());
                slElRouter.setMappingData(sdid, tdid, null);
            }

            callHistoryStore.registerEvent(imscfCallId, Event.LWC_IN, info.getMessageType().toString(), msg.getId());
            sccpProvider.send(sdm);
            if (info.getMessageType() == MessageType.TC_BEGIN) {
                callHistoryStore.registerEvent(imscfCallId, Event.fromTcap(info, false), sdid.toString(), "OTID: 0x"
                        + Long.toHexString(info.getOtid()));
            } else {
                callHistoryStore.registerEvent(imscfCallId, Event.fromTcap(info, false));
            }

            if (info.getMessageType() == MessageType.TC_END || info.getMessageType() == MessageType.TC_ABORT) {
                callHistoryStore.logAndRemoveCallHistory(imscfCallId);
            }
            SlStatistics.incMessagesSentToPc(sdm.getCalledPartyAddress().getSignalingPointCode());

        } catch (IOException e) {
            logger.error("Error sending sccp message", e);
        } finally {
            MDCParameters.clearMDC();
        }
    }

    private void handleSccpManagementMessageContent(IncomingTextMessage msg, String data) {
        try {
            MDCParameters.toMDC(MDCParameters.Parameter.IMSCF_CALLID, msg.getGroupId());
            SccpManagementMessage manMsg = SccpManagementMessage.deserialize(data);
            switch (manMsg.getType()) { // NOPMD for too few branches
            case DeleteCall:
                ImscfCallId imscfCallId = ImscfCallId.parse(msg.getGroupId());
                logger.warn(
                        "DeleteCall management message arrived for imscf call id {}, sccp dialog id {}, tcap dialog id {}",
                        imscfCallId, manMsg.getSccpDialogId(), manMsg.getTcapDialogId());
                callHistoryStore.registerEvent(imscfCallId, Event.LWC_IN, manMsg.getType().toString(), msg.getId());
                slElRouter.setMappingData(manMsg.getSccpDialogId(), manMsg.getTcapDialogId(), null);
                callHistoryStore.logAndRemoveCallHistory(imscfCallId);
                break;
            default:
                break;
            }
        } finally {
            MDCParameters.clearMDC();
        }
    }

    private void handleDiameterAnswerMessageContent(IncomingTextMessage msg, String data) {
        try {
            MDCParameters.toMDC(MDCParameters.Parameter.IMSCF_CALLID, msg.getGroupId());
            DiameterSLELCreditControlResponse drm = DiameterSerializer.deserializeResponse(data);
            logger.trace("DiameterDataMessage got: {}", drm);

            DiameterDialogId diamId = new DiameterDialogId(drm.getSessionId());

            String elNodeName = msg.getFrom().getName();
            ImscfCallId imscfCallId = ImscfCallId.parse(msg.getGroupId());

            SlElMappingData mappingData = new SlElMappingData();
            mappingData.setNodeName(elNodeName);
            mappingData.setImscfCallId(imscfCallId);

            SlElMappingData previousData = elDiameterRouterBean.setMappingData(diamId, mappingData);
            String storedElNodeName = previousData != null ? previousData.getNodeName() : null;
            if (storedElNodeName != null && !storedElNodeName.equals(elNodeName)) {
                logger.warn("Unexpected EL mapping change: {} -> {}!", storedElNodeName, elNodeName);
            }
            CCRequestType requestType = null;

            requestType = (DiameterGW.getDataForCCASessionId().get(drm.getSessionId())).getCcRequestType();

            callHistoryStore.registerEvent(imscfCallId, Event.LWC_IN);

            SLDiameterCCASessionListener.processCCResponse(drm.getSessionId(), drm, false, drm.isRemoveSession());

            callHistoryStore.registerEvent(imscfCallId,
                    Event.fromDiameter(requestType, drm.getQueryResultObject(), false));

            if (drm.isRemoveSession() || (requestType.equals(CCRequestType.DEBIT))) {
                elDiameterRouterBean.setMappingData(diamId, null);
                callHistoryStore.logAndRemoveCallHistory(imscfCallId);
            }
        } catch (Exception e) {
            logger.error("Error while sending diameter response message: {}", e.getMessage(), e);
        } finally {
            MDCParameters.clearMDC();
        }
    }

    private void handleELRouterQuery(IncomingTextMessage msg, String sccpData) {
        try {
            String queryId = msg.getGroupId();
            ImscfCallId queryIdParsed = ImscfCallId.parse(queryId);
            MDCParameters.toMDC(Parameter.IMSCF_CALLID, queryId);
            // client should actually be included in the queryId, which is an ImscfCallId, but we don't have to parse it
            String queryClient = msg.getFrom().getName();
            String queryResponse = null;

            SccpDataMessage sdm = SccpSerializer.deserialize(sccpData);
            SccpDialogId sdid = SccpDialogId.extractFromSccpMessage(sdm, true); // treat as incoming
            TCAPMessageInfo info = TCAPMessageInfo.parse(sdm.getData());
            callHistoryStore.registerEvent(queryIdParsed, Event.EL_ROUTER_QUERY_IN,
                    "DTID: 0x" + Long.toHexString(info.getDtid()));
            TcapDialogId tdid = TcapDialogId.extractFromTCAPMessageInfo(info, true);
            SlElMappingData data = slElRouter.getMappingData(sdid, tdid);
            if (data == null) {
                logger.debug("EL router query {}: no entry for {} / {}, returning not found response to {}.", queryId,
                        sdid, tdid, queryClient);
                queryResponse = SlLwcommFormat.formatELRouterNotfoundResponse(sccpData);
                callHistoryStore.registerEvent(queryIdParsed, Event.EL_ROUTER_QUERY_NOTFOUND_OUT);
            } else {
                String imscfCallId = data.getImscfCallId().toString();
                MDCParameters.toMDC(Parameter.IMSCF_CALLID, imscfCallId);
                String elNodeName = data.getNodeName();
                logger.debug("EL router query {}: found mapping for {} / {}, returning {}/{} to {}.", queryId, sdid,
                        tdid, imscfCallId, elNodeName, queryClient);
                queryResponse = SlLwcommFormat.formatELRouterSuccessResponse(imscfCallId, elNodeName, sccpData);
                callHistoryStore.registerEvent(queryIdParsed, Event.EL_ROUTER_QUERY_ANSWER_OUT, imscfCallId);
                // finalize original call history
                ImscfCallId imscfCallIdParsed = ImscfCallId.parse(imscfCallId);
                callHistoryStore.registerEvent(imscfCallIdParsed, Event.EL_ROUTER_QUERY_ANSWER_OUT, queryId);
                callHistoryStore.logAndRemoveCallHistory(imscfCallIdParsed);
                // delete mapping, as the other SL is taking over
                slElRouter.setMappingData(sdid, tdid, null);
            }

            lwc.send(slElRouter.getDirectRouteNameTo(queryClient),
                    TextMessage.builder(queryResponse).setGroupId(queryId).create()).addListener((future) -> {
                try {
                    SendResult r = future.get();
                    if (SendResult.Type.FAILURE == r.getType()) {
                        logger.warn("Failed to deliver EL router query response {} to {}", queryId, queryClient);
                        callHistoryStore.registerEvent(queryIdParsed, Event.LWC_OUT_ERR, "query_resp");
                    } else {
                        logger.trace("Delivered EL router query response {} to {}", queryId, queryClient);
                        callHistoryStore.registerEvent(queryIdParsed, Event.LWC_OUT_OK, "query_resp");
                    }
                    callHistoryStore.logAndRemoveCallHistory(queryIdParsed);
                } catch (CancellationException | ExecutionException | InterruptedException e) {
                    logger.error("Exception in lwcomm listener", e); // error, because this should never happen
                }
            }, null);
        } finally {
            MDCParameters.clearMDC();
        }
    }

    private void handleELRouterQueryResponse(IncomingTextMessage msg, String content, String extraHeaders,
            String sccpData) {
        try {
            String queryId = msg.getGroupId();
            ImscfCallId queryIdParsed = ImscfCallId.parse(queryId);
            MDCParameters.toMDC(Parameter.IMSCF_CALLID, queryId);
            slSccpListener.getQueryTimeoutService().cancelNamedTask(queryId);

            if (extraHeaders == null) {
                logger.error("EL router query response not understood. Missing Status header:\n{}", msg);
                return;
            }
            String[] headers = extraHeaders.split("\r\n");
            if (headers[0].equals("Status: notfound")) {
                // handle not found response

                // finalize query audit history
                callHistoryStore.registerEvent(queryIdParsed, Event.EL_ROUTER_QUERY_NOTFOUND_IN);
                callHistoryStore.logAndRemoveCallHistory(queryIdParsed);

                // check mapping: another query might have gotten a response while waiting for this if two queries were
                // sent in quick succession for the same call
                SccpDataMessage sdm = SccpSerializer.deserialize(sccpData);
                SccpDialogId sdid = SccpDialogId.extractFromSccpMessage(sdm, true); // treat as incoming from network
                TCAPMessageInfo info = TCAPMessageInfo.parse(sdm.getData());
                TcapDialogId tdid = TcapDialogId.extractFromTCAPMessageInfo(info, true);
                SlElMappingData mapping = slElRouter.getMappingData(sdid, tdid);
                if (mapping != null) {
                    ImscfCallId imscfCallId = mapping.getImscfCallId();
                    MDCParameters.toMDC(Parameter.IMSCF_CALLID, imscfCallId.toString()); // switch to the true call id
                    logger.debug(
                            "EL router query failed (query ID: {}) for {} / {}, but mapping was found in the meantime: {} belongs to EL node {}.",
                            msg.getGroupId(), sdid, tdid, imscfCallId.toHumanReadableString(), mapping.getNodeName());
                    // send message
                    String lwcommRouteName = slElRouter.getDirectRouteNameTo(mapping.getNodeName());
                    slSccpListener.sendSccpToElNode(lwcommRouteName, imscfCallId, info, sdid, tdid, sdm);
                } else {
                    logger.warn(
                            "Missing EL node mapping entry for {}/{} (query ID: {}) in other SL as well, dropping message!",
                            sdid, tdid, queryId);
                }

                return;
            }
            Matcher status = SlLwcommFormat.SUCCESS_STATUS_LINE_PATTERN.matcher(headers[0]);
            if (status.matches() && "SccpDataMessage".equals(content)) {
                // handle found response
                String imscfCallIdString = status.group("imscfCallId"), node = status.group("node");
                MDCParameters.toMDC(Parameter.IMSCF_CALLID, imscfCallIdString); // switch to the true call id
                ImscfCallId imscfCallId = ImscfCallId.parse(imscfCallIdString);
                SccpDataMessage sdm = SccpSerializer.deserialize(sccpData);
                SccpDialogId sdid = SccpDialogId.extractFromSccpMessage(sdm, true); // treat as incoming from network
                TCAPMessageInfo info = TCAPMessageInfo.parse(sdm.getData());
                TcapDialogId tdid = TcapDialogId.extractFromTCAPMessageInfo(info, true);
                logger.debug(
                        "EL router query success (query ID: {}) for {} / {}: {} belongs to EL node {}. Storing mapping data and forwarding message.",
                        msg.getGroupId(), sdid, tdid, imscfCallId.toHumanReadableString(), node);

                // finalize query audit history
                callHistoryStore.registerEvent(queryIdParsed, Event.EL_ROUTER_QUERY_ANSWER_IN, imscfCallIdString);
                callHistoryStore.logAndRemoveCallHistory(queryIdParsed);
                // update actual call history with reference to the query
                callHistoryStore.registerEvent(imscfCallId, Event.EL_ROUTER_QUERY_ANSWER_IN, queryId);

                // store EL mapping
                SlElMappingData mappingData = new SlElMappingData();
                mappingData.setImscfCallId(imscfCallId);
                mappingData.setNodeName(node);
                slElRouter.setMappingData(sdid, tdid, mappingData);
                // send message
                String lwcommRouteName = slElRouter.getDirectRouteNameTo(node);
                slSccpListener.sendSccpToElNode(lwcommRouteName, imscfCallId, info, sdid, tdid, sdm);
                return;
            }

            logger.error("EL router query response not understood. Invalid headers: {}", Arrays.toString(headers));
        } finally {
            MDCParameters.clearMDC();
        }

    }

    @Override
    public void onMessage(IncomingTextMessage msg) {
        logger.debug("Incoming LWCOMM message:\n " + msg);

        String payload = msg.getPayload();
        Matcher m = LWC_MESSAGE_PATTERN.matcher(payload);
        String target, extraHeaders, content, data;
        if (m.matches()) {
            target = m.group("target");
            extraHeaders = m.group("extraHeaders");
            content = m.group("contentType");
            data = m.group("contentData");
            switch (target) {
            case "SccpProvider":
                switch (content) { // NOPMD for too few branches
                case "SccpDataMessage":
                    handleSccpDataMessageContent(msg, data);
                    break;
                case "SccpManagementMessage":
                    handleSccpManagementMessageContent(msg, data);
                    break;
                default:
                    logger.error("Unknown content for SccpProvider: " + content);
                    break;
                }
                break;
            case "DiameterStack":
                switch (content) { // NOPMD for too few branches
                case "DiameterDataMessage":
                    handleDiameterAnswerMessageContent(msg, data);
                    break;
                default:
                    logger.error("Unknown content for DiameterStack: " + content);
                    break;
                }
                break;
            case "ELRouter/query":
                if ("SccpDataMessage".equals(content)) {
                    handleELRouterQuery(msg, data);
                } else {
                    logger.error("Unknown content type {} received in EL router query", content);
                }
                break;
            case "ELRouter/response":
                if ("SccpDataMessage".equals(content)) {
                    handleELRouterQueryResponse(msg, content, extraHeaders, data);
                } else {
                    logger.error("Unknown content type {} received in EL router response", content);
                }
                break;
            default:
                logger.error("Unknown target: " + target);
                break;
            }
        } else {
            logger.error("Message from EL not understood.");
        }

    }
}
