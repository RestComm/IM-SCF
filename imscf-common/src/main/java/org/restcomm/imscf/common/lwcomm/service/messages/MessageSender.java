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
package org.restcomm.imscf.common.lwcomm.service.messages;

import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.config.Route;
import org.restcomm.imscf.common.lwcomm.config.Route.Mode;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;
import org.restcomm.imscf.common.lwcomm.service.impl.LwCommServiceImpl;
import org.restcomm.imscf.common.lwcomm.service.impl.LwCommUtil;
import org.restcomm.imscf.common.lwcomm.service.impl.NettyUtil;
import org.restcomm.imscf.common.lwcomm.service.impl.NodeCatalog;
import org.restcomm.imscf.common.lwcomm.service.impl.SendResultFutureImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;

/**
 * Sender for a particular message.
 * A new instance is created for every outgoing message (normal, ACK and HB as well).
 * Handles retransmission.
 * MULTICAST mode is not implemented!
 * @author Miklos Pocsaji
 * @author Tamas Gyorgyey
 */
// TODO could be refactored
@SuppressWarnings("PMD.GodClass")
public final class MessageSender {

    private OutgoingMessage messageToSend;
    private SendChainElement currentSendChainElement;
    private NettyUtil nettyUtil;
    private ScheduledExecutorService executor;
    private LinkedList<SendChainElement> sendChain;
    private Future<?> timeoutFuture;
    // private NodeCatalog nodeCatalog;
    private SendResultFutureImpl sendResultFuture;
    private volatile boolean active = true;
    private long startTimeNanos;
    private TimeoutHandler timeoutHandler = new TimeoutHandler();

    private MessageSender(OutgoingMessage messageToSend, LinkedList<SendChainElement> sendChain,
            SendResultFutureImpl sendResultFuture) {
        this.messageToSend = messageToSend;
        this.nettyUtil = LwCommServiceImpl.getServiceImpl().getNettyUtil();
        this.executor = LwCommServiceImpl.getServiceImpl().getSendAndHeartbeatEventLoopGroup();
        // this.nodeCatalog = LwCommServiceImpl.getServiceImpl().getNodeCatalog();
        this.sendChain = sendChain;
        this.sendResultFuture = sendResultFuture;
    }

    public static MessageSender createNormal(String targetRoute, TextMessage textMessage,
            SendResultFutureImpl sendResultFuture) {
        Route route = LwCommServiceImpl.getServiceImpl().getConfiguration().getRouteByName(targetRoute);
        OutgoingMessage messageToSend = OutgoingMessage.create(route, textMessage);
        MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, messageToSend.getId());
        NodeCatalog nodeCatalog = LwCommServiceImpl.getServiceImpl().getNodeCatalog();
        Node localNode = LwCommServiceImpl.getServiceImpl().getConfiguration().getLocalNode();

        if (!route.getPossibleSources().contains(localNode)) {
            LwCommServiceImpl.LOGGER.error(
                    "Local node {} is not among the possible sources of route {}, so message '{}' cannot be sent",
                    localNode, route, textMessage);
            return null;
        }

        // First, check for multicast mode - it's not implemented yet
        if (route.getMode() == Mode.MULTICAST) {
            throw new UnsupportedOperationException("MULTICAST mode is not yet implemented");
        }

