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
package org.restcomm.imscf.el.call.impl;

import org.restcomm.imscf.el.tcap.call.TCAPCall;

/** Default delegating interface. */
interface DelegatingTcapCall extends DelegatingIMSCFCall, TCAPCall {

    @Override
    default TCAPCall getDelegate() {
        return (TCAPCall) DelegatingIMSCFCall.super.getDelegate();
    }

    @Override
    default Long getLocalTcapTrId() {
        return getDelegate().getLocalTcapTrId();
    }

    @Override
    default Long getRemoteTcapTrId() {
        return getDelegate().getRemoteTcapTrId();
    }

    @Override
    default void setRemoteTcapTrId(Long remoteTcapTrId) {
        getDelegate().setRemoteTcapTrId(remoteTcapTrId);
    }

    @Override
    default void setLocalTcapTrId(Long localTcapTrId) {
        getDelegate().setLocalTcapTrId(localTcapTrId);
    }

}
