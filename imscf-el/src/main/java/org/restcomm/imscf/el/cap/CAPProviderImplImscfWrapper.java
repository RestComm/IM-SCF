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
package org.restcomm.imscf.el.cap;

import javax.servlet.sip.SipApplicationSession;

import org.restcomm.imscf.common.ss7.tcap.ImscfTCAPUtil;
import org.restcomm.common.ss7.tcap.NamedTCListener;
import org.restcomm.imscf.el.cap.call.CapDialogCallData;
import org.restcomm.imscf.el.sip.adapters.SipApplicationSessionAdapter;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;

import org.mobicents.protocols.ss7.cap.CAPProviderImpl;
import org.mobicents.protocols.ss7.cap.CAPDialogImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;
import org.mobicents.protocols.ss7.tcap.asn.InvokeImpl;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;

/**
 * Wrapper class to extends CAPProviderImpl class functionality with IMSCF specific features.
 *
 *
 * @author Balogh Gábor
 *
 */
@SuppressWarnings("serial")
public class CAPProviderImplImscfWrapper extends CAPProviderImpl implements NamedTCListener {

    protected transient CAPTimerDefault timerDefault=null;

    private String name;

    public CAPProviderImplImscfWrapper(int subSystemNumber, TCAPProvider tcapProvider) {
       this(ImscfTCAPUtil.getCapStackNameForSsn(subSystemNumber), tcapProvider);
    }

    protected CAPProviderImplImscfWrapper(String name, TCAPProvider tcapProvider) {
       super(name, tcapProvider);
       this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public CAPTimerDefault getCAPTimerDefault() {
        return this.timerDefault;
    }

    public void setCAPTimerDefault(CAPTimerDefault timerDefault) {
        this.timerDefault = timerDefault;
    }

    @Override
    public void onInvokeTimeout(Invoke invoke) {
        CAPDialogImpl capDialogImpl = (CAPDialogImpl) this.getCAPDialog(((InvokeImpl) invoke).getDialog().getLocalDialogId());
        CapDialogCallData capData = (CapDialogCallData) capDialogImpl.getUserObject();
        SipApplicationSession sas = SipServletResources.getSipSessionsUtil().getApplicationSessionByKey(
            capData.getImscfCallId(), false);
        sas = SipApplicationSessionAdapter.unwrap(sas);
        try {
            ((MobicentsSipApplicationSession) sas).acquire();
            super.onInvokeTimeout(invoke);
		}
		finally {
            ((MobicentsSipApplicationSession) sas).release();
		}
    }

}
