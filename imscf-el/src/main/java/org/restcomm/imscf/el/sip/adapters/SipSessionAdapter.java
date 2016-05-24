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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

/**
 * Adapter class for SipSession objects.
 * @author Miklos Pocsaji
 *
 */
@SuppressWarnings("serial")
public final class SipSessionAdapter extends AdapterBase<SipSession> implements SipSession, Serializable {

    private SipSessionAdapter(SipSession delegate) {
        super(delegate);
    }

    @Override
    public SipServletRequest createRequest(String arg0) {
        return SipServletRequestAdapter.getAdapter(delegate.createRequest(arg0));
    }

    @Override
    public SipApplicationSession getApplicationSession() {
        return SipApplicationSessionAdapter.getAdapter(delegate.getApplicationSession());
    }

    @Override
    public Object getAttribute(String arg0) {
        return delegate.getAttribute(arg0);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return delegate.getAttributeNames();
    }

    @Override
    public String getCallId() {
        return delegate.getCallId();
    }

    @Override
    public long getCreationTime() {
        return delegate.getCreationTime();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public boolean getInvalidateWhenReady() {
        return delegate.getInvalidateWhenReady();
    }

    @Override
    public long getLastAccessedTime() {
        return delegate.getLastAccessedTime();
    }

    @Override
    public Address getLocalParty() {
        return delegate.getLocalParty();
    }

    @Override
    public SipApplicationRoutingRegion getRegion() {
        return delegate.getRegion();
    }

    @Override
    public Address getRemoteParty() {
        return delegate.getRemoteParty();
    }

    @Override
    public ServletContext getServletContext() {
        return delegate.getServletContext();
    }

    @Override
    public State getState() {
        return delegate.getState();
    }

    @Override
    public URI getSubscriberURI() {
        return delegate.getSubscriberURI();
    }

    @Override
    public void invalidate() {
        delegate.invalidate();
    }

    @Override
    public boolean isReadyToInvalidate() {
        return delegate.isReadyToInvalidate();
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void setHandler(String arg0) throws ServletException {
        delegate.setHandler(arg0);
    }

    @Override
    public void setInvalidateWhenReady(boolean arg0) {
        delegate.setInvalidateWhenReady(arg0);
    }

    @Override
    public void setOutboundInterface(InetAddress arg0) {
        delegate.setOutboundInterface(arg0);
    }

    @Override
    public void setOutboundInterface(InetSocketAddress arg0) {
        delegate.setOutboundInterface(arg0);
    }

    public static SipSessionAdapter getAdapter(SipSession session) {
        if (session == null)
            return null;
        if (session instanceof SipSessionAdapter)
            return (SipSessionAdapter) session;
        else
            return new SipSessionAdapter(session);
    }
}
