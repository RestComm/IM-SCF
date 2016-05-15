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
package org.restcomm.imscf.el.statistics;

import java.util.List;
import org.restcomm.imscf.common.el.statistics.TcapStatisticsMBean;

/**
 * Base class for statistics MBeans with TCAP counters.
 * @author Miklos Pocsaji
 *
 */
public abstract class TcapStatisticsMBeanBase extends SlidingWindowStatisticsMBeanBase implements TcapStatisticsMBean,
        TcapStatisticsSetter {

    /**
     * Counters provided by this MBean.
     * @author Miklos Pocsaji
     *
     */
    public enum TcapCounter {
        tcapBeginReceivedCount,

        tcapContinueReceivedCount,

        tcapEndReceivedCount,

        tcapPAbortUnrecognizedMessageTypeReceivedCount, tcapPAbortUnrecognizedTxIdReceivedCount, tcapPAbortBadlyFormattedTxPortionReceivedCount, tcapPAbortIncorrectTxPortionReceivedCount, tcapPAbortResourceLimitationReceivedCount, tcapPAbortAbnormalDialogueReceivedCount, tcapPAbortNoCommonDialoguePortionReceivedCount, tcapPAbortNoReasonGivenReceivedCount, tcapUAbortReceivedCount, tcapBeginSentCount,

        tcapContinueSentCount,

        tcapEndSentCount,

        tcapPAbortSentCount, tcapUAbortSentCount,

        tcapInvokeSentCount, tcapInvokeReceivedCount, tcapReturnResultSentCount, tcapReturnResultReceivedCount, tcapReturnResultLastSentCount, tcapReturnResultLastReceivedCount, tcapErrorSentCount, tcapErrorReceivedCount, tcapRejectSentCount, tcapRejectReceivedCount

    }

    protected TcapStatisticsMBeanBase(int windowSeconds, List<Notification> notifications) {
        super(windowSeconds, notifications);
        for (TcapCounter c : TcapCounter.values()) {
            addCounter(c.toString());
        }
    }

    @Override
    public long getTcapBeginReceivedCount() {
        return getCounter(TcapCounter.tcapBeginReceivedCount.name());
    }

    @Override
    public long getTcapContinueReceivedCount() {
        return getCounter(TcapCounter.tcapContinueReceivedCount.name());
    }

    @Override
    public long getTcapEndReceivedCount() {
        return getCounter(TcapCounter.tcapEndReceivedCount.name());
    }

    @Override
    public long getTcapPAbortUnrecognizedMessageTypeReceivedCount() {
        return getCounter(TcapCounter.tcapPAbortUnrecognizedMessageTypeReceivedCount.name());
    }

    @Override
    public long getTcapPAbortUnrecognizedTxIdReceivedCount() {
        return getCounter(TcapCounter.tcapPAbortUnrecognizedTxIdReceivedCount.name());
    }

    @Override
    public long getTcapPAbortBadlyFormattedTxPortionReceivedCount() {
        return getCounter(TcapCounter.tcapPAbortBadlyFormattedTxPortionReceivedCount.name());
    }

    @Override
    public long getTcapPAbortIncorrectTxPortionReceivedCount() {
        return getCounter(TcapCounter.tcapPAbortIncorrectTxPortionReceivedCount.name());
    }

    @Override
    public long getTcapPAbortResourceLimitationReceivedCount() {
        return getCounter(TcapCounter.tcapPAbortResourceLimitationReceivedCount.name());
    }

    @Override
    public long getTcapPAbortAbnormalDialogueReceivedCount() {
        return getCounter(TcapCounter.tcapPAbortAbnormalDialogueReceivedCount.name());
    }

    @Override
    public long getTcapPAbortNoCommonDialoguePortionReceivedCount() {
        return getCounter(TcapCounter.tcapPAbortNoCommonDialoguePortionReceivedCount.name());
    }

    @Override
    public long getTcapPAbortNoReasonGivenReceivedCount() {
        return getCounter(TcapCounter.tcapPAbortNoReasonGivenReceivedCount.name());
    }

    @Override
    public long getTcapUAbortReceivedCount() {
        return getCounter(TcapCounter.tcapUAbortReceivedCount.name());
    }

    @Override
    public long getTcapBeginSentCount() {
        return getCounter(TcapCounter.tcapBeginSentCount.name());
    }

    @Override
    public long getTcapContinueSentCount() {
        return getCounter(TcapCounter.tcapContinueSentCount.name());
    }

    @Override
    public long getTcapEndSentCount() {
        return getCounter(TcapCounter.tcapEndSentCount.name());
    }

    @Override
    public long getTcapPAbortSentCount() {
        return getCounter(TcapCounter.tcapPAbortSentCount.name());
    }

    @Override
    public long getTcapUAbortSentCount() {
        return getCounter(TcapCounter.tcapUAbortSentCount.name());
    }

    @Override
    public long getTcapInvokeSentCount() {
        return getCounter(TcapCounter.tcapInvokeSentCount.name());
    }

    @Override
    public long getTcapInvokeReceivedCount() {
        return getCounter(TcapCounter.tcapInvokeReceivedCount.name());
    }

    @Override
    public long getTcapReturnResultSentCount() {
        return getCounter(TcapCounter.tcapReturnResultSentCount.name());
    }

    @Override
    public long getTcapReturnResultReceivedCount() {
        return getCounter(TcapCounter.tcapReturnResultReceivedCount.name());
    }

    @Override
    public long getTcapReturnResultLastSentCount() {
        return getCounter(TcapCounter.tcapReturnResultLastSentCount.name());
    }

    @Override
    public long getTcapReturnResultLastReceivedCount() {
        return getCounter(TcapCounter.tcapReturnResultLastReceivedCount.name());
    }

    @Override
    public long getTcapErrorSentCount() {
        return getCounter(TcapCounter.tcapErrorSentCount.name());
    }

    @Override
    public long getTcapErrorReceivedCount() {
        return getCounter(TcapCounter.tcapErrorReceivedCount.name());
    }

    @Override
    public long getTcapRejectSentCount() {
        return getCounter(TcapCounter.tcapRejectSentCount.name());
    }

    @Override
    public long getTcapRejectReceivedCount() {
        return getCounter(TcapCounter.tcapRejectReceivedCount.name());
    }

    @Override
    public void incTcapBeginReceivedCount() {
        incCounter(TcapCounter.tcapBeginReceivedCount.name());
    }

    @Override
    public void incTcapContinueReceivedCount() {
        incCounter(TcapCounter.tcapContinueReceivedCount.name());
    }

    @Override
    public void incTcapEndReceivedCount() {
        incCounter(TcapCounter.tcapEndReceivedCount.name());
    }

    @Override
    public void incTcapPAbortUnrecognizedMessageTypeReceivedCount() {
        incCounter(TcapCounter.tcapPAbortUnrecognizedMessageTypeReceivedCount.name());
    }

    @Override
    public void incTcapPAbortUnrecognizedTxIdReceivedCount() {
        incCounter(TcapCounter.tcapPAbortUnrecognizedTxIdReceivedCount.name());
    }

    @Override
    public void incTcapPAbortBadlyFormattedTxPortionReceivedCount() {
        incCounter(TcapCounter.tcapPAbortBadlyFormattedTxPortionReceivedCount.name());
    }

    @Override
    public void incTcapPAbortIncorrectTxPortionReceivedCount() {
        incCounter(TcapCounter.tcapPAbortIncorrectTxPortionReceivedCount.name());
    }

    @Override
    public void incTcapPAbortResourceLimitationReceivedCount() {
        incCounter(TcapCounter.tcapPAbortResourceLimitationReceivedCount.name());
    }

    @Override
    public void incTcapPAbortAbnormalDialogueReceivedCount() {
        incCounter(TcapCounter.tcapPAbortAbnormalDialogueReceivedCount.name());
    }

    @Override
    public void incTcapPAbortNoCommonDialoguePortionReceivedCount() {
        incCounter(TcapCounter.tcapPAbortNoCommonDialoguePortionReceivedCount.name());
    }

    @Override
    public void incTcapPAbortNoReasonGivenReceivedCount() {
        incCounter(TcapCounter.tcapPAbortNoReasonGivenReceivedCount.name());
    }

    @Override
    public void incTcapUAbortReceivedCount() {
        incCounter(TcapCounter.tcapUAbortReceivedCount.name());
    }

    @Override
    public void incTcapPAbortSentCount() {
        incCounter(TcapCounter.tcapPAbortSentCount.name());
    }

    @Override
    public void incTcapUAbortSentCount() {
        incCounter(TcapCounter.tcapUAbortSentCount.name());
    }

    @Override
    public void incTcapBeginSentCount() {
        incCounter(TcapCounter.tcapBeginSentCount.name());
    }

    @Override
    public void incTcapContinueSentCount() {
        incCounter(TcapCounter.tcapContinueSentCount.name());
    }

    @Override
    public void incTcapEndSentCount() {
        incCounter(TcapCounter.tcapEndSentCount.name());
    }

    @Override
    public void incTcapInvokeSentCount() {
        incCounter(TcapCounter.tcapInvokeSentCount.name());
    }

    @Override
    public void incTcapInvokeReceivedCount() {
        incCounter(TcapCounter.tcapInvokeReceivedCount.name());
    }

    @Override
    public void incTcapReturnResultSentCount() {
        incCounter(TcapCounter.tcapReturnResultSentCount.name());
    }

    @Override
    public void incTcapReturnResultReceivedCount() {
        incCounter(TcapCounter.tcapReturnResultReceivedCount.name());
    }

    @Override
    public void incTcapReturnResultLastSentCount() {
        incCounter(TcapCounter.tcapReturnResultLastSentCount.name());
    }

    @Override
    public void incTcapReturnResultLastReceivedCount() {
        incCounter(TcapCounter.tcapReturnResultLastReceivedCount.name());
    }

    @Override
    public void incTcapErrorSentCount() {
        incCounter(TcapCounter.tcapErrorSentCount.name());
    }

    @Override
    public void incTcapErrorReceivedCount() {
        incCounter(TcapCounter.tcapErrorReceivedCount.name());
    }

    @Override
    public void incTcapRejectSentCount() {
        incCounter(TcapCounter.tcapRejectSentCount.name());
    }

    @Override
    public void incTcapRejectReceivedCount() {
        incCounter(TcapCounter.tcapRejectReceivedCount.name());
    }

}
