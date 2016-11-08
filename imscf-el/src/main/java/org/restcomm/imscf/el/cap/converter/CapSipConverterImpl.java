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
package org.restcomm.imscf.el.cap.converter;

import org.restcomm.imscf.common.config.SipApplicationServerGroupType;
import org.restcomm.imscf.el.call.ImscfCallLifeCycleState;
import org.restcomm.imscf.el.call.CapSipCall;
import org.restcomm.imscf.el.cap.CAPModuleBase;
import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CAPCSCall;
import org.restcomm.imscf.el.cap.call.CAPCSCall.BCSMType;
import org.restcomm.imscf.el.cap.call.CAPCSCall.CAPState;
import org.restcomm.imscf.el.cap.call.CAPCall;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.call.CapSipSmsCall;
import org.restcomm.imscf.el.cap.call.CapSmsCall;
import org.restcomm.imscf.el.cap.sip.SipDisconnectLegScenario;
import org.restcomm.imscf.el.cap.sip.SipScenarioInitialDp;
import org.restcomm.imscf.el.cap.sip.SipScenarioPrackHandshake;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.ReasonHeader;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.SIPReasonHeader;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.failover.SipAsLoadBalancer;
import org.restcomm.imscf.el.sip.routing.SipAsRouteAndInterface;
import org.restcomm.imscf.el.sip.routing.SipAsRouter;
import org.restcomm.imscf.el.sip.servlets.AppSessionHelper;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;
import org.restcomm.imscf.util.IteratorStream;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPParameterFactory;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPDialogState;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPGeneralAbortReason;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPUserAbortReason;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.CAPDialogSms;
import org.mobicents.protocols.ss7.cap.api.service.sms.InitialDPSMSRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.primitive.MOSMSCause;
import org.mobicents.protocols.ss7.cap.api.service.sms.primitive.RPCause;
import org.mobicents.protocols.ss7.isup.message.parameter.CauseIndicators;
import org.mobicents.protocols.ss7.tcap.asn.comp.PAbortCauseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CAP&lt;-&gt;SIP converter module implementation. */
@SuppressWarnings("PMD.GodClass")
public class CapSipConverterImpl extends CAPModuleBase implements CapSipConverter {

    private static final Logger LOG = LoggerFactory.getLogger(CapSipConverterImpl.class);

    @Override
    public int getAppSessionTimeoutMin() {
        // +1 to avoid race between last minute disconnect messages and the timer
        return getModuleConfiguration().getGeneralProperties().getMaxCallLengthMinutes() + 1;
    }

    public static boolean isCapCallFinished(CAPCall<?> call) {
        boolean ret = call.getCapDialog().getState() == CAPDialogState.Expunged;
        LOG.trace("CAP side of the call is {}", ret ? "finished" : "alive");
        return ret;
    }

    // no capsipcall interface available without cs/sms template
    protected boolean isCallFinished(CapSipCall<?> call) {
        return isCapCallFinished(call) && isSipCallFinished(call);
    }

    @Override
    public void scenarioFinished(SIPCall call, Scenario s) {
        LOG.trace("scenarioFinished: {}", s.getName());
        if (isCallFinished((CapSipCall<?>) call)) {
            LOG.debug("Call is finished, deleting it.");
            getCallFactory().deleteCall(call);
        }
    }

    @Override
    public void sessionExpired(SipApplicationSession sas) {
        try (SIPCall call = getCallStore().getCallByAppSessionId(sas.getId())) {
            LOG.debug("SipApplicationSession expired for {}", call);
            if (isCallFinished((CapSipCall<?>) call)) {
                LOG.debug("Call is finished, not renewing appsession");
            } else {
                // TODO: check whether call is still active, and only renew if it is
                // maybe do some SIP activity test? (OPTIONS?)
                AppSessionHelper.renewAppSessionTimeout(call, sas);
            }
        }
    }

