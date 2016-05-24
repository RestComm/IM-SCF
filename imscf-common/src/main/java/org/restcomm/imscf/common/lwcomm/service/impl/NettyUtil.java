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

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.MDC;

import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.service.messages.LwCommMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

/**
 * Various utility methods for Netty usage.
 * @author Miklos Pocsaji
 *
 */
public class NettyUtil {

    private MultithreadEventLoopGroup sendExecutor;
    private MultithreadEventLoopGroup receiveExecutor;
    private ChannelHandler channelHandler;
    private boolean epoll;
    private Configuration.ClientPortRange clientPortRange;
    private String localHost;
    private BlockingDeque<Channel> availableChannels = new LinkedBlockingDeque<Channel>();
    private Set<Channel> allChannels = new HashSet<Channel>();
    private Iterator<Integer> ports;

    /**
     * The only constructor.
     * @param config The LwComm configuration
     * @param sendExecutor The event loop group created in LwCommServiceImpl.init()
     * @param receiveExecutor The event loop group created in LwCommServiceImpl.init()
     * @param channelHandler The channel handler created by LwCommListener - the one and only channel handler
     */
    public NettyUtil(Configuration config, MultithreadEventLoopGroup sendExecutor,
            MultithreadEventLoopGroup receiveExecutor, ChannelHandler channelHandler) {
        this.sendExecutor = sendExecutor;
        this.receiveExecutor = receiveExecutor;
        this.channelHandler = channelHandler;
        this.epoll = config.getListenerMode() == Configuration.ListenerMode.EPOLL;
        this.epoll = this.epoll && Epoll.isAvailable();
        this.clientPortRange = config.getClientPortRange();
        if (clientPortRange != null && clientPortRange != Configuration.NO_CLIENT_PORT_RANGE) {
            ports = new SecureRandom().ints(clientPortRange.getPortMin(), clientPortRange.getPortMax() + 1).iterator();
        }
        this.localHost = config.getLocalNode().getHost();
    }

    public void start() {
        int count = sendExecutor.executorCount();
        final CountDownLatch cdl = new CountDownLatch(count);

        Bootstrap b = new Bootstrap();
        Class<? extends DatagramChannel> chClass = epoll ? EpollDatagramChannel.class : NioDatagramChannel.class;
        b.group(receiveExecutor).channel(chClass).handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline().addLast(channelHandler);
            }
        });

        for (int i = 0; i < count; i++) {
            int bindPort = pickBindPort();
            b.bind(localHost, bindPort).addListener(new BindListener(b, bindPort, cdl));
        }
        // Wait for all bind operations to complete:
        try {
            cdl.await();
        } catch (InterruptedException e) {
            LwCommServiceImpl.LOGGER.error("await() interrupted", e);
        }
        LwCommServiceImpl.LOGGER.info("NettyUtil initialized {} outbound channels.", availableChannels.size());
    }

    public void shutdown() {
        for (Channel ch : allChannels) {
            try {
                ch.close().await();
            } catch (InterruptedException e) {
                LwCommServiceImpl.LOGGER.warn("Error closing LwComm sender channel {}", ch, e);
            }
        }
    }

    public void sendMessage(final Node target, final LwCommMessage message) {
        MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, message.getId());
        LwCommServiceImpl.LOGGER.debug("BEGIN sendMessage()");
        sendExecutor.execute(new SenderExecutor(target, message));
        LwCommServiceImpl.LOGGER.debug("  END sendMessage()");
    }

    private int pickBindPort() {
        if (clientPortRange == null || clientPortRange == Configuration.NO_CLIENT_PORT_RANGE) {
            return 0;
        } else {
            return ports.next();
        }
    }

    /**
     * Listener which is called whin a channel bind is finished - either with success or not.
     * @author Miklos Pocsaji
     *
     */
    private class BindListener implements ChannelFutureListener {
        private int targetPort;
        private Bootstrap b;
        private CountDownLatch cdl;

        public BindListener(Bootstrap b, int targetPort, CountDownLatch cdl) {
            this.b = b;
            this.targetPort = targetPort;
            this.cdl = cdl;
        }

        @Override
        public void operationComplete(ChannelFuture cf) throws Exception {
            if (cf.isSuccess()) {
                LwCommServiceImpl.LOGGER.info("Outbound channel {} - bind successful on port {}", cf.channel(),
                        targetPort);
                availableChannels.add(cf.channel());
                allChannels.add(cf.channel());
                cdl.countDown();
            } else {
                if (clientPortRange == null || clientPortRange == Configuration.NO_CLIENT_PORT_RANGE) {
                    LwCommServiceImpl.LOGGER.error("Outbound channel {} - bind unsuccessful", cf.channel(), cf.cause());
                    allChannels.add(cf.channel());
                    cdl.countDown();
                } else {
                    // Retry since we have a range defined.
                    LwCommServiceImpl.LOGGER.info("Outbound channel {} - bind unsuccessful on port {}, reason: {}",
                            cf.channel(), targetPort, cf.cause().getMessage());
                    int newPort = pickBindPort();
                    LwCommServiceImpl.LOGGER.info("Trying to bind on port {}", newPort);
                    b.bind(localHost, newPort).addListener(new BindListener(b, newPort, cdl));
                }
            }

        }
    }

    /**
     * Task for executing the sending.
     * @author Miklos Pocsaji
     */
    private class SenderExecutor implements Runnable {
        private Node target;
        private LwCommMessage message;

        public SenderExecutor(Node target, LwCommMessage message) {
            this.target = target;
            this.message = message;
        }

        public void run() {
            MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, message.getId());
            LwCommServiceImpl.LOGGER.trace("BEGIN NettyUtil.SenderExecutor.run()");
            try {
                long then = System.nanoTime();
                Channel ch = availableChannels.takeFirst();
                long now = System.nanoTime();
                long delay = (now - then) / 1000;
                LwCommServiceImpl.LOGGER.trace("Waited for an available channel {}us", delay);
                LwCommServiceImpl.getServiceImpl().getStatistics().timeSpentWaitingForChannel(delay);
                DatagramPacket dp = new DatagramPacket(
                        Unpooled.copiedBuffer(message.toRawMessage(), CharsetUtil.UTF_8), new InetSocketAddress(
                                target.getHost(), target.getPort()));
                ch.writeAndFlush(dp).addListener(new WriteFinishedListener(message.getId()));
            } catch (InterruptedException e) {
                LwCommServiceImpl.LOGGER.trace("Interrupted while waiting for free channel to send message", e);
            }
            LwCommServiceImpl.LOGGER.trace("  END NettyUtil.SenderExecutor.run()");
        }
    }

    /**
     * Runs when the message sending is finished. Puts back the channel to the available channels.
     * @author Miklos Pocsaji
     *
     */
    private class WriteFinishedListener implements ChannelFutureListener {

        private String messageId;

        public WriteFinishedListener(String messageId) {
            this.messageId = messageId;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, messageId);
            LwCommServiceImpl.LOGGER.trace("BEGIN NettyUtil.WriteFinishedListener.operationComplete() - {}",
                    future.channel());
            availableChannels.addLast(future.channel());
            LwCommServiceImpl.LOGGER.trace("  END NettyUtil.WriteFinishedListener.operationComplete()");
        }
    }

}
