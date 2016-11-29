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
package org.restcomm.imscf.common ;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.ExecutionLayerServerType;
import org.restcomm.imscf.common.config.ListenAddressType;
import org.restcomm.imscf.common.config.LwCommParametersType;
import org.restcomm.imscf.common.config.PoolConfigurationType;
import org.restcomm.imscf.common.config.SignalingLayerServerType;
import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.config.Configuration.DeploymentMode;
import org.restcomm.imscf.common.lwcomm.config.Configuration.ListenerMode;
import org.restcomm.imscf.common.lwcomm.config.Configuration.ReceiveMode;
import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.config.PoolConfig;
import org.restcomm.imscf.common.lwcomm.config.Route;
import org.restcomm.imscf.common.lwcomm.config.Route.Mode;
import org.restcomm.imscf.common.lwcomm.config.impl.ConfigurationImpl;
import org.restcomm.imscf.common.lwcomm.config.impl.NodeImpl;
import org.restcomm.imscf.common.lwcomm.config.impl.RouteImpl;
import org.restcomm.imscf.common.lwcomm.service.LwCommService;
import org.restcomm.imscf.common.lwcomm.service.LwCommServiceProvider;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for configuring the LwComm library based on the IMSCF configuration.
 */
public final class LwcommConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(LwcommConfigurator.class);

    private LwcommConfigurator() {
    }

    public static LwCommService initLwComm(ImscfConfigType imscfConfig, String serverName, String mBeanDomain,
            MessageReceiver listener) {

        ConfigurationImpl config = new ConfigurationImpl();
        List<Node> slNodes = new ArrayList<>();
        List<Node> elNodes = new ArrayList<>();

        LwCommParametersType lwcommParams = imscfConfig.getLwCommParameters();
        PoolConfigurationType poolConfig = null;

        for (ExecutionLayerServerType el : imscfConfig.getServers().getExecutionLayerServers()) {
            if (poolConfig == null && serverName.equals(el.getName())) {
                poolConfig = (PoolConfigurationType)el.getPoolConfig();
                LOG.debug("This is EL node {}", serverName);
            }

            ListenAddressType addr = el.getConnectivity().getInternalCommunicationAddress();
            Node n = new NodeImpl(el.getName(), addr.getHost(), addr.getPort());
            elNodes.add(n);
            LOG.debug("Added EL node {}", n);
        }

        for (SignalingLayerServerType sl : imscfConfig.getServers().getSignalingLayerServers()) {
            if (poolConfig == null && serverName.equals(sl.getName())) {
                poolConfig = (PoolConfigurationType)sl.getPoolConfig();
                LOG.debug("This is SL node {}", serverName);
            }

            ListenAddressType addr = sl.getConnectivity().getInternalCommunicationAddress();
            Node n = new NodeImpl(sl.getName(), addr.getHost(), addr.getPort());
            slNodes.add(n);
            LOG.debug("Added SL node {}", n);
        }

        if (poolConfig == null) {
            throw new RuntimeException("The server named '" + serverName
                    + "' is missing from the configuration, cannot configure LwComm!");
        }

        int heartbeatIntervalMs = lwcommParams.getHeartbeatIntervalMs();
        int heartbeatTimeoutMs = lwcommParams.getHeartbeatTimeoutMs();
        int recvTPoolSize = poolConfig.getReceiveTransportPoolSize();
        int recvWPoolSize = poolConfig.getReceiveWorkerPoolSize();
        int sendPoolSize = poolConfig.getSendPoolSize();

        LOG.debug("Lwcomm options: HB {}ms/{}ms (interval/timeout), Pool sizes {}/{}/{} (Rx/W/Tx)",
                heartbeatIntervalMs, heartbeatTimeoutMs, recvTPoolSize, recvWPoolSize, sendPoolSize);

        List<Node> allNodes = new ArrayList<Node>(slNodes);
        allNodes.addAll(elNodes);
        config.setAllNodes(allNodes);
        for (Node sl : slNodes) {
            Route r = create1toMRoute(Mode.LOADBALANCE, sl, elNodes, imscfConfig);
            config.addRoute(r);
            LOG.debug("Added {} route {}", r.getMode(), r.getName());
            for (Node el : elNodes) {
                r = create1to1Route(sl, el, imscfConfig);
                config.addRoute(r);
                LOG.debug("Added {} route {}", r.getMode(), r.getName());
                r = create1to1Route(el, sl, imscfConfig);
                config.addRoute(r);
                LOG.debug("Added {} route {}", r.getMode(), r.getName());
            }
            // for internal communication, also add sl-sl routes
            for (Node otherSl : slNodes) {
                if (sl.getName().equals(otherSl.getName()))
                    continue;
                r = create1to1Route(sl, otherSl, imscfConfig);
                config.addRoute(r);
                LOG.debug("Added {} route {}", r.getMode(), r.getName());
            }
        }
        for (Node el : elNodes) {
            Route r = create1toMRoute(Mode.LOADBALANCE, el, slNodes, imscfConfig);
            config.addRoute(r);
            LOG.debug("Added {} route {}", r.getMode(), r.getName());
        }

        config.setDeploymentMode(DeploymentMode.MULTIPLE);
        config.setHeartbeatIntervalMs(heartbeatIntervalMs);
        config.setHeartbeatTimeoutMs(heartbeatTimeoutMs);
        config.setLocalNodeName(serverName);
        config.setReceiveMode(ReceiveMode.LISTENER);
        config.setListenerMode(ListenerMode.EPOLL);
        config.setReceiveTransportPoolConfig(new PoolConfig(recvTPoolSize));
        config.setReceiveWorkerPoolConfig(new PoolConfig(recvWPoolSize));
        config.setSendPoolConfig(new PoolConfig(sendPoolSize));
        config.setMessageReceiver(listener);
        config.setAckSendStrategy(Configuration.AckSendStrategy.SEND_CYCLE);
        config.setMBeanDomain(mBeanDomain);

        boolean ok = LwCommServiceProvider.init(config);
        LOG.debug("LwComm init {}", ok ? "SUCCESS" : "FAILURE");
        return ok ? LwCommServiceProvider.getService() : null;

    }

    private static Route create1to1Route(Node n, Node m, ImscfConfigType imscfConfig) {
        RouteImpl r = new RouteImpl();
        r.setName(n.getName() + " -> " + m.getName());
        r.setMode(Mode.FAILOVER);
        r.setPossibleSources(Collections.singletonList(n));
        r.setDestinations(Collections.singletonList(m));
        r.setRetransmitPattern(imscfConfig.getLwCommParameters().getRetransmitPattern());
        return r;
    }

    private static Route create1toMRoute(Mode mode, Node source, List<Node> destinations, ImscfConfigType imscfConfig) {
        RouteImpl r = new RouteImpl();
        ArrayList<String> names = new ArrayList<String>(destinations.size());
        for (Node n : destinations) {
            names.add(n.getName());
        }
        r.setName(source.getName() + " -> " + names.toString().replace(", ", "|"));
        r.setMode(mode);
        r.setPossibleSources(Collections.singletonList(source));
        r.setDestinations(new ArrayList<Node>(destinations));
        r.setRetransmitPattern(imscfConfig.getLwCommParameters().getRetransmitPattern());
        return r;
    }
}
