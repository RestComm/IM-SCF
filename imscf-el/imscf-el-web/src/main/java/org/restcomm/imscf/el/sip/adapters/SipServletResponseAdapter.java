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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter class for SipServletResponse objects.
 */
@SuppressWarnings("serial")
public final class SipServletResponseAdapter extends SipServletMessageAdapter<SipServletResponse> implements
        SipServletResponse, Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(SipServletResponseAdapter.class);

    private SipServletResponseAdapter(SipServletResponse delegate) {
        super(delegate);
    }

    @Override
    public SipServletRequest createAck() {
        return SipServletRequestAdapter.getAdapter(delegate.createAck());
    }

    @Override
    public SipServletRequest createPrack() throws Rel100Exception {
        return SipServletRequestAdapter.getAdapter(delegate.createPrack());
    }

    @Override
    public void flushBuffer() throws IOException {
        delegate.flushBuffer();
    }

    @Override
    public int getBufferSize() {
        return delegate.getBufferSize();
    }

    @Override
    public Iterator<String> getChallengeRealms() {
        return delegate.getChallengeRealms();
    }

    @Override
    public Locale getLocale() {
        return delegate.getLocale();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    @Override
    public Proxy getProxy() {
        return ProxyAdapter.getAdapter(delegate.getProxy());
    }

    @Override
    public ProxyBranch getProxyBranch() {
        return ProxyBranchAdapter.getAdapter(delegate.getProxyBranch());
    }

    @Override
    public String getReasonPhrase() {
        return delegate.getReasonPhrase();
    }

    @Override
    public SipServletRequest getRequest() {
        return SipServletRequestAdapter.getAdapter(delegate.getRequest());
    }

    @Override
    public int getStatus() {
        return delegate.getStatus();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return delegate.getWriter();
    }

    @Override
    public boolean isBranchResponse() {
        return delegate.isBranchResponse();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public void resetBuffer() {
        delegate.resetBuffer();
    }

    @Override
    public void send() throws IOException {
        delegate.send();
        LOG.debug("Sent response:\n{}", delegate);
        AppSessionHelper.renewAppSessionTimeout(getApplicationSession());
        ElEventCreator.addOutgoingSipEvent(getApplicationSession().getId(), this);
    }

    @Override
    public void sendReliably() throws Rel100Exception {
        delegate.sendReliably();
        LOG.debug("Sent response:\n{}", delegate);
        AppSessionHelper.renewAppSessionTimeout(getApplicationSession());
        ElEventCreator.addOutgoingSipEvent(getApplicationSession().getId(), this);
    }

    @Override
    public void setBufferSize(int arg0) {
        delegate.setBufferSize(arg0);
    }

    @Override
    public void setCharacterEncoding(String arg0) {
        delegate.setCharacterEncoding(arg0);
    }

    @Override
    public void setLocale(Locale arg0) {
        delegate.setLocale(arg0);
    }

    @Override
    public void setStatus(int arg0, String arg1) {
        delegate.setStatus(arg0, arg1);
    }

    @Override
    public void setStatus(int arg0) {
        delegate.setStatus(arg0);
    }

    @Override
    public void setContentLengthLong(long len) {
        delegate.setContentLengthLong(len);
    }

    public static SipServletResponseAdapter getAdapter(SipServletResponse response) {
        if (response == null)
            return null;
        if (response instanceof SipServletResponseAdapter)
            return (SipServletResponseAdapter) response;
        else
            return new SipServletResponseAdapter(response);
    }
}
