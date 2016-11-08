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
package org.restcomm.imscf.el.stack;

import java.util.Arrays;

import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.tcap.api.NamedTCListener;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.Dialog;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCBeginIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCContinueIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCEndIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCNoticeIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCPAbortIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCUniIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCUserAbortIndication;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for logging/debugging callbacks for TCAP dialogs.
 */
public class EchoTCapListener implements NamedTCListener {
    private static Logger logger = LoggerFactory.getLogger(EchoTCapListener.class);

    private String name;

    public EchoTCapListener(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onTCUserAbort(TCUserAbortIndication arg0) {
        logger.debug(getName() + ".onTCUserAbort: " + arg0);
    }

    @Override
    public void onTCUni(TCUniIndication arg0) {
        logger.debug(getName() + ".onTCUni: " + arg0);
    }

    @Override
    public void onTCPAbort(TCPAbortIndication arg0) {
        logger.debug(getName() + ".onTCPAbort: " + arg0);
    }

    @Override
    public void onTCNotice(TCNoticeIndication arg0) {
        logger.debug(getName() + ".onTCNotice: " + arg0);
    }

    @Override
    public void onTCEnd(TCEndIndication tcap) {
        logger.debug(getName() + ".onTCEnd: " + tcap);
    }

    @Override
    public void onTCContinue(TCContinueIndication tcap) {
        logger.debug(getName() + ".onTCContinue: " + tcap);
    }

    @Override
    public void onTCBegin(TCBeginIndication tcap) {
        logger.debug(getName() + ".onTCBegin: " + tcap);

        ApplicationContextName acn = tcap.getApplicationContextName();
        long[] oid = acn.getOid();
        logger.debug(getName() + ": New TCAP dialog, OID: " + Arrays.toString(oid));
        CAPApplicationContext capCtx = CAPApplicationContext.getInstance(oid);
        MAPApplicationContext mapCtx = MAPApplicationContext.getInstance(oid);
        if (capCtx != null) {
            switch (capCtx) {
            case CapV2_gsmSSF_to_gsmSCF:
                logger.debug("New CAP2 call");
                break;
            case CapV3_gsmSSF_scfGeneric:
                logger.debug("New CAP3 call");
                break;
            case CapV3_cap3_sms:
                logger.debug("New CAP3 SMS");
                break;
            case CapV4_gsmSSF_scfGeneric:
                logger.debug("New CAP4 call");
                break;
            case CapV4_cap4_sms:
                logger.debug("New CAP4 SMS");
                break;
            default:
                logger.warn("Unhandled application context {}", Arrays.toString(oid));
                break;
            }
        } else if (mapCtx != null) {
            if (MAPApplicationContextName.mmEventReportingContext.equals(mapCtx.getApplicationContextName())) {
                logger.debug("New Mobility Management MAP dialog (NoteMM-Event)");
            } else {
                logger.warn("Unhandled application context: {}", Arrays.toString(oid));
            }
        } else {
            logger.warn("Unrecognized application context: {}", Arrays.toString(oid));
        }
    }

    @Override
    public void onInvokeTimeout(Invoke arg0) {
        logger.debug(getName() + ".onInvokeTimeout: " + arg0);
    }

    @Override
    public void onDialogTimeout(Dialog arg0) {
        logger.debug(getName() + ".onDialogTimeout: " + arg0);
    }

    @Override
    public void onDialogReleased(Dialog arg0) {
        logger.debug(getName() + ".onDialogReleased: " + arg0);
    }

}
