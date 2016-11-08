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
package org.restcomm.imscf.common.lwcomm.service.impl.statistics;

/**
 * MBean interface for LwCommStatistics.
 * @see LwCommStatistics
 * @author Miklos Pocsaji
 *
 */
public interface LwCommStatisticsMBean {

    /**
     * Gets the time when the statistics started (startup or last call to resetStatistics).
     * @return A timestamp in long (millis since 1970.01.01.)
     */
    long getStartupTimestamp();

    /**
     * Count of messages sent for the first time. Equals to the count of the calls to send().
     * @return Message count
     */
    long getFirstOutgoingMessageCount();

    /**
     * Count of messages for which no ACK has been received at all.
     * @return Message count
     */
    long getTimeoutMessageCount();

    /**
     * Count of messages for which cancel has been called by the user.
     * @return Message count
     */
    long getCancelMessageCount();

    /**
     * Count of retransmit messages sent to the original destination.
     * @return Message count
     */
    long getRetransmitMessageCount();

    /**
     * Count of first failover messages sent.
     * That is, the first message sent to the failover destination.
     * @return Message count
     */
    long getFailoverMessageCount();

    /**
     * Count of retransmit messages sent to failover destination.
     * @return Message count
     */
    long getFailoverRetransmitMessageCount();

    /**
     * Count of messages received and accepted, the messages with same id counted as one.
     * @return Message count
     */
    long getProcessedIncomingMessageCount();

    /**
     * Count of messages received and rejected, the messages with same id counted as one.
     * @return Message count
     */
    long getRejectedIncomingMessageCount();

    /**
     * Count of messages received and dropped, where each dropped retransmit is counted as new.
     * @return Message count
     */
    long getDroppedIncomingMessageCount();

    /**
     * Count of incoming messages successfully put on target queue.
     * @return Message count
     */
    long getQueuedIncomingMessageCount();

    /**
     * Count of ACK messages received, the ACKs with the same id counted as one.
     * @return Message count
     */
    long getProcessedAckCount();

    /**
     * Count of NACK messages received, the NACKs with the same id counted as one.
     * @return Message count
     */
    long getProcessedNackCount();

    /**
     * Count of ACK messages sent.
     * @return Message count
     */
    long getSentAckCount();

    /**
     * Count of NACK messages sent.
     * @return Message count
     */
    long getSentNackCount();

    /**
     * Count of HB messages received.
     * @return Message count
     */
    long getReceivedHeartbeatCount();

    /**
     * Count of HB messages sent.
     * @return Message count
     */
    long getSentHeartbeatCount();

    /**
     * The number of elements in the message sender store.
     * That is, the number of concurrent outgoing messages.
     * @return Element count
     */
    long getMessageSenderStoreSize();

    /**
     * The number of messages which have been processed
     * in the near past.
     * @return Element count
     */
    long getProcessedIncomingMessageStoreSize();

    /**
     * The number of elements in the received ACK store.
     * That is, the ACK identifiers received in the "near past" to track multiple ACKs.
     * @return Element count
     */
    long getReceivedAckStoreSize();

    /**
     * The number of unparseable messages received.
     * @return Message count
     */
    long getInvalidMessageCount();

    /**
     * The number of messages, which have retransmitted to this node.
     * (But shouldn't have since this node has already processed the first message and sent ACK.)
     * @return Message count
     */
    long getOutOfOrderMessageCount();

    /**
     * The number of received ACK messages which indicate that the message has been processed
     * by more than one node.
     * @return Message Count.
     */
    long getOutOfOrderAckCount();

    /**
     * Gets the average time spent in channelRead0 - the main entry point of incoming messages.
     * @return Average time interval in microseconds (1 millisec = 1.000 microsec = 1.000.000 nanosec)
     */
    long getAverageHandlerTimeUs();

    /**
     * Gets the maximum time spent in channelRead0 - the main entry point of incoming messages.
     * @return Maximum time interval in microseconds (1 millisec = 1.000 microsec = 1.000.000 nanosec)
     */
    long getMaxHandlerTimeUs();

    /**
     * Gets the average time in microseconds to wait for an outgoing message's ACK.
     * @return Average time interval in microseconds (1 millisec = 1.000 microsec = 1.000.000 nanosec)
     */
    long getAverageAckTurnaroundTimeUs();

    /**
     * Gets the maximum time in microseconds to wait for an outgoing message's ACK.
     * @return Maximum time interval in microseconds (1 millisec = 1.000 microsec = 1.000.000 nanosec)
     */
    long getMaxAckTurnaroundTimeUs();

    /**
     * Gets the average time spent waiting for a free client channel to send a message.
     * @return Average time interval in microseconds (1 millisec = 1.000 microsec = 1.000.000 nanosec)
     */
    long getAverageSendChannelWaitTimeUs();

    /**
     * Gets the maximum time spent waiting for a free client channel to send a message.
     * @return Maximum time interval in microseconds (1 millisec = 1.000 microsec = 1.000.000 nanosec)
     */
    long getMaxSendChannelWaitTimeUs();

    /**
     * Gets the average time in microseconds for a message to be processed by the user's message receiver.
     * Not that if a message is grouped, then this time includes the waiting time of group id lock as well.
     * @return Average time interval in microseconds (1 millisec = 1.000 microsec = 1.000.000 nanosec)
     */
    long getAverageWorkerTimeUs();

    /**
     * Gets the maximum time in microseconds for a message to be processed by the user's message receiver.
     * Not that if a message is grouped, then this time includes the waiting time of group id lock as well.
     * @return Maximum time interval in microseconds (1 millisec = 1.000 microsec = 1.000.000 nanosec)
     */
    long getMaxWorkerTimeUs();

    /**
     * Resets the statistics.
     */
    void resetStatistics();
}
