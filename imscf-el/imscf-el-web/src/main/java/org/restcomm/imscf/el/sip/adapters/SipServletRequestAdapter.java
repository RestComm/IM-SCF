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

import org.restcomm.imscf.el.call.history.ElEventCreator;
import org.restcomm.imscf.el.sip.servlets.AppSessionHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter class for SipServletRequest objects.
 *
 */
@SuppressWarnings({ "serial", "PMD.GodClass" })
public final class SipServletRequestAdapter extends SipServletMessageAdapter<SipServletRequest> implements
        SipServletRequest, Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(SipServletRequestAdapter.class);

    private SipServletRequestAdapter(SipServletRequest delegate) {
        super(delegate);
    }

    @Override
    public void addAuthHeader(SipServletResponse arg0, AuthInfo arg1) {
        SipServletResponse arg0A = arg0 instanceof SipServletResponseAdapter ? ((SipServletResponseAdapter) arg0)
                .getDelegate() : arg0;
        this.delegate.addAuthHeader(arg0A, arg1);
    }

    @Override
    public void addAuthHeader(SipServletResponse arg0, String arg1, String arg2) {
        SipServletResponse arg0A = arg0 instanceof SipServletResponseAdapter ? ((SipServletResponseAdapter) arg0)
                .getDelegate() : arg0;
        this.delegate.addAuthHeader(arg0A, arg1, arg2);
    }

    @Override
    public SipServletRequest createCancel() {
        return getAdapter(delegate.createCancel());
    }

    @Override
    public SipServletResponse createResponse(int arg0) {
        return SipServletResponseAdapter.getAdapter(delegate.createResponse(arg0));
    }

    @Override
    public SipServletResponse createResponse(int arg0, String arg1) {
        return SipServletResponseAdapter.getAdapter(delegate.createResponse(arg0, arg1));
    }

    @Override
    public B2buaHelper getB2buaHelper() {
        return B2buaHelperAdapter.getAdapter(delegate.getB2buaHelper());
    }

    @Override
    public Address getInitialPoppedRoute() {
        return delegate.getInitialPoppedRoute();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public Locale getLocale() {
        return delegate.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return delegate.getLocales();
    }

    @Override
    public String getLocalName() {
        return delegate.getLocalName();
    }

    @Override
    public int getMaxForwards() {
        return delegate.getMaxForwards();
    }

    @Override
    public String getParameter(String arg0) {
        return delegate.getParameter(arg0);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return delegate.getParameterMap();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return delegate.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String arg0) {
        return delegate.getParameterValues(arg0);
    }

    @Override
    public Address getPoppedRoute() {
        return delegate.getPoppedRoute();
    }

    @Override
    public Proxy getProxy() throws TooManyHopsException {
        return ProxyAdapter.getAdapter(delegate.getProxy());
    }

    @Override
    public Proxy getProxy(boolean arg0) throws TooManyHopsException {
        return ProxyAdapter.getAdapter(delegate.getProxy(arg0));
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return delegate.getReader();
    }

    @Override
    @Deprecated
    public String getRealPath(String arg0) {
        return delegate.getRealPath(arg0);
    }

    @Override
    public SipApplicationRoutingRegion getRegion() {
        return delegate.getRegion();
    }

    @Override
    public String getRemoteHost() {
        return delegate.getRemoteHost();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        return delegate.getRequestDispatcher(arg0);
    }

    @Override
    public URI getRequestURI() {
        return delegate.getRequestURI();
    }

    @Override
    public SipApplicationRoutingDirective getRoutingDirective() {
        return delegate.getRoutingDirective();
    }

    @Override
    public String getScheme() {
        return delegate.getScheme();
    }

    @Override
    public String getServerName() {
        return delegate.getServerName();
    }

    @Override
    public int getServerPort() {
        return delegate.getServerPort();
    }

    @Override
    public URI getSubscriberURI() {
        return delegate.getSubscriberURI();
    }

    @Override
    public boolean isInitial() {
        return delegate.isInitial();
    }

    @Override
    public void pushPath(Address arg0) {
        delegate.pushPath(arg0);
    }

    @Override
    public void pushRoute(Address arg0) {
        delegate.pushRoute(arg0);
    }

    @Override
    public void pushRoute(SipURI arg0) {
        delegate.pushRoute(arg0);
    }

    @Override
    public void send() throws IOException {
        delegate.send();
        LOG.debug("Sent request:\n{}", delegate);
        AppSessionHelper.renewAppSessionTimeout(getApplicationSession());
        ElEventCreator.addOutgoingSipEvent(getApplicationSession().getId(), this);
    }

    @Override
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
        delegate.setCharacterEncoding(arg0);
    }

    @Override
    public void setMaxForwards(int arg0) {
        delegate.setMaxForwards(arg0);
    }

    @Override
    public void setRequestURI(URI arg0) {
        delegate.setRequestURI(arg0);
    }

    @Override
    public void setRoutingDirective(SipApplicationRoutingDirective arg0, SipServletRequest arg1) {
        SipServletRequest arg1A = arg1 instanceof SipServletRequestAdapter ? ((SipServletRequestAdapter) arg1)
                .getDelegate() : arg1;
        delegate.setRoutingDirective(arg0, arg1A);
    }

    @Override
    public long getContentLengthLong() {
        return delegate.getContentLengthLong();
    }

    @Override
    public ServletContext getServletContext() {
        // return ServletContextAdapter.getAdapter(delegate.getServletContext());
        return delegate.getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return delegate.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        ServletRequest servletRequestA = servletRequest instanceof SipServletRequestAdapter ? ((SipServletRequestAdapter) servletRequest)
                .getDelegate() : servletRequest;
        ServletResponse servletResponseA = servletResponse instanceof SipServletResponseAdapter ? ((SipServletResponseAdapter) servletResponse)
                .getDelegate() : servletResponse;
        return delegate.startAsync(servletRequestA, servletResponseA);
    }

    @Override
    public boolean isAsyncStarted() {
        return delegate.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return delegate.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return delegate.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return delegate.getDispatcherType();
    }

    public static SipServletRequestAdapter getAdapter(SipServletRequest request) {
        if (request == null)
            return null;
        if (request instanceof SipServletRequestAdapter)
            return (SipServletRequestAdapter) request;
        else
            return new SipServletRequestAdapter(request);
    }
}
