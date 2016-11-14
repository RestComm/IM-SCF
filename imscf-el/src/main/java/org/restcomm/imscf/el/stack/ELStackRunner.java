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

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.ImscfConfigType.Sccp;
import org.restcomm.imscf.common.config.CapModuleType;
import org.restcomm.imscf.common.config.DiameterGatewayModuleType;
import org.restcomm.imscf.common.config.ExecutionLayerServerType;
import org.restcomm.imscf.common.config.GtAddressType;
import org.restcomm.imscf.common.config.MapModuleType;
import org.restcomm.imscf.common.config.OverloadProtectionType;
import org.restcomm.imscf.common.config.SubSystemType;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.call.impl.ManagedScheduledTimerService;
import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.converter.CapSipConverterImpl;
import org.restcomm.imscf.el.cap.CAPStackImplImscfWrapper;
import org.restcomm.imscf.el.cap.CAPTimerDefault;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.config.ConfigurationChangeListener;
import org.restcomm.imscf.el.diameter.DiameterGWModuleBase;
import org.restcomm.imscf.el.diameter.DiameterModule;
import org.restcomm.imscf.el.map.MAPModule;
import org.restcomm.imscf.el.map.MAPModuleImpl;
import org.restcomm.imscf.el.map.MAPStackImplImscfWrapper;
import org.restcomm.imscf.el.modules.Module;
import org.restcomm.imscf.el.modules.ModuleInitializationException;
import org.restcomm.imscf.el.modules.ModuleStore;
import org.restcomm.imscf.el.modules.routing.ModuleRouter;
import org.restcomm.imscf.el.sccp.SccpModule;
import org.restcomm.imscf.el.sccp.SccpModuleImpl;
import org.restcomm.imscf.el.sip.failover.SipAsLoadBalancer;
import org.restcomm.imscf.el.sip.routing.SipAsRouter;
import org.restcomm.imscf.el.sip.servlets.SipServletListenerImpl;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;
import org.restcomm.imscf.el.statistics.ElStatistics;
import org.restcomm.imscf.el.statistics.TcapStatisticsListener;
import org.restcomm.imscf.common.LwcTags;
import org.restcomm.imscf.common.LwcommConfigurator;
import org.restcomm.imscf.common.ss7.tcap.TCAPStackImplImscfWrapper;
import org.restcomm.imscf.util.MBeanHelper;
import org.restcomm.imscf.common.util.ThreadLocalCleaner;
import org.restcomm.imscf.common.util.overload.OverloadProtector;
import org.restcomm.imscf.common.util.overload.OverloadProtectorParameters;
import org.restcomm.imscf.common.util.overload.OverloadProtectorParameters.NonHeapOverloadCheckPolicy;
import org.restcomm.imscf.common.lwcomm.service.LwCommService;
import org.restcomm.imscf.common.lwcomm.service.LwCommService.AcceptMode;
import org.restcomm.imscf.common.lwcomm.service.LwCommServiceProvider;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.mobicents.protocols.ss7.cap.CAPStackImpl;
import org.mobicents.protocols.ss7.cap.api.CAPStack;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPServiceCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.sms.CAPServiceSms;
import org.mobicents.protocols.ss7.map.api.MAPStack;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobility;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSms;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.tcap.TCAPStackImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton bean for starting/stopping and accessing the SS7 stack.
 */
@Startup
@Singleton
@DependsOn(value = { "ConfigBean", "SLELRouterBean" })
@SuppressWarnings("PMD.GodClass")
public class ELStackRunner implements ConfigurationChangeListener {

    private static Logger logger = LoggerFactory.getLogger(ELStackRunner.class);

    private SUAImpl sua;
    private DiameterImpl diam;

    // SSN -> TCAP+CAP+MAP
    private Map<Integer, StackTree> stacks = new HashMap<>();

    @EJB
    ConfigBean configBean;

    @EJB
    SLELRouterBean slRouterBean;

    @EJB
    CallStore callStoreBean;

    @EJB
    CallFactoryBean callFactoryBean;

