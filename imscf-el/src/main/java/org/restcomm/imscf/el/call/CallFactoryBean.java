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
package org.restcomm.imscf.el.call;

import org.restcomm.imscf.el.call.history.ElEventCreator;
import org.restcomm.imscf.el.call.impl.IMSCFCallBase;
import org.restcomm.imscf.el.call.impl.AppSessionLockAction;
import org.restcomm.imscf.el.call.impl.ImscfCallMaxAgeTimerListener;
import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.call.CAPCSCall;
import org.restcomm.imscf.el.cap.call.CAPCall;
import org.restcomm.imscf.el.cap.call.CapDialogCallData;
import org.restcomm.imscf.el.cap.call.CapSipCsCallImpl;
import org.restcomm.imscf.el.cap.call.CapSipSmsCallImpl;
import org.restcomm.imscf.el.cap.call.CapSmsCall;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.diameter.DiameterModule;
import org.restcomm.imscf.el.diameter.call.DiameterHttpCall;
import org.restcomm.imscf.el.diameter.call.DiameterHttpCallImpl;
import org.restcomm.imscf.el.map.MAPModule;
import org.restcomm.imscf.el.map.call.MAPCall;
import org.restcomm.imscf.el.map.call.MapSipCallImpl;
import org.restcomm.imscf.el.modules.Module;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.SipModule;
import org.restcomm.imscf.el.sip.servlets.AppSessionHelper;
import org.restcomm.imscf.el.sip.servlets.MainServlet;
import org.restcomm.imscf.el.stack.SLELRouterBean;
import org.restcomm.imscf.el.stack.SlElMappingData;
import org.restcomm.imscf.el.tcap.call.TCAPCall;
import org.restcomm.imscf.common.SccpDialogId;
import org.restcomm.imscf.common.TcapDialogId;
import org.restcomm.imscf.common.diameter.creditcontrol.DiameterSLELCreditControlRequest;
import org.restcomm.imscf.common.messages.SccpManagementMessage;
import org.restcomm.imscf.common.util.ImscfCallId;
import org.restcomm.imscf.util.IteratorStream;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.SendResultFuture;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;
import org.restcomm.imscf.common.lwcomm.service.impl.LwCommServiceImpl;

import java.util.Optional;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;

import org.mobicents.protocols.ss7.cap.CAPDialogImpl;
import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPUserAbortReason;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest;
import org.mobicents.protocols.ss7.cap.api.service.sms.CAPDialogSms;
import org.mobicents.protocols.ss7.cap.api.service.sms.InitialDPSMSRequest;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.dialog.ProcedureCancellationReason;
import org.mobicents.protocols.ss7.map.dialog.MAPUserAbortChoiceImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating and deleting Call instances.
 */
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
public class CallFactoryBean {
    private static final Logger LOG = LoggerFactory.getLogger(CallFactoryBean.class);

    @EJB
    private CallStore callStore;

    @EJB
    private SLELRouterBean slElRouter;

    // can be called concurrently
    private <T extends IMSCFCall> T initCall(T call, Module module) {
        if (call.getImscfCallId() == null) {
            call.setImscfCallId(ImscfCallId.generate().toString());
        }
        if (module instanceof ImscfCallLifeCycleListener) {
            call.addImscfLifeCycleListener((ImscfCallLifeCycleListener) module);
        }
        if (call instanceof SIPCall) {
            SIPCall sipCall = (SIPCall) call;
            // create or look up appsession
            SipApplicationSession sas = MainServlet.initAppSession(call.getImscfCallId());
            sas.setInvalidateWhenReady(true);
            call.setAppSessionId(sas.getId());
            AppSessionHelper.renewAppSessionTimeout(sipCall, sas);
        }

        call.setupTimer(call.getMaxAge(), new ImscfCallMaxAgeTimerListener(call), null, "MaxAgeTimer");

        return call;
    }

