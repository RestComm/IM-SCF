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
 * One-shot setter interface for TCAP counters.
 * @author Miklos Pocsaji
 *
 */
public interface TcapStatisticsSetter {
    void incTcapBeginReceivedCount();

    void incTcapContinueReceivedCount();

    void incTcapEndReceivedCount();

    void incTcapPAbortUnrecognizedMessageTypeReceivedCount();

    void incTcapPAbortUnrecognizedTxIdReceivedCount();

    void incTcapPAbortBadlyFormattedTxPortionReceivedCount();

    void incTcapPAbortIncorrectTxPortionReceivedCount();

    void incTcapPAbortResourceLimitationReceivedCount();

    void incTcapPAbortAbnormalDialogueReceivedCount();

    void incTcapPAbortNoCommonDialoguePortionReceivedCount();

    void incTcapPAbortNoReasonGivenReceivedCount();

    void incTcapUAbortReceivedCount();

    void incTcapBeginSentCount();

    void incTcapContinueSentCount();

    void incTcapEndSentCount();

    void incTcapPAbortSentCount();

    void incTcapUAbortSentCount();

    void incTcapInvokeSentCount();

    void incTcapInvokeReceivedCount();

    void incTcapReturnResultSentCount();

    void incTcapReturnResultReceivedCount();

    void incTcapReturnResultLastSentCount();

    void incTcapReturnResultLastReceivedCount();

    void incTcapErrorSentCount();

    void incTcapErrorReceivedCount();

    void incTcapRejectSentCount();

    void incTcapRejectReceivedCount();
}
