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
package org.restcomm.imscf.sl.diameter.config;

import org.restcomm.imscf.common.config.DiameterGatewayModuleType;
import org.restcomm.imscf.common.config.SctpAssociationLocalSideType;
import org.restcomm.imscf.common.config.SctpAssociationRemoteSideType;
import org.restcomm.imscf.common.config.SignalingLayerServerType;
import org.restcomm.imscf.sl.config.ConfigBean;

import java.util.ArrayList;
import java.util.List;

import org.jdiameter.api.Configuration;
import org.jdiameter.client.impl.helpers.AppConfiguration;
import org.jdiameter.server.impl.helpers.EmptyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for build diameter gw configuration.
 */
public class DiameterGWConfiguration {
    private static Logger logger = LoggerFactory.getLogger(DiameterGWConfiguration.class);

    private EmptyConfiguration config;

    private DiameterGatewayModuleType module;
    private SignalingLayerServerType signalingServers;
    private SctpAssociationLocalSideType sctpAssociationLocal;
    private SctpAssociationRemoteSideType sctpAssociationRemoteSide;

    public DiameterGWConfiguration(DiameterGatewayModuleType module, SignalingLayerServerType signalingServers,
            SctpAssociationLocalSideType sctpAssociationLocal, SctpAssociationRemoteSideType sctpAssociationRemoteSide) {
        this.module = module;
        this.signalingServers = signalingServers;
        this.sctpAssociationLocal = sctpAssociationLocal;
        this.sctpAssociationRemoteSide = sctpAssociationRemoteSide;

    }

    public Configuration getDiameterConfig() throws Exception {
        createDiameterConfig();

        return config;
    }

    // Create diameter configuration
    private void createDiameterConfig() throws Exception {
        logger.info("Set diameter configuration.");

        config = new EmptyConfiguration(true);

        setLocalPeerConfiguration();
        setParamateresConfiguration();
        setConcurrentConfiguration();
        setNetworkPeerConfiguration();
        setNetworkRealmConfiguration();
        setExtensionsConfiguration();
    }

    // Set local peer element configuration
    private void setLocalPeerConfiguration() throws Exception {
        logger.info("Set diameter local peer element configuration.");
        config.add(org.jdiameter.server.impl.helpers.Parameters.OwnDiameterURI,
                "aaa://" + signalingServers.getDiameterGwOriginHost());

        ArrayList<Configuration> localPeerItems = new ArrayList<Configuration>();

        AppConfiguration localPeerIp = new EmptyConfiguration(false);
        localPeerItems.add(localPeerIp.add(org.jdiameter.server.impl.helpers.Parameters.OwnIPAddress,
                sctpAssociationLocal.getSigtranIp1()));
        localPeerIp = new EmptyConfiguration(false);

        if (sctpAssociationLocal.getSigtranIp2() != null && sctpAssociationLocal.getSigtranIp2().length() > 0) {
            localPeerItems.add(localPeerIp.add(org.jdiameter.server.impl.helpers.Parameters.OwnIPAddress,
                    sctpAssociationLocal.getSigtranIp2()));
        }
        config.add(org.jdiameter.server.impl.helpers.Parameters.OwnIPAddresses,
                localPeerItems.toArray(new Configuration[localPeerItems.size()]));

        config.add(org.jdiameter.server.impl.helpers.Parameters.OwnRealm, module.getOriginRealm());
        config.add(org.jdiameter.server.impl.helpers.Parameters.OwnVendorID,
                Long.valueOf(module.getOriginApplicationId().getVendorId()));
        config.add(org.jdiameter.server.impl.helpers.Parameters.OwnProductName, module.getProductName());
        config.add(org.jdiameter.server.impl.helpers.Parameters.OwnFirmwareRevision, Long.valueOf(1));

        ArrayList<Configuration> localPeerApplicationItems = new ArrayList<Configuration>();
        AppConfiguration localPeerApplication = new EmptyConfiguration(false);
        localPeerApplication.add(org.jdiameter.server.impl.helpers.Parameters.VendorId,
                Long.valueOf(module.getOriginApplicationId().getVendorId()));
        localPeerApplication.add(org.jdiameter.server.impl.helpers.Parameters.AuthApplId,
                Long.valueOf(module.getOriginApplicationId().getAuthApplId()));
        localPeerApplication.add(org.jdiameter.server.impl.helpers.Parameters.AcctApplId,
                Long.valueOf(module.getOriginApplicationId().getAcctApplId()));
        localPeerApplicationItems.add(localPeerApplication);
        config.add(org.jdiameter.server.impl.helpers.Parameters.ApplicationId,
                localPeerApplicationItems.toArray(new Configuration[localPeerApplicationItems.size()]));

        config.add(org.jdiameter.server.impl.helpers.Parameters.OwnSctpStackName, "DiameterGWStack");
    }

