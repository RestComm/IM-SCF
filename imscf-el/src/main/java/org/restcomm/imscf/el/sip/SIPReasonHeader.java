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
package org.restcomm.imscf.el.sip;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletMessage;

import org.mobicents.protocols.ss7.tcap.asn.comp.PAbortCauseType;

/** Class for parsing SIP release causes from SIP Reason header. */
public class SIPReasonHeader extends ReasonHeader {

    // OCSC compatible codes
    public static final int CAUSE_CAP_CANCEL = 901;
    public static final int CAUSE_RELEASE_CALL = 902;
    // IMSCF only codes
    public static final int CAUSE_ACTIVITYTEST_FAILURE = 801;
    public static final int CAUSE_PROVIDER_ABORT = 802;
    public static final int CAUSE_MAX_CALL_LENGTH = 803;

    public static final SIPReasonHeader INSTANCE_CAP_CANCEL = new SIPReasonHeader(CAUSE_CAP_CANCEL, "cancel");
    public static final SIPReasonHeader INSTANCE_RELEASE_CALL = new SIPReasonHeader(CAUSE_RELEASE_CALL, "BYE");
    public static final SIPReasonHeader INSTANCE_ACTIVITYTEST_FAILURE = new SIPReasonHeader(CAUSE_ACTIVITYTEST_FAILURE,
            "Failed activityTest");
    public static final SIPReasonHeader INSTANCE_MAX_CALL_LENGTH = new SIPReasonHeader(CAUSE_MAX_CALL_LENGTH,
            "Max call duration exceeded");

    public static final SIPReasonHeader INSTANCE_PROVIDER_ABORT_NOREASON = new SIPReasonHeader(CAUSE_PROVIDER_ABORT,
            PAbortCauseType.NoReasonGiven.name());
    public static final SIPReasonHeader INSTANCE_PROVIDER_ABORT_UNREC_TX = new SIPReasonHeader(CAUSE_PROVIDER_ABORT,
            PAbortCauseType.UnrecognizedTxID.name());

    private static final String PROTOCOL = "SIP";

    private SIPReasonHeader() {
    }

    public SIPReasonHeader(Integer cause, String text) {
        super(PROTOCOL, cause, text);
    }

    public static SIPReasonHeader parse(SipServletMessage msg) throws ServletParseException {
        return ReasonHeader.parse(msg, p -> PROTOCOL.equals(p), SIPReasonHeader::new);
    }
}
