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
package org.restcomm.imscf.sl.stack;

import org.restcomm.imscf.sl.history.Event;
import org.restcomm.imscf.sl.history.SlCallHistoryStore;
import org.restcomm.imscf.sl.log.MDCParameters;
import org.restcomm.imscf.sl.log.MDCParameters.Parameter;
import org.restcomm.imscf.sl.overload.SlOverloadUtil;
import org.restcomm.imscf.sl.statistics.SlStatistics;
import org.restcomm.imscf.common.LwcTags;
import org.restcomm.imscf.common.SLELRouter;
import org.restcomm.imscf.common.SccpDialogId;
import org.restcomm.imscf.common.SccpSerializer;
import org.restcomm.imscf.common.TcapDialogId;
import org.restcomm.imscf.common.util.TCAPMessageInfo;
import org.restcomm.imscf.common.util.TCAPMessageInfo.MessageType;
import org.restcomm.imscf.common.util.ImscfCallId;
import org.restcomm.imscf.common.util.overload.OverloadProtector;
import org.restcomm.imscf.common.lwcomm.service.LwCommService;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.SendResult.Type;
import org.restcomm.imscf.common.lwcomm.service.SendResultFuture;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;
import org.restcomm.imscf.common.lwcomm.service.impl.NamingThreadFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mobicents.protocols.ss7.sccp.RemoteSccpStatus;
import org.mobicents.protocols.ss7.sccp.SccpListener;
import org.mobicents.protocols.ss7.sccp.SignallingPointStatus;
import org.mobicents.protocols.ss7.sccp.message.SccpDataMessage;
import org.mobicents.protocols.ss7.sccp.message.SccpMessage;
import org.mobicents.protocols.ss7.sccp.message.SccpNoticeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SL layer on top of SCCP for sending/receiving SCCP messages over LwComm.
 */
public class SLSccpListener implements SccpListener {

    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(SLSccpListener.class);

    private transient LwCommService lwc;
    private transient SLELRouter<SlElMappingData> elRouter;
    private transient SlCallHistoryStore callHistoryStore;

    private final String otherSlNode;
    private final int queryTimeout;
    private final transient NamedScheduledExecutorService queryTimeoutService;

    public SLSccpListener(SLELRouter<SlElMappingData> elRouterBean, SlCallHistoryStore callHistoryStore,
            LwCommService lwc, String otherSlNode) {
        this.elRouter = Objects.requireNonNull(elRouterBean);
        this.callHistoryStore = Objects.requireNonNull(callHistoryStore);
        this.lwc = Objects.requireNonNull(lwc);
        this.otherSlNode = otherSlNode;

        if (this.otherSlNode != null) {
            // the other SL's last retransmit can occur at timeout#(last-1) ms
            // we add the first retransmit delay to account for the remote side's processing time, gc pause, etc.
            List<Integer> pattern = lwc.getConfiguration().getAllRoutes().iterator().next().getRetransmitPattern();
            this.queryTimeout = pattern.get(Math.max(0, pattern.size() - 2)) * 2 + pattern.get(0);
            // don't keep any threads if not in use
            // TODO do we need a fixed number of threads? this is only for logging and cleaning call history for failed
            // queries
            this.queryTimeoutService = new NamedScheduledExecutorService(Executors.newScheduledThreadPool(0,
                    new NamingThreadFactory("ElRouteQueryService")));
        } else {
            this.queryTimeout = -1;
            this.queryTimeoutService = null;
        }
    }

    public NamedScheduledExecutorService getQueryTimeoutService() {
        return queryTimeoutService;
    }

    @Override
    public void onCoordRequest(int arg0, int arg1, int arg2) {
        // TODO
        logger.debug("onCoordRequest {} {} {}", arg0, arg1, arg2);
    }

    @Override
    public void onCoordResponse(int arg0, int arg1, int arg2) {
        // TODO
        logger.debug("onCoordResponse {} {} {}", arg0, arg1, arg2);
    }

