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

import java.util.Optional;

import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.call.CAPCSCall;
import org.restcomm.imscf.el.cap.call.CapSmsCall;
import org.restcomm.imscf.el.map.call.MAPCall;
import org.restcomm.imscf.el.map.call.MAPCall.MapMethod;
import org.restcomm.imscf.el.stack.CallContext;

import org.mobicents.protocols.ss7.cap.api.CAPOperationCode;
import org.mobicents.protocols.ss7.map.api.MAPOperationCode;
import org.mobicents.protocols.ss7.tcap.TCAPCounterProviderImplListener;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.Dialog;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.mobicents.protocols.ss7.tcap.asn.comp.OperationCode;
import org.mobicents.protocols.ss7.tcap.asn.comp.PAbortCauseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which is a listener for TCAP stack's counter provider.
 * Currently used to count:
 * <li>outgoing and incoming TCAP messages</li>
 * <li>outgoing CAP messages (invokes)</li>
 * <li>outgoing MAP messages (invokes)</li>
 * Incoming higher-level CAP and MAP messages are counted in CAPCSCallListener and MapModuleImpl.
 * @author Miklos Pocsaji
 */
@SuppressWarnings("PMD.GodClass")
public class TcapStatisticsListener implements TCAPCounterProviderImplListener {

    private static final Logger LOG = LoggerFactory.getLogger(TcapStatisticsListener.class);

    @Override
    public void updateDialogReleaseCount(Dialog arg0) {
        LOG.trace("Dialog released: {}", arg0);
    }

    @Override
    public void updateDialogTimeoutCount(Dialog arg0) {
        LOG.trace("Dialog timed out: {}", arg0);
    }