    @Resource
    TimerService timerService;

    @Resource
    javax.servlet.sip.SipFactory sipFactory;

    @Resource
    javax.servlet.sip.SipSessionsUtil sipSessionsUtil;

    @Resource
    javax.servlet.sip.TimerService sipTimerService;

    public SccpProvider getSccpProvider() {
        return sua;
    }

    public List<TCAPStack> getTcapStacks() {
        return Collections.unmodifiableList(stacks.values().stream().map(s -> s.tcap).filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    public List<CAPStack> getCapStacks() {
        return Collections.unmodifiableList(stacks.values().stream().map(s -> s.cap).filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    public List<MAPStack> getMapStacks() {
        return Collections.unmodifiableList(stacks.values().stream().map(s -> s.map).filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    @PostConstruct
    public void onApplicationStart() {
        try {
            logger.info("EL starting...");

            LwcommMessageReceiver lwcommListener = new LwcommMessageReceiver();
            initOverloadProtection();

            if (configBean.isSigtranStackNeeded()) {
                logger.info("Initializing SIGTRAN stack");
                initSUA(); // "sccp stack"
                lwcommListener.addModuleListener("SUA", sua); // SCCP listener

                initTCAP();

                if (configBean.isCAPUsed()) {
                    logger.info("Initializing CAP stack...");
                    initCAPStacks();
                } else {
                    logger.info("CAP not configured");
                }

                if (configBean.isMAPUsed()) {
                    logger.info("Initializing MAP stack...");
                    initMAPStacks();
                } else {
                    logger.info("MAP not configured");
                }

                SipServletResources.init(sipFactory, sipSessionsUtil, sipTimerService);

            } else {
                logger.info("SIGTRAN not configured");
            }

            if (configBean.isDiameterStackNeeded()) {
                logger.info("Initializing Diameter stack...");
                initDiameter();
                lwcommListener.addModuleListener("DiameterGW", diam);

                ManagedScheduledTimerService.initialize();
            } else {
                logger.info("Diameter not configured");
            }

            logger.info("Stacks initialized.");

            // register will trigger a first time configurationChanged callback, which is used for further
            // initialization of the modules and routers
            configBean.registerListener(this);

            // Start lwcomm only after all higher level modules are up and running.
            logger.info("Modules started, initializing LwComm...");
            initLwComm(lwcommListener);

            logger.info("EL started!");
        } catch (Exception e) {
            logger.error("Error starting EL stacks! {}", e.getMessage(), e);
        }
    }

    @Timeout
    private void startupTimeout() {
        logger.trace("Delayed startup task executing...");
        if (configBean.isSigtranStackNeeded()) {
            long seconds = 5;
            if (SipServletListenerImpl.waitForSipContext(seconds, TimeUnit.SECONDS)) {
                logger.debug("SIP servlet context OK, initializing SIP AS loadbalancer");
                SipAsLoadBalancer.getInstance().startHeartbeats();
            } else {
                logger.error("SIP servlet context still not initialized after {} seconds or the wait was interrupted",
                        seconds);
            }
        }
        logger.trace("EL running!");
    }

    @PreDestroy
    private void onApplicationStop() {
        for (StackTree s : stacks.values()) {
            if (s.map != null) {
                logger.info("Stopping MAP stack {}...", s.map.getName());
                s.map.stop();
            }
            if (s.cap != null) {
                logger.info("Stopping CAP stack {}...", s.cap.getName());
                s.cap.stop();
            }
            if (s.tcap != null) {
                logger.info("Stopping TCAP stack {}...", s.tcap.getName());
                s.tcap.stop();
            }
        }
        stacks.clear();

        // SccpProvider/SUAImpl has no stop method

        if (LwCommServiceProvider.isServiceInitialized()) {
            logger.info("Stopping LWCOMM stack...");
            LwCommServiceProvider.getService().shutdown();
        } else {
            logger.info("LWCOMM stack not running.");
        }

        if (ElStatistics.isInitialized()) {
            ElStatistics.shutdownStatistics();
        }

        OverloadProtector.shutdown();
        // Remove stuck in objects from threadlocals
        String javolutionClassPattern = "javolution\\..*";
        String gsonClassPattern = "com\\.google\\.gson\\.Gson";
        String callContextClassPattern = "org\\.restcomm\\.imscf\\.el\\.stack\\.CallContext";
        ThreadLocalCleaner.cleanThreadLocals(javolutionClassPattern, gsonClassPattern, callContextClassPattern);

        logger.info("EL stopped.");
    }

    @Override
    public void configurationChanged(ImscfConfigType newConfig) {
        logger.info("Initializing components after configuration change...");
        // update components that are allowed to change at runtime after a configuration reload

        if (ElStatistics.isInitialized()) {
            ElStatistics.shutdownStatistics();
        }
        ElStatistics.initializeStatistics(newConfig);

        if (configBean.isSigtranStackNeeded()) {

            logger.info("Initializing SCCP modules...");
            initSccpModules(); // depends on sua

            if (configBean.isCAPUsed()) {
                logger.info("Initializing CAP modules...");
                initCAPModules();
            } else {
                logger.info("CAP not configured");
            }

            if (configBean.isMAPUsed()) {
                logger.info("Initializing MAP modules...");
                initMAPModules();
            } else {
                logger.info("MAP not configured");
            }

            // both used by CAP and/or MAP
            logger.info("Initializing TCAP module router...");
            ModuleRouter.initialize(configBean.getConfig());

            logger.info("Initializing SIP AS router...");
            SipAsRouter.initialize(configBean.getConfig());

            logger.info("Initializing SIP AS loadbalancer...");
            // this will stop heartbeat messages of the previous instance as well if present
            SipAsLoadBalancer.initialize(configBean.getConfig());

        } else {
            logger.info("SIGTRAN not configured");
        }

        if (configBean.isDiameterStackNeeded()) {
            logger.info("Initializing for Diameter");

            logger.info("Initializing Diameter modules...");
            initDiameterModules();

            logger.info("Initializing Diameter HTTP AS router...");
            diam.initializeDiameterAsRoutes();
        } else {
            logger.info("Diameter not configured");
        }

        logger.info("Components initialized!");

        // TODO this is a workaround due to initialization order issues with EJB and SIP context ...
        TimerConfig cfg = new TimerConfig(null, false);
        timerService.createSingleActionTimer(1, cfg); // handled in @Timeout
    }

    private void initOverloadProtection() {
        logger.info("Initializing overload protection...");
        OverloadProtectorParameters p = new OverloadProtectorParameters();
        OverloadProtectionType opConfig = configBean.getConfig().getOverloadProtection();
        p.setCpuMeasurementWindow(opConfig.getCpuMeasurementWindow());
        p.setCpuOverloadThresholdPercent(opConfig.getCpuOverloadThresholdPercent());
        p.setDataCollectionPeriodSec(opConfig.getDataCollectionPeriodSec());
        p.setHeapOverloadThresholdPercent(opConfig.getHeapOverloadThresholdPercent());
        if (opConfig.getNonHeapOverloadThresholdAmount() != null) {
            p.setNonHeapOverloadCheckPolicy(NonHeapOverloadCheckPolicy.AMOUNT);
            p.setNonHeapOverloadThresholdAmount(opConfig.getNonHeapOverloadThresholdAmount().intValue());
        } else if (opConfig.getNonHeapOverloadThresholdPercent() != null) {
            p.setNonHeapOverloadCheckPolicy(NonHeapOverloadCheckPolicy.PERCENT);
            p.setNonHeapOverloadThresholdPercent(opConfig.getNonHeapOverloadThresholdPercent().intValue());
        } else {
            logger.error("Neither nonHeapOverloadThresholdAmount nor nonHeapOverloadThresholdPercent is set in configuration!");
        }

        OverloadProtector.init(p);
        OverloadProtector.getInstance().addListener((oldStatus, newStatus) -> {
            /*
             * TODO: maybe REJECT if memory is overloaded, but only DROP if CPU is overloaded, as that is more likely to
             * be temporary only, where a retransmit might be accepted later.
             */
            if (newStatus.isCpuOrHeapOverloaded()) {
                // reject new calls (default stays ACCEPT for in-session and management messages)
                LwCommServiceProvider.getService().setAcceptMode(AcceptMode.REJECT, LwcTags.NEW_SESSION);
            } else {
                // accept all
                LwCommServiceProvider.getService().setAcceptMode(AcceptMode.ACCEPT);
            }
            if (!oldStatus.isCpuOrHeapOverloaded() && newStatus.isCpuOrHeapOverloaded()) {
                logger.warn("System is overloaded! Current overload state is: {}", newStatus);
            } else if (oldStatus.isCpuOrHeapOverloaded() && !newStatus.isCpuOrHeapOverloaded()) {
                logger.warn("System is not overloaded. Current overload stat is: {}", newStatus);
            }
            if (!oldStatus.isNonHeapOverloaded() && newStatus.isNonHeapOverloaded()) {
                logger.warn("Metaspace is overloaded! Next redeploy might fail.");
            } else if (oldStatus.isNonHeapOverloaded() && !newStatus.isNonHeapOverloaded()) {
                logger.warn("Metaspace usage is normal.");
            }
        });
    }

    private void initLwComm(MessageReceiver listener) {
        LwCommService lwc = LwcommConfigurator.initLwComm(configBean.getConfig(), ConfigBean.SERVER_NAME,
                MBeanHelper.EL_MBEAN_DOMAIN, listener);
        if (lwc == null)
            throw new IllegalStateException("Failed to initialize LWCOMM stack!");
    }

    private void initSUA() {
        SUAImpl sua = new SUAImpl();
        sua.setSlRouter(slRouterBean);
        sua.setCallStore(callStoreBean);
        sua.setCallFactoryBean(callFactoryBean);
        sua.setConfigBean(configBean);
        this.sua = sua;
    }

    private SubSystemType getSubSystemData(GtAddressType localGt) {
        SubSystemType sys = new SubSystemType();
        sys.setSubSystemNumber(localGt.getSubSystemNumber());
        sys.setAlias(localGt.getAlias());
        return sys;
    }

    private void initTCAP() throws Exception {
        ImscfConfigType config = configBean.getConfig();

        ExecutionLayerServerType elServer = config.getServers().getExecutionLayerServers().stream()
                .filter(el -> ConfigBean.SERVER_NAME.equals(el.getName())).findFirst()
                .orElseThrow(() -> new RuntimeException("EL " + ConfigBean.SERVER_NAME + " not found in config!"));

        List<SubSystemType> subSystems = new ArrayList<SubSystemType>();
        Sccp sccp = config.getSccp();
        sccp.getSccpLocalProfile().getLocalSubSystems().forEach(ss -> subSystems.add(ss));
        sccp.getSccpLocalProfile().getLocalGtAddresses().forEach(gt -> subSystems.add(getSubSystemData(gt)));
        for (SubSystemType subSystem : subSystems) {
            initTCAP(elServer, subSystem);
        }
    }

    private void initTCAP(ExecutionLayerServerType elServer, SubSystemType subSystem) throws Exception {

        if (stacks.get(subSystem.getSubSystemNumber()) != null) {
            logger.debug("TCAP stack already created for SSN {}, skipping {}", subSystem.getSubSystemNumber(),
                    subSystem.getAlias());
            return;
        }

        logger.debug("Creating TCAP stack for {} with SSN {}", subSystem.getAlias(), subSystem.getSubSystemNumber());
        TCAPStackImpl tcapStack = new TCAPStackImplImscfWrapper(subSystem.getAlias(), getSccpProvider(),
                subSystem.getSubSystemNumber());
        // EL level TcapTransactionIdRange config is mandatory
        tcapStack.setDialogIdRangeStart(elServer.getTcapTransactionIdRange().getMinInclusive());
        tcapStack.setDialogIdRangeEnd(elServer.getTcapTransactionIdRange().getMaxInclusive());
        logger.debug("TCAP ID range set to [{}, {}]", tcapStack.getDialogIdRangeStart(),
                tcapStack.getDialogIdRangeEnd());
        int computedMax = Math.toIntExact(tcapStack.getDialogIdRangeEnd() - tcapStack.getDialogIdRangeStart() - 1);
        logger.trace("TCAP max dialog count {} derived from TCAP ID range", computedMax);
        tcapStack.setMaxDialogs(computedMax);

        logger.debug("TCAP properties: dialogIdleTimeout {}ms, invokeTimeout {}ms, maxDialogs {}",
                tcapStack.getDialogIdleTimeout(), tcapStack.getInvokeTimeout(), tcapStack.getMaxDialogs());

        tcapStack.getProvider().addTCListener(new EchoTCapListener(tcapStack.getName()));
        tcapStack.start();
        tcapStack.setStatisticsEnabled(true);
        tcapStack.setTCAPCounterEventsListener(new TcapStatisticsListener());
        StackTree s = new StackTree();
        s.tcap = tcapStack;
        stacks.put(subSystem.getSubSystemNumber(), s);

        logger.debug("Initialized TCAP Stack for " + tcapStack.getName() + "...");
    }

    private void initCAPStacks() throws Exception {
        for (Map.Entry<Integer, StackTree> entry : stacks.entrySet()) {
            int ssn = entry.getKey();
            StackTree s = entry.getValue();
            TCAPStack tcap = s.tcap;
            logger.debug("Initializing CAP stack for SSN {}...", ssn);
            CAPStackImplImscfWrapper capStack = new CAPStackImplImscfWrapper(ssn, tcap.getProvider());

            // using the maximum allowed values in the specs (in ms): 10s, 60s, 30m, 20s, 10s
            capStack.setCAPTimerDefault(new CAPTimerDefault(10 * 1000, 60 * 1000, 30 * 60 * 1000, 20 * 1000, 20 * 1000));

            // generic listener
            CapDialogLevelListener dialogListener = new CapDialogLevelListener();
            dialogListener.setCallFactory(callFactoryBean);
            dialogListener.setCallStore(callStoreBean);
            capStack.getCAPProvider().addCAPDialogListener(dialogListener);
            // CS call
            CAPServiceCircuitSwitchedCall capCSCallService = capStack.getCAPProvider()
                    .getCAPServiceCircuitSwitchedCall();
            capCSCallService.acivate();
            CAPCSCallListener csListener = new CAPCSCallListener();
            csListener.setCallFactory(callFactoryBean);
            csListener.setCallStore(callStoreBean);
            capCSCallService.addCAPServiceListener(csListener);

            // SMS
            CAPServiceSms capSMSService = capStack.getCAPProvider().getCAPServiceSms();
            capSMSService.acivate();
            CAPSMSListener smsListener = new CAPSMSListener();
            smsListener.setCallFactory(callFactoryBean);
            smsListener.setCallStore(callStoreBean);
            capSMSService.addCAPServiceListener(smsListener);
            // all done
            capStack.start();
            s.cap = capStack;
            logger.debug("Initialized CAP stack for {}.", tcap.getName());
        }
    }

    private void initMAPStacks() throws Exception {
        for (Map.Entry<Integer, StackTree> entry : stacks.entrySet()) {
            int ssn = entry.getKey();
            StackTree s = entry.getValue();
            TCAPStack tcap = s.tcap;
            logger.debug("Initializing MAP stack for SSN {}...", ssn);
            MAPStack mapStack = new MAPStackImplImscfWrapper(ssn, tcap.getProvider());
            // debug listener
            mapStack.getMAPProvider().addMAPDialogListener(new EchoMAPDialogListener());
            // setup ATI (listening for AnyTimeInterrogationResponse)
            MAPServiceMobility mapMobilityService = mapStack.getMAPProvider().getMAPServiceMobility();
            mapMobilityService.acivate();
            MAPMobilityListener mapMobilityListener = new MAPMobilityListener();
            mapMobilityListener.setCallFactory(callFactoryBean);
            mapMobilityListener.setCallStore(callStoreBean);
            mapMobilityService.addMAPServiceListener(mapMobilityListener);
            // setup SMS (listening for SendRoutingInfoForSMResponse)
            MAPServiceSms mapSMSService = mapStack.getMAPProvider().getMAPServiceSms();
            mapSMSService.acivate();
            mapSMSService.addMAPServiceListener(new MAPSmsListener());

            // TODO: service for subscriberInfoEnquiryContext (ProvideSubscriberInfo Request/Response) missing from
            // mobicents

            // TODO: service for MAPApplicationContextName.mmEventReportingContext (NoteMM-Event req/resp) missing from
            // mobicents

            // all done
            mapStack.start();
            s.map = mapStack;
            logger.debug("Initialized MAP stack for {}.", tcap.getName());
        }
    }

    private void initDiameter() {
        DiameterImpl diam = new DiameterImpl();

        diam.setCallStore(callStoreBean);
        diam.setCallFactoryBean(callFactoryBean);
        diam.setConfigBean(configBean);
        this.diam = diam;
    }

    private void initSccpModules() {
        ModuleStore.getSccpModules().clear();
        Optional.ofNullable(configBean.getConfig().getSccp()).ifPresent(sccp -> {
            SccpModule module = new SccpModuleImpl();
            /* fixed name, there's only one instance anyway */
            module.setName(ModuleStore.SCCP_MODULE_NAME);
            module.setSccpProvider(getSccpProvider());
            initModule(module, configBean.getConfig(), ModuleStore.getSccpModules());
        });
    }

    private void initCAPModules() {
        ModuleStore.getCapModules().clear();
        for (CapModuleType capModuleConfig : configBean.getConfig().getCapModules()) {
            int ssn = capModuleConfig.getLocalSsn() != null ? capModuleConfig.getLocalSsn().getSubSystemNumber()
                    : capModuleConfig.getLocalGt().getSubSystemNumber();
            CAPModule module = new CapSipConverterImpl();

            module.setName(capModuleConfig.getName());

            module.setCAPProvider(stacks.get(ssn).cap.getCAPProvider());
            module.setSccpProvider(getSccpProvider());
            initModule(module, configBean.getConfig(), ModuleStore.getCapModules());
        }
    }

    private void initMAPModules() {
        ModuleStore.getMapModules().clear();
        for (MapModuleType mapModuleConfig : configBean.getConfig().getMapModules()) {
            int ssn = mapModuleConfig.getLocalSsn() != null ? mapModuleConfig.getLocalSsn().getSubSystemNumber()
                    : mapModuleConfig.getLocalGt().getSubSystemNumber();

            MAPModule module = new MAPModuleImpl();
            module.setName(mapModuleConfig.getName());
            module.setMAPProvider(stacks.get(ssn).map.getMAPProvider());
            module.setSccpProvider(getSccpProvider());
            initModule(module, configBean.getConfig(), ModuleStore.getMapModules());
        }
    }

    private void initDiameterModules() {
        ModuleStore.getDiameterModules().clear();
        for (DiameterGatewayModuleType diameterModuleConfig : configBean.getConfig().getDiameterGatewayModules()) {
            DiameterModule module = new DiameterGWModuleBase();
            module.setName(diameterModuleConfig.getName());
            initModule(module, configBean.getConfig(), ModuleStore.getDiameterModules());
        }
    }

    private <M extends Module> void initModule(M m, ImscfConfigType config, Map<String, M> store) {
        String name = m.getName();
        try {
            m.initialize(config);
        } catch (ModuleInitializationException e) {
            logger.error("Failed to create module {}", name, e);
            return;
        }
        store.put(name, m);
        logger.info("Initialized module {}", name);
        m.start();
        logger.info("Started module {}", name);
    }
}

/**
 * Structure for a CAP-MAP stack pair sitting on top of the same TCAP stack.
 */
class StackTree {
    TCAPStack tcap;
    CAPStack cap;
    MAPStack map;
}
