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
package org.restcomm.imscf.el.call.impl;

import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.call.CapSipSmsCall;
import org.restcomm.imscf.el.diameter.call.DiameterHttpCall;
import org.restcomm.imscf.el.map.call.MAPSIPCall;
import org.restcomm.imscf.el.stack.CallContext;

/** Delegate for handling Call as a resource. */
public abstract class CallResourceAdapter implements DelegatingIMSCFCall {

    IMSCFCall delegate;

    protected CallResourceAdapter(IMSCFCall delegate) {
        this.delegate = delegate;
        // first lock is always the appsession lock, no need for TCAP locking

        ((IMSCFCallBase) delegate).lock();
        ((IMSCFCallBase) delegate).populateMDC();

        CallContext.put(CallContext.IMSCFCALLID, delegate.getImscfCallId());
    }

    @SuppressWarnings("unchecked")
    public static final <T extends IMSCFCall> T wrap(T call) {
        if (call == null)
            return null;
        else if (call instanceof CallResourceAdapter)
            return call;
        else if (call instanceof CapSipCsCall)
            return (T) new CallResourceAdapterCapSipCsCall((CapSipCsCall) call);
        else if (call instanceof CapSipSmsCall)
            return (T) new CallResourceAdapterCapSipSmsCall((CapSipSmsCall) call);
        else if (call instanceof MAPSIPCall)
            return (T) new CallResourceAdapterMapSipCall((MAPSIPCall) call);
        else if (call instanceof DiameterHttpCall)
            return (T) new CallResourceAdapterDiameterHttpCall((DiameterHttpCall) call);
        else
            throw new IllegalArgumentException();
    }

    @SuppressWarnings("unchecked")
    public static final <T extends IMSCFCall> T unwrap(T call) {
        if (call == null)
            return null;
        else if (call instanceof CallResourceAdapter)
            return (T) ((CallResourceAdapter) call).getDelegate();
        else
            return call;
    }

    @Override
    public IMSCFCall getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public final void close() {
        boolean lastHolder = ((IMSCFCallBase) delegate).unlock();

        if (lastHolder) {
            delegate.close();
            CallContext.remove(CallContext.IMSCFCALLID);
        }

    }

}

/** Subclass for CS call. */
class CallResourceAdapterCapSipCsCall extends CallResourceAdapter implements DelegatingCapSipCsCall {
    protected CallResourceAdapterCapSipCsCall(CapSipCsCall delegate) {
        super(delegate);
    }

    @Override
    public CapSipCsCall getDelegate() {
        return (CapSipCsCall) super.getDelegate();
    }
}

/** Subclass for SMS call. */
class CallResourceAdapterCapSipSmsCall extends CallResourceAdapter implements DelegatingCapSipSmsCall {
    protected CallResourceAdapterCapSipSmsCall(CapSipSmsCall delegate) {
        super(delegate);
    }

    @Override
    public CapSipSmsCall getDelegate() {
        return (CapSipSmsCall) super.getDelegate();
    }
}

/** Delegate implementation for MAP call. */
class CallResourceAdapterMapSipCall extends CallResourceAdapter implements DelegatingMapSipCall {
    protected CallResourceAdapterMapSipCall(MAPSIPCall delegate) {
        super(delegate);
    }

    @Override
    public MAPSIPCall getDelegate() {
        return (MAPSIPCall) super.getDelegate();
    }
}

/** Delegate implementation for Diameter Http call. */
class CallResourceAdapterDiameterHttpCall extends CallResourceAdapter implements DelegatingDiameterHttpCall {
    protected CallResourceAdapterDiameterHttpCall(DiameterHttpCall delegate) {
        super(delegate);
    }

    @Override
    public DiameterHttpCall getDelegate() {
        return (DiameterHttpCall) super.getDelegate();
    }
}
