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
package org.restcomm.imscf.el.cap.sip;

import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.modules.ModuleStore;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.adapters.SipServletRequestAdapter;
import org.restcomm.imscf.el.sip.servlets.MainServlet;
import org.restcomm.imscf.el.sip.servlets.ScenarioBasedServlet;
import org.restcomm.imscf.el.stack.CallContext;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SIP servlet handling messages related to CAP call handling.
 */
@javax.servlet.sip.annotation.SipServlet(name = "CAPServlet")
public class CAPServlet extends ScenarioBasedServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CAPServlet.class);

    private static final long serialVersionUID = 1L;

    @Override
    public void doInvite(SipServletRequest invite) throws IOException { // NOPMD parameter reassign
        try (SIPCall call = callStore.getCallByAppSessionId(invite.getApplicationSession().getId());) {
            if (call != null) {
                // this path should be unreachable. Expected paths are:
                // service() -- call exists --> Scenario list
                // service() -- call is null --> super.service() -> doInvite
                throw new AssertionError("doInvite for existing call with appsession id "
                        + invite.getApplicationSession(false).getId());
            }
        }

        LOG.debug("Incoming INVITE for new application initiated call.");
        // create a new call

        // scenarios should receive a wrapped message
        invite = SipServletRequestAdapter.getAdapter(invite);

        // Get target CAP module
        String targetModuleName = getCapModuleName(invite);
        CAPModule targetModule = targetModuleName == null ? null : ModuleStore.getCapModules().get(targetModuleName);
        if (targetModule == null) {
            LOG.debug("Target CAP module named '{}' does not exist", targetModuleName);
            SipServletResponse resp = invite.createResponse(SipServletResponse.SC_BAD_REQUEST);
            SipUtil.createAndSetWarningHeader(resp, "CAP module named '" + targetModuleName + "' does not exist");
            resp.send();
            return;
        }

        LOG.debug("Target CAP module found: {}", targetModule.getName());
        String imscfCallId = callFactory.newCall(invite, targetModule);
        if (imscfCallId == null) {
            SipServletResponse resp = invite.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
            SipUtil.createAndSetWarningHeader(resp, "Failed to initialize CAP call");
            resp.send();
            return;
        }

        // retrieve the newly created call
        try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getCallByImscfCallId(imscfCallId)) {
            Objects.requireNonNull(call, "Invalid state, call cannot be null"); // it was created above

            call.setAsProvidedServiceIdentifier(getAsProvidedServiceIdentifier(invite));

            // ask module to init the appropriate scenarios
            call.getCapModule().onAsInitiatedCall(imscfCallId);
            // call super to execute the scenarios (one of which should be the ICA INVITE scenario...)
            super.runScenarios(call, invite);
        }
    }

    private String getCapModuleName(SipServletRequest invite) {
        return Optional.ofNullable(MainServlet.getTopMostRouteHeader(invite))
                .map(r -> r.getParameter(SipConstants.CAP_MODULE_NAME_PARAM)).map(SipUtil::unQuote).orElse(null);
    }

    private String getAsProvidedServiceIdentifier(SipServletRequest invite) {
        return invite.getHeader(SipConstants.HEADER_IMSCF_SERVICE_IDENTIFIER); // could be null, don't care
    }

    @Override
    protected void doBye(SipServletRequest req) throws ServletException, IOException {
        // during a simultaneous disconnect, the AS may forward a BYE message after the call was deleted.

        try (SIPCall call = callStore.getCallByAppSessionId(req.getApplicationSession().getId());) {
            if (call != null) {
                // this path should be unreachable. Expected paths are:
                // service() -- call exists --> Scenario list
                // service() -- call is null --> super.service() -> doBye
                throw new AssertionError("doBye for existing call with appsession id "
                        + req.getApplicationSession(false).getId());
            }
        }

        LOG.debug("Incoming BYE for nonexistent call, probably deleted already. Sending back 200.");
        if (!req.isCommitted()) {
            SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(req.createResponse(SipServletResponse.SC_OK),
                    "Call does not exist"), "Failed to send 200 BYE in already finished call for:\n{}", req);
        }
    }
}
