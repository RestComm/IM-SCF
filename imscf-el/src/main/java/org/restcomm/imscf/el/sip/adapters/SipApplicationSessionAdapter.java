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

import org.restcomm.imscf.util.IteratorStream;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;

/**
 * Adapter class for SipApplicationSession objects.
 *
 */
public final class SipApplicationSessionAdapter extends AdapterBase<SipApplicationSession> implements
        SipApplicationSession {

    private SipApplicationSessionAdapter(SipApplicationSession delegate) {
        super(delegate);
    }

    @Override
    @Deprecated
    public void encodeURI(URI arg0) {
        delegate.encodeURI(arg0);
    }

    @Override
    public URL encodeURL(URL arg0) {
        return delegate.encodeURL(arg0);
    }

    @Override
    public String getApplicationName() {
        return delegate.getApplicationName();
    }

    @Override
    public Object getAttribute(String arg0) {
        return delegate.getAttribute(arg0);
    }

    @Override
    public Iterator<String> getAttributeNames() {
        return delegate.getAttributeNames();
    }

    @Override
    public long getCreationTime() {
        return delegate.getCreationTime();
    }

    @Override
    public long getExpirationTime() {
        return delegate.getExpirationTime();
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
    public Object getSession(String arg0, Protocol arg1) {
        Object ret = delegate.getSession(arg0, arg1);
        if (ret instanceof SipSession) {
            return SipSessionAdapter.getAdapter((SipSession) ret);
        } else {
            return ret;
        }
    }

    @Override
    public Iterator<?> getSessions() {
        return IteratorStream.of(delegate.getSessions())
                .map(o -> o instanceof SipSession ? SipSessionAdapter.getAdapter((SipSession) o) : o).iterator();

    }

    @Override
    public Iterator<?> getSessions(String arg0) {
        return IteratorStream.of(delegate.getSessions(arg0))
                .map(o -> o instanceof SipSession ? SipSessionAdapter.getAdapter((SipSession) o) : o).iterator();
    }

    @Override
    public SipSession getSipSession(String arg0) {
        return SipSessionAdapter.getAdapter(delegate.getSipSession(arg0));
    }

    @Override
    public ServletTimer getTimer(String arg0) {
        return ServletTimerAdapter.getAdapter(delegate.getTimer(arg0));
    }

    @Override
    public Collection<ServletTimer> getTimers() {
        return delegate.getTimers().stream().map(ServletTimerAdapter::getAdapter).collect(Collectors.toList());
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
    public int setExpires(int arg0) {
        return delegate.setExpires(arg0);
    }

    @Override
    public void setInvalidateWhenReady(boolean arg0) {
        delegate.setInvalidateWhenReady(arg0);
    }

    public static SipApplicationSessionAdapter getAdapter(SipApplicationSession as) {
        if (as == null)
            return null;
        if (as instanceof SipApplicationSessionAdapter)
            return (SipApplicationSessionAdapter) as;
        else
            return new SipApplicationSessionAdapter(as);
    }

    public static SipApplicationSession unwrap(SipApplicationSession sas) {
        if (sas instanceof SipApplicationSessionAdapter)
            return ((SipApplicationSessionAdapter) sas).getDelegate();
        else
            return sas;
    }
}