    @Override
    public void sessionDestroyed(SipApplicationSession sas) {
        try (SIPCall call = getCallStore().getCallByAppSessionId(sas.getId())) {
            LOG.debug("SipApplicationSession destroyed for {}", call);
            if (isCallFinished((CapSipCall<?>) call)) {
                LOG.debug("Call is finished, deleting.");
                getCallFactory().deleteCall(call);
            } else {
                LOG.warn("AppSession destroyed for active call {}", call);
                releaseCap((CAPCall<?>) call);
                getCallFactory().deleteCall(call);
            }
        }
    }

    @Override
    public void handleSipCallFinished(SIPCall call) {
        // this is called when the SIP side is dead and no scenario has properly released the CAP side,
        // e.g. if call handling is stuck after an exception
        // This method may also be called in a race condition where the SIP side simply finishes slightly earlier than
        // the CAP side, but there is no real error.
        CapSipCsCall csCall = (CapSipCsCall) call;
        if (csCall.getCapDialog().getState() == CAPDialogState.Active) {
            if (csCall.getCsCapState() == CAPState.TERMINATED && CapUtil.canSendPrimitives(csCall.getCapDialog())) {
                LOG.debug("Call terminated, closing CAP dialog with TC_END due to lack of AS control.");
                try {
                    csCall.getCapDialog().close(true);
                    getCallFactory().deleteCall(csCall);
                    return;
                } catch (CAPException e) {
                    LOG.warn("Failed to close CAP dialog with TC_END for {}", call, e);
                }
            }
            LOG.debug("SIP call finished, waiting at most 1s for CAP side to finish.");
            // no checks required in the timer, as the timer will be deleted with the call on proper finish
            csCall.setupTimer(1000, nullInfo -> {
                LOG.warn("CAP termination timeout after SIP call finished, aborting orphaned CAP side in state {}. {}",
                        csCall.getCapDialog().getState(), csCall);
                getCallFactory().deleteCall(call, CAPUserAbortReason.abnormal_processing);
            }, null, "CAP termination timeout");

        } else {
            LOG.debug("SIP call finished, deleting orphaned CAP side in state {}. {}",
                    csCall.getCapDialog().getState(), csCall);
            // deleteCall will send out an abort if necessary
            getCallFactory().deleteCall(csCall, CAPUserAbortReason.abnormal_processing);
        }
    }

    @Override
    public void imscfCallStateChanged(CapSipCsCall call) {
        imscfCallStateChanged((CapSipCall<?>) call);
    }

    @Override
    public void imscfCallStateChanged(CapSipSmsCall call) {
        imscfCallStateChanged((CapSipCall<?>) call);
    }

    protected void imscfCallStateChanged(CapSipCall<?> call) {
        if (call.getImscfState() == ImscfCallLifeCycleState.RELEASING) {
            forceReleaseCapAndSip(call, SIPReasonHeader.INSTANCE_MAX_CALL_LENGTH);
        }
    }

    @Override
    public void onInitialDPRequest(InitialDPRequest idp) {
        try (CapSipCsCall call = (CapSipCsCall) getCallStore().getCapCall(idp.getCAPDialog().getLocalDialogId())) {

            call.getCallSegmentAssociation().registerListener(new CapResetTimerListener(call));
            call.getCallSegmentAssociation().initialDP();

            switch (idp.getEventTypeBCSM()) {
            case collectedInfo:
            case analyzedInformation:
                call.setBCSMType(BCSMType.oBCSM);
                break;
            case termAttemptAuthorized:
                call.setBCSMType(BCSMType.tBCSM);
                break;
            default:
                LOG.warn("Unsupported IDP event: {}", idp.getEventTypeBCSM());
                break;
            }

            selectAppChain(call, idp);
            // if there is an AS, send out the IDP INVITE and (re)start INVITE dependent scenarios
            routeToNextAs(call);

            if (call.getCsCapState() == CAPState.TERMINATED) {
                // routing resulted in default handling
                return;
            }

            call.setSipState(CapSipCsCall.SIPState.IDP_NOTIFIED);

            startGenericCsCallScenarios(call);
        }
    }