        List<Node> aliveTargets = new ArrayList<Node>();
        route.getDestinations().forEach(n -> {
            if (nodeCatalog.isNodeAlive(n))
                aliveTargets.add(n);
        });
        // LOADBALANCE and FAILOVER modes only differ that in FAILOVER mode the targets are probed in a fix order
        if (route.getMode() == Mode.LOADBALANCE) {
            Collections.shuffle(aliveTargets);
        }
        LinkedList<SendChainElement> sendChain = new LinkedList<SendChainElement>();
        boolean failover = false;
        for (Node target : aliveTargets) {
            int retransmitCount = 0;
            int prevTimeout = 0;
            for (int timeout : route.getRetransmitPattern()) {
                SendChainElement sce = new SendChainElement(target, timeout - prevTimeout, retransmitCount++, failover);
                sendChain.add(sce);
                prevTimeout = timeout;
            }
            failover = true;
        }
        MessageSender ret = new MessageSender(messageToSend, sendChain, sendResultFuture);
        LwCommServiceImpl.getServiceImpl().getMessageSenderStore().registerMessageSender(ret);
        return ret;
    }

    public static MessageSender createAck(LwCommMessage ackFor, boolean positive, SendResultFutureImpl sendResultFuture) {
        OutgoingMessage messageToSend = positive ? OutgoingMessage.createAck(ackFor) : OutgoingMessage.createNack(ackFor);
        LinkedList<SendChainElement> sendChain = new LinkedList<SendChainElement>();
        sendChain.add(new SendChainElement(ackFor.getFrom(), -1, 0, false));
        return new MessageSender(messageToSend, sendChain, sendResultFuture);
    }

    public static MessageSender createHeartbeat(Node target, SendResultFutureImpl sendResultFuture) {
        OutgoingMessage messageToSend = OutgoingMessage.createHeartbeat();
        LinkedList<SendChainElement> sendChain = new LinkedList<SendChainElement>();
        sendChain.add(new SendChainElement(target, -1, 0, false));
        return new MessageSender(messageToSend, sendChain, sendResultFuture);
    }

    public void startSendCycle() {
        MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, messageToSend.getId());
        LwCommServiceImpl.LOGGER.trace("BEGIN MessageSender.startSendCycle()");
        currentSendChainElement = getNextSendChainElement();
        if (currentSendChainElement == null) {
            LwCommServiceImpl.LOGGER.debug("No more send chain elements for message {}", getMessageId());
            sendResultFuture.done(SendResult.FAILURE);
            LwCommServiceImpl.getServiceImpl().getMessageSenderStore().unregisterMessageSender(this);
        } else {
            sendCurrent();
        }
        LwCommServiceImpl.LOGGER.trace("END   MessageSender.startSendCycle()");
    }

    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        active = false;
        if (timeoutFuture != null) {
            boolean canceled = timeoutFuture.cancel(mayInterruptIfRunning);
            // TODO timeoutFuture = null; ?
            return canceled;
        } else
            // TODO return previous result of cancel?
            return true;
    }

    public synchronized void ackArrived(LwCommMessage ack) {
        boolean wasActive = active;
        if (active) {
            active = false;
            sendResultFuture.done(new SendResult(ack.getFrom()));
            long finished = System.nanoTime();
            LwCommServiceImpl.getServiceImpl().getStatistics().timeForAck((finished - startTimeNanos) / 1000);
        }
        if (timeoutFuture != null) {
            // Cancel fails if already running or done. If it is already running though, it is still outside of its
            // synchronized block and hasn't yet checked the active flag. As we have set active to false above, even if
            // the timeout task executes, it will not send any further messages, nor will it schedule a new timeout
            // task. Trying to cancel it twice would make no sense either, so try to cancel once and clear reference no matter what.
            if (!timeoutFuture.cancel(true)) {
                LwCommServiceImpl.LOGGER.debug("Could not cancel timeout timer (timeoutFuture: {}, wasActive: {}) for message {}", timeoutFuture, wasActive, messageToSend);
            }
            // no point in keeping the reference, clear it out even if cancel failed.
            timeoutFuture = null;
            LwCommServiceImpl.getServiceImpl().getMessageSenderStore().unregisterMessageSender(this);
        }
    }

    public synchronized void nackArrived(LwCommMessage nack) {
        boolean wasActive = active;
        // clear current retransmit timer, tryNextSend will create a new one if necessary
        if (timeoutFuture != null) {
            if (!timeoutFuture.cancel(true)) {
                LwCommServiceImpl.LOGGER.debug(
                        "Could not cancel timeout timer (timeoutFuture: {}, wasActive: {}) for message {}",
                        timeoutFuture, wasActive, messageToSend);
            }
            timeoutFuture = null;
        }
        if (active) {
            // stay active if there is a subsequent target
            // try next target, skip remaining retransmits for current node
            tryNextSend(RetryReason.REJECT);
        }
        // else ??? possibly a late NACK for a retransmit
    }

    public String getMessageId() {
        return messageToSend.getId();
    }

    private SendChainElement getNextSendChainElement() {
        return sendChain.poll();
    }

    private void sendCurrent() {
        MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, messageToSend.getId());
        LwCommServiceImpl.LOGGER.trace("BEGIN MessageSender.sendCurrent()");
        LwCommServiceImpl.LOGGER.debug("Using SendChainElement for sending message {}: {}", messageToSend.getId(),
                currentSendChainElement);
        messageToSend.setRetransmitCount(currentSendChainElement.getRetransmitCount());
        messageToSend.setFailover(currentSendChainElement.isFailover());
        updateStatistics();
        synchronized (this) {
            startTimeNanos = System.nanoTime();
            if (active) {
                nettyUtil.sendMessage(currentSendChainElement.getDestination(), messageToSend);
                if (currentSendChainElement.getTimeout() > 0) {
                    // create timeout handler
                    timeoutHandler.setCurrentTimeout(currentSendChainElement.getTimeout());
                    timeoutFuture = executor.schedule(timeoutHandler, currentSendChainElement.getTimeout(),
                            TimeUnit.MILLISECONDS);
                }
            } else {
                LwCommServiceImpl.LOGGER
                        .warn("MessageSender became inactive. Not sending message, you can safely ignore the previous warning about timeout, if any.");
            }
        }
        LwCommServiceImpl.LOGGER.trace("END   MessageSender.sendCurrent()");
    }

    private void updateStatistics() {
        if (currentSendChainElement.isFailover()) {
            if (currentSendChainElement.getRetransmitCount() > 0) {
                LwCommServiceImpl.getServiceImpl().getStatistics().incFailoverRetransmitMessageCount();
            } else {
                LwCommServiceImpl.getServiceImpl().getStatistics().incFailoverMessageCount();
            }
        } else {
            if (currentSendChainElement.getRetransmitCount() > 0) {
                LwCommServiceImpl.getServiceImpl().getStatistics().incRetransmitMessageCount();
            } else {
                switch (messageToSend.getType()) {
                case NORMAL:
                    LwCommServiceImpl.getServiceImpl().getStatistics().incFirstOutgoingMessageCount();
                    break;
                case HEARTBEAT:
                    LwCommServiceImpl.getServiceImpl().getStatistics().incSentHeartbeatCount();
                    break;
                case ACK:
                    LwCommServiceImpl.getServiceImpl().getStatistics().incSentAckCount();
                    break;
                case NACK:
                    LwCommServiceImpl.getServiceImpl().getStatistics().incSentNackCount();
                    break;
                default:
                    break;
                }
            }
        }
    }

    private SendChainElement getNextSendChainElement(boolean skipSameNode) {
        SendChainElement next = getNextSendChainElement();
        if (skipSameNode) {
            Node currentDest = currentSendChainElement.getDestination();
            while (next != null && Objects.equals(currentDest, next.getDestination())) {
                next = getNextSendChainElement();
            }
        }
        return next;
    }

    /** Reason for sending the next retransmit. */
    private static enum RetryReason {
        /** The previous transmit timed out without a response. */
        TIMEOUT,
        /** The previous transmit was rejected by the target node. */
        REJECT
    }

    private void tryNextSend(RetryReason reason) {
        assert Thread.holdsLock(this);
        // if a NACK was received, we don't try retransmits for the same node as it will just reject again
        // if a timeout occurred, we try again with the next retransmit
        currentSendChainElement = getNextSendChainElement(reason == RetryReason.REJECT);
        if (currentSendChainElement != null && currentSendChainElement.getTimeout() > 0) {
            // retransmit or failover
            sendCurrent();
        } else {
            // no next node...
            LwCommServiceImpl.LOGGER
                    .error("Final {} occured for message id {} and no more failover target nodes found - message sending failed. Message:{}",
                            reason == RetryReason.TIMEOUT ? "timeout" : "reject", messageToSend.getId(), messageToSend);
            sendResultFuture.done(SendResult.FAILURE);
            LwCommServiceImpl.getServiceImpl().getMessageSenderStore().unregisterMessageSender(MessageSender.this);
            LwCommServiceImpl.getServiceImpl().getStatistics().incTimeoutMessageCount(); // TODO more generic failed
                                                                                         // count instead?
        }
    }

    /**
     * Runs when the retransmission pattern's next timeout is due.
     * @author Miklos Pocsaji
     *
     */
    private class TimeoutHandler implements Runnable {

        private int currentTimeout;

        public void setCurrentTimeout(int currentTimeout) {
            this.currentTimeout = currentTimeout;
        }

        @Override
        public void run() {
            MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, messageToSend.getId());
            synchronized (MessageSender.this) {
                if (active) {
                    LwCommServiceImpl.LOGGER.warn("Interim ({}ms) timeout occured for message {}", currentTimeout,
                            messageToSend);
                    tryNextSend(RetryReason.TIMEOUT);
                } else {
                    // This is a simple race condition that the "active" check above is for.
                    // It can occur if the task was cancelled or an ACK arrived after the scheduled task
                    // has already started but has not yet entered the synchronized block. In this state, the task
                    // cannot be cancelled through Future.cancel(boolean) and will continue to execute.
                    LwCommServiceImpl.LOGGER.debug("Timeout occured for message but in the meantime became inactive.");
                }
            }
        }
    }

    /**
     * For every message, a list of SendChainElements is constructed.
     * This list is constructed from the mode (loadbalance or failover), retransmit pattern
     * and the destinations. A SendChainElement represents one physical outgoing UDP message.
     * @author Miklos Pocsaji
     *
     */
    private static final class SendChainElement {
        private Node destination;
        private int timeout;
        private int retransmitCount;
        private boolean failover;

        public SendChainElement(Node destination, int timeout, int retransmitCount, boolean failover) {
            this.destination = destination;
            this.timeout = timeout;
            this.retransmitCount = retransmitCount;
            this.failover = failover;
        }

        public Node getDestination() {
            return destination;
        }

        public int getTimeout() {
            return timeout;
        }

        public int getRetransmitCount() {
            return retransmitCount;
        }

        public boolean isFailover() {
            return failover;
        }

        @Override
        public String toString() {
            return "SendChainElement [destination=" + destination + ", timeout=" + timeout + ", retransmitCount="
                    + retransmitCount + ", failover=" + failover + "]";
        }

    }

//
//    public static void main(String[] args) {
//        final long s = System.currentTimeMillis();
//        class log {
//            long start = s;
//            log(String message){
//                System.out.println((System.currentTimeMillis()-start) + " ms: " + message);
//            }
//        }
//        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
//        Future<?> future;
//        synchronized (args) {
//            future = pool.schedule(() -> {
//                new log("Executor starting");
//                synchronized (args) {
//                    new log("Executor got sync");
//                    long start = System.currentTimeMillis();
//                    while (System.currentTimeMillis() - start < 5000)
//                        if (Thread.interrupted()) {
//                            new log("Executor received interrupt");
//                        }
//                    new log("Executor done");
//                }
//            }, 0, TimeUnit.MILLISECONDS);
//
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            new log("Cancel OK: " + future.cancel(true));
//        }
//
////        try {
////            Thread.sleep(1000);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
////        new log("Cancel OK: " + future.cancel(true));
//        new log("Pool shutdown, remaining tasks: " + pool.shutdownNow());
//        try {
//            pool.awaitTermination(10, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        new log("Pool terminated");
//    }
}
