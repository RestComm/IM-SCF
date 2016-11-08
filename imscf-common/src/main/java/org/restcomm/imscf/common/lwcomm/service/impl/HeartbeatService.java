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
package org.restcomm.imscf.common.lwcomm.service.impl;

import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.service.messages.MessageSender;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * LwComm's heartbeat service.
 * Inputs are: nodes to send heartbeats to, heartbeat frequency, and the executor to use sending messages through netty.
 * @author Miklos Pocsaji
 *
 */
public class HeartbeatService {

    private Collection<Node> heartbeatTargets;
    private int heartbeatIntervalMs;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    public HeartbeatService(Collection<Node> heartbeatTargets, int heartbeatIntervalMs,
            ScheduledExecutorService executor) {
        this.heartbeatTargets = heartbeatTargets;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.executor = executor;
    }

    public void start() {
        LwCommServiceImpl.LOGGER.info("Starting sending heartbeats, interval {}ms, sending heartbeats to: {}",
                heartbeatIntervalMs, heartbeatTargets);
        future = executor.scheduleAtFixedRate(new HeartbeatWorker(), 0, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        future.cancel(true);
    }

    /**
     * Periodically executing Runnable.
     * Goes through the target nodes and tries to send a heartbeat. Errors are not handled just logged.
     * @author Miklos Pocsaji
     */
    private class HeartbeatWorker implements Runnable {

        @Override
        public void run() {
            for (Node hbTarget : heartbeatTargets) {
                try {
                    LwCommServiceImpl.LOGGER.trace("Sending heartbeat to {}", hbTarget);
                    MessageSender ms = MessageSender.createHeartbeat(hbTarget, null);
                    ms.startSendCycle();
                } catch (Exception ex) {
                    LwCommServiceImpl.LOGGER.error("Error while sending heartbeat to {}.", hbTarget, ex);
                }
            }
        }

    }
}
