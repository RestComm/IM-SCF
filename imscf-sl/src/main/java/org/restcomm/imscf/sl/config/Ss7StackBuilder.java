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
package org.restcomm.imscf.sl.config;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.GtAddressType;
import org.restcomm.imscf.common.config.GtRoutingType;
import org.restcomm.imscf.common.config.M3UaRouteType;
import org.restcomm.imscf.common.config.MessageDistributionType;
import org.restcomm.imscf.common.config.RemoteGtAddressType;
import org.restcomm.imscf.common.config.RemoteSubSystemPointCodeType;
import org.restcomm.imscf.common.config.SccpRemoteProfileType;
import org.restcomm.imscf.common.config.SctpAssociationLocalSideType;
import org.restcomm.imscf.common.config.SctpAssociationRemoteSideType;
import org.restcomm.imscf.common.config.SignalingLayerServerType;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.protocols.api.IpChannelType;
import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.sctp.ManagementImpl;
import org.restcomm.imscf.sl.sctp.multiclient.MultiManagementImpl;
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.m3ua.ExchangeType;
import org.mobicents.protocols.ss7.m3ua.Functionality;
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.mobicents.protocols.ss7.m3ua.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.m3ua.parameter.RoutingContext;
import org.mobicents.protocols.ss7.m3ua.parameter.TrafficModeType;
import org.mobicents.protocols.ss7.sccp.LoadSharingAlgorithm;
import org.mobicents.protocols.ss7.sccp.OriginationType;
import org.mobicents.protocols.ss7.sccp.RuleType;
import org.mobicents.protocols.ss7.sccp.SccpStack;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.sccp.impl.parameter.GlobalTitle0011Impl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.DefaultEncodingScheme;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of common algorithms, constants which could be useful for building Ss7 stacks.
 *
 * @author Balogh Gábor
 *
 */
