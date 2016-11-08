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
package org.restcomm.imscf.el.diameter;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.DiameterGatewayModuleType;
import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.ImscfCallLifeCycleState;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.call.history.ElEventCreator;
import org.restcomm.imscf.el.diameter.call.DiameterHttpCall;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterAsRoutes;
import org.restcomm.imscf.common.diameter.creditcontrol.ServiceEndpoint;
import org.restcomm.imscf.el.modules.ModuleInitializationException;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.el.statistics.ElStatistics;
import org.restcomm.imscf.common.DiameterSerializer;
import org.restcomm.imscf.common.LwcTags;
import org.restcomm.imscf.common.diameter.creditcontrol.CCAResultCode;
import org.restcomm.imscf.common.diameter.creditcontrol.CCRequestType;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterGWCCResponseJsonWrapper;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterSLELCreditControlRequest;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterSLELCreditControlResponse;
import org.restcomm.imscf.common.util.overload.OverloadProtector;
import org.restcomm.imscf.common.lwcomm.service.LwCommServiceProvider;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.SendResult.Type;
import org.restcomm.imscf.common.lwcomm.service.SendResultFuture;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Base class for MAP module implementations.
 */
@SuppressWarnings("PMD.GodClass")
public class DiameterGWModuleBase implements DiameterModule {

    private static final Logger LOG = LoggerFactory.getLogger(DiameterGWModuleBase.class);

    private String name;
    private ImscfConfigType imscfConfiguration;
    private DiameterGatewayModuleType moduleConfiguration;

    private Random random = new Random();

    /**
     * Enum for storing distribution strategies.
     */
    private enum DistributionStrategy {
        LOADBALANCE, PRIMARY_SECONDARY
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setImscfConfiguration(ImscfConfigType configuration) {
        this.imscfConfiguration = configuration;
    }

    public ImscfConfigType getImscfConfig() {
        return imscfConfiguration;
    }

    @Override
    public void setModuleConfiguration(DiameterGatewayModuleType configuration) {
        moduleConfiguration = configuration;
    }

    @Override
    public DiameterGatewayModuleType getModuleConfiguration() {
        return moduleConfiguration;
    }

    @Override
    public void initialize(DiameterGatewayModuleType configuration) throws ModuleInitializationException {
        LOG.debug("DiameterGW module {} initializing...", getName());
        DiameterModule.super.initialize(configuration);
        this.moduleConfiguration = configuration;
        LOG.debug("DiameterGW module {} initialized.", getName());
    }

    @Override
    public void doCreditControlRequest(DiameterSLELCreditControlRequest request, String imscfCallId,
            String lwcommRouteName, String appServerGroupName) {
        LOG.debug("Diameter incoming credit control message processing.");

        // Check if system is overloaded
        if (OverloadProtector.getInstance().getCurrentState().isCpuOrHeapOverloaded()) {
            LOG.debug("System is overloaded. Sending back DIAMETER error.");
            sendErrorMessage("IMSCF Execution Layer server is overloaded", request, imscfCallId, lwcommRouteName);
            // Delete the call
            CallStore cs = CallContext.getCallStore();
            CallFactoryBean cf = CallContext.getCallFactory();
            try (DiameterHttpCall dcall = cs.getHttpCallByDiameterSessionId(request.getSessionId())) {
                if (dcall != null) {
                    LOG.debug("Deleting call {}", dcall);
                    cf.deleteCall(dcall);
                }
            }
            return;
        }

        if (request.getRequestTypeObject() == CCRequestType.BALANCE) {
            handleBalanceQuery(request, imscfCallId, lwcommRouteName, appServerGroupName);
        } else if (request.getRequestTypeObject() == CCRequestType.DEBIT) {
            handleDebitQuery(request, imscfCallId, lwcommRouteName, appServerGroupName);
        }
    }

