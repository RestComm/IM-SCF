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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MBean registered and maintained by LwComm for publishing statistics.
 * @author Miklos Pocsaji
 *
 */
@SuppressWarnings("PMD.GodClass")
public class LwCommStatistics implements LwCommStatisticsMBean {

    private static final int AVERAGE_COUNT = 1000;

    private long startupTimestamp;
    private AtomicLong firstOutgoingMessageCount = new AtomicLong();
    private AtomicLong timeoutMessageCount = new AtomicLong();
    private AtomicLong cancelMessageCount = new AtomicLong();
    private AtomicLong retransmitMessageCount = new AtomicLong();
    private AtomicLong failoverMessageCount = new AtomicLong();
    private AtomicLong failoverRetransmitMessageCount = new AtomicLong();
    private AtomicLong processedIncomingMessageCount = new AtomicLong();
    private AtomicLong rejectedIncomingMessageCount = new AtomicLong();
    private AtomicLong droppedIncomingMessageCount = new AtomicLong();
    private AtomicLong queuedIncomingMessageCount = new AtomicLong();
    private AtomicLong processedAckCount = new AtomicLong();
    private AtomicLong processedNackCount = new AtomicLong();
    private AtomicLong sentAckCount = new AtomicLong();
    private AtomicLong sentNackCount = new AtomicLong();
    private AtomicLong receivedHeartbeatCount = new AtomicLong();
    private AtomicLong sentHeartbeatCount = new AtomicLong();
    private long messageSenderStoreSize;
    private long processedIncomingMessageStoreSize;
    private long receivedAckStoreSize;
    private AtomicLong invalidMessageCount = new AtomicLong();
    private AtomicLong outOfOrderMessageCount = new AtomicLong();
    private AtomicLong outOfOrderAckCount = new AtomicLong();

    //
    // Averages and maximums:
    //

    private long[] timeChannelRead0Us = new long[AVERAGE_COUNT];
    private AtomicInteger timeChannelRead0UsIndex = new AtomicInteger();
    private long[] timeAckUs = new long[AVERAGE_COUNT];
    private AtomicInteger timeAckUsIndex = new AtomicInteger();
    private long[] timeChannelWaitUs = new long[AVERAGE_COUNT];
    private AtomicInteger timeChannelWaitUsIndex = new AtomicInteger();
    private long[] timeWorker = new long[AVERAGE_COUNT];
    private AtomicInteger timeWorkerIndex = new AtomicInteger();

    public LwCommStatistics() {
        startupTimestamp = System.currentTimeMillis();
    }

    //
    //
    //

    public void incFirstOutgoingMessageCount() {
        firstOutgoingMessageCount.incrementAndGet();
    }

    public void incTimeoutMessageCount() {
        timeoutMessageCount.incrementAndGet();
    }

    public void incCancelMessageCount() {
        cancelMessageCount.incrementAndGet();
    }

    public void incRetransmitMessageCount() {
        retransmitMessageCount.incrementAndGet();
    }

    public void incFailoverMessageCount() {
        failoverMessageCount.incrementAndGet();
    }

    public void incFailoverRetransmitMessageCount() {
        failoverRetransmitMessageCount.incrementAndGet();
    }

    public void incProcessedIncomingMessageCount() {
        processedIncomingMessageCount.incrementAndGet();
    }

    public void incRejectedIncomingMessageCount() {
        rejectedIncomingMessageCount.incrementAndGet();
    }

    public void incDroppedIncomingMessageCount() {
        droppedIncomingMessageCount.incrementAndGet();
    }

    public void incQueuedIncomingMessageCount() {
        queuedIncomingMessageCount.incrementAndGet();
    }

    public void incProcessedAckCount() {
        processedAckCount.incrementAndGet();
    }

    public void incProcessedNackCount() {
        processedNackCount.incrementAndGet();
    }

    public void incSentAckCount() {
        sentAckCount.incrementAndGet();
    }

    public void incSentNackCount() {
        sentNackCount.incrementAndGet();
    }

    public void incReceivedHeartbeatCount() {
        receivedHeartbeatCount.incrementAndGet();
    }

    public void incSentHeartbeatCount() {
        sentHeartbeatCount.incrementAndGet();
    }

    public void setMessageSenderStoreSize(long messageSenderStoreSize) {
        this.messageSenderStoreSize = messageSenderStoreSize;
    }

    public void setProcessedIncomingMessageStoreSize(long processedIncomingMessageStoreSize) {
        this.processedIncomingMessageStoreSize = processedIncomingMessageStoreSize;
    }

    public void setReceivedAckStoreSize(long receivedAckStoreSize) {
        this.receivedAckStoreSize = receivedAckStoreSize;
    }

    public void incInvalidMessageCount() {
        invalidMessageCount.incrementAndGet();
    }

    public void incOutOfOrderMessageCount() {
        outOfOrderMessageCount.incrementAndGet();
    }

    public void incOutOfOrderAckCount() {
        outOfOrderAckCount.incrementAndGet();
    }

    public void timeSpentInChannelRead0(long microSeconds) {
        timeChannelRead0Us[Math.abs(timeChannelRead0UsIndex.getAndIncrement() % AVERAGE_COUNT)] = microSeconds;
    }

    public void timeForAck(long microSeconds) {
        timeAckUs[Math.abs(timeAckUsIndex.getAndIncrement() % AVERAGE_COUNT)] = microSeconds;
    }

    public void timeSpentWaitingForChannel(long microSeconds) {
        timeChannelWaitUs[Math.abs(timeChannelWaitUsIndex.getAndIncrement() % AVERAGE_COUNT)] = microSeconds;
    }

