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
package org.restcomm.imscf.el.map.sip;

import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.map.MAPModule;
import org.restcomm.imscf.el.map.call.AtiRequest;
import org.restcomm.imscf.el.map.call.MAPCall.MapMethod;
import org.restcomm.imscf.el.map.call.MAPSIPCall;
import org.restcomm.imscf.el.map.scenarios.MapAnyTimeInterrogationRequestScenario;
import org.restcomm.imscf.el.modules.ModuleStore;
import org.restcomm.imscf.el.sip.adapters.SipServletRequestAdapter;
import org.restcomm.imscf.el.sip.servlets.ScenarioBasedServlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.mobicents.protocols.ss7.map.api.MAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SIP servlet handling messages related to MAP.
 */
@javax.servlet.sip.annotation.SipServlet(name = "MAPServlet")
public class MAPServlet extends ScenarioBasedServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MAPServlet.class);

    @Override
    protected void doSubscribe(SipServletRequest req) throws ServletException, IOException {
        LOG.debug("New MAP query.");
        // scenarios should receive a wrapped message
        SipServletRequestAdapter reqA = SipServletRequestAdapter.getAdapter(req);
        SipServletResponse resp;
        // Get target MAP module
        String targetModuleName = getMapModuleName(reqA);
        MAPModule targetModule = ModuleStore.getMapModules().get(targetModuleName);
        if (targetModule == null) {
            resp = reqA.createResponse(SipServletResponse.SC_BAD_REQUEST);
            SipUtil.createAndSetWarningHeader(resp, "MAP module '" + targetModuleName + "' does not exist");
            resp.send();
            return;
        }

        LOG.debug("Target MAP module found: {}", targetModule);
        String callId = callFactory.newCall(reqA, targetModule);
        LOG.debug("IMSCF callId of MAP call: {}", callId);
        try (MAPSIPCall call = (MAPSIPCall) callStore.getCallByImscfCallId(callId)) {
            MapMethod method = MapMethod.fromSubscribe(reqA);
            if (method == null) {
                String methodHeader = reqA.getHeader(SipConstants.HEADER_MAP_METHOD);
                resp = reqA.createResponse(SipServletResponse.SC_BAD_REQUEST);
                if (methodHeader == null) {
                    SipUtil.createAndSetWarningHeader(resp, "Missing " + SipConstants.HEADER_MAP_METHOD + " header");
                } else {
                    SipUtil.createAndSetWarningHeader(resp, "Invalid " + SipConstants.HEADER_MAP_METHOD
                            + " header value: '" + methodHeader + "'");
                }
                resp.send();
                callFactory.deleteCall(call);
                return;
            }
            call.setMapMethod(method);
            switch (method) {
            case AnyTimeInterrogation:
                AtiRequest atiRequest = new AtiRequest(reqA);
                call.setAtiRequest(atiRequest);
                try {
                    call.getMapOutgoingRequestScenarios().add(MapAnyTimeInterrogationRequestScenario.start(call));
                    CapDialogCallData data = new CapDialogCallData();
					data.setImscfCallId(callId);
					((MAPDialogImpl) call.getMAPDialog()).setUserObject(data);
                } catch (MAPException ex) {
                    LOG.error("Exception while sending ATI message: ", ex);
                    resp = reqA.createResponse(500);
                    SipUtil.createAndSetWarningHeader(resp,
                            "Error sending request to remote system '" + atiRequest.getTargetRemoteSystem() + "'");
                    callFactory.deleteCall(call);
                    return;
                }
                // Send "accepted" response
                // TODO expires==0 is ok?
                resp = reqA.createResponse(SipServletResponse.SC_ACCEPTED);
                resp.setExpires(0);
                resp.send();
                // TEST send pending notify
                // SipServletRequest pendingNotify = reqA.getSession().createRequest("NOTIFY");
                // pendingNotify.setHeader("Subscription-State", "pending;expires=0");
                // pendingNotify.send();
                break;
            case ProvideSubscriberInfo:
                LOG.error("ProvideSubscriberInfo is unimplemented");
                resp = reqA.createResponse(SipServletResponse.SC_BAD_REQUEST);
                SipUtil.createAndSetWarningHeader(resp, "ProvideSubscriberInfo is unimplemented");
                resp.send();
                callFactory.deleteCall(call);
                break;
            case SendRoutingInfoForSM:
                LOG.error("SendRoutingInfoForSM is unimplemented");
                resp = reqA.createResponse(SipServletResponse.SC_BAD_REQUEST);
                SipUtil.createAndSetWarningHeader(resp, "SendRoutingInfoForSM is unimplemented");
                resp.send();
                callFactory.deleteCall(call);
                break;
            default:
                LOG.error("unknown MAP call type {}", method);
                resp = reqA.createResponse(SipServletResponse.SC_BAD_REQUEST);
                SipUtil.createAndSetWarningHeader(resp, "Invalid " + SipConstants.HEADER_MAP_METHOD
                        + " header value: '" + method + "'");
                resp.setExpires(0);
                resp.send();
                callFactory.deleteCall(call);
                break;
            }
        }
    }

    private String getMapModuleName(SipServletRequest subscribe) {
        String ret = null;
        URI uri = subscribe.getRequestURI();
        if (uri instanceof SipURI) {
            ret = ((SipURI) uri).getUser();
        }
        return ret;
    }

}