    private int getStartPoint(List<ServiceEndpoint> endpoints, DistributionStrategy distribution) {
        if (endpoints.isEmpty()) {
            return -1;
        }
        if (distribution == DistributionStrategy.LOADBALANCE)
            return random.nextInt(endpoints.size());
        else
            return 0;
    }

    private int getNextEndpoint(List<ServiceEndpoint> endpoints, int currentEndpointIndex, boolean includeBanned,
            int startPoint) {
        int cEIndex = currentEndpointIndex;

        if (endpoints.isEmpty() || startPoint < 0)
            return -1;
        int ret = startPoint;
        if (ret >= endpoints.size())
            ret = 0;
        boolean quit = false;
        do {
            ServiceEndpoint se = endpoints.get(ret);
            if (includeBanned || !se.isBanned())
                break;
            ret++;
            if (ret == endpoints.size()) {
                ret = 0;
            }
            if (cEIndex == -1) {
                cEIndex = startPoint;
            }
            if (ret == cEIndex) {
                // Turned round no other endpoints
                ret = -1;
                break;
            }
        } while (!quit);

        return ret;
    }

    private void banEndpoint(int endpointIndex, String serverGroupName) {
        if (DiameterAsRoutes.getServerInfo().get(serverGroupName).getReenableTime() != 0) {
            DiameterAsRoutes.getServerInfo().get(serverGroupName).getServiceEndpoints().get(endpointIndex)
                    .ban(DiameterAsRoutes.getServerInfo().get(serverGroupName).getReenableTime());
        }
    }

