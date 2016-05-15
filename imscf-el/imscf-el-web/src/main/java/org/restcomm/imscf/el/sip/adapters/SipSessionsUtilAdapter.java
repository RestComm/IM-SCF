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
package org.restcomm.imscf.el.sip.adapters;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionsUtil;

/**
 * Adapter for the SipSessionsUtil.
 *
 */
public final class SipSessionsUtilAdapter extends AdapterBase<SipSessionsUtil> implements SipSessionsUtil {

    private SipSessionsUtilAdapter(SipSessionsUtil delegate) {
        super(delegate);
    }

    @Override
    public SipApplicationSession getApplicationSessionById(String arg0) {
        return SipApplicationSessionAdapter.getAdapter(delegate.getApplicationSessionById(arg0));
    }

    @Override
    public SipApplicationSession getApplicationSessionByKey(String arg0, boolean arg1) {
        return SipApplicationSessionAdapter.getAdapter(delegate.getApplicationSessionByKey(arg0, arg1));
    }

    @Override
    public SipSession getCorrespondingSipSession(SipSession arg0, String arg1) {
        SipSession arg0A = arg0 instanceof SipSessionAdapter ? ((SipSessionAdapter) arg0).getDelegate() : arg0;
        SipSession ret = delegate.getCorrespondingSipSession(arg0A, arg1);
        return SipSessionAdapter.getAdapter(ret);
    }

    public static SipSessionsUtilAdapter getAdapter(SipSessionsUtil sessionsUtil) {
        if (sessionsUtil == null)
            return null;
        if (sessionsUtil instanceof SipSessionsUtilAdapter)
            return (SipSessionsUtilAdapter) sessionsUtil;
        else
            return new SipSessionsUtilAdapter(sessionsUtil);
    }
}
