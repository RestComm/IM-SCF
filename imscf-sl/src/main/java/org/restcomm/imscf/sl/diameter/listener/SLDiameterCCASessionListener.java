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
package org.restcomm.imscf.sl.diameter.listener;

import org.restcomm.imscf.sl.diameter.DiameterDialogId;
import org.restcomm.imscf.sl.diameter.DiameterGW;
import org.restcomm.imscf.sl.diameter.SLELDiameterRouter;
import org.restcomm.imscf.sl.diameter.creditcontrol.DiameterGWCCASessionData;
import org.restcomm.imscf.sl.diameter.util.DiameterGWUtil;
import org.restcomm.imscf.sl.history.Event;
import org.restcomm.imscf.sl.history.SlCallHistoryStore;
import org.restcomm.imscf.sl.log.MDCParameters;
import org.restcomm.imscf.sl.stack.SlElMappingData;
import org.restcomm.imscf.common.DiameterSerializer;
import org.restcomm.imscf.common.LwcTags;
import org.restcomm.imscf.common.diameter.creditcontrol.CCAResultCode;
import org.restcomm.imscf.common.diameter.creditcontrol.CCRequestType;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterGWCreditControlRequest;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterGWCreditControlResponse;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterSLELCreditControlRequest;
import org.restcomm.imscf.common.util.ImscfCallId;
import org.restcomm.imscf.common.util.overload.OverloadProtector;
import org.restcomm.imscf.common.lwcomm.service.ListenableFuture;
import org.restcomm.imscf.common.lwcomm.service.LwCommService;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.SendResult.Type;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.auth.events.ReAuthAnswer;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.ServerCCASessionListener;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.client.api.IAnswer;
import org.jdiameter.common.impl.app.cca.JCreditControlAnswerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Interface for DiameterGW module listening to jdiameter stack events.
 */
@SuppressWarnings("PMD.GodClass")
public class SLDiameterCCASessionListener implements ServerCCASessionListener {

    private static Logger logger = LoggerFactory.getLogger(SLDiameterCCASessionListener.class);

    private List<String> serviceContextIds;
    private int ccaSessionTimeout;

    private transient LwCommService lwc;
    private transient SlCallHistoryStore callHistoryStore;
    private transient SLELDiameterRouter<SlElMappingData> elRouter;

    public SLDiameterCCASessionListener(List<String> serviceContextIds, int ccaSessionTimeout,
            SLELDiameterRouter<SlElMappingData> elRouterBean, LwCommService lwc, SlCallHistoryStore callHistoryStore) {
        super();
        this.serviceContextIds = new ArrayList<String>();
        this.serviceContextIds.addAll(serviceContextIds);
        this.ccaSessionTimeout = ccaSessionTimeout;
        this.elRouter = elRouterBean;
        this.lwc = lwc;
        this.callHistoryStore = callHistoryStore;
    }

    @Override
    public void doCreditControlRequest(ServerCCASession session, JCreditControlRequest request)
            throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
        logger.debug("Diameter credit control request handling. SessionId: {}", session.getSessionId());

        DiameterGWCCASessionData diameterGWCCASessionData = DiameterGWUtil.getCCASessionData(session, request);

        DiameterGWCreditControlRequest diameterGWCreditControlRequest = DiameterGWUtil
                .createCreditControlRequestForINService(request);
        logger.debug("Created diameter credit control request object for the IN service: {}",
                diameterGWCreditControlRequest.toString());