    private void startIdpScenarios(CapSipCsCall call, SipAsRouteAndInterface asRoute) {
        InitialDPRequest idp = call.getIdp();
        SipServletRequest invite = null;
        try {
            invite = SipScenarioInitialDp.start(call, asRoute);

            SipSession ss = invite.getSession();
            SipSessionAttributes.SIP_AS_GROUP.set(ss, call.getAppChain().get(0).getName());
            SipSessionAttributes.SIP_AS_NAME.set(ss, asRoute.getAsRoute().getUser());

        } catch (CAPException | IOException | ServletException | MessagingException e) {
            LOG.warn("Error sending initial invite, performing default handling for {}", call, e);
            // default handling or abort
            handleUnroutableCSCall(call);
            return;
        }

        // if this is a subsequent AS, clean the scenarios that are bound to the previous INVITE
        call.removeIf(s -> s instanceof SipScenarioInitialRelease
                || s instanceof SipScenarioInitialStatelessCallHandling
                // TODO this is bad: || s instanceof SipScenarioPrackHandshake
                || s instanceof SipScenarioInitialStatefulCallHandling);

        // wait for initial call handling error / release
        call.add(SipScenarioInitialRelease.start(invite, inviteErrorMatchers));
        // wait for initial stateless call accept
        call.add(SipScenarioInitialStatelessCallHandling.start(invite));
        // wait for 1xxrel-PRACK-OK handshake
        call.add(SipScenarioPrackHandshake.start(invite));
        // wait for stateful continue/connect
        call.add(SipScenarioInitialStatefulCallHandling.start(invite,
                getDefaultEDPs(idp.getEventTypeBCSM(), call.getCapDialog().getApplicationContext().getVersion())));
    }

    private void startGenericCsCallScenarios(CapSipCsCall call) {
        call.add(SipScenarioToggleAutomaticCallProcessing.start());

        call.add(SipRrbcsmScenario.start());
        // wait for incoming ApplyCharging
        call.add(SipScenarioApplyCharging.start());
        // wait for incoming FCI
        call.add(SipScenarioFurnishChargingInformation.start());
        // wait for incoming resetTimer
        call.add(SipScenarioResetTimer.start());

        // wait for initial mrf connection request
        call.add(SipScenarioInitialMrf.start());

        // wait for ICA request in existing call
        call.add(SipScenarioInitiateCallAttempt.start(defaultInitiateCallAttemptBCSMEventsPhase4));

        // wait for entityReleased/abandon/?? that could arrive before the AS responds
        // TODO

        // listen for disconnect on any leg
        call.add(SipScenarioDisconnectHandler.start());

        // listen for release by INFO SIP;cause=902
        call.add(SipScenarioReleaseByInfo.start());

        // listen for CAP cancel by SIP;cause=901
        call.add(SipScenarioCapCancel.start());

        // listen for move/split/continue requests
        call.add(SipScenarioLegManipulation.start());

        // listen for disconnectLeg requests
        call.add(SipScenarioDisconnectLeg.start());
    }

