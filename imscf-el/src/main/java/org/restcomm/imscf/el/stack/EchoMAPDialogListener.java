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
package org.restcomm.imscf.el.stack;

import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPDialogListener;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortSource;
import org.mobicents.protocols.ss7.map.api.dialog.MAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for logging/debugging callbacks for CAP dialogs.
 */
public class EchoMAPDialogListener implements MAPDialogListener {
    private static Logger logger = LoggerFactory.getLogger(EchoMAPDialogListener.class);

    @Override
    public void onDialogAccept(MAPDialog arg0, MAPExtensionContainer arg1) {
        logger.debug("onDialogAccept: {}, {}", arg0, arg1);
    }

    @Override
    public void onDialogClose(MAPDialog arg0) {
        logger.debug("onDialogClose: {}", arg0);
    }

    @Override
    public void onDialogDelimiter(MAPDialog arg0) {
        logger.debug("onDialogDelimiter: {}", arg0);
    }

    @Override
    public void onDialogNotice(MAPDialog arg0, MAPNoticeProblemDiagnostic arg1) {
        logger.debug("onDialogNotice: {}, {}", arg0, arg1);
    }

    @Override
    public void onDialogRelease(MAPDialog arg0) {
        logger.debug("onDialogRelease: {}", arg0);
    }

    @Override
    public void onDialogTimeout(MAPDialog arg0) {
        logger.debug("onDialogTimeout: {}", arg0);
    }

    @Override
    public void onDialogProviderAbort(MAPDialog arg0, MAPAbortProviderReason arg1, MAPAbortSource arg2,
            MAPExtensionContainer arg3) {
        logger.debug("onDialogProviderAbort: {}, {}, {}, {}", arg0, arg1, arg2, arg3);
    }

    @Override
    public void onDialogReject(MAPDialog arg0, MAPRefuseReason arg1, ApplicationContextName arg2,
            MAPExtensionContainer arg3) {
        logger.debug("onDialogReject: {}, {}, {}, {}", arg0, arg1, arg2, arg3);

    }

    @Override
    public void onDialogRequest(MAPDialog arg0, AddressString arg1, AddressString arg2, MAPExtensionContainer arg3) {
        logger.debug("onDialogRequest: {}, {}, {}, {}", arg0, arg1, arg2, arg3);

    }

    @Override
    public void onDialogRequestEricsson(MAPDialog arg0, AddressString arg1, AddressString arg2, IMSI arg3,
            AddressString arg4) {
        logger.debug("onDialogRequestEricsson: {}, {}, {}, {}, {}", arg0, arg1, arg2, arg3, arg4);

    }

    @Override
    public void onDialogUserAbort(MAPDialog arg0, MAPUserAbortChoice arg1, MAPExtensionContainer arg2) {
        logger.debug("onDialogUserAbort: {}, {}, {}", arg0, arg1, arg2);
    }

}