    /**
     * Create a new call. Returns the IMSCF call id of the new call, the instance itself can be retrieved through the CallStore.
     * @return imscfCallId
     */
    public String newCall(InitialDPRequest idp, CAPModule module) {
        CapSipCsCallImpl specificCall = new CapSipCsCallImpl();
        CapDialogCallData capData = (CapDialogCallData) idp.getCAPDialog().getUserObject();
        if (capData != null) {
            specificCall.setImscfCallId(capData.getImscfCallId());
        }
        specificCall.setCapModule(module);
        CapSipCsCallImpl call = initCall(specificCall, module);
        call.setIdp(idp);
        call.setCsCapState(CAPCSCall.CAPState.IDP_ARRIVED);
        CAPDialogCircuitSwitchedCall dialog = idp.getCAPDialog();
        call.setCapDialog(dialog);
        ((CAPDialogImpl) dialog).setDialogLockAction(new AppSessionLockAction(call.getAppSession()));
        dialog.setIdleTaskTimeout(module.getTcapIdleTimeoutMillis());
        call.setLocalTcapTrId(idp.getCAPDialog().getLocalDialogId());
        call.setRemoteTcapTrId(idp.getCAPDialog().getRemoteDialogId());
        // e.g. "->IDP2(LTID:0xacb123, cap2Module, SK_00016)
        String eventStr = new StringBuilder("->IDP")
                .append(idp.getCAPDialog().getApplicationContext().getVersion().getVersion()).append('(')
                .append("LTID:0x").append(Long.toHexString(idp.getCAPDialog().getLocalDialogId())).append(", ")
                .append(module.getName()).append(", ").append(call.getServiceIdentifier()).append(')').toString();
        call.getCallHistory().addEvent(eventStr);
        ((IMSCFCallBase) call).populateMDC();
        LOG.debug("Created CS call {}", call);
        IMSCFCallBase.clearMDC();
        callStore.updateCall(call);
        call.setImscfState(ImscfCallLifeCycleState.ACTIVE);
        return call.getImscfCallId();
    }

    /**
     * Create a new CALL for SUBSCRIBE requests arrived from AS. Returns the IMSCF call id of the new call, the instance itself can be retrieved through the CallStore.
     * @param subscribeRequest The SIP SUBSCRIBE request received
     * @param mapModule The MAP module to which the servicing is routed to
     * @return imscfCallId
     */
    public String newCall(SipServletRequest subscribeRequest, MAPModule mapModule) {
        MapSipCallImpl specificCall = new MapSipCallImpl();
        // setImscfCallId to the one generated for the new appsession created for the SUBSCRIBE
        // initCall will operate using this imscfCallId as appSessionKey
        specificCall.setImscfCallId(MainServlet.getAppSessionKey(subscribeRequest.getApplicationSession()));
        specificCall.setMapModule(mapModule);
        if (mapModule instanceof SipModule) {
            specificCall.setSipModule((SipModule) mapModule);
        }

        MapSipCallImpl call = initCall(specificCall, mapModule);
        call.getCallHistory().addEvent(ElEventCreator.createIncomingSipEvent(subscribeRequest));
        ((IMSCFCallBase) call).populateMDC();
        callStore.updateCall(call);
        LOG.debug("Created MAP call: {}", call);
        IMSCFCallBase.clearMDC();
        return call.getImscfCallId();
    }

