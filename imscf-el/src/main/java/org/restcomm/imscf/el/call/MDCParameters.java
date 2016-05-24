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
package org.restcomm.imscf.el.call;

/**
 * MDC parameter names.
 *
 */
public enum MDCParameters {

    SCCP_REMOTE("sccp.remote"),
    SCCP_LOCAL("sccp.local"),
    TCAP_APPCONTEXT("tcap.appCtx"),
    TCAP_LOCAL_DIALOG_ID("tcap.localTID"),
    TCAP_REMOTE_DIALOG_ID("tcap.remoteTID"),
    CAP_CALLING("cap.calling"),
    CAP_CALLED("cap.called"),
    CAP_CALLEDBCD("cap.calledBCD"),
    CAP_DESTINATION_SUBSCRIBER("cap.destSub"),
    CAP_MSISDN("cap.msisdn"),
    IMSCF_CALLID("imscf.callid");

    private final String key;

    private MDCParameters(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
