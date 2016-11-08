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
package org.restcomm.imscf.el.sip.adapters;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

/**
 * Adapter for class ProxyBranch.
 * @author Miklos Pocsaji
 *
 */
public final class ProxyBranchAdapter extends AdapterBase<ProxyBranch> implements ProxyBranch {

    private ProxyBranchAdapter(ProxyBranch delegate) {
        super(delegate);
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }

    @Override
    public void cancel(String[] arg0, int[] arg1, String[] arg2) {
        delegate.cancel(arg0, arg1, arg2);
    }

    @Override
    public boolean getAddToPath() {
        return delegate.getAddToPath();
    }

    @Override
    public SipURI getPathURI() {
        return delegate.getPathURI();
    }

    @Override
    public Proxy getProxy() {
        return ProxyAdapter.getAdapter(delegate.getProxy());
    }

    @Override
    public int getProxyBranchTimeout() {
        return delegate.getProxyBranchTimeout();
    }

    @Override
    public boolean getRecordRoute() {
        return delegate.getRecordRoute();
    }

    @Override
    public SipURI getRecordRouteURI() {
        return delegate.getRecordRouteURI();
    }

    @Override
    public boolean getRecurse() {
        return delegate.getRecurse();
    }

    @Override
    public List<ProxyBranch> getRecursedProxyBranches() {
        return delegate.getRecursedProxyBranches().stream().map(ProxyBranchAdapter::getAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public SipServletRequest getRequest() {
        return SipServletRequestAdapter.getAdapter(delegate.getRequest());
    }

    @Override
    public SipServletResponse getResponse() {
        return SipServletResponseAdapter.getAdapter(delegate.getResponse());
    }

    @Override
    public boolean isStarted() {
        return delegate.isStarted();
    }

    @Override
    public void setAddToPath(boolean arg0) {
        delegate.setAddToPath(arg0);
    }

    @Override
    public void setOutboundInterface(InetSocketAddress arg0) {
        delegate.setOutboundInterface(arg0);
    }

    @Override
    public void setOutboundInterface(InetAddress arg0) {
        delegate.setOutboundInterface(arg0);
    }

    @Override
    public void setProxyBranchTimeout(int arg0) {
        delegate.setProxyBranchTimeout(arg0);
    }

    @Override
    public void setRecordRoute(boolean arg0) {
        delegate.setRecordRoute(arg0);
    }

    @Override
    public void setRecurse(boolean arg0) {
        delegate.setRecurse(arg0);
    }

    public static ProxyBranchAdapter getAdapter(ProxyBranch pb) {
        if (pb == null)
            return null;
        if (pb instanceof ProxyBranchAdapter)
            return (ProxyBranchAdapter) pb;
        else
            return new ProxyBranchAdapter(pb);
    }

}
