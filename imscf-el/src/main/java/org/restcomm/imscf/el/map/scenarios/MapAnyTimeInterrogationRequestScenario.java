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
package org.restcomm.imscf.el.map.scenarios;

import java.io.IOException;
import javax.mail.MessagingException;
import javax.servlet.sip.ServletParseException;

import org.restcomm.imscf.common.config.NatureOfAddressType;
import org.restcomm.imscf.common.config.NumberingPlanType;
import org.restcomm.imscf.common.config.Ss7AddressType;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.map.MAPModule;
import org.restcomm.imscf.el.map.MapPrimitiveMapper;
import org.restcomm.imscf.el.map.call.AtiRequest;
import org.restcomm.imscf.el.map.call.MAPSIPCall;
import org.restcomm.imscf.el.modules.ModuleStore;
import org.restcomm.imscf.el.stack.CallContext;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPMessage;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.SubscriberIdentity;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.DomainType;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.SubscriberIdentityImpl;
import org.mobicents.protocols.ss7.map.service.mobility.subscriberInformation.RequestedInfoImpl;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scenario implementation for outgoing MAP ATI requests.
 * @author Miklos Pocsaji
 *
 */
public final class MapAnyTimeInterrogationRequestScenario implements MapOutgoingRequestScenario {

    private static final Logger LOG = LoggerFactory.getLogger(MapAnyTimeInterrogationRequestScenario.class);

    private static final String SUBSCRIPTION_STATE_REASON_MAP_UNKNOWNSUBSCRIBER = "map_unknownsubscriber";
    private static final String SUBSCRIPTION_STATE_REASON_MAP_TIMEOUT = "map_timeout";
    private static final String SUBSCRIPTION_STATE_REASON_MAP_SYSTEMFAILURE = "map_systemfailure";
    private static final String SUBSCRIPTION_STATE_REASON_UNKNOWN = "unknown";

    private Long invokeId;
    private Long localDialogId;
    private AtiRequest atiRequest;

    public static MapAnyTimeInterrogationRequestScenario start(MAPSIPCall call) throws MAPException {
        MAPDialogMobility mapDialog = createMapAtiDialog(call);
        Long ii = addAnyTimeInterrogationRequest(call);
        mapDialog.send();
        return new MapAnyTimeInterrogationRequestScenario(mapDialog.getLocalDialogId(), ii, call.getAtiRequest());
    }

    private MapAnyTimeInterrogationRequestScenario(Long localDialogId, Long invokeId, AtiRequest atiRequest) {
        this.localDialogId = localDialogId;
        this.invokeId = invokeId;
        this.atiRequest = atiRequest;
    }

    @Override
    public Long getInvokeId() {
        return invokeId;
    }

    @Override
    public void onReturnResult(MAPMessage response) {

        if (!(response instanceof AnyTimeInterrogationResponse)) {
            LOG.error("Expected AnyTimeInterrogationResponse, got {}", response == null ? "<null>" : response
                    .getClass().getName());
            return;
        }
        AnyTimeInterrogationResponse atiResp = (AnyTimeInterrogationResponse) response;
        try (MAPSIPCall call = (MAPSIPCall) getCallStore().getCallByLocalTcapTrId(localDialogId)) {
            call.getSipScenarios().add(SipMapNotifyScenario.startAtiSuccess(atiRequest, call, atiResp));
        } catch (ServletParseException | MessagingException | IOException ex) {
            LOG.error("Cannot create or send NOTIFY to AS", ex);
        }
    }

    @Override
    public void onErrorComponent(MAPErrorMessage mapErrorMessage) {
        String reason = SUBSCRIPTION_STATE_REASON_UNKNOWN;
        if (mapErrorMessage.isEmUnknownSubscriber()) {
            reason = SUBSCRIPTION_STATE_REASON_MAP_UNKNOWNSUBSCRIBER;
        } else if (mapErrorMessage.isEmSystemFailure()) {
            reason = SUBSCRIPTION_STATE_REASON_MAP_SYSTEMFAILURE;
        }
        CallStore callStore = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (MAPSIPCall call = (MAPSIPCall) callStore.getCallByLocalTcapTrId(localDialogId)) {
            call.getSipScenarios().add(SipMapNotifyScenario.startAtiError(atiRequest, call, reason));
        } catch (Exception ex) {
            LOG.error("Exception in onErrorComponent", ex);
        }
    }

