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
package org.restcomm.imscf.el.stack;

import static org.restcomm.imscf.el.stack.LwcommMessageReceiver.LWC_MESSAGE_PATTERN;
import org.restcomm.imscf.common.config.DiameterRoutingConfigType;
import org.restcomm.imscf.common.config.HttpApplicationServerGroupType;
import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.diameter.DiameterModule;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterAppServerData;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterAsRoutes;
import org.restcomm.imscf.common.diameter.creditcontrol.ServiceEndpoint;
import org.restcomm.imscf.el.modules.ModuleStore;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;
import org.restcomm.imscf.el.statistics.ElStatistics;
import org.restcomm.imscf.common.DiameterSerializer;
import org.restcomm.imscf.common.diameter.creditcontrol.CCRequestType;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterSLELCreditControlRequest;
import org.restcomm.imscf.common.util.ImscfCallId;
import org.restcomm.imscf.common.lwcomm.service.IncomingTextMessage;
import org.restcomm.imscf.common.lwcomm.service.LwCommServiceProvider;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Class for handle messages from diameter stack.
 */
public class DiameterImpl implements MessageReceiver {
    private static Logger logger = LoggerFactory.getLogger(DiameterImpl.class);

    private transient CallStore callStore;
    private transient ConfigBean configBean;
    private transient CallFactoryBean callFactoryBean;

    public void setCallStore(CallStore callStore) {
        this.callStore = callStore;
    }

    public void setConfigBean(ConfigBean configBean) {
        this.configBean = configBean;
    }

    public void setCallFactoryBean(CallFactoryBean callFactoryBean) {
        this.callFactoryBean = callFactoryBean;
    }

    public void initializeDiameterAsRoutes() {
        DiameterAsRoutes.reset();
        for (DiameterRoutingConfigType diameterAsRouting : configBean.getConfig().getDiameterRouting()) {
            DiameterAppServerData diamAppServer = new DiameterAppServerData();

            for (HttpApplicationServerGroupType httpServerParameters : configBean.getConfig()
                    .getHttpApplicationServers()) {
                if (diameterAsRouting.getHttpApplicationServerGroup().getName().equals(httpServerParameters.getName())) {
                    List<ServiceEndpoint> roamingsmsUrls = new ArrayList<ServiceEndpoint>();
                    for (String url : httpServerParameters.getUrl()) {
                        roamingsmsUrls.add(new ServiceEndpoint(url));
                    }
                    diamAppServer.setConnectTimeout(httpServerParameters.getConnectTimeoutMs());
                    diamAppServer.setReadTimeout(httpServerParameters.getReadTimeoutMs());
                    diamAppServer.setReenableTime(httpServerParameters.getReenableTimeMs());
                    diamAppServer.setServiceEndpoints(roamingsmsUrls);

                    String serviceContextIdsStr = diameterAsRouting.getServiceContextIds();
                    // whitespace allowed around tokens
                    for (String serviceId : serviceContextIdsStr.trim().split("\\s*,\\s*")) {
                        DiameterAsRoutes.getServiceHttpServerPairs().put(serviceId,
                                diameterAsRouting.getHttpApplicationServerGroup().getName());

                        DiameterAsRoutes.getServiceDiameterModulePairs().put(serviceId,
                                diameterAsRouting.getDiameterGatewayModule().getName());
                    }
                    break;
                }
            }

            DiameterAsRoutes.getServerInfo().putIfAbsent(diameterAsRouting.getHttpApplicationServerGroup().getName(),
                    diamAppServer);
        }
    }

    @Override
    public void onMessage(IncomingTextMessage lwcommMessage) {
        MDC.clear();
        String payload = lwcommMessage.getPayload();
        Matcher m = LWC_MESSAGE_PATTERN.matcher(payload);
        String content, data;
        if (m.matches()) {
            // group 1 is the target, i.e. this listener
            content = m.group(2);
            data = m.group(3);
            switch (content) { // NOPMD ignore switch with less than 2 branches check
            case "DiameterDataMessage":
                try {
                    DiameterSLELCreditControlRequest diameterCCRequest = DiameterSerializer.deserializeRequest(data);

                    DiameterModule targetModule = ModuleStore.getDiameterModules().get(
                            DiameterAsRoutes.getServiceDiameterModulePairs().get(
                                    diameterCCRequest.getServiceContextId()));
                    if (targetModule != null) {
                        String imscfCallId = ImscfCallId.parse(lwcommMessage.getGroupId()).toString();
                        try (IMSCFCall call = callStore.getHttpCallByDiameterSessionId(diameterCCRequest.getSessionId())) {
                            if (call != null) {
                                call.getCallHistory().addEvent(
                                        "->LWC(" + diameterCCRequest.getRequestType() + ", " + lwcommMessage.getId()
                                                + ")");
                            } else {
                                imscfCallId = callFactoryBean.newCall(diameterCCRequest, imscfCallId,
                                        lwcommMessage.getId(), targetModule);
                            }
                        }

                        if (diameterCCRequest.getRequestTypeObject() == CCRequestType.BALANCE) {
                            ElStatistics.createOneShotDiameterStatisticsSetter(diameterCCRequest.getServiceContextId(),
                                    targetModule.getName()).incBalanceQueryReceivedCount();
                        } else {
                            ElStatistics.createOneShotDiameterStatisticsSetter(diameterCCRequest.getServiceContextId(),
                                    targetModule.getName()).incDebitQueryReceivedCount();
                        }

                        try (ContextLayer cl = CallContext.with(callStore, callFactoryBean)) {
                            String redirectRoute = LwCommServiceProvider.getService().getConfiguration()
                                    .getLocalNodeName()
                                    + " -> " + lwcommMessage.getFrom().getName();

                            targetModule.doCreditControlRequest(
                                    diameterCCRequest,
                                    imscfCallId,
                                    redirectRoute,
                                    DiameterAsRoutes.getServiceHttpServerPairs().get(
                                            diameterCCRequest.getServiceContextId()));
                        }
                    } else {
                        logger.warn("There was not diamater module for handling this request: {}", diameterCCRequest);
                    }
                } catch (Exception e) {
                    logger.error("Incoming diameter message handling error. {}", e.getMessage(), e);
                }
                break;
            default:
                logger.error("Unknown content: " + content);
                break;
            }
        } else {
            logger.error("Message from SL not understood.");
        }
    }

}