    private void handleBalanceQuery(DiameterSLELCreditControlRequest request, String imscfCallId, String lwcommRouteName,
            String appServerGroupName) {
        LOG.debug("Diameter balance query processing started. SessionId: {}", request.getSessionId());

        int startPoint = getStartPoint(DiameterAsRoutes.getServerInfo().get(appServerGroupName).getServiceEndpoints(),
                DistributionStrategy.LOADBALANCE);
        int currentEndpointIndex = startPoint;
        boolean firstTry = true;
        boolean success = false;
        boolean deleteCall = false;
        CallStore cs = CallContext.getCallStore();
        CallFactoryBean cf = CallContext.getCallFactory();
        try (DiameterHttpCall diameterHttpCallData = cs.getHttpCallByDiameterSessionId(request.getSessionId())) {

            while (!success && (firstTry || startPoint != currentEndpointIndex)) {
                // Service endpoint selection
                int urlIndex = getNextEndpoint(DiameterAsRoutes.getServerInfo().get(appServerGroupName)
                        .getServiceEndpoints(), currentEndpointIndex, false, startPoint);
                if (urlIndex == -1) {
                    break;
                }
                startPoint = (urlIndex + 1)
                        % DiameterAsRoutes.getServerInfo().get(appServerGroupName).getServiceEndpoints().size();
                firstTry = false;
                String url = DiameterAsRoutes.getServerInfo().get(appServerGroupName).getServiceEndpoints()
                        .get(urlIndex).getUrl();
                LOG.debug("Diameter balance query processing. SessionId: {}, selected endpoint: {}",
                        request.getSessionId(), url);
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(DiameterAsRoutes.getServerInfo().get(appServerGroupName).getConnectTimeout());
                    conn.setReadTimeout(DiameterAsRoutes.getServerInfo().get(appServerGroupName).getReadTimeout());
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty("Accept-Charset", "UTF-8");
                    conn.setRequestProperty("Content-Language", "en-US");
                    conn.setUseCaches(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    Gson gson = new Gson();

                    LOG.debug("Diameter balance query processing. SessionId: {}, json wrapper for request: {}",
                            request.getSessionId(), request.getWrapper());

                    String json = gson.toJson(request.getWrapper());

                    byte[] requestJsonString = json.getBytes("UTF-8");
                    conn.setRequestProperty("Content-Length", "" + requestJsonString.length);
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

                    wr.write(requestJsonString);
                    wr.flush();
                    wr.close();

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                    LOG.debug("Diameter balance query processing. SessionId: {}, http response code: {}",
                            request.getSessionId(), conn.getResponseCode());

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        ElEventCreator.addIncomingHttpEvent(request, conn.getResponseCode());

                        // Storing the JSessionID and the selected service endpoint
                        String jSessionId = conn.getHeaderField("Set-Cookie");
                        diameterHttpCallData.setjSessionId(jSessionId);
                        diameterHttpCallData.setInInstanceIndex(urlIndex);

                        DiameterGWCCResponseJsonWrapper wrapper = gson.fromJson(br,
                                DiameterGWCCResponseJsonWrapper.class);

                        LOG.debug("Diameter balance query processing. SessionId: {}, response wrapper: {}",
                                request.getSessionId(), wrapper.toString());

                        DiameterSLELCreditControlResponse creditContorolResponse = new DiameterSLELCreditControlResponse();
                        creditContorolResponse.loadFromWrapper(wrapper);

                        creditContorolResponse.setSessionId(request.getSessionId());

                        if (creditContorolResponse.getQueryResultObject() == CCAResultCode.END_USER_SERVICE_DENIED) {
                            deleteCall = true;
                            creditContorolResponse.setRemoveSession(true);
                        }

                        success = true;

                        sendMessageToSL(creditContorolResponse, request, imscfCallId, lwcommRouteName);

                    } else if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                        ElEventCreator.addIncomingHttpEvent(request, conn.getResponseCode());

                        LOG.warn(
                                "Diameter balance query processing. Http response: 404 not found. Banning endpoint: {}",
                                url);
                        banEndpoint(urlIndex, appServerGroupName);
                    } else {
                        ElEventCreator.addIncomingHttpEvent(request, conn.getResponseCode());

                        LOG.warn("Diameter balance query processing. SessionId: {}, invalid http response code: {}",
                                request.getSessionId(), conn.getResponseCode());
                        // We have to send back a diameter_rating_failed message to the diameter stack if the
                        // answer from the as is not a succes (200) or a http not found (404) message
                        diameterHttpCallData.setTechnicalError(conn.getResponseCode());
                        sendErrorMessage("Invalid http response code: " + conn.getResponseCode() + "!", request,
                                imscfCallId, lwcommRouteName);
                        success = true;
                    }
                } catch (java.net.SocketTimeoutException e) {
                    ElEventCreator.addIncomingHttpTErrorEvent(request);

                    // (connect/read timeout -> reenable.time) For a specific period of time (reenable.time) the
                    // given endpont is banned. Skip to the next endpoint.
                    LOG.warn("Diameter balance query processing. Http connection timeout: " + e.getMessage()
                            + ". Banning endpoint: " + url);
                    banEndpoint(urlIndex, appServerGroupName);
                } catch (IOException e) {
                    ElEventCreator.addIncomingHttpTErrorEvent(request);

                    // (connection failed -> reenable.time) For a specific period of time (reenable.time) the given
                    // endpont is banned. Skip to the next endpoint.
                    LOG.warn("Diameter balance query processing. Http connection failed: " + e.getMessage()
                            + ". Banning endpoint: " + url);
                    banEndpoint(urlIndex, appServerGroupName);
                } catch (Exception e) {
                    LOG.warn("Diameter balance query processing. Error while processing diameter balance message. {}",
                            e.getMessage(), e);
                }
            }

            if (!success) {
                LOG.error("Diameter balance query processing. SessionId: {}, cannot reach tsdp!",
                        request.getSessionId());
                diameterHttpCallData.setTechnicalError(999);
                sendErrorMessage("Cannot reach tsdp!", request, imscfCallId, lwcommRouteName);
            }

