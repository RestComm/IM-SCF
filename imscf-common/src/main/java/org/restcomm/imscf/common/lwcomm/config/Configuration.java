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
package org.restcomm.imscf.common.lwcomm.config;

import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for the LwComm system.
 * @author Miklos Pocsaji
 *
 */
@SuppressWarnings("PMD.GodClass")
public abstract class Configuration {

    public static final String PARAMETER_LWCOMM_NAME = "lwcomm.name";
    public static final ClientPortRange NO_CLIENT_PORT_RANGE = new ClientPortRange(-1, -1);
    public static final ClientPortRange DEFAULT_CLIENT_PORT_RANGE = NO_CLIENT_PORT_RANGE;
    /** The default listener mode is NIO_SEPARATE. */
    public static final ListenerMode DEFAULT_LISTENER_MODE = ListenerMode.NIO;
    /**
     * The default value to use for ackSendStrategy when none specified.
     */
    public static final AckSendStrategy DEFAULT_ACK_SEND_STRATEGY = AckSendStrategy.IMMEDIATELY;

    /**
     * How LwComm is deployed.
     * @author Miklos Pocsaji
     *
     */
    public enum DeploymentMode {
        /** Deployed in JBoss, one per JBoss instance. Local node name is determined by checking JVM parameter "jboss.server.name". */
        JBOSS,
        /** Deployed in Weblogic, one per managed server instance. Local node name is determined by checking JVM parameter "weblogic.server". */
        WEBLOGIC,
        /** Deployed in standalone mode. Local node name is determined by checking JVM parameter "lwcomm.name". */
        STANDALONE,
        /** Generic deployment mode. Local node name is determined by the configuration. */
        MULTIPLE
    }

    /**
     * How LwComm is listening to incoming UDP packets.
     * @author Miklos Pocsaji
     *
     */
    public enum ListenerMode {
        /** Using netty's NioEventLoopGroup. Uses receive transport pool for event loop and worker pool for handling messages. */
        NIO,
        /** Using Linux epoll. Uses transport pool for epoll event loop and uses worker pool for handling messages.
         * Works only on Linux with kernels 3.9+ or on RHEL 6.5. */
        EPOLL
    }

    /**
     * How messages are deliviered on the receiving side.
     * @author Miklos Pocsaji
     */
    public enum ReceiveMode {
        /** The received messages' payload is converted to a javax.jms.TextMessage and put into a queue.
         * The queue JNDI is determined by the message or the route's default queue name.
         */
        JMS_QUEUE,
        /**
         * The client must register a MessageReceiver in configuration phase.
         * This receiver will get all the messages.
         */
        LISTENER
    }

    /**
     * Possible methods of sending ACK message.
     * @author Miklos Pocsaji
     */
    public enum AckSendStrategy {
        /** When IMMEDIATELY is set, the ACK is sent back immediately when the incoming message was received.
         * In this case, the receive transport pool is loaded with the extra processing.
         */
        IMMEDIATELY,
        /**
         * When SEND_CYCLE is set, the ACK is sent back in LwComm's standard message sending mechanism,
         * so it does not load receive transport pool, the sending is done in send pool.
         */
        SEND_CYCLE
    }

    /**
     * Structure for defining a port range for LwComm.
     * If set, the client sockets will be opened in this range.
     * @author Miklos Pocsaji
     *
     */
    public static class ClientPortRange {
        private int portMin;
        private int portMax;

        public ClientPortRange(int portMin, int portMax) {
            this.portMin = portMin;
            this.portMax = portMax;
        }

        public int getPortMin() {
            return portMin;
        }

        public int getPortMax() {
            return portMax;
        }

        @Override
        public String toString() {
            return "ClientPortRange [portMin=" + portMin + ", portMax=" + portMax + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + portMax;
            result = prime * result + portMin;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ClientPortRange other = (ClientPortRange) obj;
            if (portMax != other.portMax)
                return false;
            if (portMin != other.portMin)
                return false;
            return true;
        }

    }

    public abstract Set<Node> getAllNodes();

    public abstract Set<Route> getAllRoutes();

    public abstract Node getNodeByName(String name);

    public abstract Route getRouteByName(String name);