    /**
     * Create a new CALL for ICA INVITE request received from AS. Returns the IMSCF call id of the new call, the instance itself can be retrieved through the CallStore.
     * @param inviteRequest The received SIP INVITE request
     * @param capModule The CAP module which will handle the call
     * @return imscfCallId
     */
    public String newCall(SipServletRequest inviteRequest, CAPModule capModule) {
        if (!"INVITE".equals(inviteRequest.getMethod()) || !inviteRequest.isInitial()) {
            throw new IllegalArgumentException("This method only accepts initial INVITE methods");
        }
        CapSipCsCallImpl call = new CapSipCsCallImpl();
        // setImscfCallId to the one generated for the new appsession created for the INVITE
        // initCall will operate using this imscfCallId as appSessionKey
        call.setImscfCallId(MainServlet.getAppSessionKey(inviteRequest.getApplicationSession()));
        call.setCapModule(capModule);
        if (capModule instanceof SipModule) {
            call.setSipModule((SipModule) capModule);
        }
        initCall(call, capModule);
        call.getCallHistory().addEvent(ElEventCreator.createIncomingSipEvent(inviteRequest));

        CAPDialogCircuitSwitchedCall capDialog;
        try {
            capDialog = capModule
                    .getCAPProvider()
                    .getCAPServiceCircuitSwitchedCall()
                    .createNewDialog(CAPApplicationContext.CapV4_scf_gsmSSFGeneric, capModule.getLocalSccpAddress(),
                            null);
        } catch (CAPException e) {
            LOG.warn("Failed to create CAP dialog for AS initiated call {}", call.getImscfCallId(), e);
            deleteCall(call);
            return null;
        }
        ((CAPDialogImpl) capDialog).setDialogLockAction(new AppSessionLockAction(call.getAppSession()));
        capDialog.setIdleTaskTimeout(capModule.getTcapIdleTimeoutMillis());
        call.setCapDialog(capDialog);

        ((IMSCFCallBase) call).populateMDC();
        callStore.updateCall(call);
        call.setImscfState(ImscfCallLifeCycleState.ACTIVE);

        LOG.debug("Created CAP AS initiated call: {}", call);
        IMSCFCallBase.clearMDC();
        return call.getImscfCallId();
    }

    /**
     * Create a new call. Returns the IMSCF call id of the new call, the instance itself can be retrieved through the CallStore.
     * @return imscfCallId
     */
    public String newCall(InitialDPSMSRequest idpSms, CAPModule module) {
        CapSipSmsCallImpl specificCall = new CapSipSmsCallImpl();
        CapDialogCallData capData = (CapDialogCallData) idpSms.getCAPDialog().getUserObject();
        if (capData != null) {
            specificCall.setImscfCallId(capData.getImscfCallId());
        }
        specificCall.setCapModule(module);
        CapSmsCall call = initCall(specificCall, module);
        call.setIdpSMS(idpSms);
        call.setSmsCapState(CapSmsCall.CAPState.IDPSMS_ARRIVED);
        CAPDialogSms dialog = idpSms.getCAPDialog();
        call.setCapDialog(dialog);
        ((CAPDialogImpl) dialog).setDialogLockAction(new AppSessionLockAction(call.getAppSession()));
        dialog.setIdleTaskTimeout(module.getTcapIdleTimeoutMillis());
        call.setLocalTcapTrId(idpSms.getCAPDialog().getLocalDialogId());
        call.setRemoteTcapTrId(idpSms.getCAPDialog().getRemoteDialogId());
        call.getCallHistory().addEvent(
			"->IDPSMS" + idpSms.getCAPDialog().getApplicationContext().getVersion().getVersion() + "("
                        + module.getName() + ")");
        ((IMSCFCallBase) call).populateMDC();
        LOG.debug("Created SMS call {}", call);
        IMSCFCallBase.clearMDC();
        callStore.updateCall(call);
        call.setImscfState(ImscfCallLifeCycleState.ACTIVE);
        return call.getImscfCallId();
    }

    /**
     * Create a new call. Returns the IMSCF call id of the new call, the instance itself can be retrieved through the CallStore.
     * @return imscfCallId
     */
    public String newCall(DiameterSLELCreditControlRequest diameterRequest, String callId, String msgId,
            DiameterModule module) {
        DiameterHttpCall specificCall = new DiameterHttpCallImpl();
        specificCall.setDiameterModule(module);
        specificCall.setImscfCallId(callId);
        DiameterHttpCall call = initCall(specificCall, module);
        call.setDiameterSessionId(diameterRequest.getSessionId());
        call.setServiceContextId(diameterRequest.getServiceContextId());
        call.getCallHistory().addEvent("->LWC(" + diameterRequest.getRequestType() + ", " + msgId + ")");
        ((IMSCFCallBase) call).populateMDC();
        LOG.debug("Created Diameter call {}", call);
        IMSCFCallBase.clearMDC();
        callStore.updateCall(call);
        call.setImscfState(ImscfCallLifeCycleState.ACTIVE);
        return call.getImscfCallId();
    }