    // Set parameters element configuration
    private void setParamateresConfiguration() throws Exception {
        logger.info("Set diameter parameters element configuration.");

        config.add(org.jdiameter.server.impl.helpers.Parameters.QueueSize, module.getQueueSize());
        config.add(org.jdiameter.server.impl.helpers.Parameters.MessageTimeOut,
                Long.valueOf(module.getMessageTimeoutMs()));
        config.add(org.jdiameter.server.impl.helpers.Parameters.StopTimeOut, Long.valueOf(module.getStopTimeoutMs()));
        config.add(org.jdiameter.server.impl.helpers.Parameters.CeaTimeOut, Long.valueOf(module.getCeaTimeoutMs()));
        config.add(org.jdiameter.server.impl.helpers.Parameters.IacTimeOut, Long.valueOf(module.getIacTimeoutMs()));
        config.add(org.jdiameter.server.impl.helpers.Parameters.DwaTimeOut, Long.valueOf(module.getDwaTimeoutMs()));
        config.add(org.jdiameter.server.impl.helpers.Parameters.DpaTimeOut, Long.valueOf(module.getDpaTimeoutMs()));
        config.add(org.jdiameter.server.impl.helpers.Parameters.RecTimeOut, Long.valueOf(module.getRecTimeoutMs()));
        config.add(org.jdiameter.server.impl.helpers.Parameters.DuplicateProtection, module.isDuplicationProtection());
        config.add(org.jdiameter.server.impl.helpers.Parameters.DuplicateTimer,
                Long.valueOf(module.getDuplicateTimer()));
        config.add(org.jdiameter.server.impl.helpers.Parameters.DuplicateSize, module.getDuplicateSize());
        config.add(org.jdiameter.server.impl.helpers.Parameters.AcceptUndefinedPeer, module.isAcceptUndefinedPeer());
    }

