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

import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ThreadDeathWatcher;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.service.LwCommService;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.SendResultFuture;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;
import org.restcomm.imscf.common.lwcomm.service.impl.statistics.LwCommStatistics;
import org.restcomm.imscf.common.lwcomm.service.messages.MessageSender;
import org.restcomm.imscf.common.lwcomm.service.messages.MessageSenderStore;

/**
 * LwCommService implementation.
 * @author Miklos Pocsaji
 * @author Tamas Gyorgyey
 */
public class LwCommServiceImpl implements LwCommService {

    public static final Logger LOGGER = LoggerFactory.getLogger(LwCommServiceImpl.class);

    private static final String STATISTICS_MBEAN_BASENAME = "org.restcomm.imscf.common.lwcomm:type=LwCommStatistics";

    private static ReadWriteLock initShutdownLock = new ReentrantReadWriteLock();

    private static LwCommServiceImpl service;

    private Configuration configuration;
    private NodeCatalog nodeCatalog;
    private LwCommListener listener;
    private HeartbeatService heartbeatService;
    private MultithreadEventLoopGroup sendAndHeartbeatEventLoopGroup;
    private MultithreadEventLoopGroup receiveTransportEventLoopGroup;
    private NettyUtil nettyUtil;
    private MessageSenderStore messageSenderStore;
    private LwCommStatistics statisticsMBean;
    private String statisticsMBeanName;
    private AcceptMode defaultAcceptMode;
    private ConcurrentHashMap<String, AcceptMode> overrideAcceptModes = new ConcurrentHashMap<>();

    private boolean inited = false;