    @Override
    public void onInitialDPSMSRequest(InitialDPSMSRequest arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onAsInitiatedCall(String imscfCallId) {
        try (CapSipCsCall call = (CapSipCsCall) getCallStore().getCallByImscfCallId(imscfCallId)) {

            call.setBCSMType(BCSMType.oBCSM);

            call.getCallSegmentAssociation().registerListener(new CapResetTimerListener(call));

            startGenericCsCallScenarios(call);
        }
    }

    @Override
    public void onDialogProviderAbort(CAPDialog arg0, PAbortCauseType arg1) {
        try (CAPCall<?> call = getCallStore().getCapCall(arg0.getLocalDialogId())) {
            LOG.warn("DialogProviderAbort for {}, dialog {}, cause {}", call, arg0, arg1);
            // CAP side is done.
            // the two cause types below are treated specially as they may represent a normal call case, e.g. abandon
            SIPReasonHeader sipReason;
            if (arg1 == PAbortCauseType.NoReasonGiven)
                sipReason = SIPReasonHeader.INSTANCE_PROVIDER_ABORT_NOREASON;
            else if (arg1 == PAbortCauseType.UnrecognizedTxID)
                sipReason = SIPReasonHeader.INSTANCE_PROVIDER_ABORT_UNREC_TX;
            else
                sipReason = null;

            releaseSip((SIPCall) call, "Dialog provider abort - " + arg1, sipReason);
        }
    }

    @Override
    public void onDialogUserAbort(CAPDialog arg0, CAPGeneralAbortReason arg1, CAPUserAbortReason arg2) {
        try (CAPCall<?> call = getCallStore().getCapCall(arg0.getLocalDialogId())) {
            LOG.warn("DialogUserAbort for {}, dialog {}, reason: {}, {}", call, arg0, arg1, arg2);
            // CAP side is done.
            releaseSip((SIPCall) call, "Dialog user abort - " + arg1 + "/" + arg2, null);
        }
    }

    @Override
    public void onDialogClose(CAPDialog arg0) {
        try (CapSipCall<?> call = (CapSipCall<?>) getCallStore().getCapCall(arg0.getLocalDialogId())) {
            LOG.debug("Dialog close: TC-END received. {}", call);
            // i.e. no more SIP legs to disconnect, so scenarioFinished() will not delete the call
            if (isSipCallFinished(call)) {
                LOG.debug("SIP side is finished, deleting call in 50ms.");
                // this is put in a timer just to be executed asynchronously after this callback has returned and the
                // stack has properly updated the dialog state to expunged (it is still active during this callback)
                call.setupTimer(50, nullInfo -> {
                    LOG.debug("DeleteCall delay expired after TC_END.");
                    getCallFactory().deleteCall(call);
                }, null, "DeleteCall delay after TC_END");
            } else {
                releaseSip(call, "Dialog close due to TC-END", null);
            }
        }
    }

    @Override
    public void onDialogFailure(CAPDialog dialog) {
        try (CapSipCall<?> call = (CapSipCall<?>) getCallStore().getCapCall(dialog.getLocalDialogId())) {
            LOG.debug("CAP dialog failure, releasing call {}", call);
            // ignore all but incoming disconnect messages
            call.getSipScenarios().removeIf(s -> !(s instanceof SipScenarioDisconnectHandler));
            releaseSip(call, "MSS did not respond to activityTest", SIPReasonHeader.INSTANCE_ACTIVITYTEST_FAILURE);
        }
    }

    protected void releaseCap(CAPCall<?> call) {
        LOG.debug("Releasing CAP side...");
        CAPDialog dialog = call.getCapDialog();
        switch (dialog.getState()) {
        case Active:
        case InitialReceived:
            try {
                CAPProvider prov = call.getCapDialog().getService().getCAPProvider();
                CAPParameterFactory cap = prov.getCAPParameterFactory();
                // TODO: configurable release causes for CS/O-SMS/T-SMS?
                if (call instanceof CAPCSCall) {
                    ReleaseCallUtil.releaseCall((CAPCSCall) call, CauseIndicators._CV_TIMEOUT_RECOVERY, true);
                } else if (call instanceof CapSmsCall) {
                    RPCause cause = cap.createRPCause(MOSMSCause.systemFailure.getCode());
                    ((CAPDialogSms) dialog).addReleaseSMSRequest(cause);
                    dialog.close(false);
                }
            } catch (CAPException e) {
                LOG.warn("Failed to create releaseCall request, aborting instead", e);
                try {
                    dialog.abort(CAPUserAbortReason.application_timer_expired);
                } catch (CAPException e1) {
                    LOG.warn("Failed to create abort request", e);
                    dialog.release();
                }
            }

            break;
        case InitialSent:
            // cannot send either CAP_RLC or TC_ABORT, simply kill the local dialog
            LOG.trace("Releasing CAP dialog {}", dialog);
            dialog.release();
            break;
        case Expunged:
        case Idle:
        default:
            LOG.trace("Nothing to do in state {}", dialog.getState());
            break;
        }
    }

    public void releaseSip(SIPCall call, String imscfReleaseReason, ReasonHeader reason) {
        LOG.trace("Releasing SIP side...");
        // disable further initial requests to ensure that no new dialogs are created
        call.disableSipDialogCreation();
        // and release the existing dialogs
        SipApplicationSession sas = SipServletResources.getSipSessionsUtil().getApplicationSessionById(
                call.getAppSessionId());
        if (sas != null && sas.isValid()) {
            IteratorStream.of(AppSessionHelper.getSipSessions(sas)).filter(s -> !SipUtil.isUaTerminated(s))
                    .forEach(session -> {
                        SipDisconnectLegScenario.start(call, session, imscfReleaseReason, reason);
                    });
        } else {
            LOG.trace("Nothing to do, no appsession");
        }
    }

    protected void forceReleaseCapAndSip(CapSipCall<?> call, ReasonHeader reasonHeader) {
        LOG.debug("Force release of call requested");
        releaseCap(call);
        releaseSip(call, null, reasonHeader);
    }

    @Override
    public void handleAsUnavailableError(SIPCall call) {
        LOG.debug("AS failed to handle the call, trying next AS if possible");
        routeToNextAs((CapSipCsCall) call);
    }

    @Override
    public void handleAsReactionTimeout(SIPCall call) {
        LOG.debug("AS did not respond in time");
        handleUnroutableCSCall((CapSipCsCall) call);
    }

    private void routeToNextAs(CapSipCsCall call) {
        SipAsRouteAndInterface next = selectNextAppServer(call);
        if (next == null) {
            handleUnroutableCSCall(call);
            return;
        }
        startIdpScenarios(call, next);
    }

    private void selectAppChain(SIPCall call, InitialDPRequest idp) {
        List<SipApplicationServerGroupType> appChain = SipAsRouter.getInstance().route(idp);
        if (appChain.isEmpty()) {
            LOG.debug("No SIP AS group to route the request to! {} SK {}", idp.getCAPDialog().getApplicationContext(),
                    idp.getServiceKey());
            return;
        }

        LOG.debug("Routing IDP with SK {} to SIP AS group chain: {}", idp.getServiceKey(), new AsGroupListPrinter(
                appChain));
        call.getAppChain().addAll(appChain);
        call.setFailoverContext(SipAsLoadBalancer.getInstance().newContext(appChain.get(0).getName()));
    }

    private SipAsRouteAndInterface selectNextAppServer(SIPCall call) {
        if (call.getAppChain().isEmpty()) {
            LOG.debug("No current target application present");
            return null;
        }
        // select an available AS for the current app
        String groupName = call.getAppChain().get(0).getName();
        SipAsRouteAndInterface ret = SipAsLoadBalancer.getInstance().getNextAvailableAsRouteAndInterface(groupName,
                call.getFailoverContext());
        if (ret == null) {
            LOG.debug("No more available AS endpoints for current application '{}'", groupName);
            return null;
        }
        LOG.debug("Selected next application server: {}", ret.getAsRoute());
        return ret;
    }

}

/** Logging utility class for printing the names of a Scenario list. */
class AsGroupListPrinter {
    private final List<SipApplicationServerGroupType> list;

    AsGroupListPrinter(List<SipApplicationServerGroupType> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return list.stream().map(SipApplicationServerGroupType::getName).collect(Collectors.joining(", "));
    }
}