    @Override
    public void onMessage(SccpDataMessage msg) {
        logger.debug("SL onMessage SccpDataMessage {}", msg);
        switch (msg.getType()) {
        case SccpMessage.MESSAGE_TYPE_UDTS:
        case SccpMessage.MESSAGE_TYPE_XUDTS:
        case SccpMessage.MESSAGE_TYPE_LUDTS:
        case SccpMessage.MESSAGE_TYPE_UNDEFINED:
            logger.warn("SccpMessage type is (X/L/U)DTS or UNDEFINED: {}", msg.getType());
            return;
        default:
            break;
        }
        SlStatistics.incMessagesReceivedFromPc(msg.getIncomingOpc());
        TCAPMessageInfo info = TCAPMessageInfo.parse(msg.getData());
        logger.debug("TCAP info: " + info.toString());
        SccpDialogId sdid = SccpDialogId.extractFromSccpMessage(msg, true);
        TcapDialogId tdid = TcapDialogId.extractFromTCAPMessageInfo(info, true);
        String lwcommRouteName;
        ImscfCallId callid;

        switch (info.getMessageType()) {
        case TC_BEGIN:
            // If the system is overloaded, send back TCAP_ABORT immediately
            if (OverloadProtector.getInstance().getCurrentState().isCpuOrHeapOverloaded()) {
                logger.debug("System is overloaded. Sending back TCAP ABORT.");
                SlOverloadUtil.rejectBeginFromNetworkWithPAbort(msg, tdid.getRemoteTcapTID());
                return;
            }
            // choose an arbitrary route to a new EL
            // we don't need to save the route yet, as no message can arrive
            // from the originator before EL sends a TU answer accepting the
            // dialogue
            lwcommRouteName = elRouter.getRouteToAnyNode().getName();
            callid = ImscfCallId.generate();
            logger.debug("New call with id {}. Sending new transaction to any EL node on route: {}",
                    callid.toHumanReadableString(), lwcommRouteName);
            break;
        case TC_ABORT:
        case TC_CONTINUE:
        case TC_END:
            // choose dedicated route to existing EL based on dialog id
            // dialog id was saved when EL sent back the first TC_CONTINUE
            // accepting the dialogue started in TC_BEGIN (see LwcommMessageReceiver)
            // or when the EL started the dialog with a TC_BEGIN (e.g. ICA)
            SlElMappingData data = elRouter.getMappingData(sdid, tdid);
            if (data == null) {
                if (otherSlNode == null) {
                    logger.debug("Missing EL node mapping entry for {} / {} and no other SL to query!", sdid, tdid);
                    return;
                }

                logger.debug("Missing EL node mapping entry for {} / {}, querying other SL.", sdid, tdid);
                startELRouterQuery(info, msg);
                return;
            }
            callid = data.getImscfCallId();
            String elNodeName = data.getNodeName();
            lwcommRouteName = elRouter.getDirectRouteNameTo(elNodeName);
            logger.debug("Sending continuation to EL node {} on route: {}", elNodeName, lwcommRouteName);
            break;
        default:
            return;
        }

        try {
            MDCParameters.toMDC(Parameter.IMSCF_CALLID, callid.toString());
            if (info.getMessageType() == MessageType.TC_BEGIN) {
                callHistoryStore.registerEvent(callid, Event.fromTcap(info, true),
                        "OTID: 0x" + Long.toHexString(info.getOtid()));
            } else {
                callHistoryStore.registerEvent(callid, Event.fromTcap(info, true));
            }
            sendSccpToElNode(lwcommRouteName, callid, info, sdid, tdid, msg);
        } finally {
            MDCParameters.clearMDC();
        }
    }