            if (deleteCall) {
                cf.deleteCall(diameterHttpCallData);
            }
        }
        LOG.debug("Diameter balance query processing finished. SessionId: {}", request.getSessionId());
    }

    private void handleDebitQuery(DiameterSLELCreditControlRequest request, String imscfCallId, String lwcommRouteName,
            String appServerGroupName) {
        LOG.debug("Diameter debit query processing started. SessionId: {}", request.getSessionId());

        int urlIndex = -1;

        CallStore cs = CallContext.getCallStore();
        CallFactoryBean cf = CallContext.getCallFactory();
        try (DiameterHttpCall diameterHttpCallData = cs.getHttpCallByDiameterSessionId(request.getSessionId())) {
            try {
                if (diameterHttpCallData.wasTechnicalError()) {
                    LOG.debug(
                            "Diameter debit query processing. there was a technical error earlier in the session (sessionId: {}). Sending error response.",
                            request.getSessionId());
                    sendErrorMessage("There was a technical error earlier in the session. Code: "
                            + diameterHttpCallData.getTechnicalError() + "!", request, imscfCallId, lwcommRouteName);
                } else if (diameterHttpCallData.getjSessionId() == null
                        || diameterHttpCallData.getInInstanceIndex() < 0) {
                    LOG.debug("Diameter debit query processing. Invalid session id or JSessionID for debiting. sessionId="
                            + request.getSessionId() + ". Sending error response.");
                    sendErrorMessage(
                            "Invalid session id or JSessionID for debiting. sessionId=" + request.getSessionId(),
                            request, imscfCallId, lwcommRouteName);
                } else {
                    urlIndex = diameterHttpCallData.getInInstanceIndex();
                    LOG.debug("Diameter debit query processing. sessionId="
                            + request.getSessionId()
                            + ", requested endpoint: "
                            + DiameterAsRoutes.getServerInfo().get(appServerGroupName).getServiceEndpoints()
                                    .get(urlIndex).getUrl());

                    HttpURLConnection conn = (HttpURLConnection) new URL(DiameterAsRoutes.getServerInfo()
                            .get(appServerGroupName).getServiceEndpoints().get(urlIndex).getUrl()).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(DiameterAsRoutes.getServerInfo().get(appServerGroupName).getConnectTimeout());
                    conn.setReadTimeout(DiameterAsRoutes.getServerInfo().get(appServerGroupName).getReadTimeout());
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty("Accept-Charset", "UTF-8");
                    conn.setRequestProperty("Content-Language", "en-US");
                    // Setting the stored JSessionID
                    conn.setRequestProperty("Cookie", diameterHttpCallData.getjSessionId());
                    conn.setUseCaches(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    Gson gson = new Gson();

                    LOG.debug("Diameter debit query processing. sessionId=" + request.getSessionId()
                            + ", json wrapper for request: " + request.getWrapper());
                    String json = gson.toJson(request.getWrapper());

                    byte[] requestJsonString = json.getBytes("UTF-8");
                    conn.setRequestProperty("Content-Length", "" + requestJsonString.length);

                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

                    wr.write(requestJsonString);
                    wr.flush();
                    wr.close();

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                    LOG.debug("Diameter debit query processing. sessionId=" + request.getSessionId()
                            + ", http response code: " + conn.getResponseCode());

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        ElEventCreator.addIncomingHttpEvent(request, conn.getResponseCode());

                        // Create DiameterGWCreditControlResponse object from the response of the IN service
                        DiameterGWCCResponseJsonWrapper wrapper = gson.fromJson(br,
                                DiameterGWCCResponseJsonWrapper.class);
                        LOG.debug("Diameter debit query processing. sessionId=" + request.getSessionId()
                                + ", response wrapper=" + wrapper.toString());

                        DiameterSLELCreditControlResponse creditContorolResponse = new DiameterSLELCreditControlResponse();
                        creditContorolResponse.loadFromWrapper(wrapper);
                        creditContorolResponse.setSessionId(request.getSessionId());
                        creditContorolResponse.setRemoveSession(true);

                        sendMessageToSL(creditContorolResponse, request, imscfCallId, lwcommRouteName);

                    } else if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                        ElEventCreator.addIncomingHttpEvent(request, conn.getResponseCode());

                        LOG.warn(
                                "Diameter debit query processing. Http response: 404 not found. SessionId: {}, banning endpoint: ",
                                request.getSessionId(), DiameterAsRoutes.getServerInfo().get(appServerGroupName)
                                        .getServiceEndpoints().get(urlIndex).getUrl());
                        banEndpoint(urlIndex, appServerGroupName);
                        sendErrorMessage(
                                "Cannot reach tsdp endpoint: "
                                        + DiameterAsRoutes.getServerInfo().get(appServerGroupName)
                                                .getServiceEndpoints().get(urlIndex).getUrl() + "!", request,
                                imscfCallId, lwcommRouteName);
                    } else {
                        ElEventCreator.addIncomingHttpEvent(request, conn.getResponseCode());

                        LOG.warn("Diameter debit query processing. SessionId: {}, invalid http response code: {}!",
                                request.getSessionId(), conn.getResponseCode());
                        // We have to send back a diameter_rating_failed message to the diameter stack if the
                        // answer from the as is not a succes (200) or a http not found (404) message
                        sendErrorMessage("Invalid http response code: " + conn.getResponseCode() + "!", request,
                                imscfCallId, lwcommRouteName);
                    }
                }

            } catch (java.net.SocketTimeoutException e) {
                ElEventCreator.addIncomingHttpTErrorEvent(request);

                // (connect/read timeout -> reenable.time) For a specific period of time (reenable.time) the given
                // endpont is banned.
                LOG.warn(
                        "Diameter debit query processing. SessionId: {}. Http connection timeout: {}. Banning endpoint: {}. And returning {} error in diameter response!",
                        request.getSessionId(), e.getMessage(), DiameterAsRoutes.getServerInfo()
                                .get(appServerGroupName).getServiceEndpoints().get(urlIndex).getUrl(),
                        CCAResultCode.TECHNICAL_ERROR.toString());
                banEndpoint(urlIndex, appServerGroupName);
                sendErrorMessage(
                        "Cannot reach tsdp endpoint: "
                                + DiameterAsRoutes.getServerInfo().get(appServerGroupName).getServiceEndpoints()
                                        .get(urlIndex).getUrl() + "!", request, imscfCallId, lwcommRouteName);
            } catch (IOException e) {
                ElEventCreator.addIncomingHttpTErrorEvent(request);

                // (connection failed -> reenable.time) For a specific period of time (reenable.time) the given
                // endpont is banned.
                LOG.warn("Diameter debit query processing. SessionId: "
                        + request.getSessionId()
                        + ". Http connection failed: "
                        + e.getMessage()
                        + ". Banning endpoint: "
                        + DiameterAsRoutes.getServerInfo().get(appServerGroupName).getServiceEndpoints().get(urlIndex)
                                .getUrl() + ". And returning " + CCAResultCode.TECHNICAL_ERROR.toString()
                        + " error in diameter response!");
                banEndpoint(urlIndex, appServerGroupName);
                sendErrorMessage(
                        "Cannot reach tsdp endpoint: "
                                + DiameterAsRoutes.getServerInfo().get(appServerGroupName).getServiceEndpoints()
                                        .get(urlIndex).getUrl() + "!", request, imscfCallId, lwcommRouteName);
            } catch (Exception e) {
                LOG.warn("Diameter debit query processing. Error while processing diameter debit message. {}",
                        e.getMessage(), e);
            }

            cf.deleteCall(diameterHttpCallData);
        }
        LOG.debug("Diameter debit query processing finished. SessionId: {}", request.getSessionId());
    }

    private void sendErrorMessage(String errorMsg, DiameterSLELCreditControlRequest request, String imscfCallId,
            String lwcommRouteName) {
        LOG.debug("Diameter error message sending. SessionId: {}, error msg: {}", request.getSessionId(), errorMsg);

        DiameterSLELCreditControlResponse creditContorolResponse = new DiameterSLELCreditControlResponse();
        creditContorolResponse.setQueryResult(CCAResultCode.TECHNICAL_ERROR);
        creditContorolResponse.setErrorCode(errorMsg);
        creditContorolResponse.setResponseCaller(request.getCaller());
        creditContorolResponse.setResponseCallee(request.getCallee());
        creditContorolResponse.setResponseSmscAddress(request.getSmscAddress());
        creditContorolResponse.setSessionId(request.getSessionId());
        if (request.getRequestTypeObject().equals(CCRequestType.DEBIT)) {
            creditContorolResponse.setRemoveSession(true);
        }
        sendMessageToSL(creditContorolResponse, request, imscfCallId, lwcommRouteName);
    }

    private void sendMessageToSL(DiameterSLELCreditControlResponse creditContorolResponse,
            DiameterSLELCreditControlRequest request, String imscfCallId, String lwcommRouteName) {
        try {
            if (creditContorolResponse.getErrorCode() != null && creditContorolResponse.getErrorCode().length() > 90) {
                String erMessage = creditContorolResponse.getErrorCode().substring(0, 90);
                creditContorolResponse.setErrorCode(erMessage);
            }

            String diameterAsString = DiameterSerializer.serialize(creditContorolResponse);

            CallStore cs = CallContext.getCallStore();
            try (DiameterHttpCall diameterHttpCallData = cs.getHttpCallByDiameterSessionId(request.getSessionId())) {
                if (request.getRequestTypeObject() == CCRequestType.BALANCE) {
                    ElStatistics.createOneShotDiameterStatisticsSetter(diameterHttpCallData.getServiceIdentifier(),
                            diameterHttpCallData.getDiameterModule().getName()).incBalanceQueryAnsweredCount();
                } else {
                    ElStatistics.createOneShotDiameterStatisticsSetter(diameterHttpCallData.getServiceIdentifier(),
                            diameterHttpCallData.getDiameterModule().getName()).incDebitQueryAnsweredCount();
                }
            }

            String payload = "" + //
                    "Target: DiameterStack\r\n" + //
                    "Content: DiameterDataMessage\r\n" + //
                    "\r\n" + //
                    diameterAsString;

            LOG.debug("Sending {} {} to SL on route: [{}], payload:\n{}", request.getSessionId(),
                    request.getRequestTypeObject(), lwcommRouteName, payload);

            SendResultFuture<SendResult> result = LwCommServiceProvider.getService().send(lwcommRouteName,
                    TextMessage.builder(payload).setGroupId(imscfCallId).setTag(LwcTags.IN_SESSION).create());
            try {
                SendResult sr = result.get();
                if (sr.getType() == Type.SUCCESS) {
                    ElEventCreator.addEventByDiameterSessionId(request.getSessionId(),
                            "<-LWC_OK(" + request.getRequestTypeObject() + ", " + result.getMessageId() + ")");
                    LOG.debug("Message sent to SL: {}", sr);
                } else {
                    LOG.error("Failed to send message to SL node! {} -> {}, result: {}", request.getSessionId(),
                            lwcommRouteName, sr);
                    LOG.error("Failed to send message to SL node!");
                    ElEventCreator.addEventByDiameterSessionId(request.getSessionId(),
                            "<-LWC_ERROR(" + request.getRequestTypeObject() + ", " + result.getMessageId() + ")");
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error getting SendResult", e);
            }
        } catch (Exception e) {
            LOG.error("Error while send message to SL. {}", e.getMessage(), e);
        }
    }

    @Override
    public void imscfCallStateChanged(IMSCFCall call) {
        if (call.getImscfState() == ImscfCallLifeCycleState.RELEASING) {
            LOG.debug("Delete diameter call, after max age timeout({}).", call.getImscfCallId());
            CallFactoryBean cf = CallContext.getCallFactory();
            cf.deleteCall(call);
        }
    }
}
