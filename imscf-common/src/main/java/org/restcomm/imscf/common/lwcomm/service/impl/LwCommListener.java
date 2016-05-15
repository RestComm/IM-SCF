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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.InitialContext;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.config.Configuration.ListenerMode;
import org.restcomm.imscf.common.lwcomm.config.Node;

/**
 * The instance of this class is listening on the UDP port specified.
 * Handles ACK, HB and normal messages.
 * @author Miklos Pocsaji
 *
 */
public class LwCommListener {

    private EventLoopGroup receiveTransportGroup;
    private QueueConnectionFactory queueCf;
    private QueueConnection queueConn;
    private Configuration config;
    private LwCommListenerHandler channelHandler;
    private Set<Channel> channels;

    public LwCommListener(Configuration config, EventLoopGroup receiveTransportGroup) {
        this.config = config;
        this.receiveTransportGroup = receiveTransportGroup;

        // Determine that processed message ids should be stored for how much time
        // maxTimeout holds the maximum timeout among routes towards this node
        int receiveThreads = config.getReceiveTransportPoolConfig().getMaxThreads();
        // the routes which contain the local node as a destination
        Node localNode = config.getLocalNode();
        OptionalInt maxTimeout = config.getAllRoutes().stream().filter(r -> r.getDestinations().contains(localNode))
                .map(r -> r.getRetransmitPattern()) // list of retransmit patterns (list of list of ints)
                .flatMap(l -> l.stream()) // list of ints
                .mapToInt(i -> i).max();
        // By default, hold the processed message id two times of the maximum timeout.
        // If there is no route towards this node, the message age will be zero, so no message will be stored.
        int messageAge = maxTimeout.isPresent() ? (int) Math.ceil(2.0d * maxTimeout.getAsInt() / 1000d) : 0;
        LwCommServiceImpl.LOGGER.info("Constructing aging set for processed messages. Message age: {} seconds.",
                messageAge);
        // Stores ids of messages which have been processed recently. Used to avoid double processing of messages.
        Cache<String, Object> processedMessageStore = CacheBuilder.newBuilder().concurrencyLevel(receiveThreads)
                .expireAfterWrite(messageAge, TimeUnit.SECONDS).build();

        // Determine that how much time the received ACK messages should be stored
        // How long should we wait for an ACK? It's the maximum message delivery timeout.
        // Which is for a given route: the last element of the retransmit pattern (the message timeout towards a node)
        // times the node count in the destinations list times two
        OptionalInt maxAckTimeout = config.getAllRoutes().stream()
                .filter(r -> r.getPossibleSources().contains(localNode)).mapToInt(r -> (int) Math.ceil(
                // the last element of the retransmit pattern (the message timeout towards a node):
                        r.getRetransmitPattern().get(r.getRetransmitPattern().size() - 1) / 1000d
                        // times the node count in the destinations list
                                * r.getDestinations().size()
                                // times two
                                * 2.0d)).max();
        int ackAge = maxAckTimeout.orElse(0);
        LwCommServiceImpl.LOGGER.info("Constructing aging set for received ACK messages. Age: {} seconds", ackAge);
        // Stores the received ACK messages
        Cache<String, Set<Node>> receivedAckStore = CacheBuilder.newBuilder().concurrencyLevel(receiveThreads)
                .expireAfterWrite(ackAge, TimeUnit.SECONDS).build();

        if (config.getDeploymentMode() != Configuration.DeploymentMode.STANDALONE
                && config.getDeploymentMode() != Configuration.DeploymentMode.MULTIPLE) {
            try {
                InitialContext ctx = new InitialContext();
                queueCf = (QueueConnectionFactory) ctx.lookup(config.getConnectionFactoryJndi());
                queueConn = queueCf.createQueueConnection();
            } catch (Exception ex) {
                LwCommServiceImpl.LOGGER.error("Error while looking up connection factory", ex);
            }
        }

        channelHandler = new LwCommListenerHandler(queueConn, config, processedMessageStore, receivedAckStore);
    }

