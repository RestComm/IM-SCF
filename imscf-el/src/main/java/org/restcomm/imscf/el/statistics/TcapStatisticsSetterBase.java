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
package org.restcomm.imscf.el.statistics;

/**
 * Base class for TCAP mbean counter setters.
 * @author Miklos Pocsaji
 *
 */
public abstract class TcapStatisticsSetterBase extends StatisticsSetterBase implements TcapStatisticsSetter {

    @Override
    public void incTcapBeginReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapBeginReceivedCount.name());
    }

    @Override
    public void incTcapContinueReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapContinueReceivedCount.name());
    }

    @Override
    public void incTcapEndReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapEndReceivedCount.name());
    }

    @Override
    public void incTcapPAbortUnrecognizedMessageTypeReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapPAbortUnrecognizedMessageTypeReceivedCount.name());
    }

    @Override
    public void incTcapPAbortUnrecognizedTxIdReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapPAbortUnrecognizedTxIdReceivedCount.name());
    }

    @Override
    public void incTcapPAbortBadlyFormattedTxPortionReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapPAbortBadlyFormattedTxPortionReceivedCount.name());
    }

    @Override
    public void incTcapPAbortIncorrectTxPortionReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapPAbortIncorrectTxPortionReceivedCount.name());
    }

    @Override
    public void incTcapPAbortResourceLimitationReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapPAbortResourceLimitationReceivedCount.name());
    }

    @Override
    public void incTcapPAbortAbnormalDialogueReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapPAbortAbnormalDialogueReceivedCount.name());
    }

    @Override
    public void incTcapPAbortNoCommonDialoguePortionReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapPAbortNoCommonDialoguePortionReceivedCount.name());
    }

    @Override
    public void incTcapPAbortNoReasonGivenReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapPAbortNoReasonGivenReceivedCount.name());
    }

    @Override
    public void incTcapUAbortReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapUAbortReceivedCount.name());
    }

    @Override
    public void incTcapBeginSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapBeginSentCount.name());
    }

    @Override
    public void incTcapContinueSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapContinueSentCount.name());
    }

    @Override
    public void incTcapEndSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapEndSentCount.name());
    }

    @Override
    public void incTcapPAbortSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapPAbortSentCount.name());
    }

    @Override
    public void incTcapUAbortSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapUAbortSentCount.name());
    }

    @Override
    public void incTcapInvokeSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapInvokeSentCount.name());
    }

    @Override
    public void incTcapInvokeReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapInvokeReceivedCount.name());
    }

    @Override
    public void incTcapReturnResultSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapReturnResultSentCount.name());
    }

    @Override
    public void incTcapReturnResultReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapReturnResultReceivedCount.name());
    }

    @Override
    public void incTcapReturnResultLastSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapReturnResultLastSentCount.name());
    }

    @Override
    public void incTcapReturnResultLastReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapReturnResultLastReceivedCount.name());
    }

    @Override
    public void incTcapErrorSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapErrorSentCount.name());
    }

    @Override
    public void incTcapErrorReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapErrorReceivedCount.name());
    }

    @Override
    public void incTcapRejectSentCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapRejectSentCount.name());
    }

    @Override
    public void incTcapRejectReceivedCount() {
        setCounterName(TcapStatisticsMBeanBase.TcapCounter.tcapErrorReceivedCount.name());
    }

}
