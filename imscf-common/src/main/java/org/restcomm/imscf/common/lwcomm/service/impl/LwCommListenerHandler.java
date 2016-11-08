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

import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.service.LwCommService.AcceptMode;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;
import org.restcomm.imscf.common.lwcomm.service.messages.LwCommMessage;
import org.restcomm.imscf.common.lwcomm.service.messages.MessageSender;
import org.restcomm.imscf.common.lwcomm.service.messages.OutgoingMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.slf4j.MDC;

import com.google.common.cache.Cache;

/**
 * Netty handler, called when a new UDP packet arrives.
 * <b>Important:</b> Use only one instance for all incoming packets!!!
 * @author Miklos Pocsaji
 * @author Tamas Gyorgyey
 */
@Sharable
// TODO could be refactored
@SuppressWarnings("PMD.GodClass")
public final class LwCommListenerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Object MESSAGE_SENDER_STORE_ACCEPTED_VALUE = new Object();
    private static final Object MESSAGE_SENDER_STORE_REJECTED_VALUE = new Object();

    private QueueConnection queueConn;
    private Configuration.DeploymentMode deploymentMode;
    private Configuration.ReceiveMode receiveMode;
    private Configuration.AckSendStrategy ackSendStrategy;
    private volatile MessageReceiver messageReceiver;
    private volatile Cache<String, Object> processedMessageStore;
    private volatile Cache<String, Set<Node>> receivedAckStore;
    private OrderedExecutor messageDeliveryExecutor;

    /**
     * Creates a new handler.
     * @param queueConn The queue connection created.
     * Can be null if the message transport does not involve queues.
     * @param conf The LwComm configuration.
     * @param processedMessageStore The store of messages which have been processed in the near future.
     * @param receivedAckStore The store of ACK messages (identifier maps to sender Node) which
     * have been processed in the near future.
     * @param release If true, a release() will be called on the incoming packet when processing completes.
     * Is set to true when the handling is done in a different thread, not netty's thread (i.e. when
     * the listener mode is NIO_SEPARATE).
     */
    public LwCommListenerHandler(QueueConnection queueConn, Configuration conf,
            Cache<String, Object> processedMessageStore, Cache<String, Set<Node>> receivedAckStore) {
        this.queueConn = queueConn;
        this.deploymentMode = conf.getDeploymentMode();
        this.receiveMode = conf.getReceiveMode();
        this.messageReceiver = conf.getMessageReceiver();
        this.ackSendStrategy = conf.getAckSendStrategy();
        this.processedMessageStore = processedMessageStore;
        this.receivedAckStore = receivedAckStore;
        this.messageDeliveryExecutor = new OrderedExecutor(Executors.newFixedThreadPool(conf
                .getReceiveWorkerPoolConfig().getMaxThreads(), new NamingThreadFactory("lwcomm_receive_worker")));
    }

    public void channelRead0(ChannelHandlerContext context, DatagramPacket packet) throws Exception {
        long nanoBegin = System.nanoTime();
        try {
            MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, "unknown");
            // TODO: content will be truncated at the receive buffer size. See LwCommListener.start()
            String content = packet.content().toString(CharsetUtil.UTF_8);
            LwCommServiceImpl.LOGGER.trace("Message got from {}, content:\n{}", packet.sender(), content);
            LwCommMessage message = new LwCommMessage(content);
            MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, message.getId());
            LwCommServiceImpl.LOGGER.debug("LwCommHandler.channelRead0 - thread: {}", Thread.currentThread());
            switch (message.getType()) {
            case NORMAL:
                handleNormalMessage(message, content, context);
                break;
            case ACK:
                handleAck(message, content);
                break;
            case NACK:
                handleNack(message);
                break;
            case HEARTBEAT:
                LwCommServiceImpl.LOGGER.debug("heartbeat from {}", message.getFrom());
                LwCommServiceImpl.getServiceImpl().getNodeCatalog().heartbeatFromNode(message.getFrom());
                LwCommServiceImpl.getServiceImpl().getStatistics().incReceivedHeartbeatCount();
                break;
            case INVALID:
                LwCommServiceImpl.LOGGER.warn("invalid message: {}", content);
                LwCommServiceImpl.getServiceImpl().getStatistics().incInvalidMessageCount();
                break;
            default:
                LwCommServiceImpl.LOGGER.error("Unexpected message type: {}", message.getType());
                break;
            }
        } finally {
            long nanoEnd = System.nanoTime();
            long delay = (nanoEnd - nanoBegin) / 1000;
            LwCommServiceImpl.getServiceImpl().getStatistics().timeSpentInChannelRead0(delay);
            LwCommServiceImpl.LOGGER.debug("Receive handler took: {}us.", delay);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LwCommServiceImpl.LOGGER.error("Exception in LwComm receiver handler", cause);
    }

    public void shutdown() {
        messageDeliveryExecutor.shutdown();
    }

    private void handleNormalMessage(LwCommMessage message, String content, ChannelHandlerContext context) {
        if (message.getPayloadBytes() != message.getCalculatedPayloadBytes()) {
            LwCommServiceImpl.LOGGER.error(
                    "Payload size mismatch in message id {}. In header: {}, actual: {}, raw text received is:\n{}",
                    message.getId(), message.getPayloadBytes(), message.getCalculatedPayloadBytes(), content);
        }
        LwCommServiceImpl.LOGGER.debug("Message is normal message");

        AcceptMode acceptMode = LwCommServiceImpl.getServiceImpl().getAcceptMode(message.getUserTag());
        boolean sendAck = false, sendNack = false, deliver = false;
        Object oldValue;

        switch (acceptMode) {
        case ACCEPT:
            oldValue = processedMessageStore.asMap().putIfAbsent(message.getId(), MESSAGE_SENDER_STORE_ACCEPTED_VALUE);
            if (oldValue == null) {
                deliver = true;
                sendAck = true;
                LwCommServiceImpl.LOGGER.debug("Accepting message");
            }
            break;
        case REJECT:
            oldValue = processedMessageStore.asMap().putIfAbsent(message.getId(), MESSAGE_SENDER_STORE_REJECTED_VALUE);
            if (oldValue == null) {
                sendNack = true;
                LwCommServiceImpl.LOGGER.debug("Rejecting message");
                LwCommServiceImpl.getServiceImpl().getStatistics().incRejectedIncomingMessageCount();
            }
            break;
        case DROP:
            // don't store in map to allow accepting a later retransmit if the acceptMode changes back to ACCEPT
            oldValue = processedMessageStore.asMap().get(message.getId());
            if (oldValue == null) {
                LwCommServiceImpl.LOGGER.debug("Dropping message");
                LwCommServiceImpl.getServiceImpl().getStatistics().incDroppedIncomingMessageCount();
            }
            break;
        default:
            LwCommServiceImpl.LOGGER.error("Invalid acceptMode: {}", acceptMode);
            return;
        }

        if (MESSAGE_SENDER_STORE_ACCEPTED_VALUE.equals(oldValue)) {
            sendAck = true;
            LwCommServiceImpl.LOGGER.debug("Sending ACK again to previously accepted message");
        } else if (MESSAGE_SENDER_STORE_REJECTED_VALUE.equals(oldValue)) {
            sendNack = true;
            LwCommServiceImpl.LOGGER.debug("Sending NACK again to previously rejected message");
        }

        if (deliver) {
            LwCommServiceImpl.LOGGER.debug("Delivering message");
            messageDeliveryExecutor.execute(new DeliverMessageHandler(message), message.getGroupId());
            LwCommServiceImpl.getServiceImpl().getStatistics().incProcessedIncomingMessageCount();
        } else if (oldValue != null) {
            // There was already a value in the cache
            LwCommServiceImpl.LOGGER
                    .info("Received message with id {} more than one times! However, this does not cause errors since the message has been delivered at most once. Message content:\n{}",
                            message.getId(), content);
            LwCommServiceImpl.getServiceImpl().getStatistics().incOutOfOrderMessageCount();
        }

        LwCommServiceImpl.getServiceImpl().getStatistics()
                .setProcessedIncomingMessageStoreSize(processedMessageStore.size());

        if (!sendAck && !sendNack)
            return;

        switch (ackSendStrategy) {
        case IMMEDIATELY:
            // Sending ACK/NACK in this channel instead of on a new one
            LwCommServiceImpl.LOGGER.debug("Sending back {} immediately", sendAck ? "ACK" : "NACK");
            sendAckImmediately(message, sendAck, context);
            break;
        case SEND_CYCLE:
            // Sending ACK/NACK in normal send cycle
            LwCommServiceImpl.LOGGER.debug("Sending back {} in standard loop to dedicated port", sendAck ? "ACK"
                    : "NACK");
            MessageSender.createAck(message, sendAck, null).startSendCycle();
            break;
        default:
            LwCommServiceImpl.LOGGER.error("Invalid ackSendStrategy: {}", ackSendStrategy);
            break;
        }

    }

    private void sendAckImmediately(LwCommMessage message, boolean positive, ChannelHandlerContext context) {
        OutgoingMessage ack = positive ? OutgoingMessage.createAck(message) : OutgoingMessage.createNack(message);
        DatagramPacket ackDp = new DatagramPacket(Unpooled.copiedBuffer(ack.toRawMessage(), CharsetUtil.UTF_8),
                new InetSocketAddress(message.getFrom().getHost(), message.getFrom().getPort()));
        context.writeAndFlush(ackDp);
    }

    private void handleAck(LwCommMessage message, String content) {
        LwCommServiceImpl.LOGGER.debug("ACK arrived for {}", message.getId());
        MessageSender ms = LwCommServiceImpl.getServiceImpl().getMessageSenderStore().getMessageSender(message.getId());
        Set<Node> acksReceivedFrom = receivedAckStore.getIfPresent(message.getId());
        if (ms != null) {
            ms.ackArrived(message);
            LwCommServiceImpl.getServiceImpl().getStatistics().incProcessedAckCount();
        } else {
            if (acksReceivedFrom == null) {
                LwCommServiceImpl.LOGGER.warn(
                        "No MessageSender found for ACK message, and no ACK received in the near past. Message:\n{}",
                        message);
            } else {
                LwCommServiceImpl.LOGGER
                        .warn("Multiple ACK received for message with id {}. This is usually caused by retransmits. Check the sender side logs. Message: {}",
                                message.getId(), message);
            }
        }
        if (acksReceivedFrom == null) {
            acksReceivedFrom = new HashSet<Node>();
            receivedAckStore.put(message.getId(), acksReceivedFrom);
        }
        acksReceivedFrom.add(message.getFrom());
        if (acksReceivedFrom.size() > 1) {
            LwCommServiceImpl.LOGGER
                    .error("ACKs received for message ({}) from multiple nodes: {} -- This message has been processed multiple times! Message content:\n{}",
                            message.getId(), acksReceivedFrom, content);
            LwCommServiceImpl.getServiceImpl().getStatistics().incOutOfOrderAckCount();
        }
        LwCommServiceImpl.getServiceImpl().getStatistics().setReceivedAckStoreSize(receivedAckStore.size());
    }

    private void handleNack(LwCommMessage message) {
        LwCommServiceImpl.LOGGER.debug("NACK arrived for {}", message.getId());
        MessageSender ms = LwCommServiceImpl.getServiceImpl().getMessageSenderStore().getMessageSender(message.getId());
        if (ms != null) {
            ms.nackArrived(message);
            LwCommServiceImpl.getServiceImpl().getStatistics().incProcessedNackCount();
        }
    }

    /**
     * This runnable is executed in messageDeliveryExecutor.
     * Delivers the message to the client or puts it on the queue
     * @author Miklos Pocsaji
     *
     */
    private class DeliverMessageHandler implements Runnable {

        private LwCommMessage message;

        public DeliverMessageHandler(LwCommMessage message) {
            this.message = message;
        }

        @Override
        public void run() {
            MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, message.getId());
            deliverMessage(message);
        }

        private void deliverMessage(LwCommMessage message) {
            long then = System.nanoTime();
            MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, message.getId());
            try {
                switch (receiveMode) {
                case JMS_QUEUE:
                    writeQueue(message.getTargetQueue(), message.getPayload());
                    break;
                case LISTENER:
                    try {
                        messageReceiver.onMessage(message.asIncomingTextMessage());
                    } catch (Exception ex) {
                        LwCommServiceImpl.LOGGER.error(
                                "Error in group id locking or message receiver threw exception.", ex);
                    }
                    break;
                default:
                    break;
                }
            } finally {
                long now = System.nanoTime();
                long delay = (now - then) / 1000;
                LwCommServiceImpl.LOGGER.trace("deliverMessage() took {}us", delay);
                LwCommServiceImpl.getServiceImpl().getStatistics().timeWorker(delay);
            }
        }

        private void writeQueue(String queue, String message) {
            if (deploymentMode == Configuration.DeploymentMode.STANDALONE)
                return;
            if (queueConn != null) {
                QueueSession qsess = null;
                QueueSender qsender = null;
                try {
                    InitialContext ctx = new InitialContext();
                    Queue q = (Queue) ctx.lookup(queue);
                    qsess = queueConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                    qsender = qsess.createSender(q);
                    TextMessage tm = qsess.createTextMessage(message);
                    // TODO handle JMS/HornetQ grouping
                    // TODO how to put "From" on JMS queue?
                    qsender.send(tm);
                    LwCommServiceImpl.getServiceImpl().getStatistics().incQueuedIncomingMessageCount();
                } catch (Exception ex) {
                    LwCommServiceImpl.LOGGER.error("Error while putting message into queue", ex);
                } finally {
                    if (qsender != null) {
                        try {
                            qsender.close();
                        } catch (Exception ex) {
                            LwCommServiceImpl.LOGGER.warn("Error closing queue sender", ex);
                        }
                    }
                    if (qsess != null) {
                        try {
                            qsess.close();
                        } catch (Exception ex) {
                            LwCommServiceImpl.LOGGER.warn("Error closing queue session", ex);
                        }
                    }
                }
            } else {
                LwCommServiceImpl.LOGGER
                        .error("Message will not be sent to queue {} because queue connection could not be resolved at service init.",
                                queue);
            }
        }
    }

}