    void start() {
        // TODO: handle AS-resolved pools
        int receiveTransportThreads = config.getReceiveTransportPoolConfig().getMaxThreads();
        int receiveWorkerThreads = config.getReceiveWorkerPoolConfig().getMaxThreads();

        // Netty 4.0 does not handle parallel UDP servers well.
        // See: https://github.com/netty/netty/issues/1706
        // We differentiate two listener modes:
        //
        // a) NIO
        // ------
        // In this case a simple NioEventLoopGroup is used. The NioEventLoopGroup is given
        // "receiveTransportThreads" number of threads. User listener will be called
        // in a different executor which has receiveWorkerThreads number of threads.
        // This does not work well with netty 4.0 but still implemented here
        // in case of it will be fixed in a future netty version (the problem is
        // that regardless of setting the nThreads parameter in NioEventLoopGroup only
        // one thread is used for incoming packet processing...).
        //
        // c) EPOLL
        // --------
        // The solution offered in the link above:
        // 1) Use the epoll transport (Linux only)
        // 2) Turn on SO_REUSEPORT option
        // 3) Create multiple datagram channels bound to the same port
        // According to this: http://stackoverflow.com/questions/3261965/so-reuseport-on-linux
        // only works on Linux with kernel 3.9+ or RHEL 6.5+ -- if epoll is not available,
        // it falls back to NIO mode.

        LwCommServiceImpl.LOGGER.info(
                "Starting LwCommListener. Receive transport threads: {}, receive worker threads: {}",
                receiveTransportThreads, receiveWorkerThreads);
        Configuration.ListenerMode listenerMode = config.getListenerMode();
        LwCommServiceImpl.LOGGER.info("Listener mode configured is {}", config.getListenerMode());
        if (listenerMode == Configuration.ListenerMode.EPOLL && !Epoll.isAvailable()) {
            LwCommServiceImpl.LOGGER
                    .warn("Listener mode EPOLL is configured but is not available. Falling back to NIO mode.");
            listenerMode = Configuration.ListenerMode.NIO;
        }
        Bootstrap b = new Bootstrap();

        b.group(receiveTransportGroup);
        if (receiveTransportGroup instanceof EpollEventLoopGroup) {
            b.channel(EpollDatagramChannel.class);
            b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            b.option(EpollChannelOption.SO_REUSEPORT, true);
        } else {
            b.channel(NioDatagramChannel.class);
            b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
        channels = new HashSet<Channel>();
        b.handler(new ChannelInitializer<DatagramChannel>() {
            protected void initChannel(DatagramChannel channel) throws Exception {
                LwCommServiceImpl.LOGGER.info("Initializing channel: '{}'", channel);
                channels.add(channel);
                channel.pipeline().addLast(channelHandler);
            }
        });
        // TODO FIXME: hardcoded 256K limit for receive buffer!
        b.option(ChannelOption.SO_RCVBUF, 256 * 1024);
        b.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(10240));

        InetAddress host = null;
        int port = config.getLocalNode().getPort();
        try {
            host = InetAddress.getByName(config.getLocalNode().getHost());
            ChannelFuture future;
            if (listenerMode == ListenerMode.NIO) {
                future = b.bind(host, port).sync();
                if (!future.isSuccess()) {
                    LwCommServiceImpl.LOGGER.error("Error while binding socket to {}:{}", host, port);
                } else {
                    LwCommServiceImpl.LOGGER.info("Binding socket to {}:{} - SUCCESS", host, port);
                }
            } else {
                for (int i = 0; i < receiveTransportThreads; i++) {
                    future = b.bind(host, port).sync();
                    if (!future.isSuccess()) {
                        LwCommServiceImpl.LOGGER.error("Error while binding {} of {} socket to {}:{}", i + 1,
                                receiveTransportThreads, host, port);
                    } else {
                        LwCommServiceImpl.LOGGER.info("Successfully bound socket {} of {} to {}:{} - ", i + 1,
                                receiveTransportThreads, host, port, future.channel());
                    }
                }
            }
        } catch (Exception e) {
            LwCommServiceImpl.LOGGER.error("Error while binding socket or getting local node address.", e);
        }
    }

    void shutdown() {
        for (Channel ch : channels) {
            try {
                ch.close().await();
            } catch (InterruptedException e) {
                LwCommServiceImpl.LOGGER.warn("Error closing receive channel {}", ch, e);
            }
        }
        try {
            if (queueConn != null) {
                queueConn.close();
            }
        } catch (Exception ex) {
            LwCommServiceImpl.LOGGER.warn("Error while closing Queue connection factory", ex);
        }
        channelHandler.shutdown();
    }

    ChannelHandler getChannelHandler() {
        return channelHandler;
    }
}
