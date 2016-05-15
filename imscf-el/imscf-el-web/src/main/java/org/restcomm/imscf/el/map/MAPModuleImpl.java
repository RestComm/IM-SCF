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
package org.restcomm.imscf.el.map;

import java.util.stream.Stream;

import javax.servlet.sip.SipApplicationSession;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.MapModuleType;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.map.call.MAPCall;
import org.restcomm.imscf.el.map.call.MAPSIPCall;
import org.restcomm.imscf.el.map.scenarios.MapOutgoingRequestScenario;
import org.restcomm.imscf.el.modules.ModuleInitializationException;
import org.restcomm.imscf.el.sccp.SccpPrimitiveMapper;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipModule;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.el.statistics.ElStatistics;

import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for MAP module implementations.
 */
public class MAPModuleImpl implements MAPModule, SipModule {

    private static final Logger LOG = LoggerFactory.getLogger(MAPModuleImpl.class);

    private String name;
    private ImscfConfigType imscfConfiguration;
    private MapModuleType moduleConfiguration;
    private MAPProvider mapProvider;
    private SccpProvider sccpProvider;
    private SccpAddress localSccpAddress;

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ImscfConfigType getImscfConfiguration() {
        return imscfConfiguration;
    }

    @Override
    public void setImscfConfiguration(ImscfConfigType imscfConfiguration) {
        this.imscfConfiguration = imscfConfiguration;
    }

    @Override
    public MapModuleType getModuleConfiguration() {
        return moduleConfiguration;
    }

    @Override
    public void setModuleConfiguration(MapModuleType moduleConfiguration) {
        this.moduleConfiguration = moduleConfiguration;
    }

    @Override
    public void setMAPProvider(MAPProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    @Override
    public MAPProvider getMAPProvider() {
        return mapProvider;
    }

    @Override
    public void setSccpProvider(SccpProvider provider) {
        this.sccpProvider = provider;
    }

    @Override
    public SccpProvider getSccpProvider() {
        return sccpProvider;
    }

    @Override
    public SccpAddress getLocalSccpAddress() {
        return localSccpAddress;
    }

    @Override
    public void initialize(ImscfConfigType configuration) throws ModuleInitializationException {
        MAPModule.super.initialize(configuration);
        LOG.debug("MAP module {} processing shared config", getName());
    }

    @Override
    public void initialize(MapModuleType configuration) throws ModuleInitializationException {
        LOG.debug("MAP module {} initializing...", getName());
        MAPModule.super.initialize(configuration);
        ParameterFactory sccpParameterFactory = getSccpProvider().getParameterFactory();
        if (configuration.getLocalGt() != null) {
            localSccpAddress = SccpPrimitiveMapper.createSccpAddress(configuration.getLocalGt(), sccpParameterFactory);
        } else if (configuration.getLocalSsn() != null) {
            localSccpAddress = SccpPrimitiveMapper.createSccpAddressPcFilledBySl(configuration.getLocalSsn(),
                    sccpParameterFactory);
        } else {
            LOG.error("Invalid configuration for MAP module {}, neither local GT or SSN is set.", getName());
        }

        LOG.debug("MAP gsmSCF address: {}", configuration.getMapGsmScfAddress());
        LOG.debug("MAP timeout: {}s", configuration.getMapTimeoutSec());

        this.moduleConfiguration = configuration;
        LOG.debug("MAP module {} initialized.", getName());
    }

    // MAP SMS listener methods

    @Override
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse resp) {
        CallStore callStore = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (MAPSIPCall call = (MAPSIPCall) callStore.getCallByLocalTcapTrId(resp.getMAPDialog().getLocalDialogId())) {
            getMatchingOutgoingScenarios(call, resp).forEachOrdered(s -> s.onReturnResult(resp));
        }
    }

    // MAP Mobility Management listener methods

    @Override
    public void onAnyTimeInterrogationResponse(AnyTimeInterrogationResponse atiResp) {
        LOG.debug("ATI response arrived: {}", atiResp);

        CallStore callStore = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (MAPSIPCall call = (MAPSIPCall) callStore.getCallByLocalTcapTrId(atiResp.getMAPDialog().getLocalDialogId())) {
            getMatchingOutgoingScenarios(call, atiResp).forEachOrdered(s -> s.onReturnResult(atiResp));
            ElStatistics.createOneShotMapStatisticsSetter(call.getAtiRequest().getTargetRemoteSystem())
                    .incAnyTimeInterrogationResultCount();
        }
    }

