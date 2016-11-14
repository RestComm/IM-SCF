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
package org.restcomm.imscf.el.cap;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.CapModuleType;
import org.restcomm.imscf.common.config.InviteErrorActionType;
import org.restcomm.imscf.common.config.MediaResourceWrapperType;
import org.restcomm.imscf.common.config.ReleaseCauseType;
import org.restcomm.imscf.el.call.ImscfCallLifeCycleState;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.call.CAPCSCall;
import org.restcomm.imscf.el.cap.call.CAPCSCall.CAPState;
import org.restcomm.imscf.el.cap.call.CAPCall;
import org.restcomm.imscf.el.cap.converter.ContinueUtil;
import org.restcomm.imscf.el.cap.converter.InviteErrorMatcher;
import org.restcomm.imscf.el.cap.converter.ReleaseCallUtil;
import org.restcomm.imscf.el.cap.scenarios.CapIncomingRequestScenario;
import org.restcomm.imscf.el.cap.scenarios.CapOutgoingRequestScenario;
import org.restcomm.imscf.el.cap.scenarios.CapScenarioTypesCsCall.CapScenarioActivityTest;
import org.restcomm.imscf.el.map.MapPrimitiveMapper;
import org.restcomm.imscf.el.modules.ModuleInitializationException;
import org.restcomm.imscf.el.modules.ModuleStore;
import org.restcomm.imscf.el.stack.CallContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mobicents.protocols.ss7.cap.api.CAPApplicationContextVersion;
import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPMessage;
import org.mobicents.protocols.ss7.cap.api.CAPParameterFactory;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPDialogState;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPUserAbortReason;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.cap.api.primitives.EventTypeBCSM;
import org.mobicents.protocols.ss7.cap.api.primitives.MonitorMode;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ActivityTestResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CallInformationReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectLegResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitiateCallAttemptResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.MoveLegResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationResponse;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SpecializedResourceReportRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SplitLegResponse;
import org.mobicents.protocols.ss7.cap.api.service.sms.EventReportSMSRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.InitialDPSMSRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.primitive.EventTypeSMS;
import org.mobicents.protocols.ss7.cap.api.service.sms.primitive.SMSEvent;
import org.mobicents.protocols.ss7.cap.primitives.BCSMEventImpl;
import org.mobicents.protocols.ss7.inap.api.primitives.BothwayThroughConnectionInd;
import org.mobicents.protocols.ss7.inap.api.primitives.LegType;
import org.mobicents.protocols.ss7.inap.primitives.LegIDImpl;
import org.mobicents.protocols.ss7.isup.ISUPParameterFactory;
import org.mobicents.protocols.ss7.isup.message.parameter.CalledPartyNumber;
import org.mobicents.protocols.ss7.isup.message.parameter.CauseIndicators;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for CAP modules. */
@SuppressWarnings("PMD.GodClass")
public abstract class CAPModuleBase implements CAPModule {

    private static final Logger LOG = LoggerFactory.getLogger(CAPModuleBase.class);

    private String name;
    private ImscfConfigType imscfConfiguration;
    private CapModuleType moduleConfiguration;
    private CAPProvider capProvider;
    private SccpProvider sccpProvider;

    protected InviteErrorMatcher[] inviteErrorMatchers;
    protected ISDNAddressString gsmScfAddress;
    protected SccpAddress localSccpAddress;

    protected List<BCSMEvent> defaultOriginatingBCSMEventsPhase2;
    protected List<BCSMEvent> defaultOriginatingBCSMEventsPhase3;
    protected List<BCSMEvent> defaultOriginatingBCSMEventsPhase4;
    // protected List<BCSMEvent> defaultOriginatingBCSMEventsPhase4ForMrf;
    // protected List<BCSMEvent> defaultTerminatingBCSMEventsPhase4ForMrf;
    protected List<BCSMEvent> defaultInitiateCallAttemptBCSMEventsPhase4;
    protected List<BCSMEvent> defaultTerminatingBCSMEventsPhase2;
    protected List<BCSMEvent> defaultTerminatingBCSMEventsPhase3;
    protected List<BCSMEvent> defaultTerminatingBCSMEventsPhase4;
    protected List<SMSEvent> defaultOriginatingSMSEventsPhase3;
    // no terminating sms in phase 3
    protected List<SMSEvent> defaultOriginatingSMSEventsPhase4;
    protected List<SMSEvent> defaultTerminatingSMSEventsPhase4;

