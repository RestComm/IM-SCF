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

import org.restcomm.imscf.common.config.DiameterGatewayModuleType;
import org.restcomm.imscf.common.config.DiameterRoutingConfigType;
import org.restcomm.imscf.common.config.OverloadProtectionType;
import org.restcomm.imscf.common.config.SctpAssociationLocalSideType;
import org.restcomm.imscf.common.config.SctpAssociationRemoteSideProfileType;
import org.restcomm.imscf.common.config.SctpAssociationRemoteSideWrapperType;
import org.restcomm.imscf.common.config.SignalingLayerServerType;
import org.restcomm.imscf.sl.config.ImscfSigtranStack;
import org.restcomm.imscf.sl.config.Ss7StackBuilder;
import org.restcomm.imscf.sl.config.Ss7StackParameters;
import org.restcomm.imscf.sl.mgmt.sctp.ImscfManagedSctpMultiManagementWrapper;
import org.restcomm.imscf.sl.mgmt.sctp.SctpLinkManager;
import org.restcomm.imscf.sl.config.ConfigBean;
import org.restcomm.imscf.sl.diameter.ELDiameterRouterBean;
import org.restcomm.imscf.sl.diameter.SLELDiameterRouter;
import org.restcomm.imscf.sl.diameter.config.DiameterGWConfiguration;
import org.restcomm.imscf.sl.diameter.listener.SLDiameterCCASessionListener;
import org.restcomm.imscf.sl.diameter.listener.SLDiameterCCAStateChangeListener;
import org.restcomm.imscf.sl.history.SlCallHistoryStore;
import org.restcomm.imscf.sl.overload.SlOverloadUtil;
import org.restcomm.imscf.sl.statistics.SlStatistics;
import org.restcomm.imscf.common.LwcTags;
import org.restcomm.imscf.common.LwcommConfigurator;
import org.restcomm.imscf.common.util.ThreadLocalCleaner;
import org.restcomm.imscf.common.util.overload.OverloadProtector;
import org.restcomm.imscf.common.util.overload.OverloadProtectorParameters;
import org.restcomm.imscf.common.util.overload.OverloadProtectorParameters.NonHeapOverloadCheckPolicy;
import org.restcomm.imscf.common.lwcomm.service.LwCommService;
import org.restcomm.imscf.common.lwcomm.service.LwCommService.AcceptMode;
import org.restcomm.imscf.common.lwcomm.service.LwCommServiceProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.DisconnectCause;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Network;
import org.jdiameter.api.Stack;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.common.impl.app.cca.CCASessionFactoryImpl;
import org.jdiameter.server.impl.StackImpl;
import org.mobicents.protocols.ss7.m3ua.As;
import org.mobicents.protocols.ss7.m3ua.Asp;
import org.mobicents.protocols.ss7.m3ua.M3UAManagement;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for starting and shutting down the SS7 and Diameter stack.
 */
@Startup
@Singleton
@DependsOn(value = { "ConfigBean", "ELRouterBean", "ELDiameterRouterBean" })
public class SLStackRunner {
    private static Logger logger = LoggerFactory.getLogger(SLStackRunner.class);
    private static final String SCTP_LINK_MANAGER_MBEAN_NAME = ConfigBean.SL_MBEAN_DOMAIN + ":type=SctpLinkManager";
    private static final String SCTP_LINK_STATUS_NOTIFIER_MBEAN_NAME = ConfigBean.SL_MBEAN_DOMAIN
            + ":type=SctpLinkStatus";

    private ConcurrentMap<String, CCASessionFactoryImpl> ccaSessionFactories;

    ImscfSigtranStack stack;
    List<Stack> jdiameterStacks = new ArrayList<>();

    @EJB
    ConfigBean configBean;

    @EJB
    ELRouterBean elRouterBean;

    @EJB
    ELDiameterRouterBean eldiameterRouterBean;