    public static boolean init(Configuration config) {
        try {
            initShutdownLock.writeLock().lock();
            LOGGER.info("Initializing LwComm... local node: {}", config.getLocalNode());
            MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, LwCommUtil.LOGGER_MDC_UNKNOWN_MSGID);
            if (service != null && service.inited) {
                service.shutdown();
            }

            service = new LwCommServiceImpl();
            service.configuration = config;

            // Create executors
            // TODO handle PoolConfigs with REFERENCE type
            LOGGER.info("Listener mode of configuration: {}, Epoll available: {}", config.getListenerMode(),
                    Epoll.isAvailable());
            if (config.getListenerMode() == Configuration.ListenerMode.EPOLL && Epoll.isAvailable()) {
                service.sendAndHeartbeatEventLoopGroup = new EpollEventLoopGroup(config.getSendPoolConfig()
                        .getMaxThreads(), new NamingThreadFactory("lwcomm_send_epoll"));
                service.receiveTransportEventLoopGroup = new EpollEventLoopGroup(config.getReceiveTransportPoolConfig()
                        .getMaxThreads(), new NamingThreadFactory("lwcomm_receive_transport_epoll"));
            } else {
                if (config.getListenerMode() == Configuration.ListenerMode.EPOLL) {
                    LOGGER.warn("EPOLL listener mode is configured but it is unavailable. Cause follows.",
                            Epoll.unavailabilityCause());
                }
                service.sendAndHeartbeatEventLoopGroup = new NioEventLoopGroup(config.getSendPoolConfig()
                        .getMaxThreads(), new NamingThreadFactory("lwcomm_send"));
                service.receiveTransportEventLoopGroup = new NioEventLoopGroup(config.getReceiveTransportPoolConfig()
                        .getMaxThreads(), new NamingThreadFactory("lwcomm_receive_transport"));
            }

            // Create node catalog
            service.nodeCatalog = new NodeCatalog(config.getNodesToExpectHbFrom(config.getLocalNode()),
                    config.getHeartbeatTimeoutMs(), service.sendAndHeartbeatEventLoopGroup);
            // Create listener service
            service.listener = new LwCommListener(config, service.receiveTransportEventLoopGroup);
            // Create heartbeat service
            service.heartbeatService = new HeartbeatService(config.getHeartbeatTargetsForNode(config.getLocalNode()),
                    config.getHeartbeatIntervalMs(), service.sendAndHeartbeatEventLoopGroup);
            // Create message sender storage
            service.messageSenderStore = new MessageSenderStore();

            // Create netty utility (message sending)
            service.nettyUtil = new NettyUtil(config, service.sendAndHeartbeatEventLoopGroup,
                    service.receiveTransportEventLoopGroup, service.listener.getChannelHandler());

            // Initialize statistics MBean
            try {
                service.statisticsMBean = new LwCommStatistics();
                if (config.getMBeanDomain() != null) {
                    service.statisticsMBeanName = config.getMBeanDomain() + ":type=LwCommStatistics";
                } else {
                    service.statisticsMBeanName = STATISTICS_MBEAN_BASENAME + ",name="
                            + config.getLocalNode().getName();
                }
                ObjectName statMbeanName = new ObjectName(service.statisticsMBeanName);
                MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
                try {
                    mbeanServer.unregisterMBean(statMbeanName);
                } catch (Exception ex) {
                    LOGGER.info("Unregistering statistics mbean was unsuccesful, this is okay: {}", ex.getMessage());
                }
                mbeanServer.registerMBean(service.statisticsMBean, statMbeanName);
            } catch (Exception ex) {
                LOGGER.error("Error registering statistics mbean", ex);
            }
            // Start services
            service.nettyUtil.start();
            service.nodeCatalog.start();
            service.listener.start();
            service.heartbeatService.start();

            service.setAcceptMode(AcceptMode.ACCEPT);
            // Finished.
            service.inited = true;
            LOGGER.info("LwComm service initialized");
            return true;
        } finally {
            initShutdownLock.writeLock().unlock();
        }
    }

    public static LwCommService getService() {
        return service;
    }

    public static LwCommServiceImpl getServiceImpl() {
        return service;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public NettyUtil getNettyUtil() {
        return nettyUtil;
    }

    public NodeCatalog getNodeCatalog() {
        return nodeCatalog;
    }

    public MultithreadEventLoopGroup getSendAndHeartbeatEventLoopGroup() {
        return sendAndHeartbeatEventLoopGroup;
    }

    public MessageSenderStore getMessageSenderStore() {
        return messageSenderStore;
    }

    public LwCommStatistics getStatistics() {
        return statisticsMBean;
    }

    @Override
    public void setAcceptMode(AcceptMode mode, String... tags) {
        Objects.requireNonNull(mode, "AcceptMode parameter cannot be null");
        if (tags == null || tags.length == 0) {
            overrideAcceptModes.clear();
            defaultAcceptMode = mode;
        } else {
            for (String string : tags) {
                overrideAcceptModes.put(string, mode);
            }
        }
        LOGGER.info("LwComm tag accept mode changed to default {} with overrides {}", defaultAcceptMode,
                overrideAcceptModes);
    }

    AcceptMode getAcceptMode(String tag) {
        if (tag == null)
            return defaultAcceptMode;
        return overrideAcceptModes.getOrDefault(tag, defaultAcceptMode);
    }

    @Override
    public SendResultFuture<SendResult> send(String targetRoute, TextMessage message) {
        MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, LwCommUtil.LOGGER_MDC_UNKNOWN_MSGID);
        LOGGER.trace("BEGIN LwCommServiceImpl.send({}, {})", targetRoute, message);
        try {
            initShutdownLock.readLock().lock();
            if (!inited) {
                LOGGER.error("LwCommService not initialized!");
                SendResultFutureImpl ret = new SendResultFutureImpl(message);
                ret.done(SendResult.FAILURE);
                return ret;
            }
            SendResultFutureImpl ret = new SendResultFutureImpl(message);
            MessageSender ms = MessageSender.createNormal(targetRoute, message, ret);
            if (ms == null) {
                ret.done(SendResult.FAILURE);
            } else {
                ret.setMessageId(ms.getMessageId());
                ms.startSendCycle();
            }
            return ret;
        } finally {
            initShutdownLock.readLock().unlock();
            LOGGER.trace("END   LwCommServiceImpl.send({}, {})", targetRoute, message);
        }
    }

    @Override
    public void shutdown() {
        try {
            LOGGER.info("LwComm shutting down...");
            initShutdownLock.writeLock().lock();
            if (!inited) {
                LOGGER.info("LwComm service is not initialized, no shutdown needed.");
            }
            // Unregister statistics MBean
            try {
                LOGGER.info("Unregistering MBean {} ...", statisticsMBeanName);
                ObjectName on = new ObjectName(statisticsMBeanName);
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(on);
            } catch (Exception ex) {
                LOGGER.warn("Error unregistering statistics MBean", ex);
            }
            listener.shutdown();
            heartbeatService.shutdown();
            nettyUtil.shutdown();
            try {
                sendAndHeartbeatEventLoopGroup.shutdownGracefully().await();
            } catch (InterruptedException e) {
                LOGGER.warn("Error shutting down event loop group for sending messages and heartbeats.", e);
            }
            try {
                receiveTransportEventLoopGroup.shutdownGracefully().await();
            } catch (InterruptedException e) {
                LOGGER.warn("Error shutting down event loop group for receive transport event loop.", e);
            }
            LOGGER.info("Shutting down GlobalEventExecutor and calling ThreadDeathWatcher.awaitInactivity(). This can take at most 10 seconds...");
            try {
                GlobalEventExecutor.INSTANCE.awaitInactivity(5, TimeUnit.SECONDS);
                ThreadDeathWatcher.awaitInactivity(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Exception while waiting for inactivity...", e);
            }
            LOGGER.info("LwComm service shut down.");
        } finally {
            inited = false;
            service = null;
            initShutdownLock.writeLock().unlock();
        }
    }
}
