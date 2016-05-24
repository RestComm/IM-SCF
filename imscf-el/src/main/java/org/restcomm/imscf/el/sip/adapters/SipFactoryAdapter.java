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

import org.restcomm.imscf.el.sip.SipApplicationSessionAttributes;

import java.util.Objects;

import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

/**
 * Adapter class for SipFactory objects.
 * @author Miklos Pocsaji
 *
 */
public final class SipFactoryAdapter extends AdapterBase<SipFactory> implements SipFactory {

    private SipFactoryAdapter(SipFactory delegate) {
        super(delegate);
    }

    @Override
    public Address createAddress(String arg0) throws ServletParseException {
        return delegate.createAddress(arg0);
    }

    @Override
    public Address createAddress(URI arg0, String arg1) {
        return delegate.createAddress(arg0, arg1);
    }

    @Override
    public Address createAddress(URI arg0) {
        return delegate.createAddress(arg0);
    }

    @Override
    public SipApplicationSession createApplicationSession() {
        return SipApplicationSessionAdapter.getAdapter(delegate.createApplicationSession());
    }

    @Override
    public SipApplicationSession createApplicationSessionByKey(String arg0) {
        return SipApplicationSessionAdapter.getAdapter(delegate.createApplicationSessionByKey(arg0));
    }

    @Override
    public AuthInfo createAuthInfo() {
        return delegate.createAuthInfo();
    }

    @Override
    public Parameterable createParameterable(String arg0) throws ServletParseException {
        return delegate.createParameterable(arg0);
    }

    @Override
    public SipServletRequest createRequest(SipApplicationSession arg0, String arg1, Address arg2, Address arg3) {
        Objects.requireNonNull(arg0, "SipApplicationSession cannot be null");
        if (arg0.isValid() && Boolean.TRUE.equals(SipApplicationSessionAttributes.SIP_CALL_FINISHED.get(arg0)))
            throw new IllegalStateException("Cannot create SipServletRequest in already terminated SIP call!");

        SipApplicationSession arg0A = arg0 instanceof SipApplicationSessionAdapter ? ((SipApplicationSessionAdapter) arg0)
                .getDelegate() : arg0;
        return SipServletRequestAdapter.getAdapter(delegate.createRequest(arg0A, arg1, arg2, arg3));
    }

    @Override
    public SipServletRequest createRequest(SipApplicationSession arg0, String arg1, String arg2, String arg3)
            throws ServletParseException {
        Objects.requireNonNull(arg0, "SipApplicationSession cannot be null");
        if (arg0.isValid() && Boolean.TRUE.equals(SipApplicationSessionAttributes.SIP_CALL_FINISHED.get(arg0)))
            throw new IllegalStateException("Cannot create SipServletRequest in already terminated SIP call!");

        SipApplicationSession arg0A = arg0 instanceof SipApplicationSessionAdapter ? ((SipApplicationSessionAdapter) arg0)
                .getDelegate() : arg0;
        return SipServletRequestAdapter.getAdapter(delegate.createRequest(arg0A, arg1, arg2, arg3));
    }

    @Override
    public SipServletRequest createRequest(SipApplicationSession arg0, String arg1, URI arg2, URI arg3) {
        Objects.requireNonNull(arg0, "SipApplicationSession cannot be null");
        if (arg0.isValid() && Boolean.TRUE.equals(SipApplicationSessionAttributes.SIP_CALL_FINISHED.get(arg0)))
            throw new IllegalStateException("Cannot create SipServletRequest in already terminated SIP call!");

        SipApplicationSession arg0A = arg0 instanceof SipApplicationSessionAdapter ? ((SipApplicationSessionAdapter) arg0)
                .getDelegate() : arg0;
        return SipServletRequestAdapter.getAdapter(delegate.createRequest(arg0A, arg1, arg2, arg3));
    }

    @Override
    @Deprecated
    public SipServletRequest createRequest(SipServletRequest arg0, boolean arg1) {
        SipServletRequest arg0A = arg0 instanceof SipServletRequestAdapter ? ((SipServletRequestAdapter) arg0)
                .getDelegate() : arg0;
        return SipServletRequestAdapter.getAdapter(delegate.createRequest(arg0A, arg1));
    }

    @Override
    public SipURI createSipURI(String arg0, String arg1) {
        return delegate.createSipURI(arg0, arg1);
    }

    @Override
    public URI createURI(String arg0) throws ServletParseException {
        return delegate.createURI(arg0);
    }

    public static SipFactoryAdapter getAdapter(SipFactory sf) {
        if (sf == null)
            return null;
        if (sf instanceof SipFactoryAdapter)
            return (SipFactoryAdapter) sf;
        else
            return new SipFactoryAdapter(sf);
    }
}