    public void timeWorker(long microSeconds) {
        timeWorker[Math.abs(timeWorkerIndex.getAndIncrement() % AVERAGE_COUNT)] = microSeconds;
    }

    //
    // MBean interface
    //

    @Override
    public long getStartupTimestamp() {
        return startupTimestamp;
    }

    @Override
    public long getFirstOutgoingMessageCount() {
        return firstOutgoingMessageCount.get();
    }

    @Override
    public long getTimeoutMessageCount() {
        return timeoutMessageCount.get();
    }

    @Override
    public long getCancelMessageCount() {
        return cancelMessageCount.get();
    }

    @Override
    public long getRetransmitMessageCount() {
        return retransmitMessageCount.get();
    }

    @Override
    public long getFailoverMessageCount() {
        return failoverMessageCount.get();
    }

    @Override
    public long getFailoverRetransmitMessageCount() {
        return failoverRetransmitMessageCount.get();
    }

    @Override
    public long getProcessedIncomingMessageCount() {
        return processedIncomingMessageCount.get();
    }

    @Override
    public long getRejectedIncomingMessageCount() {
        return rejectedIncomingMessageCount.get();
    }

    @Override
    public long getDroppedIncomingMessageCount() {
        return droppedIncomingMessageCount.get();
    }

    @Override
    public long getQueuedIncomingMessageCount() {
        return queuedIncomingMessageCount.get();
    }

    @Override
    public long getProcessedAckCount() {
        return processedAckCount.get();
    }

    @Override
    public long getProcessedNackCount() {
        return processedNackCount.get();
    }

    @Override
    public long getSentAckCount() {
        return sentAckCount.get();
    }

    @Override
    public long getSentNackCount() {
        return sentNackCount.get();
    }

    @Override
    public long getReceivedHeartbeatCount() {
        return receivedHeartbeatCount.get();
    }

    @Override
    public long getSentHeartbeatCount() {
        return sentHeartbeatCount.get();
    }

    @Override
    public long getMessageSenderStoreSize() {
        return messageSenderStoreSize;
    }

    @Override
    public long getProcessedIncomingMessageStoreSize() {
        return processedIncomingMessageStoreSize;
    }

    @Override
    public long getReceivedAckStoreSize() {
        return receivedAckStoreSize;
    }

    @Override
    public long getInvalidMessageCount() {
        return invalidMessageCount.get();
    }

    @Override
    public long getOutOfOrderMessageCount() {
        return outOfOrderMessageCount.get();
    }

    @Override
    public long getOutOfOrderAckCount() {
        return outOfOrderAckCount.get();
    }

    @Override
    public synchronized long getAverageHandlerTimeUs() {
        return getAvgOfLongArray(timeChannelRead0Us);
    }

    @Override
    public long getMaxHandlerTimeUs() {
        return getMaxOfLongArray(timeChannelRead0Us);
    }

    @Override
    public long getAverageAckTurnaroundTimeUs() {
        return getAvgOfLongArray(timeAckUs);
    }

    @Override
    public long getMaxAckTurnaroundTimeUs() {
        return getMaxOfLongArray(timeAckUs);
    }

    @Override
    public long getAverageSendChannelWaitTimeUs() {
        return getAvgOfLongArray(timeChannelWaitUs);
    }

    @Override
    public long getMaxSendChannelWaitTimeUs() {
        return getMaxOfLongArray(timeChannelWaitUs);
    }

    @Override
    public long getAverageWorkerTimeUs() {
        return getAvgOfLongArray(timeWorker);
    }

    @Override
    public long getMaxWorkerTimeUs() {
        return getMaxOfLongArray(timeWorker);
    }

    private long getAvgOfLongArray(long[] array) {
        long sum = 0;
        long cnt = 0;
        for (long entry : array) {
            if (entry > 0) {
                sum += entry;
                cnt++;
            }
        }
        if (cnt == 0)
            return -1;
        return sum / cnt;
    }

    private long getMaxOfLongArray(long[] array) {
        long ret = 0;
        for (long entry : array) {
            if (entry > ret)
                ret = entry;
        }
        return ret;
    }

    @Override
    public void resetStatistics() {
        startupTimestamp = System.currentTimeMillis();
        cancelMessageCount.set(0);
        failoverMessageCount.set(0);
        failoverRetransmitMessageCount.set(0);
        firstOutgoingMessageCount.set(0);
        invalidMessageCount.set(0);
        messageSenderStoreSize = 0;
        outOfOrderAckCount.set(0);
        outOfOrderMessageCount.set(0);
        processedAckCount.set(0);
        processedNackCount.set(0);
        processedIncomingMessageCount.set(0);
        rejectedIncomingMessageCount.set(0);
        droppedIncomingMessageCount.set(0);
        processedIncomingMessageStoreSize = 0;
        queuedIncomingMessageCount.set(0);
        receivedAckStoreSize = 0;
        receivedHeartbeatCount.set(0);
        retransmitMessageCount.set(0);
        sentHeartbeatCount.set(0);
        sentAckCount.set(0);
        sentNackCount.set(0);
        timeoutMessageCount.set(0);
        Arrays.fill(timeChannelRead0Us, 0);
        Arrays.fill(timeAckUs, 0);
        Arrays.fill(timeChannelWaitUs, 0);
        Arrays.fill(timeWorker, 0);
        timeChannelRead0UsIndex.set(0);
        timeAckUsIndex.set(0);
        timeChannelWaitUsIndex.set(0);
        timeWorkerIndex.set(0);
    }
}