    @PostConstruct
    public void onApplicationStart() {
        logger.info("SL starting...");

        SlCallHistoryStore callHistoryStore = new SlCallHistoryStore();

        LwcommMessageReceiver mr = new LwcommMessageReceiver(callHistoryStore);
        LwCommService lwc = LwcommConfigurator.initLwComm(configBean.getConfig(), ConfigBean.SERVER_NAME,
                ConfigBean.SL_MBEAN_DOMAIN, mr);

        initOverloadProtection();

        if (configBean.isSigtranStackNeeded()) {
            logger.info("Initializing for SIGTRAN");
            final String persistDir = new File(ConfigBean.CONFIG_DIR, "tmp").getAbsolutePath();
            // set properties needed by the jss7 stack
            System.setProperty("sctp.persist.dir", persistDir);
            System.setProperty("m3ua.persist.dir", persistDir);
            System.setProperty("sccpmanagement.persist.dir", persistDir);
            System.setProperty("sccpresource.persist.dir", persistDir);
            System.setProperty("sccprouter.persist.dir", persistDir);
            try {
                ImscfSigtranStack stack = Ss7StackBuilder.buildImscfSigtranStack(configBean.getConfig(),
                        Ss7StackParameters.createDefaultParameters(), ConfigBean.SERVER_NAME);
                M3UAManagement m3ua = stack.getM3uaManagement();
                SlStatistics.initializeStatistics(stack);

                SctpLinkManager.getInstance().registerLinkManager(SCTP_LINK_MANAGER_MBEAN_NAME,
                        SCTP_LINK_STATUS_NOTIFIER_MBEAN_NAME);
                SctpLinkManager.getInstance().registerManagement(stack.getSctpManagement());

                for (As as : m3ua.getAppServers()) {
                    for (Asp asp : as.getAspList()) {
                        logger.debug("Starting ASP: {} of AS {} ...", asp.getName(), as.getName());
                        m3ua.startAsp(asp.getName());
                    }
                }

                SccpProvider sccpProvider = stack.getSccpStack().getSccpProvider();

                // TODO: correctly handle multiple listeners (1/ssn).
                // for (SccpLocalProfileType prof : stack.getConfig().getSccpProfiles().getSccpLocalProfile()) {
                // for (LocalSubSystemPointCodeType ss : prof.getLocalSubSystems()) {
                // SLSccpListener listener = new SLSccpListener(ss.getSubSystemNumber(), sccpProvider, elRouterBean);
                // listener.setLwc(lwc);
                // }
                // }

                String otherSlNode = configBean.getConfig().getServers().getSignalingLayerServers().stream()
                        .filter(s -> !(s.getName().equals(ConfigBean.SERVER_NAME))).findFirst()
                        .map(SignalingLayerServerType::getName).orElse(null);

                // handle incoming sccp -> lwcomm
                SLSccpListener sccpListener = new SLSccpListener(elRouterBean, callHistoryStore, lwc, otherSlNode);
                sccpProvider.registerSccpListener(146 /* CAP */, sccpListener);

                // handle incoming lwcomm -> sccp
                mr.setupForSigtran(sccpProvider, stack.getServer(), elRouterBean, lwc, sccpListener);

                // Setup overload utility
                SlOverloadUtil.configure(sccpProvider);

                this.stack = stack;
            } catch (Exception e) {
                throw new RuntimeException("Failed to start SS7 stack", e);
            }
        } else {
            logger.info("SIGTRAN not configured");
        }

        // Build diameter stack
        if (configBean.isDiameterStackNeeded()) {
            logger.info("Initializing for Diameter");
            mr.setupForDiameter(eldiameterRouterBean);

            SignalingLayerServerType actSignalingServer = findActualSignalingServer();

            for (DiameterGatewayModuleType module : configBean.getConfig().getDiameterGatewayModules()) {
                logger.info("Starting jdiameter stack for module {}", module.getName());
                try {
                    // Find diameter module and SCTP association local side pairs
                    for (SctpAssociationLocalSideType sctpAssociationLocal : actSignalingServer
                            .getSctpAssociationLocalSides()) {

                        if (sctpAssociationLocal.getSctpAssociationRemoteSideProfile() != null
                                && module.getSctpAssociationRemoteSideProfile().getName()
                                        .equals(sctpAssociationLocal.getSctpAssociationRemoteSideProfile().getName())) {
                            SctpAssociationRemoteSideProfileType actSctpAssociationRemoteSideProfile = findActualSctpAssociationRemoteSideProfile(sctpAssociationLocal);

                            // Find SCTP association remote side
                            if (actSctpAssociationRemoteSideProfile.getSctpAssociationRemoteSideWrapper() != null) {
                                for (SctpAssociationRemoteSideWrapperType remoteSide : actSctpAssociationRemoteSideProfile
                                        .getSctpAssociationRemoteSideWrapper()) {
                                    Stack diamStack = new StackImpl();
                                    ISessionFactory factory;

                                    DiameterGWConfiguration diamConf;
                                    diamConf = new DiameterGWConfiguration(module, actSignalingServer,
                                            sctpAssociationLocal, remoteSide.getSctpAssociationRemoteSide());

                                    factory = (ISessionFactory) diamStack.init(diamConf.getDiameterConfig());

                                    ApplicationId aid = ApplicationId.createByAuthAppId(module
                                            .getDestinationApplicationId().getVendorId(), module
                                            .getDestinationApplicationId().getAuthApplId());

                                    String stackName = ConfigBean.SERVER_NAME
                                            + remoteSide.getSctpAssociationRemoteSide().getName();

                                    // Find service context ids
                                    List<String> serviceContextIds = new ArrayList<String>();
                                    serviceContextIds.addAll(findServiceContextIdsForDiameterStack(module));

                                    int ccaSessionTimeout = module.getSessionTimeoutSec();

                                    diamStack.unwrap(Network.class).addNetworkReqListener(
                                            new SLDiameterReqListener(getServerCCASessionFactory(factory, stackName,
                                                    serviceContextIds, ccaSessionTimeout, eldiameterRouterBean, lwc,
                                                    callHistoryStore), aid), aid);

                                    diamStack.start();

                                    this.jdiameterStacks.add(diamStack);
                                    logger.info("Done.");

                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to start diameter stack for module {}", module.getName(), e);
                }
            }
        } else {
            logger.info("Diameter not configured");
        }

        logger.info("SL started!");
    }

    // Find SL server
    public SignalingLayerServerType findActualSignalingServer() {
        SignalingLayerServerType actSignalingServer = new SignalingLayerServerType();
        for (SignalingLayerServerType signalingServers : configBean.getConfig().getServers().getSignalingLayerServers()) {
            if (signalingServers.getName().equals(ConfigBean.SERVER_NAME)) {
                actSignalingServer = signalingServers;
                break;
            }
        }
        return actSignalingServer;
    }

    // Find SCTP association remote side profile
    public SctpAssociationRemoteSideProfileType findActualSctpAssociationRemoteSideProfile(
            SctpAssociationLocalSideType sctpAssociationLocal) {
        SctpAssociationRemoteSideProfileType actSctpAssociationRemoteSideProfile = new SctpAssociationRemoteSideProfileType();
        for (SctpAssociationRemoteSideProfileType sctpAssociationRemoteSideProfile : configBean.getConfig()
                .getSctpAssociationRemoteSideProfiles()) {
            if (sctpAssociationLocal.getSctpAssociationRemoteSideProfile().getName()
                    .equals(sctpAssociationRemoteSideProfile.getName())) {
                actSctpAssociationRemoteSideProfile = sctpAssociationRemoteSideProfile;
                break;
            }
        }
        return actSctpAssociationRemoteSideProfile;
    }

    // Find serviceContextIds
    public List<String> findServiceContextIdsForDiameterStack(DiameterGatewayModuleType module) {
        List<String> serviceContextIds = new ArrayList<String>();

        for (DiameterRoutingConfigType diameterRouting : configBean.getConfig().getDiameterRouting()) {
            if (diameterRouting.getDiameterGatewayModule().getName().equals(module.getName())) {

                StringTokenizer st = new StringTokenizer(diameterRouting.getServiceContextIds(), ",");
                while (st.hasMoreElements()) {
                    serviceContextIds.add(st.nextToken());
                }
            }
        }

        return serviceContextIds;
    }

    // Get CCA session Factory
    private CCASessionFactoryImpl getServerCCASessionFactory(ISessionFactory factory, String whichStack,
            List<String> serviceContextIds, int ccaSessionTimeout, SLELDiameterRouter<SlElMappingData> elRouterBean,
            LwCommService lwc, SlCallHistoryStore callHistoryStore) {
        if (ccaSessionFactories == null) {
            ccaSessionFactories = new ConcurrentHashMap<String, CCASessionFactoryImpl>();
            CCASessionFactoryImpl ccaSessionFactory = new CCASessionFactoryImpl(factory);
            ccaSessionFactory.setServerSessionListener(new SLDiameterCCASessionListener(serviceContextIds,
                    ccaSessionTimeout, elRouterBean, lwc, callHistoryStore));
            ccaSessionFactory.setStateListener(new SLDiameterCCAStateChangeListener());
            ccaSessionFactories.put(whichStack, ccaSessionFactory);
        } else if (ccaSessionFactories.get(whichStack) == null) {
            CCASessionFactoryImpl ccaSessionFactory = new CCASessionFactoryImpl(factory);
            ccaSessionFactory.setServerSessionListener(new SLDiameterCCASessionListener(serviceContextIds,
                    ccaSessionTimeout, elRouterBean, lwc, callHistoryStore));
            ccaSessionFactory.setStateListener(new SLDiameterCCAStateChangeListener());
            ccaSessionFactories.put(whichStack, ccaSessionFactory);
        }
        return ccaSessionFactories.get(whichStack);
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

    @PreDestroy
    private void onApplicationStop() {
        logger.info("SL stopping...");

        OverloadProtector.shutdown();

        logger.info("Stopping LWCOMM stack...");
        LwCommServiceProvider.getService().shutdown();

        if (stack != null) {
            logger.info("Stopping SIGTRAN stack...");

            logger.debug("Stopping SCCP stack...");
            stack.getSccpStack().stop();

            logger.debug("Stopping M3UA stack...");
            try {
                stack.getM3uaManagement().stop();
            } catch (Exception e) {
                logger.error("Error stopping M3UAManagement", e);
            }

            logger.debug("Stopping SCTP stack...");
            SctpLinkManager.getInstance().stopManagements(
                    m -> ImscfManagedSctpMultiManagementWrapper.class.isAssignableFrom(m.getClass()));
            SctpLinkManager.getInstance().unregisterLinkManager();
            try {
                stack.getSctpManagement().stop();
            } catch (Exception e) {
                logger.error("Error stopping SCTP", e);
            }
            SlStatistics.shutdownStatistics(stack);
            stack = null;
            logger.info("SIGTRAN stack is down.");
        }

        if (!jdiameterStacks.isEmpty()) {
            logger.info("Stopping diameter stacks...");
            for (Stack stack : jdiameterStacks) {
                if (stack.isActive()) {
                    try {
                        stack.stop(10, TimeUnit.SECONDS, DisconnectCause.REBOOTING);
                    } catch (IllegalDiameterStateException | InternalException e) {
                        logger.error("Error stopping jdiameter stack!", e);
                    }
                }
                stack.destroy();
            }
            jdiameterStacks.clear();
            logger.info("Diameter stacks are down.");
        }

        // Shut down overload utility
        SlOverloadUtil.destroy();

        // Remove stuck objects from threadlocals
        String javolutionClassPattern = "javolution\\..*";
        String gsonClassPattern = "com\\.google\\.gson\\.Gson";
        ThreadLocalCleaner.cleanThreadLocals(javolutionClassPattern, gsonClassPattern);

        logger.info("SL stopped!");
    }
}
