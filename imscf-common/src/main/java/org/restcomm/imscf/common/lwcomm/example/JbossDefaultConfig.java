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
 * Default configuration for JBoss default installation.
 * Can be used for testing on locally installed JBoss.
 * @author Miklos Pocsaji
 *
 */
public class JbossDefaultConfig extends Configuration {

    private HashMap<String, Node> nodes;
    private HashMap<String, Route> routes;
    private PoolConfig recvTransportPool = new PoolConfig(5);
    private PoolConfig recvWorkerPool = new PoolConfig(10);
    private PoolConfig sendPool = new PoolConfig(10);

    public JbossDefaultConfig() {
        Node serverOne = new NodeImpl("server-one", "localhost", 3200);
        Node serverTwo = new NodeImpl("server-two", "localhost", 3201);
        Node serverThree = new NodeImpl("server-three", "localhost", 3300);

        nodes = new HashMap<String, Node>();
        nodes.put(serverOne.getName(), serverOne);
        nodes.put(serverTwo.getName(), serverTwo);
        nodes.put(serverThree.getName(), serverThree);

        routes = new HashMap<String, Route>();
        RouteImpl fromThreeToOneTwo = new RouteImpl();
        fromThreeToOneTwo.setName("failover");
        fromThreeToOneTwo.setDefaultQueue("jms/queue/TestQueue");
        fromThreeToOneTwo.setPossibleSources(Collections.singleton(serverThree));
        fromThreeToOneTwo.setDestinations(Arrays.asList(serverOne, serverTwo));
        fromThreeToOneTwo.setMode(Route.Mode.FAILOVER);
        fromThreeToOneTwo.setRetransmitPattern(Arrays.asList(200, 600, 1000));
        routes.put(fromThreeToOneTwo.getName(), fromThreeToOneTwo);
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
        return DeploymentMode.JBOSS;
    }

    @Override
    public int getHeartbeatIntervalMs() {
        return 10000;
    }

    @Override
    public int getHeartbeatTimeoutMs() {
        return 11000;
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
        return "java:/ConnectionFactory";
    }

    @Override
    public String getLocalNodeName() {
        return getLocalNode().getName();
    }

    @Override
    public ReceiveMode getReceiveMode() {
        return ReceiveMode.JMS_QUEUE;
    }

    @Override
    public MessageReceiver getMessageReceiver() {
        return null;
    }

    @Override
    public ListenerMode getListenerMode() {
        return ListenerMode.NIO;
    }

    @Override
    public AckSendStrategy getAckSendStrategy() {
        return Configuration.DEFAULT_ACK_SEND_STRATEGY;
    }

    @Override
    public ClientPortRange getClientPortRange() {
        return Configuration.NO_CLIENT_PORT_RANGE;
    }

    @Override
    public String getMBeanDomain() {
        return null;
    }
}
