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
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

/**
 * Adapter for class Proxy.
 * @author Miklos Pocsaji
 *
 */
public final class ProxyAdapter extends AdapterBase<Proxy> implements Proxy {

    private ProxyAdapter(Proxy delegate) {
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
    public List<ProxyBranch> createProxyBranches(List<? extends URI> arg0) {
        return delegate.createProxyBranches(arg0).stream().map(ProxyBranchAdapter::getAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public boolean getAddToPath() {
        return delegate.getAddToPath();
    }

    @Override
    public boolean getNoCancel() {
        return delegate.getNoCancel();
    }

    @Override
    public SipServletRequest getOriginalRequest() {
        return SipServletRequestAdapter.getAdapter(delegate.getOriginalRequest());
    }

    @Override
    public boolean getParallel() {
        return delegate.getParallel();
    }

    @Override
    public SipURI getPathURI() {
        return delegate.getPathURI();
    }

    @Override
    public ProxyBranch getProxyBranch(URI arg0) {
        return ProxyBranchAdapter.getAdapter(delegate.getProxyBranch(arg0));
    }

    @Override
    public List<ProxyBranch> getProxyBranches() {
        return delegate.getProxyBranches().stream().map(ProxyBranchAdapter::getAdapter).collect(Collectors.toList());
    }

    @Override
    public int getProxyTimeout() {
        return delegate.getProxyTimeout();
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
    @Deprecated
    public int getSequentialSearchTimeout() {
        return delegate.getSequentialSearchTimeout();
    }

    @Override
    @Deprecated
    public boolean getStateful() {
        return delegate.getStateful();
    }

    @Override
    public boolean getSupervised() {
        return delegate.getSupervised();
    }

    @Override
    public void proxyTo(URI arg0) {
        delegate.proxyTo(arg0);
    }

    @Override
    public void proxyTo(List<? extends URI> arg0) {
        delegate.proxyTo(arg0);
    }

    @Override
    public void setAddToPath(boolean arg0) {
        delegate.setAddToPath(arg0);
    }

    @Override
    public void setNoCancel(boolean arg0) {
        delegate.setNoCancel(arg0);
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
    public void setParallel(boolean arg0) {
        delegate.setParallel(arg0);
    }

    @Override
    public void setProxyTimeout(int arg0) {
        delegate.setProxyTimeout(arg0);
    }

    @Override
    public void setRecordRoute(boolean arg0) {
        delegate.setRecordRoute(arg0);
    }

    @Override
    public void setRecurse(boolean arg0) {
        delegate.setRecurse(arg0);
    }

    @Override
    @Deprecated
    public void setSequentialSearchTimeout(int arg0) {
        delegate.setSequentialSearchTimeout(arg0);
    }

    @Override
    @Deprecated
    public void setStateful(boolean arg0) {
        delegate.setStateful(arg0);
    }

    @Override
    public void setSupervised(boolean arg0) {
        delegate.setSupervised(arg0);
    }

    @Override
    public void startProxy() {
        delegate.startProxy();
    }

    public static ProxyAdapter getAdapter(Proxy p) {
        if (p == null)
            return null;
        if (p instanceof ProxyAdapter)
            return (ProxyAdapter) p;
        else
            return new ProxyAdapter(p);
    }
}