    // Set parameters element concurrent field configuration
    private void setConcurrentConfiguration() throws Exception {
        logger.info("Set diameter parameters element concurrent field configuration.");

        List<Configuration> parametersConcurrentItems = new ArrayList<Configuration>();

        AppConfiguration concurrentItem = new EmptyConfiguration(false);
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityName, "ThreadGroup");
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityPoolSize, module
                .getThreadPools().getThreadGroupSize()); // NOPMD
        parametersConcurrentItems.add(concurrentItem);

        concurrentItem = new EmptyConfiguration(false);
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityName, "ProcessingMessageTimer");
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityPoolSize, module
                .getThreadPools().getProcessingMessageTimerSize());
        parametersConcurrentItems.add(concurrentItem);

        concurrentItem = new EmptyConfiguration(false);
        concurrentItem
                .add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityName, "DuplicationMessageTimer");
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityPoolSize, module
                .getThreadPools().getDplicationMessageTimerSize());
        parametersConcurrentItems.add(concurrentItem);

        concurrentItem = new EmptyConfiguration(false);
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityName, "RedirectMessageTimer");
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityPoolSize, module
                .getThreadPools().getRedirectMessageTimerSize());
        parametersConcurrentItems.add(concurrentItem);

        concurrentItem = new EmptyConfiguration(false);
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityName, "PeerOverloadTimer");
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityPoolSize, module
                .getThreadPools().getPeerOverloadTimerSize());
        parametersConcurrentItems.add(concurrentItem);

        concurrentItem = new EmptyConfiguration(false);
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityName, "ConnectionTimer");
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityPoolSize, module
                .getThreadPools().getConnectionTimerSize());
        parametersConcurrentItems.add(concurrentItem);

        concurrentItem = new EmptyConfiguration(false);
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityName, "StatisticTimer");
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityPoolSize, module
                .getThreadPools().getStatisticTimerSize());
        parametersConcurrentItems.add(concurrentItem);

        concurrentItem = new EmptyConfiguration(false);
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityName, "ApplicationSession");
        concurrentItem.add(org.jdiameter.server.impl.helpers.Parameters.ConcurrentEntityPoolSize, module
                .getThreadPools().getApplicationSessionSize());
        parametersConcurrentItems.add(concurrentItem);

        config.add(org.jdiameter.server.impl.helpers.Parameters.Concurrent,
                parametersConcurrentItems.toArray(new Configuration[parametersConcurrentItems.size()]));
    }

    // Set network element peer configuration
    private void setNetworkPeerConfiguration() throws Exception {
        logger.info("Set diameter network element peer configuration.");

        ArrayList<Configuration> networkPeerItems = new ArrayList<Configuration>();
        AppConfiguration networkPeer = new EmptyConfiguration(false);

        networkPeer.add(org.jdiameter.server.impl.helpers.Parameters.PeerRating, 1);
        networkPeer.add(org.jdiameter.server.impl.helpers.Parameters.PeerAttemptConnection, true);

        networkPeer.add(org.jdiameter.server.impl.helpers.Parameters.PeerName, "aaa://" + ConfigBean.SERVER_NAME + "."
                + sctpAssociationRemoteSide.getName() + "." + module.getDestinationRealm() + ":"
                + sctpAssociationRemoteSide.getRemotePort());

        networkPeer.add(org.jdiameter.server.impl.helpers.Parameters.PeerIp, sctpAssociationRemoteSide.getRemoteIp1());

        networkPeer.add(org.jdiameter.server.impl.helpers.Parameters.PeerLocalPortRange, sctpAssociationLocal.getPort()
                + "-" + sctpAssociationLocal.getPort());

        networkPeer.add(org.jdiameter.server.impl.helpers.Parameters.PeerAssociationName,
                sctpAssociationRemoteSide.getName());
        networkPeerItems.add(networkPeer);

        config.add(org.jdiameter.server.impl.helpers.Parameters.PeerTable,
                networkPeerItems.toArray(new Configuration[networkPeerItems.size()]));
    }

    // Set network element realm configuration
    private void setNetworkRealmConfiguration() throws Exception {
        logger.info("Set diameter network element realm configuration.");

        ArrayList<Configuration> networkRealmItems = new ArrayList<Configuration>();
        AppConfiguration networkRealm = new EmptyConfiguration(false);
        AppConfiguration networkRealmProperties = new EmptyConfiguration(false);

        networkRealmProperties
                .add(org.jdiameter.server.impl.helpers.Parameters.RealmName, module.getDestinationRealm());
        networkRealmProperties.add(org.jdiameter.server.impl.helpers.Parameters.RealmHosts, ConfigBean.SERVER_NAME
                + "." + sctpAssociationRemoteSide.getName() + "." + module.getDestinationRealm());

        networkRealmProperties.add(org.jdiameter.server.impl.helpers.Parameters.RealmLocalAction, "LOCAL");
        networkRealmProperties.add(org.jdiameter.server.impl.helpers.Parameters.RealmEntryIsDynamic, false);
        networkRealmProperties.add(org.jdiameter.server.impl.helpers.Parameters.RealmEntryExpTime, Long.valueOf(1));

        AppConfiguration networkRealmApplication = new EmptyConfiguration(false);
        networkRealmApplication.add(org.jdiameter.server.impl.helpers.Parameters.VendorId,
                Long.valueOf(module.getDestinationApplicationId().getVendorId()));
        networkRealmApplication.add(org.jdiameter.server.impl.helpers.Parameters.AuthApplId,
                Long.valueOf(module.getDestinationApplicationId().getAuthApplId()));
        networkRealmApplication.add(org.jdiameter.server.impl.helpers.Parameters.AcctApplId,
                Long.valueOf(module.getDestinationApplicationId().getAcctApplId()));
        networkRealmProperties.add(org.jdiameter.server.impl.helpers.Parameters.ApplicationId,
                new Configuration[] { networkRealmApplication });

        networkRealm.add(org.jdiameter.client.impl.helpers.Parameters.RealmEntry, networkRealmProperties);
        networkRealmItems.add(networkRealm);

        config.add(org.jdiameter.server.impl.helpers.Parameters.RealmTable,
                networkRealmItems.toArray(new Configuration[networkRealmItems.size()]));
    }

    // Set extensions element configuration
    private void setExtensionsConfiguration() throws Exception {
        logger.info("Set diameter extensions element configuration.");

        Configuration[] extensionConfigurationConn = config
                .getChildren(org.jdiameter.server.impl.helpers.Parameters.Extensions.ordinal());
        AppConfiguration internalConnection = (AppConfiguration) extensionConfigurationConn[org.jdiameter.server.impl.helpers.ExtensionPoint.Internal
                .id()];
        internalConnection.add(org.jdiameter.server.impl.helpers.ExtensionPoint.InternalConnectionClass,
                "org.jdiameter.client.impl.transport.sctp.SCTPClientConnection");

        Configuration[] extensionConfigurationNet = config
                .getChildren(org.jdiameter.server.impl.helpers.Parameters.Extensions.ordinal());
        AppConfiguration internalNetworkGuard = (AppConfiguration) extensionConfigurationNet[org.jdiameter.server.impl.helpers.ExtensionPoint.Internal
                .id()];
        internalNetworkGuard.add(org.jdiameter.server.impl.helpers.ExtensionPoint.InternalNetworkGuard,
                "org.jdiameter.server.impl.io.sctp.NetworkGuard");

        Configuration[] extensionConfigurationMan = config
                .getChildren(org.jdiameter.server.impl.helpers.Parameters.Extensions.ordinal());
        AppConfiguration internalSctpManagement = (AppConfiguration) extensionConfigurationMan[org.jdiameter.server.impl.helpers.ExtensionPoint.Internal
                .id()];
        internalSctpManagement.add(
                org.jdiameter.client.impl.helpers.ExtensionPoint.InternalSctpManagementConfiguration,
                "org.restcomm.imscf.mgmt.sctp.ImscfManagedSctpMultiManagementWrapper");

        Configuration[] extensionConfigurationPeerFac = config
                .getChildren(org.jdiameter.server.impl.helpers.Parameters.Extensions.ordinal());
        AppConfiguration internalPeerFsmFactory = (AppConfiguration) extensionConfigurationPeerFac[org.jdiameter.server.impl.helpers.ExtensionPoint.Internal
                .id()];
        internalPeerFsmFactory.add(org.jdiameter.client.impl.helpers.ExtensionPoint.InternalPeerFsmFactory,
                "org.restcomm.imscf.diameter.ImscfServerFsmFactoryImpl");
    }
}
