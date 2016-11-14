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
package org.restcomm.imscf.el.map;

import java.util.Map;
import java.util.HashMap;

import javax.servlet.sip.SipApplicationSession;

import org.mobicents.ss7.congestion.CongestionTicket;

import org.restcomm.imscf.common.ss7.tcap.ImscfTCAPUtil;
import org.restcomm.common.ss7.tcap.NamedTCListener;
import org.restcomm.imscf.el.cap.call.CapDialogCallData;
import org.restcomm.imscf.el.sip.adapters.SipApplicationSessionAdapter;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;

import org.mobicents.protocols.ss7.map.MAPProviderImpl;
import org.mobicents.protocols.ss7.map.MAPDialogImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;
import org.mobicents.protocols.ss7.tcap.asn.InvokeImpl;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;


/**
 * Wrapper class to extends MAPProviderImpl class functionality with IMSCF specific features.
 *
 *
 * @author Balogh Gábor
 *
 */
@SuppressWarnings("serial")
public class MAPProviderImplImscfWrapper extends MAPProviderImpl implements NamedTCListener {

    /**
     * Congestion sources name list. Congestion is where this collection is not empty
     */
    protected transient Map<String, String> congSources = new HashMap<String, String>();

    private String name;

    public MAPProviderImplImscfWrapper(int subSystemNumber, TCAPProvider tcapProvider) {
        this(ImscfTCAPUtil.getMapStackNameForSsn(subSystemNumber), tcapProvider);
    }

    protected MAPProviderImplImscfWrapper(String name, TCAPProvider tcapProvider) {
        super(name, tcapProvider);
        this.name = name;
    }

    @Override
    public String getName() {
       return name;
    }

    public void onCongestionFinish(CongestionTicket ticket) {
        synchronized (this.congSources) {
            this.congSources.put(ticket.getSource(), ticket.getSource());
        }
    }

    public void onCongestionStart(CongestionTicket ticket) {
        synchronized (this.congSources) {
            this.congSources.remove(ticket.getSource());
        }
    }

    public boolean isCongested() {
        if (this.congSources.size() > 0)
            return true;
        else
            return false;
    }

    @Override
    public void onInvokeTimeout(Invoke invoke) {
        MAPDialogImpl mapDialogImpl = (MAPDialogImpl) this.getMAPDialog(((InvokeImpl) invoke).getDialog().getLocalDialogId());
        CapDialogCallData capData = (CapDialogCallData) mapDialogImpl.getUserObject();
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