    private void startELRouterQuery(TCAPMessageInfo info, SccpDataMessage msg) {
        ImscfCallId queryId = ImscfCallId.generate();
        callHistoryStore.registerEvent(queryId, Event.fromTcap(info, true),
                "DTID: 0x" + Long.toHexString(info.getDtid()));
        callHistoryStore.registerEvent(queryId, Event.EL_ROUTER_QUERY_OUT);
        logger.debug("Sending EL router query with id {}", queryId.toString());

        // schedule response timeout beforehand, as response may arrive before we process the SendResult. Processing the
        // response cancels this timer. This also means it has to be cancelled on send failure.
        // the timeout value now has to take into account the LwComm retransmit times in both directions
        queryTimeoutService.scheduleNamedTask(queryId.toString(), () -> {
            callHistoryStore.registerEvent(queryId, Event.EL_ROUTER_QUERY_TIMEOUT);
            callHistoryStore.logAndRemoveCallHistory(queryId);
            logger.warn("Response timeout for EL router query {} after {}ms, dropped message!", queryId, queryTimeout);
        }, queryTimeout, TimeUnit.MILLISECONDS);

        SendResultFuture<SendResult> f = lwc.send(elRouter.getDirectRouteNameTo(otherSlNode),
                TextMessage.builder(SlLwcommFormat.formatELRouterQuery(msg)).setGroupId(queryId.toString()).create());
        f.addListener((future) -> {
            try {
                SendResult r = future.get();
                if (SendResult.Type.FAILURE == r.getType()) {
                    queryTimeoutService.cancelNamedTask(queryId.toString());
                    logger.warn("Failed to deliver EL router query {} to {}", queryId, otherSlNode);
                    callHistoryStore.registerEvent(queryId, Event.LWC_OUT_ERR, "query");
                    callHistoryStore.logAndRemoveCallHistory(queryId);
                } else {
                    logger.trace("Delivered EL router query {} to {}, waiting for response.", queryId, otherSlNode);
                    /* LWC_OUT_OK is not registered, as it could result in a call history induced memory leak */
                }
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                // error, because this should never happen
                logger.error("Exception in lwcomm listener", e);
            }
        }, null);
    }

    public void sendSccpToElNode(String lwcommRouteName, ImscfCallId callid, TCAPMessageInfo info, SccpDialogId sdid,
            TcapDialogId tdid, SccpDataMessage msg) {

        String sccpAsString = SccpSerializer.serialize(msg);

        String lwcommPayload = "" + //
                "Target: SUA\r\n" + //
                "Content: SccpDataMessage\r\n" + //
                "\r\n" + //
                sccpAsString;
        String lwcTag = info.getMessageType() == MessageType.TC_BEGIN ? LwcTags.NEW_SESSION : LwcTags.IN_SESSION;
        SendResultFuture<SendResult> sendresult = lwc.send(lwcommRouteName, TextMessage.builder(lwcommPayload)
                .setGroupId(callid.toString()).setTag(lwcTag).create());
        try {
            SendResult sr = sendresult.get();
            logger.debug("result: {}", sr);
            if (sr.getType() == Type.SUCCESS) {
                callHistoryStore.registerEvent(callid, Event.LWC_OUT_OK, info.getMessageType().toString(),
                        sendresult.getMessageId());
                String name = sr.getActualDestination().getName();
                logger.debug("Message sent to EL node {}", name);
                SlElMappingData data = new SlElMappingData();
                data.setNodeName(name);
                data.setImscfCallId(callid);
                if (info.getMessageType() == MessageType.TC_CONTINUE) {
                    elRouter.setMappingData(sdid, tdid, data);
                }
            } else {
                callHistoryStore.registerEvent(callid, Event.LWC_OUT_ERR, info.getMessageType().toString(),
                        sendresult.getMessageId());
                if (info.getMessageType() == MessageType.TC_BEGIN || info.getMessageType() == MessageType.TC_CONTINUE) {
                    callHistoryStore.logAndRemoveCallHistory(callid);
                }
                logger.warn("Failed to send message to EL node. {}", msg);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error sending message", e);
        }

        switch (info.getMessageType()) {
        case TC_END:
        case TC_ABORT:
            logger.debug("dialog end for {} / {} due to {}, removing EL mapping", sdid, tdid, info.getMessageType());
            elRouter.setMappingData(sdid, tdid, null);
            callHistoryStore.logAndRemoveCallHistory(callid);
            break;
        default:
            break;
        }
    }

    @Override
    public void onNotice(SccpNoticeMessage arg0) {
        // TODO Auto-generated method stub
        logger.debug("onNotice {}", arg0);
    }

    @Override
    public void onPcState(int arg0, SignallingPointStatus arg1, int arg2, RemoteSccpStatus arg3) {
        // TODO Auto-generated method stub
        logger.debug("onPcState {} {} {} {}", arg0, arg1, arg2, arg3);
    }

    @Override
    public void onState(int arg0, int arg1, boolean arg2, int arg3) {
        // TODO Auto-generated method stub
        logger.debug("onState {} {} {} {}", arg0, arg1, arg2, arg3);
    }

}