    // ConnectToResourceArg for each mrf alias
    private Map<String, ConnectToResourceArg> connectToResourceArgsForMrfAliases;

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
    public CapModuleType getModuleConfiguration() {
        return moduleConfiguration;
    }

    @Override
    public void setModuleConfiguration(CapModuleType moduleConfiguration) {
        this.moduleConfiguration = moduleConfiguration;
    }

    @Override
    public void setCAPProvider(CAPProvider provider) {
        this.capProvider = provider;
    }

    @Override
    public CAPProvider getCAPProvider() {
        return capProvider;
    }

    @Override
    public void setSccpProvider(SccpProvider sccpProvider) {
        this.sccpProvider = sccpProvider;
    }

    @Override
    public ISDNAddressString getGsmScfAddress() {
        return gsmScfAddress;
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
        CAPModule.super.initialize(configuration);
        LOG.debug("CAP module {} processing shared config.", getName());
        Optional.ofNullable(configuration.getSipApplicationServers())
                .orElseThrow(
                        () -> new IllegalStateException("SIP application servers must be specified for CAP module"))
                .getSipApplicationServerGroups()
                .stream()
                .forEach(
                        sasg -> {
                            LOG.debug("Found SIP AS group: {} ({})", sasg.getName(), sasg.getDistribution());
                            sasg.getSipApplicationServer()
                                    .stream()
                                    .forEachOrdered(
                                            as -> {
                                                LOG.debug("  AS {} {}:{} HB_{}", as.getName(), as.getHost(),
                                                        as.getPort(), as.isHeartbeatEnabled() ? "ON" : "OFF");
                                            });
                        });
        LOG.debug("Done.");
    }

