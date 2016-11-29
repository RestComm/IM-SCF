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

import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipMessageDetector;
import org.restcomm.imscf.el.sip.SipMessageHandler;
import org.restcomm.imscf.el.stack.CallContext;
import javax.servlet.sip.SipServletMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scenario for enabling/disabling automatic CWA sending. */
public final class SipScenarioToggleAutomaticCallProcessing extends Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(SipScenarioToggleAutomaticCallProcessing.class);

    /** Since this scenario never finishes, and all of name, detector and handler are immutable and shared,
     *  the scenario itself is effectively immutable and can be shared. */
    static final SipScenarioToggleAutomaticCallProcessing SHARED_INSTANCE = new SipScenarioToggleAutomaticCallProcessing();

    public static SipScenarioToggleAutomaticCallProcessing start() {
        return SHARED_INSTANCE;
    }

    private SipScenarioToggleAutomaticCallProcessing() {
        super("Waiting for Auto CPS toggle", Detector.SHARED_INSTANCE, Handler.SHARED_INSTANCE);
    }

    /** Detector. */
    private static final class Detector implements SipMessageDetector {
        static final Detector SHARED_INSTANCE = new Detector();

        private Detector() {
        }

        @Override
        public boolean accept(SipServletMessage msg) {
            return msg.getHeader(SipConstants.HEADER_AUTOMATIC_CALL_PROCESSING_SUSPENSION) != null;
        }
    }

    /** Handler. */
    private static final class Handler implements SipMessageHandler {
        static final Handler SHARED_INSTANCE = new Handler();

        private Handler() {
        }

        @Override
        public void handleMessage(Scenario scenario, SipServletMessage msg) {
            try (CapSipCsCall call = (CapSipCsCall) CallContext.getCallStore().getSipCall(msg)) {
                String val = msg.getHeader(SipConstants.HEADER_AUTOMATIC_CALL_PROCESSING_SUSPENSION);
                switch (val) {
                case SipConstants.HVALUE_CPS_START:
                    call.setAutomaticCallProcessingEnabled(false);
                    LOG.debug("Automatic call processing disabled");
                    break;
                case SipConstants.HVALUE_CPS_STOP:
                    call.setAutomaticCallProcessingEnabled(true);
                    LOG.debug("Automatic call processing enabled");
                    break;
                default:
                    LOG.warn("Invalid value '{}' in '{}' header!", val,
                            SipConstants.HEADER_AUTOMATIC_CALL_PROCESSING_SUSPENSION);
                    break;
                }
            }
        }
    }
}
