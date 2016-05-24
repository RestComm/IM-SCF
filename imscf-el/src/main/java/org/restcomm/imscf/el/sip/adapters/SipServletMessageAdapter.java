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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;

import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

/**
 * Common adapter class for SipServletMessage objects.
 * @param <T> SipServletRequest or SipServletResponse
 */
@SuppressWarnings("PMD.GodClass")
public abstract class SipServletMessageAdapter<T extends SipServletMessage> extends AdapterBase<T> implements
        SipServletMessage {

    protected SipServletMessageAdapter() {
        // only here for subclasses
    }

    protected SipServletMessageAdapter(T delegate) {
        super(delegate);
    }

    @Override
    public void addAcceptLanguage(Locale arg0) {
        delegate.addAcceptLanguage(arg0);
    }

    @Override
    public void addAddressHeader(String arg0, Address arg1, boolean arg2) {
        delegate.addAddressHeader(arg0, arg1, arg2);
    }

    @Override
    public void addHeader(String arg0, String arg1) {
        delegate.addHeader(arg0, arg1);
    }

    @Override
    public void addParameterableHeader(String arg0, Parameterable arg1, boolean arg2) {
        delegate.addParameterableHeader(arg0, arg1, arg2);
    }

    @Override
    public Locale getAcceptLanguage() {
        return delegate.getAcceptLanguage();
    }

    @Override
    public Iterator<Locale> getAcceptLanguages() {
        return delegate.getAcceptLanguages();
    }

    @Override
    public Address getAddressHeader(String arg0) throws ServletParseException {
        return delegate.getAddressHeader(arg0);
    }

    @Override
    public ListIterator<Address> getAddressHeaders(String arg0) throws ServletParseException {
        return delegate.getAddressHeaders(arg0);
    }

    @Override
    public SipApplicationSession getApplicationSession() {
        return SipApplicationSessionAdapter.getAdapter(delegate.getApplicationSession());
    }

    @Override
    public SipApplicationSession getApplicationSession(boolean arg0) {
        return SipApplicationSessionAdapter.getAdapter(delegate.getApplicationSession(arg0));
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
    public String getCharacterEncoding() {
        return delegate.getCharacterEncoding();
    }

    @Override
    public Object getContent() throws IOException {
        return delegate.getContent();
    }

    @Override
    public Locale getContentLanguage() {
        return delegate.getContentLanguage();
    }

    @Override
    public int getContentLength() {
        return delegate.getContentLength();
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public int getExpires() {
        return delegate.getExpires();
    }

    @Override
    public Address getFrom() {
        return delegate.getFrom();
    }

    @Override
    public String getHeader(String arg0) {
        return delegate.getHeader(arg0);
    }

    @Override
    public HeaderForm getHeaderForm() {
        return delegate.getHeaderForm();
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return delegate.getHeaderNames();
    }

    @Override
    public ListIterator<String> getHeaders(String arg0) {
        return delegate.getHeaders(arg0);
    }

    @Override
    public String getInitialRemoteAddr() {
        return delegate.getInitialRemoteAddr();
    }

    @Override
    public int getInitialRemotePort() {
        return delegate.getInitialRemotePort();
    }

    @Override
    public String getInitialTransport() {
        return delegate.getInitialTransport();
    }

    @Override
    public String getLocalAddr() {
        return delegate.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    @Override
    public String getMethod() {
        return delegate.getMethod();
    }

    @Override
    public Parameterable getParameterableHeader(String arg0) throws ServletParseException {
        return delegate.getParameterableHeader(arg0);
    }

    @Override
    public ListIterator<? extends Parameterable> getParameterableHeaders(String arg0) throws ServletParseException {
        return delegate.getParameterableHeaders(arg0);
    }

    @Override
    public String getProtocol() {
        return delegate.getProtocol();
    }

    @Override
    public byte[] getRawContent() throws IOException {
        return delegate.getRawContent();
    }

    @Override
    public String getRemoteAddr() {
        return delegate.getRemoteAddr();
    }

    @Override
    public int getRemotePort() {
        return delegate.getRemotePort();
    }

    @Override
    public String getRemoteUser() {
        return delegate.getRemoteUser();
    }

    @Override
    public SipSession getSession() {
        return SipSessionAdapter.getAdapter(delegate.getSession());
    }

    @Override
    public SipSession getSession(boolean arg0) {
        return SipSessionAdapter.getAdapter(delegate.getSession(arg0));
    }

    @Override
    public Address getTo() {
        return delegate.getTo();
    }

    @Override
    public String getTransport() {
        return delegate.getTransport();
    }

    @Override
    public Principal getUserPrincipal() {
        return delegate.getUserPrincipal();
    }

    @Override
    public boolean isCommitted() {
        return delegate.isCommitted();
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    @Override
    public boolean isUserInRole(String arg0) {
        return delegate.isUserInRole(arg0);
    }

    @Override
    public void removeAttribute(String arg0) {
        delegate.removeAttribute(arg0);
    }

    @Override
    public void removeHeader(String arg0) {
        delegate.removeHeader(arg0);
    }

    @Override
    public void setAcceptLanguage(Locale arg0) {
        delegate.setAcceptLanguage(arg0);
    }

    @Override
    public void setAddressHeader(String arg0, Address arg1) {
        delegate.setAddressHeader(arg0, arg1);
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
        delegate.setAttribute(arg0, arg1);
    }

    @Override
    public void setContentLanguage(Locale arg0) {
        delegate.setContentLanguage(arg0);
    }

    @Override
    public void setContentLength(int arg0) {
        delegate.setContentLength(arg0);
    }

    @Override
    public void setContentType(String arg0) {
        delegate.setContentType(arg0);
    }

    @Override
    public void setExpires(int arg0) {
        delegate.setExpires(arg0);
    }

    @Override
    public void setHeader(String arg0, String arg1) {
        delegate.setHeader(arg0, arg1);
    }

    @Override
    public void setHeaderForm(HeaderForm arg0) {
        delegate.setHeaderForm(arg0);
    }

    @Override
    public void setParameterableHeader(String arg0, Parameterable arg1) {
        delegate.setParameterableHeader(arg0, arg1);
    }

    @Override
    public void setContent(Object arg0, String arg1) throws UnsupportedEncodingException {
        delegate.setContent(arg0, arg1);
    }

    @SuppressWarnings("unchecked")
    public static <T extends SipServletMessage, A extends SipServletMessageAdapter<T>> A getAdapter(T msg) {
        if (msg instanceof SipServletRequest)
            return (A) SipServletRequestAdapter.getAdapter((SipServletRequest) msg);
        else if (msg instanceof SipServletResponse)
            return (A) SipServletResponseAdapter.getAdapter((SipServletResponse) msg);
        else
            return null;
    }

}
