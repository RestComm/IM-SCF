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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.UAMode;

/**
 * Adapter for class B2buaHelper.
 *
 */
public final class B2buaHelperAdapter extends AdapterBase<B2buaHelper> implements B2buaHelper {

    private B2buaHelperAdapter(B2buaHelper delegate) {
        super(delegate);
    }

    @Override
    public SipServletRequest createCancel(SipSession arg0) {
        SipSession arg0A = arg0 instanceof SipSessionAdapter ? ((SipSessionAdapter) arg0).getDelegate() : arg0;
        return SipServletRequestAdapter.getAdapter(delegate.createCancel(arg0A));
    }

    @Override
    public SipServletRequest createRequest(SipServletRequest arg0, boolean arg1, Map<String, List<String>> arg2)
            throws TooManyHopsException {
        SipServletRequest arg0A = arg0 instanceof SipServletRequestAdapter ? ((SipServletRequestAdapter) arg0)
                .getDelegate() : arg0;
        return SipServletRequestAdapter.getAdapter(delegate.createRequest(arg0A, arg1, arg2));
    }

    @Override
    public SipServletRequest createRequest(SipServletRequest arg0) {
        SipServletRequest arg0A = arg0 instanceof SipServletRequestAdapter ? ((SipServletRequestAdapter) arg0)
                .getDelegate() : arg0;
        return SipServletRequestAdapter.getAdapter(delegate.createRequest(arg0A));
    }

    @Override
    public SipServletRequest createRequest(SipSession arg0, SipServletRequest arg1, Map<String, List<String>> arg2) {
        SipSession arg0A = arg0 instanceof SipSessionAdapter ? ((SipSessionAdapter) arg0).getDelegate() : arg0;
        SipServletRequest arg1A = arg1 instanceof SipServletRequestAdapter ? ((SipServletRequestAdapter) arg1)
                .getDelegate() : arg1;
        return SipServletRequestAdapter.getAdapter(delegate.createRequest(arg0A, arg1A, arg2));
    }

    @Override
    public SipServletResponse createResponseToOriginalRequest(SipSession arg0, int arg1, String arg2) {
        SipSession arg0A = arg0 instanceof SipSessionAdapter ? ((SipSessionAdapter) arg0).getDelegate() : arg0;
        return SipServletResponseAdapter.getAdapter(delegate.createResponseToOriginalRequest(arg0A, arg1, arg2));
    }

    @Override
    public SipSession getLinkedSession(SipSession arg0) {
        SipSession arg0A = arg0 instanceof SipSessionAdapter ? ((SipSessionAdapter) arg0).getDelegate() : arg0;
        return SipSessionAdapter.getAdapter(delegate.getLinkedSession(arg0A));
    }

    @Override
    public SipServletRequest getLinkedSipServletRequest(SipServletRequest arg0) {
        SipServletRequest arg0A = arg0 instanceof SipServletRequestAdapter ? ((SipServletRequestAdapter) arg0)
                .getDelegate() : arg0;
        return SipServletRequestAdapter.getAdapter(delegate.getLinkedSipServletRequest(arg0A));
    }

    @Override
    public List<SipServletMessage> getPendingMessages(SipSession arg0, UAMode arg1) {
        SipSession arg0A = arg0 instanceof SipSessionAdapter ? ((SipSessionAdapter) arg0).getDelegate() : arg0;

        return delegate.getPendingMessages(arg0A, arg1).stream()
                .<SipServletMessage> map(SipServletMessageAdapter::getAdapter).collect(Collectors.toList());
    }

    @Override
    public void linkSipSessions(SipSession arg0, SipSession arg1) {
        SipSession arg0A = arg0 instanceof SipSessionAdapter ? ((SipSessionAdapter) arg0).getDelegate() : arg0;
        SipSession arg1A = arg1 instanceof SipSessionAdapter ? ((SipSessionAdapter) arg1).getDelegate() : arg1;
        delegate.linkSipSessions(arg0A, arg1A);
    }

    @Override
    public void unlinkSipSessions(SipSession arg0) {
        SipSession arg0A = arg0 instanceof SipSessionAdapter ? ((SipSessionAdapter) arg0).getDelegate() : arg0;
        delegate.unlinkSipSessions(arg0A);
    }

    public static B2buaHelperAdapter getAdapter(B2buaHelper helper) {
        if (helper == null)
            return null;
        if (helper instanceof B2buaHelperAdapter)
            return (B2buaHelperAdapter) helper;
        else
            return new B2buaHelperAdapter(helper);
    }
}