    @Override
    public void onRejectComponent(Problem problem) {
        CallStore callStore = (CallStore) CallContext.get(CallContext.CALLSTORE);
        try (MAPSIPCall call = (MAPSIPCall) callStore.getCallByLocalTcapTrId(localDialogId)) {
            call.getSipScenarios().add(
                    SipMapNotifyScenario.startAtiError(atiRequest, call, SUBSCRIPTION_STATE_REASON_MAP_SYSTEMFAILURE));
        } catch (Exception ex) {
            LOG.error("Error creating or sending notify.", ex);
        }
    }

    @Override
    public void onInvokeTimeout() {
        try (MAPSIPCall call = (MAPSIPCall) getCallStore().getCallByLocalTcapTrId(localDialogId)) {
            call.getSipScenarios().add(
                    SipMapNotifyScenario.startAtiError(atiRequest, call, SUBSCRIPTION_STATE_REASON_MAP_TIMEOUT));
        } catch (Exception ex) {
            LOG.error("Error creating or sending notify.", ex);
        }
    }

    public AtiRequest getAtiRequest() {
        return atiRequest;
    }

    private static MAPDialogMobility createMapAtiDialog(MAPSIPCall call) throws MAPException {
        MAPApplicationContext ctx = MAPApplicationContext.getInstance(MAPApplicationContextName.anyTimeEnquiryContext,
                MAPApplicationContextVersion.version3);

        MAPModule mapModule = call.getMapModule();
        AtiRequest atiRequest = call.getAtiRequest();

        SccpAddress localSccpAddress = mapModule.getLocalSccpAddress();

        String hlrAlias = atiRequest.getTargetRemoteSystem();
        SccpAddress remoteSccpAddress = ModuleStore.getSccpModule().getRemoteAddress(hlrAlias);
        if (remoteSccpAddress == null) {
            LOG.error("Cannot get SCCP address of remote alias '{}'", hlrAlias);
            return null;
        }

        MAPDialogMobility mapDialog = mapModule.getMAPProvider().getMAPServiceMobility()
                .createNewDialog(ctx, localSccpAddress, null, remoteSccpAddress, null);

        mapDialog.setIdleTaskTimeout((mapModule.getModuleConfiguration().getMapTimeoutSec() + 1) * 1000);
        call.setMAPDialog(mapDialog);
        call.setLocalTcapTrId(mapDialog.getLocalDialogId());
        getCallStore().updateCall(call);

        return mapDialog;
    }

    private static long addAnyTimeInterrogationRequest(MAPSIPCall call) throws MAPException {
        MAPModule mapModule = call.getMapModule();
        AtiRequest atiRequest = call.getAtiRequest();
        MAPParameterFactory mapParameterFactory = mapModule.getMAPProvider().getMAPParameterFactory();

        Ss7AddressType target = new Ss7AddressType();
        target.setAddress(atiRequest.getTargetNumber());
        target.setNoa(NatureOfAddressType.INTERNATIONAL);
        target.setNumberingPlan(NumberingPlanType.ISDN);
        ISDNAddressString isdnTarget = MapPrimitiveMapper.createISDNAddressString(target, mapParameterFactory);

        RequestedInfo requestedInfo = new RequestedInfoImpl(true, true, null, false, DomainType.csDomain, false, false,
                false);
        ISDNAddressString gsmSCFAddress = new ISDNAddressStringImpl(AddressNature.international_number,
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, mapModule.getModuleConfiguration()
                        .getMapGsmScfAddress().getAddress());
        SubscriberIdentity subsId = new SubscriberIdentityImpl(isdnTarget);

        long invokeTimeoutMillis = mapModule.getModuleConfiguration().getMapTimeoutSec() * 1000;

        long invokeId = ((MAPDialogMobility) call.getMAPDialog()).addAnyTimeInterrogationRequest(invokeTimeoutMillis,
                subsId, requestedInfo, gsmSCFAddress, null);
        return invokeId;
    }

    private static CallStore getCallStore() {
        return (CallStore) CallContext.get(CallContext.CALLSTORE);
    }

}