    // MAP general methods

    @Override
    public void onErrorComponent(MAPDialog mapDialog, Long invokeId, MAPErrorMessage mapErrorMessage) {
        LOG.debug("MAPModuleImpl.onErrorComponent(mapDialog: {}, invokeId: {}, mapErrorMessage: {})", mapDialog,
                invokeId, mapErrorMessage);
        CallStore callStore = CallContext.getCallStore();
        try (MAPSIPCall call = (MAPSIPCall) callStore.getCallByLocalTcapTrId(mapDialog.getLocalDialogId())) {
            getMatchingOutgoingScenarios(call, invokeId).forEachOrdered(s -> s.onErrorComponent(mapErrorMessage));
        }
    }

    @Override
    public void onInvokeTimeout(MAPDialog mapDialog, Long invokeId) {
        LOG.debug("MAPModuleImpl.onInvokeTimeout(mapDialog: {}, invokeId: {})", mapDialog, invokeId);
        CallStore callStore = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (MAPSIPCall call = (MAPSIPCall) callStore.getCallByLocalTcapTrId(mapDialog.getLocalDialogId())) {
            getMatchingOutgoingScenarios(call, invokeId).forEachOrdered(s -> s.onInvokeTimeout());
        }
    }

    @Override
    public void onMAPMessage(MAPMessage msg) {
        LOG.debug("MAPModuleImpl.onMAPMessage({})", msg);
    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long invokeId, Problem problem, boolean isLocalOriginated) {
        LOG.debug("MAPModuleImpl.onRejectComponent(mapDialog: {}, invokeId: {}, problem: {}, isLocalOriginated: {}",
                mapDialog, invokeId, problem, isLocalOriginated);
        CallStore callStore = CallContext.getCallStore();
        try (MAPSIPCall call = (MAPSIPCall) callStore.getCallByLocalTcapTrId(mapDialog.getLocalDialogId())) {
            getMatchingOutgoingScenarios(call, invokeId).forEachOrdered(s -> s.onRejectComponent(problem));
        }
    }

    // SipModule methods

    @Override
    public int getAppSessionTimeoutMin() {
        // div, but round up instead of down
        return -Math.floorDiv(getModuleConfiguration().getMapTimeoutSec(), -60); // second to minute
    }

    @Override
    public void scenarioFinished(SIPCall call, Scenario s) {
        LOG.debug("MapModuleImpl.scenarioFinished, call: {}, scenario: {}", call, s);
    }

    @Override
    public void sessionExpired(SipApplicationSession sas) {
        LOG.debug("MapModuleImpl.sessionExpired: {}", sas.getId());
    }

    @Override
    public void sessionDestroyed(SipApplicationSession sas) {
        LOG.debug("MapModuleImpl.sessionDestroyed: {}", sas.getId());
    }

    private Stream<MapOutgoingRequestScenario> getMatchingOutgoingScenarios(MAPCall call, MAPMessage response) {
        return getMatchingOutgoingScenarios(call, response.getInvokeId());
    }

    private Stream<MapOutgoingRequestScenario> getMatchingOutgoingScenarios(MAPCall call, Long invokeId) {
        return call.getMapOutgoingRequestScenarios().stream().filter(s -> s.getInvokeId().equals(invokeId));
    }

    @Override
    public void handleAsUnavailableError(SIPCall call) {
        LOG.debug("handleAsUnavailableError {}", call);
        // TODO ...
    }

    @Override
    public void handleAsReactionTimeout(SIPCall call) {
        LOG.debug("handleAsReactionTimeout {}", call);
        // TODO ...
    }

    @Override
    public boolean isSipCallFinished(SIPCall call) {
        if (SipModule.super.isSipCallFinished(call))
            return true;
        // TODO: handle SUBSCRIBE dialog specific UA termination cases if not handled by the container-returned
        // SipSession.State:
        // SUBSCRIBE UAS: NOTIFY with "Subscription-State: terminated" already sent
        // SUBSCRIBE UAC: NOTIFY with "Subscription-State: terminated" already received
        return false;
    }

    @Override
    public void handleSipCallFinished(SIPCall call) {
        LOG.debug("handleSipCallFinished {}", call);
        // TODO handle MAP call release
        // ATI calls: Nothing to do for now, as they consist of a single TC_BEGIN-TC_END pair.
        // ---------- Neither MAP release message nor TC_ABORT can be sent after the TC_BEGIN.
    }
}
