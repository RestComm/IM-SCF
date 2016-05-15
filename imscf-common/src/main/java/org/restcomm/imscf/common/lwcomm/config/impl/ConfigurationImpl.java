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
package org.restcomm.imscf.common.lwcomm.config.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.config.PoolConfig;
import org.restcomm.imscf.common.lwcomm.config.Route;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;

/**
 * Configuration implementation.
 * @author Miklos Pocsaji
 *
 */
public class ConfigurationImpl extends Configuration {

    private Map<String, Node> allNodes = new HashMap<String, Node>();
    private Map<String, Route> allRoutes = new HashMap<String, Route>();
    private DeploymentMode deploymentMode = DeploymentMode.JBOSS;
    private int heartbeatIntervalMs = 5000;
    private int heartbeatTimeoutMs = 6000;
    private PoolConfig receiveTransportPoolConfig;
    private PoolConfig receiveWorkerPoolConfig;
    private PoolConfig sendPoolConfig;
    private String connectionFactoryJndi;
    private String localNodeName;
    private ReceiveMode receiveMode;
    private MessageReceiver messageReceiver;
    private ListenerMode listenerMode = DEFAULT_LISTENER_MODE;
    private AckSendStrategy ackSendStrategy = DEFAULT_ACK_SEND_STRATEGY;
    private ClientPortRange clientPortRange = DEFAULT_CLIENT_PORT_RANGE;
    private String mBeanDomain;

    public void setAllNodes(Collection<Node> nodes) {
        allNodes.clear();
        nodes.forEach(n -> allNodes.put(n.getName(), n));
    }

    public void setAllRoutes(Collection<Route> routes) {
        allRoutes.clear();
        routes.forEach(r -> allRoutes.put(r.getName(), r));
    }

    public void addNode(Node n) {
        allNodes.put(n.getName(), n);
    }

    public void addRoute(Route r) {
        allRoutes.put(r.getName(), r);
    }

    public void setDeploymentMode(DeploymentMode deploymentMode) {
        this.deploymentMode = deploymentMode;
    }

    public void setHeartbeatIntervalMs(int heartbeatIntervalMs) {
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }

    public void setHeartbeatTimeoutMs(int heartbeatTimeoutMs) {
        this.heartbeatTimeoutMs = heartbeatTimeoutMs;
    }

    public void setReceiveTransportPoolConfig(PoolConfig receiveTransportPoolConfig) {
        this.receiveTransportPoolConfig = receiveTransportPoolConfig;
    }

    public void setReceiveWorkerPoolConfig(PoolConfig receiveWorkerPoolConfig) {
        this.receiveWorkerPoolConfig = receiveWorkerPoolConfig;
    }

    public void setSendPoolConfig(PoolConfig sendPoolConfig) {
        this.sendPoolConfig = sendPoolConfig;
    }

    public void setConnectionFactoryJndi(String connectionFactoryJndi) {
        this.connectionFactoryJndi = connectionFactoryJndi;
    }

    public void setLocalNodeName(String localNodeName) {
        this.localNodeName = localNodeName;
    }

    public void setReceiveMode(ReceiveMode receiveMode) {
        this.receiveMode = receiveMode;
    }

    public void setMessageReceiver(MessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }

    public void setListenerMode(ListenerMode listenerMode) {
        this.listenerMode = listenerMode;
    }

    public void setAckSendStrategy(AckSendStrategy ackSendStrategy) {
        this.ackSendStrategy = ackSendStrategy;
    }

    public void setClientPortRange(ClientPortRange clientPortRange) {
        this.clientPortRange = clientPortRange;
    }

    public void setMBeanDomain(String mBeanDomain) {
        this.mBeanDomain = mBeanDomain;
    }

    //
    // Configuration interface
    //

    @Override
    public Set<Node> getAllNodes() {
        return new HashSet<Node>(allNodes.values());
    }

    @Override
    public Set<Route> getAllRoutes() {
        return new HashSet<Route>(allRoutes.values());
    }

    @Override
    public Node getNodeByName(String name) {
        return allNodes.get(name);
    }

    @Override
    public Route getRouteByName(String name) {
        return allRoutes.get(name);
    }

    @Override
    public DeploymentMode getDeploymentMode() {
        return deploymentMode;
    }

    @Override
    public int getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    @Override
    public int getHeartbeatTimeoutMs() {
        return heartbeatTimeoutMs;
    }

    @Override
    public PoolConfig getReceiveTransportPoolConfig() {
        return receiveTransportPoolConfig;
    }

    @Override
    public PoolConfig getReceiveWorkerPoolConfig() {
        return receiveWorkerPoolConfig;
    }

    @Override
    public PoolConfig getSendPoolConfig() {
        return sendPoolConfig;
    }

    @Override
    public String getConnectionFactoryJndi() {
        return connectionFactoryJndi;
    }

    @Override
    public String getLocalNodeName() {
        return localNodeName;
    }

    @Override
    public ReceiveMode getReceiveMode() {
        return receiveMode;
    }

    @Override
    public MessageReceiver getMessageReceiver() {
        return messageReceiver;
    }

    @Override
    public ListenerMode getListenerMode() {
        return listenerMode;
    }

    @Override
    public AckSendStrategy getAckSendStrategy() {
        return ackSendStrategy;
    }

    @Override
    public ClientPortRange getClientPortRange() {
        return clientPortRange;
    }

    @Override
    public String getMBeanDomain() {
        return mBeanDomain;
    }
}