        if (DiameterGW.getDataForCCASessionId().get(diameterGWCCASessionData.getSessionId()) != null
                && diameterGWCCASessionData.getCcRequestType() == CCRequestType.BALANCE) {
            // session id has already used (this situation never happened, because jdiameter managed it)
            logger.debug(
                    "Diameter credit control request handling. SessionId ({}) is already in use. Sending error message.",
                    diameterGWCCASessionData.getSessionId());

            try {
                sendError(diameterGWCreditControlRequest, diameterGWCCASessionData.getSessionId(),
                        "SessionId is already in use.");
            } catch (Exception e) {
                logger.warn("Diameter credit control request handling. Error while sending response message. {}",
                        e.getMessage(), e);
            }

        } else {
            try {
                if (diameterGWCCASessionData.getCcRequestType() == CCRequestType.BALANCE) {
                    // Sotore session data
                    cleanupOldSessions();
                    DiameterGW.getDataForCCASessionId().put(diameterGWCCASessionData.getSessionId(),
                            diameterGWCCASessionData);
                } else {
                    if (DiameterGW.getDataForCCASessionId().get(diameterGWCCASessionData.getSessionId()) == null) {
                        // No session for this debit
                        cleanupOldSessions();
                        DiameterGW.getDataForCCASessionId().put(diameterGWCCASessionData.getSessionId(),
                                diameterGWCCASessionData);
                    } else {
                        // Update session
                        DiameterGWCCASessionData oldDiameterGWCCASessionData = DiameterGW.getDataForCCASessionId().get(
                                diameterGWCCASessionData.getSessionId());
                        oldDiameterGWCCASessionData.updateSession(diameterGWCCASessionData);
                    }
                }
                String serviceContextId = DiameterGWUtil.getServiceContextIdFromCCR(request);
                processCCRequest(session.getSessionId(), diameterGWCreditControlRequest, serviceContextId,
                        serviceContextIds, elRouter, lwc, callHistoryStore);
            } catch (Exception e) {
                logger.warn(
                        "Diameter credit control request handling. Error while processing incoming diameter message. {}",
                        e.getMessage(), e);
            }
        }
    }

    @Override
    public void doOtherEvent(AppSession arg0, AppRequestEvent arg1, AppAnswerEvent arg2) throws InternalException,
            IllegalDiameterStateException, RouteException, OverloadException {
        logger.debug("DiameterGW server CCA session listener doOtherEvent Event!");
    }

    @Override
    public void doReAuthAnswer(ServerCCASession arg0, ReAuthRequest arg1, ReAuthAnswer arg2) throws InternalException,
            IllegalDiameterStateException, RouteException, OverloadException {
        logger.debug("DiameterGW server CCA session listener doReAuthAnswer Event!");
    }

    public static void processCCRequest(String sessionId,
            DiameterGWCreditControlRequest diameterGWCreditControlRequest, String serviceContextId,
            List<String> serviceContextIds, SLELDiameterRouter<SlElMappingData> elRouter, LwCommService lwc,
            SlCallHistoryStore callHistoryStore) {
        logger.debug("Processing diameter credit control request. Request: {}",
                diameterGWCreditControlRequest.toString());
        // Check if system is overloaded
        if (OverloadProtector.getInstance().getCurrentState().isCpuOrHeapOverloaded()) {
            logger.debug("System is overloaded. Send back diameter error.");
            try {
                sendError(diameterGWCreditControlRequest, sessionId, "IMSCF Signaling Layer Server is overloaded.");
            } catch (Exception e) {
                logger.warn("Error sending overload protection Diameter error.", e);
            }
        }
        try {

            String lwcommRouteName = null;
            String lwcTag = null;
            ImscfCallId callid = null;
            DiameterDialogId diamId = new DiameterDialogId(sessionId);

            CCRequestType typeOfCCRequest = CCRequestType.getCCRequestTypeByString(diameterGWCreditControlRequest
                    .getRequestType());

            if (typeOfCCRequest != null && typeOfCCRequest.equals(CCRequestType.BALANCE)) {
                // Choose an arbitrary route to a new EL
                lwcommRouteName = elRouter.getRouteToAnyNode().getName();
                lwcTag = LwcTags.NEW_SESSION;
                callid = ImscfCallId.generate();
                logger.debug("New call with id {}. Sending new transaction to any EL node on route: {}",
                        callid.toHumanReadableString(), lwcommRouteName);
            } else if (typeOfCCRequest != null && typeOfCCRequest.equals(CCRequestType.DEBIT)) {
                // Choose dedicated route to existing EL based on dialog id
                SlElMappingData data = elRouter.getMappingData(diamId);
                if (data == null) {
                    DiameterGWCreditControlResponse diameterGWCreditControlResponse = new DiameterGWCreditControlResponse();
                    if (serviceContextId != null && serviceContextIds.contains(serviceContextId)
                            && DiameterGW.getDataForCCASessionId().get(sessionId).wasTechnicalError()) {
                        // If we got a Debit request which belongs to an unsuccesful Balance request and contain an
                        // accpeted service id, we will have to send back a TECHNICAL_ERROR message.
                        // In this case the valuse of mapping data is null because balance was unsuccesful.
                        // Mapping data is writing only after a succes balance request.
                        diameterGWCreditControlResponse.setQueryResult(CCAResultCode.TECHNICAL_ERROR);
                        diameterGWCreditControlResponse
                                .setErrorCode("There was a technical error earlier in the session.");
                    } else {
                        // If we got a Debit request which is not belongs to any mapping data, we will to send back a
                        // TECHNICAL_ERROR message.
                        diameterGWCreditControlResponse.setQueryResult(CCAResultCode.TECHNICAL_ERROR);
                        diameterGWCreditControlResponse.setErrorCode("Invalid session id: " + sessionId
                                + ". There was not balance query before this debit request!");
                    }
                    diameterGWCreditControlResponse.setResponseCaller(diameterGWCreditControlRequest.getCaller());
                    diameterGWCreditControlResponse.setResponseCallee(diameterGWCreditControlRequest.getCallee());
                    diameterGWCreditControlResponse.setResponseSmscAddress(diameterGWCreditControlRequest
                            .getSmscAddress());

                    SLDiameterCCASessionListener.processCCResponse(sessionId, diameterGWCreditControlResponse, true,
                            true);

                    logger.warn("Missing EL node mapping entry for {} / {}, DEBIT message!", sessionId, sessionId);
                    return;
                } else {
                    callid = data.getImscfCallId();
                    String elNodeName = data.getNodeName();
                    lwcommRouteName = elRouter.getDirectRouteNameTo(elNodeName);
                    lwcTag = LwcTags.IN_SESSION;
                    logger.debug("Sending continuation to EL node {} on route: {}", elNodeName, lwcommRouteName);
                }
            } else {
                logger.warn("Call controll request type is not suitable.");
                return;
            }

            // Check service context id
            if (serviceContextId == null || !(serviceContextIds.contains(serviceContextId))) {
                // If the service context id is not suitable, we have to send a DIAMETER_END_USER_SERVICE_DENIED message
                // with content: Invalid service context id!
                logger.warn(
                        "Invalid service context id, sending back END_USER_SERVICE_DENIED message. SessionId: {}, ServiceContextId: {}",
                        sessionId, serviceContextId);

                callHistoryStore.registerEvent(callid,
                        Event.fromDiameter(diameterGWCreditControlRequest.getRequestTypeObject(), null, true));

                callHistoryStore.registerEvent(callid, Event.fromDiameter(
                        diameterGWCreditControlRequest.getRequestTypeObject(), CCAResultCode.END_USER_SERVICE_DENIED,
                        false), "Invalid service context id!");

                DiameterGWCreditControlResponse diameterGWCreditControlResponse = new DiameterGWCreditControlResponse();
                diameterGWCreditControlResponse.setQueryResult(CCAResultCode.END_USER_SERVICE_DENIED);
                diameterGWCreditControlResponse.setErrorCode("Invalid service context id!");
                diameterGWCreditControlResponse.setResponseCaller(diameterGWCreditControlRequest.getCaller());
                diameterGWCreditControlResponse.setResponseCallee(diameterGWCreditControlRequest.getCallee());
                diameterGWCreditControlResponse.setResponseSmscAddress(diameterGWCreditControlRequest.getSmscAddress());

                SLDiameterCCASessionListener.processCCResponse(sessionId, diameterGWCreditControlResponse, true, true);

                // Mapping data only exist after a succes balance request, so that's why if the actual request is a
                // debit we have to delete mapping information
                if (typeOfCCRequest.equals(CCRequestType.DEBIT)) {
                    elRouter.setMappingData(diamId, null);
                }

                callHistoryStore.logAndRemoveCallHistory(callid);
            } else {
                try {
                    MDCParameters.toMDC(MDCParameters.Parameter.IMSCF_CALLID, callid.toString());

                    callHistoryStore.registerEvent(callid,
                            Event.fromDiameter(diameterGWCreditControlRequest.getRequestTypeObject(), null, true));

                    DiameterSLELCreditControlRequest diamReq = new DiameterSLELCreditControlRequest();
                    diamReq.setCaller(diameterGWCreditControlRequest.getCaller());
                    diamReq.setCallee(diameterGWCreditControlRequest.getCallee());
                    diamReq.setRequestType(diameterGWCreditControlRequest.getRequestType());
                    diamReq.setCallerImsi(diameterGWCreditControlRequest.getCallerImsi());
                    diamReq.setVlrGt(diameterGWCreditControlRequest.getVlrGt());
                    diamReq.setSmscAddress(diameterGWCreditControlRequest.getSmscAddress());
                    diamReq.setSmsSubmissionResult(diameterGWCreditControlRequest.getSmsSubmissionResult());
                    diamReq.setSessionId(sessionId);
                    diamReq.setServiceContextId(serviceContextId);
                    String diameterAsString = DiameterSerializer.serialize(diamReq);
                    String lwcommPayload = "" + //
                            "Target: DiameterGW\r\n" + //
                            "Content: DiameterDataMessage\r\n" + //
                            "\r\n" + //
                            diameterAsString;
                    ListenableFuture<SendResult> sendresult = lwc.send(lwcommRouteName,
                            TextMessage.builder(lwcommPayload).setGroupId(callid.toString()).setTag(lwcTag).create());

                    try {
                        SendResult sr = sendresult.get();
                        logger.debug("result: " + sr);
                        if (sr.getType() == Type.SUCCESS) {
                            callHistoryStore.registerEvent(callid, Event.LWC_OUT_OK);
                            String name = sr.getActualDestination().getName();
                            logger.debug("Message sent to EL node " + name);
                            SlElMappingData data = new SlElMappingData();
                            data.setNodeName(name);
                            data.setImscfCallId(callid);
                            if (typeOfCCRequest.equals(CCRequestType.BALANCE)) {
                                elRouter.setMappingData(diamId, data);
                            }
                        } else {
                            handleLwcProcessError(diameterGWCreditControlRequest, sessionId, elRouter,
                                    callHistoryStore, callid);

                            callHistoryStore.logAndRemoveCallHistory(callid);
                            logger.error("Failed to send message to EL node. " + diameterAsString);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        handleLwcProcessError(diameterGWCreditControlRequest, sessionId, elRouter, callHistoryStore,
                                callid);

                        callHistoryStore.logAndRemoveCallHistory(callid);
                        logger.error("Error sending message", e);
                    }

                } finally {
                    MDCParameters.clearMDC();
                }
            }
        } catch (Exception e) {
            logger.error(
                    "Processing diameter credit control request. Error while processing incoming diameter message. {}",
                    e.getMessage(), e);
        }
    }

    public static void processCCResponse(String sessionId, DiameterGWCreditControlResponse response,
            boolean wasDiameterError, boolean removeSession) throws Exception {
        logger.debug("Processing diameter credit control response: " + response.toString());
        DiameterGWCCASessionData diameterGWCCASessionData = DiameterGW.getDataForCCASessionId().get(sessionId);
        ServerCCASession session = diameterGWCCASessionData.getSession();
        IAnswer answer = DiameterGWUtil.createCreditControlAnswerMessage(response, sessionId, diameterGWCCASessionData);

        session.sendCreditControlAnswer(new JCreditControlAnswerImpl(answer));
        logger.debug("Diameter credit control response sent.");

        if (removeSession) {
            removeCCSession(sessionId);
        }
    }

    public static void handleLwcProcessError(DiameterGWCreditControlRequest request, String sessionId,
            SLELDiameterRouter<SlElMappingData> elRouter, SlCallHistoryStore callHistoryStore, ImscfCallId callid) {
        DiameterDialogId diamId = new DiameterDialogId(sessionId);
        try {
            CCRequestType typeOfCCRequest = CCRequestType.getCCRequestTypeByString(request.getRequestType());
            if (typeOfCCRequest != null && typeOfCCRequest.equals(CCRequestType.BALANCE)) {
                // Mapping data only exist after a succes balance request, so that's why we don't have to delete it.
                DiameterGW.getDataForCCASessionId().get(sessionId).setTechnicalError(true);
                sendError(request, sessionId, "Failed to send message to EL node.");
            }

            if (typeOfCCRequest != null && typeOfCCRequest.equals(CCRequestType.DEBIT)) {
                sendError(request, sessionId, "Failed to send message to EL node.");
                removeCCSession(sessionId);
                elRouter.setMappingData(diamId, null);
            }
            callHistoryStore.registerEvent(callid, Event.LWC_OUT_ERR);

        } catch (Exception e) {
            logger.error("Error while sending diameter response message: {}", e.getMessage(), e);
        }
    }

    public static void sendError(DiameterGWCreditControlRequest request, String sessionId, String errorMsg)
            throws Exception {
        logger.debug("Diameter error message handling: " + errorMsg);
        DiameterGWCreditControlResponse diameterGWCreditControlResponse = new DiameterGWCreditControlResponse();
        diameterGWCreditControlResponse.setQueryResult(CCAResultCode.TECHNICAL_ERROR);
        diameterGWCreditControlResponse.setErrorCode(errorMsg);
        diameterGWCreditControlResponse.setResponseCaller(request.getCaller());
        diameterGWCreditControlResponse.setResponseCallee(request.getCallee());
        diameterGWCreditControlResponse.setResponseSmscAddress(request.getSmscAddress());

        SLDiameterCCASessionListener.processCCResponse(sessionId, diameterGWCreditControlResponse, false, false);
    }

    public static void removeCCSession(String sessionId) {
        logger.debug("Remove diameter session. Session id: " + sessionId);
        try {
            DiameterGWCCASessionData diameterGWCCASessionData = DiameterGW.getDataForCCASessionId().get(sessionId);
            if (diameterGWCCASessionData != null) {
                diameterGWCCASessionData.getSession().release();
                DiameterGW.getDataForCCASessionId().remove(sessionId);
            }
        } catch (Exception e) {
            logger.warn("Error while removing the diameter session with id {" + sessionId + "}: {}", e.getMessage(), e);
        }
    }

    private void cleanupOldSessions() throws Exception {
        // Delete old sessions and related mapping information
        for (Entry<String, DiameterGWCCASessionData> entry : DiameterGW.getDataForCCASessionId().entrySet()) {
            if ((System.currentTimeMillis() - entry.getValue().getStartTime()) > (ccaSessionTimeout * 1000L)) {
                DiameterGWCCASessionData diameterGWCCASessionData = DiameterGW.getDataForCCASessionId().remove(
                        entry.getKey());
                if (diameterGWCCASessionData != null) {
                    try {
                        diameterGWCCASessionData.getSession().release();
                    } catch (Exception e) {
                        logger.warn(
                                "Error while releasing the diameter session with id {" + entry.getValue() + "}: {}",
                                e.getMessage(), e);
                    }

                    logger.debug("Diameter old session removed with sessionId: " + entry.getKey());
                    // Delete mapping and call history informaiton too
                    DiameterDialogId diamId = new DiameterDialogId(entry.getKey());
                    SlElMappingData data = elRouter.setMappingData(diamId, null);
                    if (data != null && data.getImscfCallId() != null) {
                        callHistoryStore.logAndRemoveCallHistory(data.getImscfCallId());
                    } else {
                        logger.warn("Try to remove call history whithout imscf call id. Diameter sessionId: {}",
                                entry.getKey());
                    }
                }
            }
        }
    }
}
