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

/** Base class for adapters with a delegated equals method.
 * @param <K> the delegate type .
 */
public class AdapterBase<K> {

    protected K delegate;

    // only here to suppress serializable warning. Should never be called.
    protected AdapterBase() {
        throw new UnsupportedOperationException();
    }

    // protected as this base class should not be instantiated directly
    protected AdapterBase(K delegate) {
        this.delegate = delegate;
    }

    public final K getDelegate() {
        return delegate;
    }

    public final void setDelegate(K k) {
        delegate = k;
    }

    @Override
    public final String toString() {
        return delegate.toString();
    }

    @Override
    public final int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        // compare the two delegates directly if obj is an adapter as well
        Object other = (obj instanceof AdapterBase) ? ((AdapterBase<?>) obj).delegate : obj;
        return delegate.equals(other);
    }
}
