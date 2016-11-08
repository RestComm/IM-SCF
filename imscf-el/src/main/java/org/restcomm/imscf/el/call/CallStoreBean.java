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

import org.restcomm.imscf.el.call.impl.CallResourceAdapter;
import org.restcomm.imscf.el.cap.call.CAPCall;
import org.restcomm.imscf.el.diameter.call.DiameterCall;
import org.restcomm.imscf.el.diameter.call.DiameterHttpCall;
import org.restcomm.imscf.el.map.call.MAPCall;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.tcap.call.TCAPCall;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.servlet.sip.SipServletMessage;

/**
 * Singleton bean implementing {@link CallStore}.
 *
 */
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
public class CallStoreBean implements CallStore {

    // primary store, all calls have an imscf call id
    Map<String, IMSCFCall> callsByImscfCallId;
    // different views of the same store
    Map<String, SIPCall> callsByAppSessionId;
    Map<Long, TCAPCall> callsByLocalTcapTrId;
    Map<String, DiameterCall> callsByDiameterSessionId;

    @PostConstruct
    public void init() {
        callsByAppSessionId = new ConcurrentHashMap<String, SIPCall>();
        callsByImscfCallId = new ConcurrentHashMap<String, IMSCFCall>();
        callsByLocalTcapTrId = new ConcurrentHashMap<Long, TCAPCall>();
        callsByDiameterSessionId = new ConcurrentHashMap<String, DiameterCall>();
    }

    @Override
    public IMSCFCall getCallByImscfCallId(String imscfCallId) {
        return CallResourceAdapter.wrap(callsByImscfCallId.get(imscfCallId));
    }

    @Override
    public SIPCall getCallByAppSessionId(String appSessionId) {
        return CallResourceAdapter.wrap(callsByAppSessionId.get(appSessionId));
    }

    @Override
    public TCAPCall getCallByLocalTcapTrId(Long localTcapTrId) {
        return CallResourceAdapter.wrap(callsByLocalTcapTrId.get(localTcapTrId));
    }

    @Override
    public DiameterCall getCallByDiameterSessionId(String sessionId) {
        return CallResourceAdapter.wrap(callsByDiameterSessionId.get(sessionId));
    }

    @Override
    public DiameterHttpCall getHttpCallByDiameterSessionId(String sessionId) {
        return CallResourceAdapter.wrap((DiameterHttpCall) callsByDiameterSessionId.get(sessionId));
    }

    @Override
    public CAPCall<?> getCapCall(Long localTcapTrId) {
        return (CAPCall<?>) getCallByLocalTcapTrId(localTcapTrId);
    }

    @Override
    public MAPCall getMapCall(Long localTcapTrId) {
        return (MAPCall) getCallByLocalTcapTrId(localTcapTrId);
    }

    @Override
    public SIPCall getSipCall(SipServletMessage msg) {
        return getCallByAppSessionId(msg.getApplicationSession(false).getId());
    }

    @Override
    public void updateCall(IMSCFCall wrappedCall) {
        IMSCFCall call = CallResourceAdapter.unwrap(wrappedCall);
        if (call.getImscfCallId() != null)
            callsByImscfCallId.put(call.getImscfCallId(), call);
        if (call instanceof SIPCall) {
            SIPCall s = (SIPCall) call;
            if (s.getAppSessionId() != null)
                callsByAppSessionId.put(s.getAppSessionId(), s);
        }
        if (call instanceof TCAPCall) {
            TCAPCall t = (TCAPCall) call;
            if (t.getLocalTcapTrId() != null)
                callsByLocalTcapTrId.put(t.getLocalTcapTrId(), t);
        }
        if (call instanceof DiameterCall) {
            DiameterCall d = (DiameterCall) call;
            if (d.getDiameterSessionId() != null)
                callsByDiameterSessionId.put(d.getDiameterSessionId(), d);
        }
    }

    @Override
    public void removeCall(IMSCFCall wrappedCall) {
        IMSCFCall call = CallResourceAdapter.unwrap(wrappedCall);
        if (call.getImscfCallId() != null)
            callsByImscfCallId.remove(call.getImscfCallId());
        if (call instanceof SIPCall) {
            SIPCall s = (SIPCall) call;
            if (s.getAppSessionId() != null)
                callsByAppSessionId.remove(s.getAppSessionId());
        }
        if (call instanceof TCAPCall) {
            TCAPCall t = (TCAPCall) call;
            if (t.getLocalTcapTrId() != null)
                callsByLocalTcapTrId.remove(t.getLocalTcapTrId());
        }
        if (call instanceof DiameterCall) {
            DiameterCall d = (DiameterCall) call;
            if (d.getDiameterSessionId() != null)
                callsByDiameterSessionId.remove(d.getDiameterSessionId());
        }
    }
}