    private static CAPUserAbortReason getDefaultCapAbortReason() {
        return CAPUserAbortReason.application_timer_expired;
    }

    private static MAPUserAbortChoice getDefaultMapAbortChoice() {
        MAPUserAbortChoice mc = new MAPUserAbortChoiceImpl();
        mc.setProcedureCancellationReason(ProcedureCancellationReason.callRelease);
        return mc;
    }

    public void deleteCall(IMSCFCall call) {
        deleteCall(call, getDefaultCapAbortReason(), getDefaultMapAbortChoice());
    }

    public void deleteCall(IMSCFCall call, CAPUserAbortReason capAbortReason) {
        deleteCall(call, capAbortReason, getDefaultMapAbortChoice());
    }

    public void deleteCall(IMSCFCall call, MAPUserAbortChoice mapAbortChoice) {
        deleteCall(call, getDefaultCapAbortReason(), mapAbortChoice);
    }

    public void deleteCall(IMSCFCall call, CAPUserAbortReason capAbortReason, MAPUserAbortChoice mapAbortChoice) {
        LOG.trace("deleteCall {}", call);
        // if some scenario deleted the call, a check to this state will prevent subsequent scenarios from executing
        call.setImscfState(ImscfCallLifeCycleState.FINISHED);

        // if an abort needs to be sent, send it first, so that TcapStatisticsSetter can still link it to the call
        if (call instanceof CAPCall) {
            CAPDialog cd = ((CAPCall<?>) call).getCapDialog();
            switch (cd.getState()) {
            case Active:
            case InitialReceived:
                try {
                    cd.abort(capAbortReason);
                } catch (CAPException e) {
                    LOG.warn("Error aborting CAP call", e);
                }
                break;
            case Idle:
            case Expunged:
            case InitialSent:
            default:
                break;
            }
            // always release(), it is a noop after an abort()
            // Note: this is NOT a CAP releaseCall, just a local deletion of the dialog!
            cd.release();

        } else if (call instanceof MAPCall) {
            MAPDialog md = ((MAPCall) call).getMAPDialog();
            switch (md.getState()) {
            case ACTIVE:
            case INITIAL_RECEIVED:
                try {
                    md.abort(mapAbortChoice);
                } catch (MAPException e) {
                    LOG.warn("Error aborting MAP call", e);
                }
                break;
            case IDLE:
            case EXPUNGED:
            case INITIAL_SENT:
            default:
                break;
            }
            // always release(), it is a noop after an abort()
            md.release();
        }

        // remove first, as appsession listener will check for an existing call in the destroyed callback
        callStore.removeCall(call);

        call.cancelAllTimers();

        Optional.ofNullable(call.getAppSession()).filter(SipApplicationSession::isValid).ifPresent(sas -> {
            /*
             * Cancel all active timers, and allow the SAS to invalidate itself when all the SipSessions are ready.
             * Remove all Call related attributes from the SipSessions to reduce the memory footprint of the lingering
             * sessions until they get ready to invalidate. At that point, the SipApplicationSession will be deleted as
             * well.
             */
            sas.getTimers().forEach(ServletTimer::cancel);
            IteratorStream.of(AppSessionHelper.getSipSessions(sas)).forEach(SipSessionAttributes::removeAll);
            LOG.trace("Cleaned SipApplicationSession {}", sas.getId());
        });

        // only check for a stuck mapping after possibly sending out an abort above
        removeCallFromSl(call);
        call.getCallHistory().log();

        LOG.debug("Deleted {}", call);
    }

