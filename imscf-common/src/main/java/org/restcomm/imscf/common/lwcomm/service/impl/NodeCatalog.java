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
package org.restcomm.imscf.common.lwcomm.service.impl;

import org.restcomm.imscf.common.lwcomm.config.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Stores if a remote node is reachable or not.
 * Schedules a task heartbeatTimeout later for each node - if the task is due, the remote node is marked unavailable.
 * If a heartbeat arrives for a node, the schedule above is reset and the node is marked available.
 * @author Miklos Pocsaji
 *
 */
public class NodeCatalog {

    private int heartbeatTimeoutMs;
    private Map<Node, NodeInfo> nodeInfoMap;
    private ScheduledExecutorService executor;

    public NodeCatalog(Collection<Node> allNodes, int heartbeatTimeoutMs, ScheduledExecutorService executor) {
        this.heartbeatTimeoutMs = heartbeatTimeoutMs;
        this.executor = executor;
        this.nodeInfoMap = new HashMap<Node, NodeCatalog.NodeInfo>();
        for (Node n : allNodes) {
            NodeInfo ni = new NodeInfo();
            nodeInfoMap.put(n, ni);
        }
        LwCommServiceImpl.LOGGER.info("NodeCatalog initialized, hb timeout: {}, nodeinfomap: {}", heartbeatTimeoutMs,
                nodeInfoMap);
    }

    public boolean isNodeAlive(Node n) {
        return nodeInfoMap.get(n).alive;
    }

    public boolean isInfoAvailable(Node n) {
        return nodeInfoMap.containsKey(n);
    }

    protected void start() {
        for (Map.Entry<Node, NodeCatalog.NodeInfo> e : nodeInfoMap.entrySet()) {
            NodeInfo ni = e.getValue();
            ni.invalidator = executor.schedule(new InvalidatorTask(e.getKey()), heartbeatTimeoutMs,
                    TimeUnit.MILLISECONDS);
        }
    }

    protected void heartbeatFromNode(Node n) {
        NodeInfo ni = nodeInfoMap.get(n);
        if (!ni.invalidator.isDone() && !ni.invalidator.isCancelled()) {
            ni.invalidator.cancel(true);
        }
        ni.invalidator = executor.schedule(new InvalidatorTask(n), heartbeatTimeoutMs, TimeUnit.MILLISECONDS);
        if (!ni.alive) {
            LwCommServiceImpl.LOGGER.info("HB received from {}, marking as alive.", n);
        }
        ni.alive = true;
    }

    /**
     * Holds the following two information about the node.
     * <li>alive flag</li>
     * <li>the scheduled task which sets the alive flag to false (this task is cancelled and rescheduled when a hb arrives</li>
     * @author Miklos Pocsaji
     *
     */
    private static class NodeInfo {
        private volatile boolean alive = false;
        private volatile ScheduledFuture<?> invalidator;
    }

    /**
     * The invalidator task.
     * This task is scheduled for the hb timeout interval and is cancelled every time when a hb arrives.
     * @author Miklos Pocsaji
     *
     */
    private class InvalidatorTask implements Runnable {
        private Node nodeToInvalidate;

        public InvalidatorTask(Node nodeToInvalidate) {
            this.nodeToInvalidate = nodeToInvalidate;
        }

        @Override
        public void run() {
            LwCommServiceImpl.LOGGER.info("No HB received from {}, marking as not alive.", nodeToInvalidate);
            nodeInfoMap.get(nodeToInvalidate).alive = false;
        }
    }
}