    @Override
    public void updateInvokeReceivedCount(Dialog arg0, Invoke invoke) {
        LOG.trace("Invoke received in dialog {}, invoke: {}", arg0, invoke);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapInvokeReceivedCount());
    }

    @Override
    public void updateInvokeSentCount(Dialog arg0, Invoke invoke) {
        LOG.trace("Invoke sent in dialog {}, invoke: {}", arg0, invoke);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapInvokeSentCount());
        incInvokeStatistics(arg0, invoke.getOperationCode());
    }

    @Override
    public void updateRejectReceivedCount(Dialog arg0) {
        LOG.trace("Reject received in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapRejectReceivedCount());
    }

    @Override
    public void updateRejectSentCount(Dialog arg0) {
        LOG.trace("Reject sent in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapRejectSentCount());
    }

    @Override
    public void updateReturnErrorReceivedCount(Dialog arg0) {
        LOG.trace("ReturnError received in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapErrorReceivedCount());
    }

    @Override
    public void updateReturnErrorSentCount(Dialog arg0) {
        LOG.trace("ReturnError sent in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapErrorSentCount());
    }

    @Override
    public void updateReturnResultLastReceivedCount(Dialog arg0) {
        LOG.trace("ReturnResultLast received in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapReturnResultLastReceivedCount());
    }

    @Override
    public void updateReturnResultLastSentCount(Dialog arg0) {
        LOG.trace("ReturnResultLast sent in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapReturnResultLastSentCount());
    }

    @Override
    public void updateReturnResultReceivedCount(Dialog arg0) {
        LOG.trace("ReturnResult received in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapReturnResultReceivedCount());
    }

    @Override
    public void updateReturnResultSentCount(Dialog arg0) {
        LOG.trace("ReturnResult sent in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapReturnResultSentCount());
    }

    @Override
    public void updateTcBeginReceivedCount(Dialog arg0) {
        LOG.trace("TC-BEGIN received in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapBeginReceivedCount());
    }

    @Override
    public void updateTcBeginSentCount(Dialog arg0) {
        LOG.trace("TC-BEGIN sent in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapBeginSentCount());
    }

    @Override
    public void updateTcContinueReceivedCount(Dialog arg0) {
        LOG.trace("TC-CONTINUE received in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapContinueReceivedCount());
    }

    @Override
    public void updateTcContinueSentCount(Dialog arg0) {
        LOG.trace("TC-CONTINUE sent in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapContinueSentCount());
    }

    @Override
    public void updateTcEndReceivedCount(Dialog arg0) {
        LOG.trace("TC-END received in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapEndReceivedCount());
    }

    @Override
    public void updateTcEndSentCount(Dialog arg0) {
        LOG.trace("TC-END sent in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapEndSentCount());
    }

    @Override
    public void updateTcPAbortReceivedCount(Dialog arg0, PAbortCauseType abortCause) {
        LOG.trace("TC-P-ABORT received in dialog: {}", arg0);
        TcapStatisticsSetter setter = getTcapStatisticsSetter(arg0);
        if (setter != null) {
            switch (abortCause) {
            case AbnormalDialogue:
                setter.incTcapPAbortAbnormalDialogueReceivedCount();
                break;
            case BadlyFormattedTxPortion:
                setter.incTcapPAbortBadlyFormattedTxPortionReceivedCount();
                break;
            case IncorrectTxPortion:
                setter.incTcapPAbortIncorrectTxPortionReceivedCount();
                break;
            case NoCommonDialoguePortion:
                setter.incTcapPAbortNoCommonDialoguePortionReceivedCount();
                break;
            case NoReasonGiven:
                setter.incTcapPAbortNoReasonGivenReceivedCount();
                break;
            case ResourceLimitation:
                setter.incTcapPAbortResourceLimitationReceivedCount();
                break;
            case UnrecognizedMessageType:
                setter.incTcapPAbortUnrecognizedMessageTypeReceivedCount();
                break;
            case UnrecognizedTxID:
                setter.incTcapPAbortUnrecognizedTxIdReceivedCount();
                break;
            default:
                LOG.warn("Unknown PAbortCauseType: {}", abortCause);
                break;
            }
        }
    }

    @Override
    public void updateTcPAbortSentCount(byte[] originatingTransactionId, PAbortCauseType abortCause) {
        LOG.trace("TC-P-ABORT sent with originating transaction id: {}", originatingTransactionId);
        // FIXME how to get the dialog?
    }

    @Override
    public void updateTcUniReceivedCount(Dialog arg0) {
        LOG.trace("TC-UNI received in dialog: {}", arg0);
    }

    @Override
    public void updateTcUniSentCount(Dialog arg0) {
        LOG.trace("TC-UNI sent in dialog: {}", arg0);
    }

    @Override
    public void updateTcUserAbortReceivedCount(Dialog arg0) {
        LOG.trace("TC-U-ABORT received in dialog: {}", arg0);
    }

    @Override
    public void updateTcUserAbortSentCount(Dialog arg0) {
        LOG.trace("TC-U-ABORT received in dialog: {}", arg0);
        Optional.ofNullable(getTcapStatisticsSetter(arg0)).ifPresent(s -> s.incTcapUAbortSentCount());
    }

    private void incInvokeStatistics(Dialog dialog, OperationCode operationCode) {
        CallStore callStore = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (IMSCFCall call = callStore.getCallByLocalTcapTrId(dialog.getLocalDialogId())) {
            if (call instanceof CAPCSCall) {
                incServiceKeyInvokeStatistics(call.getServiceIdentifier(), operationCode);
            } else if (call instanceof CapSmsCall) {
                // TODO increase SMS counters
                LOG.warn("Increasing SMS counters is not yet implemented!");
            } else if (call instanceof MAPCall) {
                incMapInvokeStatistics((MAPCall) call, operationCode);
            }
        }
    }

    private void incMapInvokeStatistics(MAPCall call, OperationCode operationCode) {
        String serviceIdentifier = call.getServiceIdentifier();
        MapStatisticsSetter setter = ElStatistics.createOneShotMapStatisticsSetter(serviceIdentifier);
        // do we need this check and the warn here?
        if (call.getMapMethod() == MapMethod.AnyTimeInterrogation) {
            if (operationCode.getLocalOperationCode() == MAPOperationCode.anyTimeInterrogation) {
                setter.incAnyTimeInterrogationCount();
            } else {
                LOG.warn(
                        "outgoing MAP invoke for MAP call type AnyTimeInterrogation, but operation code is {} instead of {}",
                        operationCode.getLocalOperationCode(), MAPOperationCode.anyTimeInterrogation);
            }
        }
    }

    private void incServiceKeyInvokeStatistics(String serviceIdentifier, OperationCode operationCode) {
        ServiceKeyStatisticsSetter osskss = ElStatistics.createOneShotServiceKeyStatisticsSetter(serviceIdentifier);
        switch (operationCode.getLocalOperationCode().intValue()) {
        case CAPOperationCode.activityTest:
            osskss.incActivityTestRequestCount();
            break;
        case CAPOperationCode.applyCharging:
            osskss.incApplyChargingCount();
            break;
        case CAPOperationCode.cancelCode:
            osskss.incCancelCount();
            break;
        case CAPOperationCode.connect:
            osskss.incConnectCount();
            break;
        case CAPOperationCode.connectToResource:
            osskss.incConnectToResourceCount();
            break;
        case CAPOperationCode.continueCode:
            osskss.incContinueCount();
            break;
        case CAPOperationCode.continueWithArgument:
            osskss.incContinueWithArgumentCount();
            break;
        case CAPOperationCode.disconnectForwardConnection:
            osskss.incDisconnectForwardConnectionCount();
            break;
        case CAPOperationCode.dFCWithArgument:
            osskss.incDisconnectForwardConnectionWithArgumentCount();
            break;
        case CAPOperationCode.disconnectLeg:
            osskss.incDisconnectLegCount();
            break;
        case CAPOperationCode.furnishChargingInformation:
            osskss.incFurnishChargingInformationCount();
            break;
        case CAPOperationCode.initiateCallAttempt:
            osskss.incInitiateCallAttemptRequestCount();
            break;
        case CAPOperationCode.moveLeg:
            osskss.incMoveLegRequestCount();
            break;
        case CAPOperationCode.playAnnouncement:
            osskss.incPlayAnnouncementCount();
            break;
        case CAPOperationCode.promptAndCollectUserInformation:
            osskss.incPromptAndCollectUserInformationCount();
            break;
        case CAPOperationCode.releaseCall:
            osskss.incReleaseCallCount();
            break;
        case CAPOperationCode.requestReportBCSMEvent:
            osskss.incRequestReportBcsmEventCount();
            break;
        case CAPOperationCode.resetTimer:
            osskss.incResetTimerCount();
            break;
        case CAPOperationCode.splitLeg:
            osskss.incSplitLegCount();
            break;
        default:
            LOG.info("Unknown CAP operation code for statistics: {}", operationCode.getLocalOperationCode());
            break;
        }
    }

    private TcapStatisticsSetter getTcapStatisticsSetter(Dialog dialog) {
        TcapStatisticsSetter ret = null;
        CallStore callStore = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (IMSCFCall call = callStore.getCallByLocalTcapTrId(dialog.getLocalDialogId())) {
            if (call instanceof CAPCSCall) {
                ret = ElStatistics.createOneShotServiceKeyStatisticsSetter(call.getServiceIdentifier());
            } else if (call instanceof MAPCall) {
                ret = ElStatistics.createOneShotMapStatisticsSetter(call.getServiceIdentifier());
            } else {
                LOG.debug("No statistics setter implemented for call {}", call);
            }
        }
        return ret;
    }

}
