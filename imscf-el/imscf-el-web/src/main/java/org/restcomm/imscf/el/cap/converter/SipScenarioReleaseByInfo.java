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
package org.restcomm.imscf.el.cap.converter;

import java.io.IOException;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.isup.CauseCap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.cap.CapUtil;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipReleaseCallByInfoDetector;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.stack.CallContext;

/**
 * SIP scenario for detecting SIP INFO with Reason: SIP;cause=902 and releasing the call.
 */
public final class SipScenarioReleaseByInfo extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioReleaseByInfo.class);

    public static SipScenarioReleaseByInfo start() {
        return new SipScenarioReleaseByInfo();
    }

    private SipScenarioReleaseByInfo() {
        super("Release by INFO 902", SipReleaseCallByInfoDetector.SHARED_INSTANCE, (scenario, msg) -> {
            CallStore cs = (CallStore) CallContext.get(CallContext.CALLSTORE);
            try (CapSipCsCall call = (CapSipCsCall) cs.getSipCall(msg)) {
                scenario.setFinished();

                // check that the CAP dialog is usable beforehand instead of failing later
                if (!CapUtil.canSendPrimitives(call.getCapDialog())) {
                    SipUtil.sendOrWarn(SipUtil.createAndSetWarningHeader(
                            ((SipServletRequest) msg).createResponse(SipServletResponse.SC_BAD_REQUEST),
                            "Cannot send releaseCall in dialog state: " + call.getCapDialog().getState()),
                            "Failed to send error response to releaseCall INFO");
                    return;
                }

                try {
                    ((SipServletRequest) msg).createResponse(SipServletResponse.SC_OK).send();
                } catch (IOException e) {
                    LOG.warn("Failed to send 200 OK to INFO", e);
                }

                // send() instead of close() to allow any pending reports to arrive from MSS.
                try {
                    CauseCap cause = call.getPendingReleaseCause();
                    LOG.debug("Using stored release cause {} in releaseCall", cause);
                    ReleaseCallUtil.releaseCall(call, cause, false);
                } catch (CAPException e) {
                    LOG.warn("Failed to send releaseCall", e);
                }
            }
        });
    }
}
