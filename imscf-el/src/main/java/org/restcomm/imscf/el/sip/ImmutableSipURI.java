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
package org.restcomm.imscf.el.sip;

import java.util.Iterator;

import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

/** Immutable view of a SIP URI. */
public final class ImmutableSipURI implements SipURI {
    SipURI delegate;

    public ImmutableSipURI(SipURI delegate) {
        this.delegate = delegate;
    }

    @Override
    public void removeParameter(String arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setParameter(String arg0, String arg1) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void removeHeader(String arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setHeader(String arg0, String arg1) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setHost(String arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setLrParam(boolean arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setMAddrParam(String arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setMethodParam(String arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setPort(int arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setSecure(boolean arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setTTLParam(int arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setTransportParam(String arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setUser(String arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setUserParam(String arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public void setUserPassword(String arg0) {
        throw new UnsupportedOperationException("This SipURI instance is immutable");
    }

    @Override
    public URI clone() {
        return delegate.clone();
    }

    @Override
    public boolean equals(Object arg0) {
        return delegate.equals(arg0 instanceof ImmutableSipURI ? ((ImmutableSipURI) arg0).delegate : arg0);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String getHeader(String arg0) {
        return delegate.getHeader(arg0);
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return delegate.getHeaderNames();
    }

    @Override
    public String getHost() {
        return delegate.getHost();
    }

    @Override
    public boolean getLrParam() {
        return delegate.getLrParam();
    }

    @Override
    public String getMAddrParam() {
        return delegate.getMAddrParam();
    }

    @Override
    public String getMethodParam() {
        return delegate.getMethodParam();
    }

    @Override
    public String getParameter(String arg0) {
        return delegate.getParameter(arg0);
    }

    @Override
    public Iterator<String> getParameterNames() {
        return delegate.getParameterNames();
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }

    @Override
    public String getScheme() {
        return delegate.getScheme();
    }

    @Override
    public int getTTLParam() {
        return delegate.getTTLParam();
    }

    @Override
    public String getTransportParam() {
        return delegate.getTransportParam();
    }

    @Override
    public String getUser() {
        return delegate.getUser();
    }

    @Override
    public String getUserParam() {
        return delegate.getUserParam();
    }

    @Override
    public String getUserPassword() {
        return delegate.getUserPassword();
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    @Override
    public boolean isSipURI() {
        return delegate.isSipURI();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
