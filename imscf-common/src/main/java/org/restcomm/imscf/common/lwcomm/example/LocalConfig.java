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
package org.restcomm.imscf.common.lwcomm.example;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.config.PoolConfig;
import org.restcomm.imscf.common.lwcomm.config.Route;
import org.restcomm.imscf.common.lwcomm.config.impl.NodeImpl;
import org.restcomm.imscf.common.lwcomm.config.impl.RouteImpl;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;

/**
 * Configuration for local, standalone testing.
 * Used when running tests from the ant buildfile (run_local_* tasks).
 * @author Miklos Pocsaji
 *
 */
public class LocalConfig extends Configuration {

    private HashMap<String, Node> nodes;
    private HashMap<String, Route> routes;
    private PoolConfig recvTransportPool = new PoolConfig(20);
    private PoolConfig recvWorkerPool = new PoolConfig(40);
    private PoolConfig sendPool = new PoolConfig(40);
    private String localNodeName;
    private MessageReceiver messageReceiver = new LocalMessageReceiver();

    public LocalConfig(String localNodeName) {
        this.localNodeName = localNodeName;

        Node receiver1 = new NodeImpl("receiver1", "localhost", 3200);
        Node receiver2 = new NodeImpl("receiver2", "localhost", 3201);
        Node sender = new NodeImpl("sender", "localhost", 3300);

        nodes = new HashMap<String, Node>();
        nodes.put(receiver1.getName(), receiver1);
        nodes.put(receiver2.getName(), receiver2);
        nodes.put(sender.getName(), sender);

        routes = new HashMap<String, Route>();
        RouteImpl senderToReceiverFailover = new RouteImpl();
        senderToReceiverFailover.setName("failover");
        senderToReceiverFailover.setDefaultQueue("jms/queue/TestQueue");
        senderToReceiverFailover.setPossibleSources(Collections.singleton(sender));
        senderToReceiverFailover.setDestinations(Arrays.asList(receiver1, receiver2));
        senderToReceiverFailover.setMode(Route.Mode.FAILOVER);
        senderToReceiverFailover.setRetransmitPattern(Arrays.asList(200, 600, 1000));
        routes.put(senderToReceiverFailover.getName(), senderToReceiverFailover);

        RouteImpl senderToReceiverLoadbalance = new RouteImpl();
        senderToReceiverLoadbalance.setName("loadbalance");
        senderToReceiverLoadbalance.setDefaultQueue("jms/queue/TestQueue");
        senderToReceiverLoadbalance.setPossibleSources(Collections.singleton(sender));
        senderToReceiverLoadbalance.setDestinations(Arrays.asList(receiver1, receiver2));
        senderToReceiverLoadbalance.setMode(Route.Mode.LOADBALANCE);
        senderToReceiverLoadbalance.setRetransmitPattern(Arrays.asList(200, 600, 1000));
        routes.put(senderToReceiverLoadbalance.getName(), senderToReceiverLoadbalance);
    }

    @Override
    public Set<Node> getAllNodes() {
        return new HashSet<Node>(nodes.values());
    }

    @Override
    public Set<Route> getAllRoutes() {
        return new HashSet<Route>(routes.values());
    }

    @Override
    public Node getNodeByName(String name) {
        return nodes.get(name);
    }

    @Override
    public Route getRouteByName(String name) {
        return routes.get(name);
    }

    @Override
    public DeploymentMode getDeploymentMode() {
        return DeploymentMode.MULTIPLE;
    }

    @Override
    public int getHeartbeatIntervalMs() {
        return 5000;
    }

    @Override
    public int getHeartbeatTimeoutMs() {
        return 6000;
    }

    @Override
    public PoolConfig getReceiveTransportPoolConfig() {
        return recvTransportPool;
    }

    @Override
    public PoolConfig getReceiveWorkerPoolConfig() {
        return recvWorkerPool;
    }

    @Override
    public PoolConfig getSendPoolConfig() {
        return sendPool;
    }

    @Override
    public String getConnectionFactoryJndi() {
        return null;
    }

    @Override
    public String getLocalNodeName() {
        return localNodeName;
    }

    @Override
    public ReceiveMode getReceiveMode() {
        return ReceiveMode.LISTENER;
    }

    @Override
    public MessageReceiver getMessageReceiver() {
        return messageReceiver;
    }

    @Override
    public ListenerMode getListenerMode() {
        return ListenerMode.EPOLL;
    }

    @Override
    public AckSendStrategy getAckSendStrategy() {
        return Configuration.DEFAULT_ACK_SEND_STRATEGY;
    }

    @Override
    public ClientPortRange getClientPortRange() {
        return new ClientPortRange(55000, 57000);
    }

    @Override
    public String getMBeanDomain() {
        return null;
    }
}