    public abstract DeploymentMode getDeploymentMode();

    public abstract int getHeartbeatIntervalMs();

    public abstract int getHeartbeatTimeoutMs();

    public abstract PoolConfig getReceiveTransportPoolConfig();

    public abstract PoolConfig getReceiveWorkerPoolConfig();

    public abstract PoolConfig getSendPoolConfig();

    public abstract String getConnectionFactoryJndi();

    public abstract String getLocalNodeName();

    public abstract ReceiveMode getReceiveMode();

    public abstract MessageReceiver getMessageReceiver();

    public abstract ListenerMode getListenerMode();

    public abstract AckSendStrategy getAckSendStrategy();

    public abstract ClientPortRange getClientPortRange();

    public abstract String getMBeanDomain();

    public final Node getLocalNode() {
        switch (getDeploymentMode()) {
        case JBOSS:
            return getNodeByName(System.getProperty("jboss.server.name"));
        case WEBLOGIC:
            return getNodeByName(System.getProperty("weblogic.server"));
        case STANDALONE:
            return getNodeByName(System.getProperty(PARAMETER_LWCOMM_NAME));
        case MULTIPLE:
            return getNodeByName(getLocalNodeName());
        default:
            return null;
        }
    }

    public final Set<Route> getAllRoutesFromLocalNode() {
        return getAllRoutesFrom(getLocalNode());
    }

    public final Set<Route> getAllRoutesFrom(Node node) {
        Set<Route> ret = new HashSet<Route>();
        for (Route r : getAllRoutes()) {
            if (r.getPossibleSources().contains(node))
                ret.add(r);
        }
        return ret;
    }

    public final Set<Route> getAllRoutesToLocalNode() {
        return getAllRoutesTo(getLocalNode());
    }

    public final Set<Route> getAllRoutesTo(Node node) {
        Set<Route> ret = new HashSet<Route>();
        for (Route r : getAllRoutes()) {
            if (r.getDestinations().contains(node))
                ret.add(r);
        }
        return ret;
    }

    public final Map<Node, Set<Node>> getHeartbeatMap() {
        Map<Node, Set<Node>> ret = new HashMap<Node, Set<Node>>();
        for (Route route : getAllRoutes()) {
            for (Node dest : route.getDestinations()) {
                Set<Node> nodesToSendHbTo = ret.get(dest);
                if (nodesToSendHbTo == null) {
                    nodesToSendHbTo = new HashSet<Node>();
                    ret.put(dest, nodesToSendHbTo);
                }
                nodesToSendHbTo.addAll(route.getPossibleSources());
            }
        }
        return ret;
    }

    public final Set<Node> getHeartbeatTargetsForNode(Node n) {
        return getHeartbeatMap().get(n);
    }

    public final Set<Node> getNodesToExpectHbFrom(Node n) {
        HashSet<Node> ret = new HashSet<Node>();
        for (Route r : getAllRoutes()) {
            if (r.getPossibleSources().contains(n)) {
                ret.addAll(r.getDestinations());
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        return "Configuration [getAllNodes()=" + getAllNodes() + ", getAllRoutes()=" + getAllRoutes()
                + ", getDeploymentMode()=" + getDeploymentMode() + ", getHeartbeatIntervalMs()="
                + getHeartbeatIntervalMs() + ", getHeartbeatTimeoutMs()=" + getHeartbeatTimeoutMs()
                + ", getReceiveTransportPoolConfig()=" + getReceiveTransportPoolConfig()
                + ", getReceiveWorkerPoolConfig()=" + getReceiveWorkerPoolConfig() + ", getSendPoolConfig()="
                + getSendPoolConfig() + ", getConnectionFactoryJndi()=" + getConnectionFactoryJndi()
                + ", getLocalNodeName()=" + getLocalNodeName() + ", getReceiveMode()=" + getReceiveMode()
                + ", getMessageReceiver()=" + getMessageReceiver() + ", getListenerMode()=" + getListenerMode()
                + ", getAckSendStrategy()=" + getAckSendStrategy() + ", getClientPortRange()=" + getClientPortRange()
                + ", getLocalNode()=" + getLocalNode() + "]";
    }

}