    private SccpDialogId getSccpDialogId(CAPDialog capDialog) {
        SccpAddress remote = Optional.ofNullable(capDialog.getRemoteAddress()).orElseThrow(
                () -> new RuntimeException("Remote SCCP address must always be present for CAP dialogs!"));
        // remote GT is optional
        String remoteGT = Optional.ofNullable(remote.getGlobalTitle()).map(GlobalTitle::getDigits).orElse(null);
        // remote SSN is mandatory
        int remoteSSN = remote.getSubsystemNumber();

        // local SSN is mandatory
        int localSSN = Optional.ofNullable(capDialog.getLocalAddress())
                .orElseThrow(() -> new RuntimeException("Local SCCP address must always be present for CAP dialogs!"))
                .getSubsystemNumber();

        return new SccpDialogId(remoteGT, remoteSSN, localSSN);
    }

    private TcapDialogId getTcapDialogId(TCAPCall tcapCall) {
        return new TcapDialogId(tcapCall.getRemoteTcapTrId(), tcapCall.getLocalTcapTrId());
    }

    private SccpDialogId getSccpDialogId(MAPDialog mapDialog) {
        SccpAddress remote = Optional.ofNullable(mapDialog.getRemoteAddress()).orElseThrow(
                () -> new RuntimeException("Remote SCCP address must always be present for MAP dialogs!"));
        // remote GT is optional
        String remoteGT = Optional.ofNullable(remote.getGlobalTitle()).map(GlobalTitle::getDigits).orElse(null);
        // remote SSN is mandatory
        int remoteSSN = remote.getSubsystemNumber();

        // local SSN is mandatory
        int localSSN = Optional.ofNullable(mapDialog.getLocalAddress())
                .orElseThrow(() -> new RuntimeException("Local SCCP address must always be present for MAP dialogs!"))
                .getSubsystemNumber();

        return new SccpDialogId(remoteGT, remoteSSN, localSSN);
    }

    /**
     * Checks if the call should be purged from the mapping and from the SL.
     * If the call's SL mapping is still available then
     * <li>Clears the mapping</li>
     * <li>Writes a WARN log (there must have been something unusual happened to the call before)</li>
     * <li>Sends an SCCP Management message to SL because likely the call has been stuck on SL side as well</li>
     * @param call The call to check
     */
    private void removeCallFromSl(IMSCFCall call) {
        SccpDialogId sccpDialogId;
        TcapDialogId tcapDialogId;
        LOG.debug("Checking if delete call message should be sent to SL...");
        if (call instanceof CAPCall) {
            CAPCall<?> cc = (CAPCall<?>) call;
            sccpDialogId = getSccpDialogId(cc.getCapDialog());
            tcapDialogId = getTcapDialogId(cc);
        } else if (call instanceof MAPCall) {
            MAPCall mc = (MAPCall) call;
            sccpDialogId = getSccpDialogId(mc.getMAPDialog());
            tcapDialogId = getTcapDialogId(mc);
        } else if (call instanceof DiameterHttpCall) {
            LOG.debug("SL mapping for DiameterHttpCall is automatically removed on the SL side, no cleanup necessary.");
            return;
        } else {
            LOG.warn("call is {}, sending delete call message to SL is not implemented!", call);
            return;
        }
        SlElMappingData mapping = slElRouter.getMappingData(sccpDialogId, tcapDialogId);
        if (mapping != null) {
            LOG.warn(
                    "SL mapping still found for call {} at deleteCall(). Clearing mapping and sending delete call management message to {}.",
                    call.getImscfCallId(), mapping.getNodeName());
            slElRouter.setMappingData(sccpDialogId, tcapDialogId, null);
            String route = slElRouter.getDirectRouteNameTo(mapping.getNodeName());
            SccpManagementMessage manMsg = SccpManagementMessage.createDeleteCallMessage(sccpDialogId, tcapDialogId);
            String payload = "Target: SccpProvider\r\n" + "Content: SccpManagementMessage\r\n" + "\r\n"
                    + manMsg.serialize();
            SendResultFuture<SendResult> result = LwCommServiceImpl.getService().send(route,
                    TextMessage.builder(payload).setGroupId(call.getImscfCallId()).create());
            call.getCallHistory().addEvent("<-LWC(" + manMsg.getType() + ", " + result.getMessageId() + ")");
        }
    }
}