@SuppressWarnings("PMD")
public final class Ss7StackBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ss7StackBuilder.class);
    private static final AtomicInteger REMOTE_SPC_SEQ = new AtomicInteger();
    private static final AtomicInteger REMOTE_SSN_SEQ = new AtomicInteger();
    private static final AtomicInteger SCCP_ROUTER_RULE_SEQ = new AtomicInteger();
    private static final AtomicInteger SCCP_ROUTER_ROUTING_ADDR_SEQ = new AtomicInteger();

    /**
     * To avoid being instantiated.
     */
    private Ss7StackBuilder() {

    }

    private static void initSctp(ImscfSigtranStack stack) throws Exception {
        LOGGER.debug("Creating SCTP Stack: {}", stack.getServerName());
        Management sctpStack;
        Ss7StackParameters parameters = stack.getStackParameters();
        SignalingLayerServerType server = stack.getServer();
        if (parameters.isSctpUseMultiManagement()) {
            sctpStack = new MultiManagementImpl(server.getName());
            ((MultiManagementImpl) sctpStack).setInitPayloadData(stack.getStackParameters().getInitPayloadData());
        } else {
            sctpStack = new ManagementImpl(server.getName());
        }
        stack.setSctpManagement(sctpStack);

        if (isSctpSingleThread(stack)) {
            sctpStack.setSingleThread(true);
        } else {
            sctpStack.setSingleThread(false);
            sctpStack.setWorkerThreads(stack.getServer().getSctpWorkerThreadCount());
        }
        sctpStack.start();
        sctpStack.setConnectDelay(parameters.getSctpConnectDelayMillis());
        sctpStack.removeAllResourses();
    }

    private static boolean isSctpSingleThread(ImscfSigtranStack stack) {
        return stack.getServer().getSctpWorkerThreadCount() <= 1;
    }

    private static void initM3UA(ImscfSigtranStack stack) throws Exception {
        LOGGER.info("Initializing M3UA Stack for {}...", stack.getServerName());
        M3UAManagementImpl m3Management = new M3UAManagementImpl(stack.getServerName(), "IM-SCF");
        stack.setM3uaManagement(m3Management);
        m3Management.setTransportManagement(stack.getSctpManagement());
        if (stack.getServer().getMtpDeliveryTransferMessageThreadCount() != null) {
            m3Management.setDeliveryMessageThreadCount(stack.getServer().getMtpDeliveryTransferMessageThreadCount());
        }
        m3Management.start();

        ParameterFactoryImpl factory = new ParameterFactoryImpl();
        SignalingLayerServerType server = stack.getServer();
        for (SctpAssociationLocalSideType localSide : server.getSctpAssociationLocalSides()) {
            if (localSide.getM3UaProfile() != null) {
                for (M3UaRouteType m3Route : localSide.getM3UaProfile().getM3UaRoutes()) {
                    String primaryAssocName = addAssociation(m3Route.getPrimaryAssociation(), localSide,
                            stack.getSctpManagement());
                    String secondaryAssocName = null;
                    if (m3Route.getSecondaryAssociation() != null) {
                        secondaryAssocName = addAssociation(m3Route.getSecondaryAssociation(), localSide,
                                stack.getSctpManagement());
                    }
                    RoutingContext rc = factory.createRoutingContext(new long[] { localSide.getM3UaProfile()
                            .getRoutingContext() });
                    TrafficModeType trafficModeType = factory.createTrafficModeType(TrafficModeType.Loadshare);

                    String asName = "AS_" + m3Route.getName();
                    String primaryAspName = "ASP_" + m3Route.getName() + "_PRIMARY";
                    String secondaryAspName = "ASP_" + m3Route.getName() + "_SECONDARY";

                    m3Management
                            .createAs(asName, Functionality.AS, ExchangeType.SE, null, rc, trafficModeType, 1, null);

                    if (m3Route.getPrimaryAssociation() != null) {
                        m3Management.createAspFactory(primaryAspName, primaryAssocName);
                        m3Management.assignAspToAs(asName, primaryAspName);
                    }
                    if (m3Route.getSecondaryAssociation() != null) {
                        m3Management.createAspFactory(secondaryAspName, secondaryAssocName);
                        m3Management.assignAspToAs(asName, secondaryAspName);
                    }

                    m3Management.addRoute((int) m3Route.getPointCode(), -1, -1, asName);

                }
            }
        }
    }

    private static String addAssociation(SctpAssociationRemoteSideType associationRemoteSide,
            SctpAssociationLocalSideType associationLocalSide, Management sctpManagement) throws Exception {
        String assocName = null;
        String[] extraHostIps = associationLocalSide.getSigtranIp2() == null ? null
                : new String[] { associationLocalSide.getSigtranIp2() };
        assocName = associationRemoteSide.getName();
        if (sctpManagement instanceof MultiManagementImpl && associationRemoteSide.getRemoteIp2() != null
                && !associationRemoteSide.getRemoteIp2().isEmpty()) {
            ((MultiManagementImpl) sctpManagement).addAssociation(associationLocalSide.getSigtranIp1(), associationLocalSide.getPort(),
                    associationRemoteSide.getRemoteIp1(), associationRemoteSide.getRemotePort(), assocName,
                    IpChannelType.SCTP, extraHostIps, associationRemoteSide.getRemoteIp2());
        } else {
            sctpManagement.addAssociation(associationLocalSide.getSigtranIp1(), associationLocalSide.getPort(),
                    associationRemoteSide.getRemoteIp1(), associationRemoteSide.getRemotePort(), assocName,
                    IpChannelType.SCTP, extraHostIps);
        }
        return assocName;
    }

    private static void configureSccpServiceAccesPoint(int pointCode, int localNetworkIndicator,
            SccpRemoteProfileType remoteProfile, ImscfSigtranStack stack) throws Exception {
        SccpStack sccp = stack.getSccpStack();
        LOGGER.debug("configureSccpServiceAccesPoint: localSide={} remoteProfile={}", pointCode, remoteProfile);
        sccp.getRouter().addMtp3ServiceAccessPoint(pointCode, // id
                1, // mtp3Id
                pointCode, // opc
                localNetworkIndicator, // ni
                0); //networkId
        int destIndex = 0;
        HashSet<String> configuredDestination = new HashSet<String>();
        for (RemoteSubSystemPointCodeType rsys : remoteProfile.getRemoteSubSystemPointCodeAddresses()) {
            if (configuredDestination.contains(rsys.getPointCode() + " - " + rsys.getSubSystemNumber())) {
                continue;
            }
            sccp.getRouter().addMtp3Destination(pointCode, // sapId
                    destIndex, // destId
                    rsys.getPointCode(), // firstDpc
                    rsys.getPointCode(), // lastDpc
                    0, // firstSls
                    255, // lastSls
                    255 // slsMask
                    );
            configuredDestination.add(rsys.getPointCode() + " - " + rsys.getSubSystemNumber());
            destIndex++;
            if (sccp.getSccpResource().getRemoteSpcByPC(rsys.getPointCode()) == null) {
                int remoteSpcId = REMOTE_SPC_SEQ.incrementAndGet();
                int remoteSsnId = REMOTE_SSN_SEQ.incrementAndGet();

                sccp.getSccpResource().addRemoteSpc(remoteSpcId, // remoteSpcId
                        rsys.getPointCode(), // remoteSpc
                        0, // remoteSpcFlag
                        255 // mask
                        );

                sccp.getSccpResource().addRemoteSsn(remoteSsnId, // remoteSsnid
                        rsys.getPointCode(), // remoteSpc
                        rsys.getSubSystemNumber(), // remoteSsn
                        0, // remoteSsnFlag
                        false // markProhibitedWhenSpcResuming TODO false == we do not send SST when resuming to SPCs.
                              // Check if it meets the business requirements.
                        );
            }
        }

        for (RemoteGtAddressType rsys : remoteProfile.getRemoteGtAddresses()) {
            if (configuredDestination.contains(rsys.getPointCode() + " - " + rsys.getSubSystemNumber())) {
                continue;
            }
            sccp.getRouter().addMtp3Destination(pointCode, // sapId
                    destIndex, // destId
                    rsys.getPointCode(), // firstDpc
                    rsys.getPointCode(), // lastDpc
                    0, // firstSls
                    255, // lastSls
                    255 // slsMask
                    );
            configuredDestination.add(rsys.getPointCode() + " - " + rsys.getSubSystemNumber());
            destIndex++;
            if (sccp.getSccpResource().getRemoteSpcByPC(rsys.getPointCode()) == null) {
                int remoteSpcId = REMOTE_SPC_SEQ.incrementAndGet();
                int remoteSsnId = REMOTE_SSN_SEQ.incrementAndGet();

                sccp.getSccpResource().addRemoteSpc(remoteSpcId, // remoteSpcId
                        rsys.getPointCode(), // remoteSpc
                        0, // remoteSpcFlag
                        255 // mask
                        );

                int remoteSsn = rsys.getSubSystemNumber();

                sccp.getSccpResource().addRemoteSsn(remoteSsnId, // remoteSsnid
                        rsys.getPointCode(), // remoteSpc
                        remoteSsn, // remoteSsn
                        0, // remoteSsnFlag
                        false // markProhibitedWhenSpcResuming TODO false == we do not send SST when resuming to SPCs.
                              // Check if it meets the business requirements.
                        );
            }
        }

    }

    private static void initSccp(ImscfSigtranStack stack) throws Exception {
        SignalingLayerServerType server = ImscfConfigUtil.getServerConfigByName(stack.getServerName(), stack.getConfig());
        LOGGER.debug("Initializing SCCP Stack for {}...", server.getName());

        SccpStackImpl sccpStack = new SccpStackImpl(server.getName());
        sccpStack.setMtp3UserPart(1, stack.getM3uaManagement());
        sccpStack.setRemoveSpc(stack.getConfig().getSccp().getSccpLocalProfile().isRemovePcWhenRouteOnGt());
        sccpStack.start();
        sccpStack.removeAllResourses();
        stack.setSccpStack(sccpStack);

        if (stack.getConfig().getSccp() == null) {
            LOGGER.warn("SCCP configuration for server: is not found.", server.getName());
            return;
        }

        configureSccpServiceAccesPoint(
                server.getPointCode(),
                ImscfConfigUtil.getNetworkIndicatorIntValue(stack.getConfig().getSccp().getSccpLocalProfile()
                        .getLocalNetworkIndicator()), stack.getConfig().getSccp().getSccpRemoteProfile(), stack);

        if (stack.getConfig().getSccp().getSccpRemoteProfile().getGtRouting() != null) {
            configureGtRouting(stack.getConfig().getSccp().getSccpRemoteProfile().getGtRouting(),
                    server.getPointCode(), stack);
        }
        LOGGER.debug("Initialized SCCP Stack ....");
    }

    public static ImscfSigtranStack buildImscfSigtranStack(ImscfConfigType imscfConfig, Ss7StackParameters params,
            String serverName) throws Exception {
        ImscfSigtranStack stack = new ImscfSigtranStack(params, imscfConfig, serverName);

        initSctp(stack);
        initM3UA(stack);
        initSccp(stack);
        return stack;
    }

    private static void configureGtRouting(GtRoutingType gtRouting, int localPointCode, ImscfSigtranStack stack)
            throws Exception {
        configureLocalOriginatedGttRules(gtRouting, localPointCode, stack);
        configureRemoteOriginatedGttRules(localPointCode, stack);
    }

    private static void configureLocalOriginatedGttRules(GtRoutingType gtRouting, int localPointCode,
            ImscfSigtranStack stack) throws Exception {
        for (RemoteGtAddressType remoteGt : stack.getConfig().getSccp().getSccpRemoteProfile().getRemoteGtAddresses()) {
            configureLocalOriginatedRuleForRemoteGt(remoteGt, stack);
        }
        configureLocalOriginatedGttRule(gtRouting, stack);
    }

    private static void configureLocalOriginatedRuleForRemoteGt(RemoteGtAddressType remoteGt, ImscfSigtranStack stack)
            throws Exception {
        GlobalTitle gtMatcher = new GlobalTitle0011Impl(remoteGt.getGlobalTitle(), remoteGt.getGtTranslationType(),
                new DefaultEncodingScheme(), NumberingPlan.ISDN_TELEPHONY);
        SccpAddress sccpGTMatcherAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN, gtMatcher, -1,
                remoteGt.getSubSystemNumber());
        GlobalTitle gtMask = new GlobalTitle0011Impl(remoteGt.getGlobalTitle(), remoteGt.getGtTranslationType(),
                new DefaultEncodingScheme(), NumberingPlan.ISDN_TELEPHONY);
        int translatedSSN = remoteGt.getSubSystemNumber();
        SccpAddress sccpGttPrimaryRouteAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE,
                gtMask, remoteGt.getPointCode(), translatedSSN);

        SccpStack sccpStack = stack.getSccpStack();

        int routingAddrIndex = SCCP_ROUTER_ROUTING_ADDR_SEQ.incrementAndGet();

        sccpStack.getRouter().addRoutingAddress(routingAddrIndex, sccpGttPrimaryRouteAddress);

        int nextRuleIndex = SCCP_ROUTER_RULE_SEQ.incrementAndGet();
        sccpStack.getRouter()
                .addRule(nextRuleIndex, RuleType.SOLITARY, LoadSharingAlgorithm.Undefined,
                        OriginationType.LOCAL, sccpGTMatcherAddress, "R", routingAddrIndex, -1,
                        null, 0);
        LOGGER.debug("adding sccp routing rule: " + sccpStack.getRouter().getRule(nextRuleIndex));
    }

    private static void configureLocalOriginatedGttRule(GtRoutingType gtRouting, ImscfSigtranStack stack)
            throws Exception {
        GlobalTitle gtMatcher = new GlobalTitle0011Impl("*", Ss7StackParameters.TRANSLATION_TYPE_FOR_GTT,
                new DefaultEncodingScheme(), NumberingPlan.ISDN_TELEPHONY);
        SccpAddress sccpGTMatcherAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE,
                gtMatcher, -1, Ss7StackParameters.CAP_SSN);
        GlobalTitle gtMask = new GlobalTitle0011Impl("-", Ss7StackParameters.TRANSLATION_TYPE_FOR_GTT,
                new DefaultEncodingScheme(), NumberingPlan.ISDN_TELEPHONY);
        SccpAddress sccpGttPrimaryRouteAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE,
                gtMask, gtRouting.getPrimaryGttPointCode(), Ss7StackParameters.CAP_SSN);

        SccpStack sccpStack = stack.getSccpStack();

        int routingAddrIndexMatcher = SCCP_ROUTER_ROUTING_ADDR_SEQ.incrementAndGet();
        int routingAddrIndexPrimary = SCCP_ROUTER_ROUTING_ADDR_SEQ.incrementAndGet();

        sccpStack.getRouter().addRoutingAddress(routingAddrIndexMatcher, sccpGTMatcherAddress);
        sccpStack.getRouter().addRoutingAddress(routingAddrIndexPrimary, sccpGttPrimaryRouteAddress);

        int routingAddrIndexSecondary = Optional.ofNullable(gtRouting.getSecondaryGttPointCode())
                .map(secondaryPC -> {
                    // returns: address index
                    // side effect: actually add the address
                    int idx = SCCP_ROUTER_ROUTING_ADDR_SEQ.incrementAndGet();
                    try {
                        sccpStack.getRouter().addRoutingAddress(idx, new SccpAddressImpl(
                                RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gtMask, secondaryPC,
                                Ss7StackParameters.CAP_SSN));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return idx;
                }).orElse(-1); // -1 means no secondary address

        LOGGER.debug("Routing addresses: " + sccpStack.getRouter().getRoutingAddresses() + " size: "
                + sccpStack.getRouter().getRoutingAddresses().size());

        int nextRuleIndex = SCCP_ROUTER_RULE_SEQ.incrementAndGet();
        LoadSharingAlgorithm algo;
        RuleType ruleType;
        if (routingAddrIndexSecondary == -1) {
            algo = LoadSharingAlgorithm.Undefined;
            ruleType = RuleType.SOLITARY;
        } else if (gtRouting.getDistribution() == MessageDistributionType.LOADBALANCE) {
            algo = LoadSharingAlgorithm.Bit0;
            ruleType = RuleType.LOADSHARED;
        } else {
            //failover
            algo = LoadSharingAlgorithm.Undefined;
            ruleType = RuleType.DOMINANT;
        }

        sccpStack.getRouter().addRule(nextRuleIndex, ruleType, algo,
                OriginationType.LOCAL, sccpGTMatcherAddress, "K", routingAddrIndexPrimary, routingAddrIndexSecondary,
                null, 0);
    }

    private static void configureRemoteOriginatedGttRules(int localPointCode, ImscfSigtranStack stack) throws Exception {
        for (GtAddressType localGt : stack.getConfig().getSccp().getSccpLocalProfile().getLocalGtAddresses()) {
            configureRemoteOriginatedRuleForLocalGt(localGt, localPointCode, stack);
        }
    }

    private static void configureRemoteOriginatedRuleForLocalGt(GtAddressType localGt, int localPointCode,
            ImscfSigtranStack stack) throws Exception {
        GlobalTitle gtLocalMask = new GlobalTitle0011Impl("-", 0, new DefaultEncodingScheme(), NumberingPlan.ISDN_TELEPHONY);
        SccpAddress sccpGtLocalMask = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN, gtLocalMask, localPointCode,
                localGt.getSubSystemNumber());

        int nextRoutingAddressIndex = SCCP_ROUTER_ROUTING_ADDR_SEQ.incrementAndGet();
        stack.getSccpStack().getRouter().addRoutingAddress(nextRoutingAddressIndex, sccpGtLocalMask);

        stack.getSccpStack()
                .getRouter()
                .addRule(
                        SCCP_ROUTER_RULE_SEQ.incrementAndGet(),
                        RuleType.SOLITARY,
                        LoadSharingAlgorithm.Undefined,
                        OriginationType.REMOTE,
                        new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, new GlobalTitle0011Impl(
							localGt.getGlobalTitle(), 0, new DefaultEncodingScheme(), NumberingPlan.ISDN_TELEPHONY), localPointCode,
							localGt.getSubSystemNumber()), "K",
                        	nextRoutingAddressIndex, -1, null, 0);
    }

}
