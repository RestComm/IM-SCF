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

import org.mobicents.protocols.ss7.m3ua.As;
import org.mobicents.protocols.ss7.m3ua.Asp;
import org.mobicents.protocols.ss7.m3ua.M3UAManagement;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for starting and shutting down the SS7 stack.
 */
@Startup
@Singleton
@DependsOn(value = { "ConfigBean", "ELRouterBean" })
public class SLStackRunner {
    private static Logger logger = LoggerFactory.getLogger(SLStackRunner.class);
    private static final String SCTP_LINK_MANAGER_MBEAN_NAME = ConfigBean.SL_MBEAN_DOMAIN + ":type=SctpLinkManager";
    private static final String SCTP_LINK_STATUS_NOTIFIER_MBEAN_NAME = ConfigBean.SL_MBEAN_DOMAIN
            + ":type=SctpLinkStatus";

    ImscfSigtranStack stack;

    @EJB
    ConfigBean configBean;

    @EJB
    ELRouterBean elRouterBean;

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

        // Shut down overload utility
        SlOverloadUtil.destroy();

        // Remove stuck objects from threadlocals
        String javolutionClassPattern = "javolution\\..*";
        String gsonClassPattern = "com\\.google\\.gson\\.Gson";
        ThreadLocalCleaner.cleanThreadLocals(javolutionClassPattern, gsonClassPattern);

        logger.info("SL stopped!");
    }
}
