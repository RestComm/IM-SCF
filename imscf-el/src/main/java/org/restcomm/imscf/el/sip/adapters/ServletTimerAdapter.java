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

import java.io.Serializable;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;

/**
 * Adapter class for ServletTimer objects.
 * @author Miklos Pocsaji
 *
 */
public final class ServletTimerAdapter extends AdapterBase<ServletTimer> implements ServletTimer {

    private ServletTimerAdapter(ServletTimer delegate) {
        super(delegate);
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }

    @Override
    public SipApplicationSession getApplicationSession() {
        return SipApplicationSessionAdapter.getAdapter(delegate.getApplicationSession());
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public Serializable getInfo() {
        return delegate.getInfo();
    }

    @Override
    public long getTimeRemaining() {
        return delegate.getTimeRemaining();
    }

    @Override
    public long scheduledExecutionTime() {
        return delegate.scheduledExecutionTime();
    }

    public static ServletTimerAdapter getAdapter(ServletTimer st) {
        if (st == null) {
            return null;
        }

        if (st instanceof ServletTimerAdapter)
            return (ServletTimerAdapter) st;
        else
            return new ServletTimerAdapter(st);
    }
}