    @Override
    public void initialize(CapModuleType configuration) throws ModuleInitializationException {
        LOG.debug("CAP module {} initializing...", getName());
        CAPModule.super.initialize(configuration);

        gsmScfAddress = Optional
                .ofNullable(configuration.getGsmScfAddress())
                .map(address -> MapPrimitiveMapper.createISDNAddressString(address,
                        capProvider.getMAPParameterFactory())).orElse(null);

        initDefaultTriggerEvents(configuration);

        inviteErrorMatchers = configuration.getInviteErrorHandlers().stream().map(InviteErrorMatcher::new)
                .toArray(InviteErrorMatcher[]::new);

        if (configuration.getLocalGt() != null) {
            localSccpAddress = ModuleStore.getSccpModule().getLocalAddress(configuration.getLocalGt().getAlias());
        } else if (configuration.getLocalSsn() != null) {
            localSccpAddress = ModuleStore.getSccpModule().getLocalAddress(configuration.getLocalSsn().getAlias());
        }
        if (localSccpAddress == null) {
            throw new IllegalStateException("localGt or localSsn must be specified for CAP module!");
        }

        List<MediaResourceWrapperType> mediaResources = configuration.getMediaResources();
        connectToResourceArgsForMrfAliases = new HashMap<String, ConnectToResourceArg>();
        for (MediaResourceWrapperType mrtw : mediaResources) {
            ConnectToResourceArg ctra = new ConnectToResourceArg();
            ctra.setExtensions(null);

            CAPParameterFactory capParameterFactory = getCAPProvider().getCAPParameterFactory();

            // setting the cdpn
            ISUPParameterFactory isup = getCAPProvider().getISUPParameterFactory();
            CalledPartyNumber cdpn = isup.createCalledPartyNumber();
            cdpn.setAddress(mrtw.getMediaResource().getAddressDigits());
            switch (mrtw.getMediaResource().getNatureOfAddress()) {
            case INTERNATIONAL:
                cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_INTERNATIONAL_NUMBER);
                break;
            case NATIONAL:
                cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_NATIONAL_SN);
                break;
            case UNKNOWN:
                cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_UNKNOWN);
                break;
            case SUBSCRIBER_NUMBER:
                cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_SUBSCRIBER_NUMBER);
                break;
            case NETWORK_SPECIFIC:
                cdpn.setNatureOfAddresIndicator(CalledPartyNumber._NAI_NETWORK_SPECIFIC_NUMBER_NU);
                break;
            default:
                throw new IllegalArgumentException("Unsupported NOA " + mrtw.getMediaResource().getNatureOfAddress());
            }

            switch (mrtw.getMediaResource().getNumberingPlan()) {
            case DATA:
                cdpn.setNumberingPlanIndicator(CalledPartyNumber._NPI_DATA);
                break;
            case ISDN:
                cdpn.setNumberingPlanIndicator(CalledPartyNumber._NPI_ISDN);
                break;
            case TELEX:
                cdpn.setNumberingPlanIndicator(CalledPartyNumber._NPI_TELEX);
                break;
            default:
                throw new IllegalArgumentException("Unsupported NumberingPlan "
                        + mrtw.getMediaResource().getNumberingPlan());
            }
            cdpn.setInternalNetworkNumberIndicator(CalledPartyNumber._INN_ROUTING_NOT_ALLOWED);

            try {
                ctra.setResourceAddressIPRoutingAddress(capParameterFactory.createCalledPartyNumberCap(cdpn));
            } catch (CAPException e) {
                LOG.warn("Unable to set IPRoutingAddress");
            }

            // setting resourceAddress_Null
            ctra.setResourceAddressNull(false);

            // bothwayThrough
            BothwayThroughConnectionInd bothwayThrough = BothwayThroughConnectionInd.bothwayPathNotRequired;
            switch (mrtw.getMediaResource().getBothwayThroughConnectionInd()) {
            case BOTHWAY_PATH_NOT_REQUIRED:
                bothwayThrough = BothwayThroughConnectionInd.bothwayPathNotRequired;
                break;
            case BOTHWAY_PATH_REQUIRED:
                bothwayThrough = BothwayThroughConnectionInd.bothwayPathRequired;
                break;
            default:
                throw new IllegalArgumentException("Unsupported BothwayThroughConnectionInd "
                        + mrtw.getMediaResource().getBothwayThroughConnectionInd());

            }
            ctra.setServiceInteractionIndicatorsTwo(capParameterFactory.createServiceInteractionIndicatorsTwo(null,
                    null, bothwayThrough, null, false, null, null, null));
            connectToResourceArgsForMrfAliases.put(mrtw.getMediaResource().getAlias(), ctra);

        }

        Optional.ofNullable(configuration.getSipProperties()).ifPresent(sipProp -> {
            /* no properties defined yet */
        });

        // all good
        this.moduleConfiguration = configuration;
        LOG.debug("CAP module {} initialized.", getName());
    }

    private void initDefaultTriggerEvents(CapModuleType configuration) {
        defaultOriginatingBCSMEventsPhase4 = Collections.unmodifiableList(configuration
                .getInTriggering()
                .getOBcsm()
                .stream()
                .map(tt -> TriggerTypeMapper.getAsCapBCSMEvent(tt, capProvider.getCAPParameterFactory(),
                        capProvider.getINAPParameterFactory())).collect(Collectors.toList()));

        // List<BCSMEvent> bcsmEventListForMRfOriginating = new ArrayList<BCSMEvent>();
        // bcsmEventListForMRfOriginating.add(new BCSMEventImpl(EventTypeBCSM.oDisconnect, MonitorMode.interrupted,
        // new LegIDImpl(true, LegType.leg1), null, false));
        // bcsmEventListForMRfOriginating.add(new BCSMEventImpl(EventTypeBCSM.oAbandon, MonitorMode.interrupted, null,
        // null, false));
        // defaultOriginatingBCSMEventsPhase4ForMrf = Collections.unmodifiableList(bcsmEventListForMRfOriginating);
        //
        // List<BCSMEvent> bcsmEventListForMRfTerminating = new ArrayList<BCSMEvent>();
        // bcsmEventListForMRfTerminating.add(new BCSMEventImpl(EventTypeBCSM.tDisconnect, MonitorMode.interrupted,
        // new LegIDImpl(true, LegType.leg1), null, false));
        // bcsmEventListForMRfTerminating.add(new BCSMEventImpl(EventTypeBCSM.tAbandon, MonitorMode.interrupted, null,
        // null, false));
        // defaultTerminatingBCSMEventsPhase4ForMrf = Collections.unmodifiableList(bcsmEventListForMRfTerminating);

        defaultTerminatingBCSMEventsPhase4 = Collections.unmodifiableList(configuration
                .getInTriggering()
                .getTBcsm()
                .stream()
                .map(tt -> TriggerTypeMapper.getAsCapBCSMEvent(tt, capProvider.getCAPParameterFactory(),
                        capProvider.getINAPParameterFactory())).collect(Collectors.toList()));

        defaultOriginatingBCSMEventsPhase3 = Collections.unmodifiableList(defaultOriginatingBCSMEventsPhase4.stream()
                .filter(event -> {
                    switch (event.getEventTypeBCSM()) {
                    // these are only available in phase 4:
                    case oTermSeized:
                    case oMidCall:
                    case oChangeOfPosition:
                    case oServiceChange:
                        return false;
                    default:
                        return true;
                    }
                }).collect(Collectors.toList()));

        defaultTerminatingBCSMEventsPhase3 = Collections.unmodifiableList(defaultTerminatingBCSMEventsPhase4.stream()
                .filter(event -> {
                    switch (event.getEventTypeBCSM()) {
                    // these are only available in phase 4:
                    case callAccepted:
                    case tMidCall:
                    case tChangeOfPosition:
                    case tServiceChange:
                        return false;
                    default:
                        return true;
                    }
                }).collect(Collectors.toList()));

        // same as phase 3
        defaultOriginatingBCSMEventsPhase2 = defaultOriginatingBCSMEventsPhase3;
        defaultTerminatingBCSMEventsPhase2 = defaultTerminatingBCSMEventsPhase3;

        defaultOriginatingSMSEventsPhase4 = Collections.unmodifiableList(configuration.getInTriggering().getOSms()
                .stream().map(tt -> TriggerTypeMapper.getAsCapSMSEvent(tt, capProvider.getCAPParameterFactory()))
                .collect(Collectors.toList()));

        defaultTerminatingSMSEventsPhase4 = Collections.unmodifiableList(configuration.getInTriggering().getTSms()
                .stream().map(tt -> TriggerTypeMapper.getAsCapSMSEvent(tt, capProvider.getCAPParameterFactory()))
                .collect(Collectors.toList()));

        // same orig events are available on phase 3
        defaultOriginatingSMSEventsPhase3 = defaultOriginatingSMSEventsPhase4;

        defaultInitiateCallAttemptBCSMEventsPhase4 = Collections.unmodifiableList(configuration
                .getInTriggering()
                .getIca()
                .stream()
                .map(tt -> TriggerTypeMapper.getAsCapBCSMEvent(tt, capProvider.getCAPParameterFactory(),
                        capProvider.getINAPParameterFactory())).collect(Collectors.toList()));
    }

    protected List<BCSMEvent> getDefaultEDPs(EventTypeBCSM detectionPoint, CAPApplicationContextVersion contextVersion) {
        switch (detectionPoint) {
        case analyzedInformation:
        case collectedInfo:
            switch (contextVersion) {
            case version2:
                return defaultOriginatingBCSMEventsPhase2;
            case version3:
                return defaultOriginatingBCSMEventsPhase3;
            case version4:
                return defaultOriginatingBCSMEventsPhase4;
            default:
                throw new IllegalArgumentException("Unsupported CAMEL CS phase " + contextVersion);
            }
        case termAttemptAuthorized:
            switch (contextVersion) {
            case version2:
                return defaultTerminatingBCSMEventsPhase2;
            case version3:
                return defaultTerminatingBCSMEventsPhase3;
            case version4:
                return defaultTerminatingBCSMEventsPhase4;
            default:
                throw new IllegalArgumentException("Unsupported CAMEL CS phase " + contextVersion);
            }
        default:
            throw new IllegalArgumentException("Unsupported CS IDP " + detectionPoint);
        }
    }

    public List<BCSMEvent> getEDPsForMrf(CAPCSCall call, int csId) {
        return getEDPsForMrf(call, call.getCallSegmentAssociation().getCallSegment(csId).getLegs());
    }

    public List<BCSMEvent> getEDPsForMrf(CAPCSCall call, Set<Integer> legs) {

        EventTypeBCSM abandon = null;
        EventTypeBCSM disconnect = null;
        switch (call.getBCSMType()) {
        case oBCSM:
            abandon = EventTypeBCSM.oAbandon;
            disconnect = EventTypeBCSM.oDisconnect;
            break;
        case tBCSM:
            abandon = EventTypeBCSM.tAbandon;
            disconnect = EventTypeBCSM.tDisconnect;
            break;
        default:
            throw new AssertionError("Unexpected enum value " + call.getBCSMType());
        }

        List<BCSMEvent> bcsmEvents = new ArrayList<BCSMEvent>();
        for (Integer i : legs) {
            bcsmEvents.add(new BCSMEventImpl(abandon, MonitorMode.interrupted, null, null, false));
            bcsmEvents.add(new BCSMEventImpl(disconnect, MonitorMode.interrupted, new LegIDImpl(true, LegType
                    .getInstance(i)), null, false));
        }

        return bcsmEvents;
    }

    protected List<SMSEvent> getDefaultEDPs(EventTypeSMS detectionPoint, CAPApplicationContextVersion contextVersion) {
        switch (detectionPoint) {
        case smsCollectedInfo:
            switch (contextVersion) {
            case version3:
                return defaultOriginatingSMSEventsPhase3;
            case version4:
                return defaultOriginatingSMSEventsPhase4;
            default:
                throw new IllegalArgumentException("Unsupported CAMEL SMS phase " + contextVersion);
            }
        case smsDeliveryRequested:
            if (contextVersion == CAPApplicationContextVersion.version4) {
                return defaultTerminatingSMSEventsPhase4;
            } else {
                throw new IllegalArgumentException("Unsupported CAMEL SMS phase " + contextVersion);
            }
        default:
            throw new IllegalArgumentException("Unsupported IDP " + detectionPoint);
        }
    }

    protected CallStore getCallStore() {
        return (CallStore) CallContext.get(CallContext.CALLSTORE);
    }

    protected CAPCall<?> getCapCall(CAPDialog dialog) {
        return getCallStore().getCapCall(dialog.getLocalDialogId());
    }

    protected CAPCall<?> getCapCallUnlocked(CAPDialog dialog) {
        return getCallStore().getCapCallUnlocked(dialog.getLocalDialogId());
    }

    protected CallFactoryBean getCallFactory() {
        return (CallFactoryBean) CallContext.get(CallContext.CALLFACTORY);
    }

    @Override
    public long getTcapIdleTimeoutMillis() {
        int activityTestSec = getModuleConfiguration().getGeneralProperties().getActivityTestIntervalSec();
        if (activityTestSec > 0)
            return 1000L * activityTestSec; // second to millisecond
        else
            // minutes to milliseconds
            // +1 min to avoid race between last-minute release and timeout callback)
            return (getModuleConfiguration().getGeneralProperties().getMaxCallLengthMinutes() + 1L) * 60000L;
    }

    @Override
    public long getAsReactionTimeoutMillis() {
        return getModuleConfiguration().getGeneralProperties().getAsReactionTimeoutSec() * 1000;
    }

    protected Stream<CapOutgoingRequestScenario> findMatchingCapScenarios(CAPCall<?> call, Long invokeId) {
        return new ArrayList<>(call.getCapOutgoingRequestScenarios()).stream().filter(
                cs -> cs.getInvokeId().equals(invokeId));
    }

    @SuppressWarnings("unchecked")
    <R extends CAPMessage> void handleIncomingRequest(CAPCall<?> call, R request) {
        List<CapIncomingRequestScenario<?>> allScenarios = call.getCapIncomingRequestScenarios();
        List<CapIncomingRequestScenario<R>> matchingScenarios = allScenarios.stream()
                .filter(cirs -> cirs.getRequestClass().isInstance(request)).map(s -> (CapIncomingRequestScenario<R>) s)
                .collect(Collectors.toList());
        LOG.trace("Matching CAP scenarios: {}. All scenarios: {}", new CapScenarioListPrinter(matchingScenarios),
                new CapScenarioListPrinter(allScenarios));
        for (CapIncomingRequestScenario<R> cirs : matchingScenarios) {
            cirs.onRequest(request);
            if (cirs.isFinished()) {
                LOG.trace("CAP incoming scenario finished: {}", cirs.getName());
                call.getCapIncomingRequestScenarios().remove(cirs);
            }
            if (call.getImscfState() == ImscfCallLifeCycleState.FINISHED) {
                LOG.debug("Call terminated, skipping further scenarios.");
                break;
            }
        }
    }

    // CS call gsmSCF methods

    @Override
    public void onActivityTestResponse(ActivityTestResponse arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("ActivityTest response for {}", call);
            findMatchingCapScenarios(call, arg0.getInvokeId()).forEachOrdered(cs -> {
                call.getCapOutgoingRequestScenarios().remove(cs);
                cs.onReturnResult(arg0);
            });
        }
    }

    @Override
    public void onApplyChargingReportRequest(ApplyChargingReportRequest arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("ApplyChargingReport for {}", call);
            handleIncomingRequest(call, arg0);
        }
    }

    @Override
    public void onCallInformationReportRequest(CallInformationReportRequest arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("CallInformationReport for {}", call);
            handleIncomingRequest(call, arg0);
        }
    }

    @Override
    public void onDisconnectLegResponse(DisconnectLegResponse arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("DisconnectLeg response for {}", call);
            findMatchingCapScenarios(call, arg0.getInvokeId()).forEachOrdered(cs -> {
                call.getCapOutgoingRequestScenarios().remove(cs);
                cs.onReturnResult(arg0);
            });
        }
    }

    @Override
    public void onEventReportBCSMRequest(EventReportBCSMRequest arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("EventReportBCSM for {}", call);
            handleIncomingRequest(call, arg0);
        }
    }

    @Override
    public abstract void onInitialDPRequest(InitialDPRequest arg0);

    @Override
    public void onInitiateCallAttemptResponse(InitiateCallAttemptResponse arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("InitiateCallAttempt response for {}", call);
            findMatchingCapScenarios(call, arg0.getInvokeId()).forEachOrdered(cs -> {
                call.getCapOutgoingRequestScenarios().remove(cs);
                cs.onReturnResult(arg0);
            });
        }
    }

    @Override
    public void onMoveLegResponse(MoveLegResponse arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("MoveLeg response for {}", call);
            findMatchingCapScenarios(call, arg0.getInvokeId()).forEachOrdered(cs -> {
                call.getCapOutgoingRequestScenarios().remove(cs);
                cs.onReturnResult(arg0);
            });
        }
    }

    @Override
    public void onPromptAndCollectUserInformationResponse(PromptAndCollectUserInformationResponse arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("PromptAndCollectUserInformation response for {}", call);
            findMatchingCapScenarios(call, arg0.getInvokeId()).forEachOrdered(cs -> {
                call.getCapOutgoingRequestScenarios().remove(cs);
                cs.onReturnResult(arg0);
            });
        }
    }

    @Override
    public void onSpecializedResourceReportRequest(SpecializedResourceReportRequest arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("SpecializedResourceReport for {}", call);
            handleIncomingRequest(call, arg0);
        }
    }

    @Override
    public void onSplitLegResponse(SplitLegResponse arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("SplitLeg response for {}", call);
            findMatchingCapScenarios(call, arg0.getInvokeId()).forEachOrdered(cs -> {
                call.getCapOutgoingRequestScenarios().remove(cs);
                cs.onReturnResult(arg0);
            });
        }
    }

    // SMS gsmSCF methods

    @Override
    public void onEventReportSMSRequest(EventReportSMSRequest arg0) {
        try (CAPCall<?> call = getCapCall(arg0.getCAPDialog())) {
            LOG.debug("EventReportSMS for {}", call);
            handleIncomingRequest(call, arg0);
        }
    }

    @Override
    public abstract void onInitialDPSMSRequest(InitialDPSMSRequest arg0);

    // Common CAP service methods

    @Override
    public void onCAPMessage(CAPMessage arg0) {
        // No action by default, handled in the more specific methods
        LOG.trace("onCAPMessage not implemented");
    }

    @Override
    public void onErrorComponent(CAPDialog arg0, Long arg1, CAPErrorMessage arg2) {
        try (CAPCall<?> call = getCapCall(arg0)) {
            LOG.debug("errorComponent {} (error: {}) for {}", arg1, arg2, call);
            findMatchingCapScenarios(call, arg1).forEachOrdered(cs -> {
                call.getCapOutgoingRequestScenarios().remove(cs);
                cs.onErrorComponent(arg2);
            });
        }
    }

    @Override
    public void onInvokeTimeout(CAPDialog arg0, Long arg1) {
        try (CAPCall<?> call = getCapCall(arg0)) {
            LOG.debug("invokeTimeout {} for {}", arg1, call);
            findMatchingCapScenarios(call, arg1).forEachOrdered(cs -> {
                call.getCapOutgoingRequestScenarios().remove(cs);
                cs.onInvokeTimeout();
            });
        }
    }

    @Override
    public void onRejectComponent(CAPDialog arg0, Long arg1, Problem arg2, boolean arg3) {
        try (CAPCall<?> call = getCapCall(arg0)) {
            LOG.debug("rejectComponent {} (problem: {}, {}) for {}", arg1, arg2, arg3, call);
            if (arg3) {
                LOG.trace("Ignoring local originated reject");
            } else {
                findMatchingCapScenarios(call, arg1).forEachOrdered(cs -> {
                    call.getCapOutgoingRequestScenarios().remove(cs);
                    cs.onRejectComponent(arg2);
                });
            }
        }
    }

    // Common CAP dialog methods

    // @Override
    // public void onDialogProviderAbort(CAPDialog arg0, PAbortCauseType arg1) {
    // }

    @Override
    public void onDialogRelease(CAPDialog arg0) {
        try (CAPCall<?> call = getCapCall(arg0)) {
            LOG.debug("Dialog release for {}", call);
            // since the dialog was released, CAP scenarios are now useless
            call.getCapIncomingRequestScenarios().clear();
            call.getCapOutgoingRequestScenarios().clear();
        }
    }

    @Override
    public void onDialogTimeout(CAPDialog arg0) {
        try(CAPCall<?> call = getCapCallUnlocked(arg0)) {
            if (getModuleConfiguration().getGeneralProperties().getActivityTestIntervalSec() > 0) {
                // if activityTest interval > 0, this callback will be fired accordingly and we should send AT.
                LOG.debug("Dialog timeout for {}", call);
                sendActivityTest(call);
            } else {
                // If it is <= 0, the callback should never have fired
                LOG.warn("Dialog timeout with activityTest turned off for {}", call);
            }
		}
    }

    // @Override
    // public void onDialogUserAbort(CAPDialog arg0, CAPGeneralAbortReason arg1, CAPUserAbortReason arg2) {
    // }

    private void sendActivityTest(CAPCall<?> call) {
        try {
            CAPDialogCircuitSwitchedCall dialog = (CAPDialogCircuitSwitchedCall) call.getCapDialog();
            if (dialog.getState() != CAPDialogState.Active) {
                LOG.debug("Cannot send activityTest in '{}' dialog state", dialog.getState());
                return;
            }

            LOG.debug("Sending activityTest");
            final Long invokeId = dialog.addActivityTestRequest();
            dialog.send();
            call.getCapOutgoingRequestScenarios().add(new CapScenarioActivityTest() {

                @Override
                public void onFailureTimeout() {
                    LOG.warn("Failed activityTest in call {}", call.getImscfCallId());
                    dialog.release();
                    onDialogFailure(dialog);
                }

                @Override
                public Long getInvokeId() {
                    return invokeId;
                }

                @Override
                public void onSuccess(ActivityTestResponse response) {
                    LOG.debug("ActivityTest success in call {}", call.getImscfCallId());
                    // OK, NOOP
                }
            });
        } catch (CAPException e) {
            LOG.warn("Error sending activityTest.", e);
        }
    }

    public Map<String, ConnectToResourceArg> getConnectToResourceArgsForMrfAliases() {
        return connectToResourceArgsForMrfAliases;
    }

    protected void handleUnroutableCSCall(CAPCSCall call) {
        LOG.debug("CS call is not routable to any AS, trying default handling");
        call.setCsCapState(CAPState.TERMINATED);

        CallFactoryBean cf = CallContext.getCallFactory();

        InviteErrorActionType action = null;
        for (InviteErrorMatcher handler : inviteErrorMatchers) {
            if (handler.matches(0, call.getIdp().getServiceKey())) {
                LOG.trace("match: {}", handler);
                action = handler.getAction();
                break;
            }
        }
        if (action == null) {
            LOG.warn("Couldn't find default inviteErrorHandler for unroutable call with sk {}. Sending abort.", call
                    .getIdp().getServiceKey());
            // this is a configuration error that should have not have passed validation
            try {
                call.getCapDialog().abort(CAPUserAbortReason.abnormal_processing);
            } catch (CAPException e) {
                LOG.warn("Failed to send abort for unroutable call", e);
            }
            cf.deleteCall(call);
            return;
        }

        try {
            switch (action.getAction()) {
            case CONTINUE:
                LOG.debug("Action to perform: continue");
                ContinueUtil.continueCall(call.getCapDialog(), true);
                break;
            case RELEASE:
                ReleaseCauseType rct = action.getReleaseCause();
                LOG.debug("Action to perform: release with cause {}", rct);
                Integer causeValue = ReleaseCauseMapper.releaseCauseToCauseValue(rct);
                if (causeValue == null) {
                    LOG.warn(
                            "Requested cause value {} cannot be mapped to jss7 release cause. Using NORMAL_UNSPECIFIED instead.",
                            rct);
                    causeValue = CauseIndicators._CV_NORMAL_UNSPECIFIED;
                }
                ReleaseCallUtil.releaseCall(call, causeValue, true);
                break;
            default:
                throw new IllegalStateException("Invalid configuration: invalid initialErrorHandler action type: "
                        + action.getAction());
            }
        } catch (CAPException e) {
            LOG.warn("Failed to send CAP message for call", e);
            try {
                call.getCapDialog().abort(CAPUserAbortReason.abnormal_processing);
            } catch (CAPException ex) {
                LOG.warn("Failed to send abort for unroutable call", ex);
            }
        }

        cf.deleteCall(call);
        LOG.trace("Initial invite error handling finished");
    }

    /** Logging utility class for printing the names of the active cap scenarios. */
    private static class CapScenarioListPrinter {
        private final List<? extends CapIncomingRequestScenario<?>> list;

        CapScenarioListPrinter(List<? extends CapIncomingRequestScenario<?>> list) {
            this.list = list;
        }

        @Override
        public String toString() {
            return list.stream().map(CapIncomingRequestScenario::getName).collect(Collectors.joining(", "));
        }
    }
}
