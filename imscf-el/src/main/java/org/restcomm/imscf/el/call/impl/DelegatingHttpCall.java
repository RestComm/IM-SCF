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

import org.restcomm.imscf.el.http.HttpCall;

/** Default delegating interface. */
public interface DelegatingHttpCall extends DelegatingIMSCFCall, HttpCall {

    @Override
    default HttpCall getDelegate() {
        return (HttpCall) DelegatingIMSCFCall.super.getDelegate();
    }

    @Override
    default void setInInstanceIndex(int inInstanceIndex) {
        getDelegate().setInInstanceIndex(inInstanceIndex);
    }

    @Override
    default int getInInstanceIndex() {
        return getDelegate().getInInstanceIndex();
    }

    @Override
    default void setjSessionId(String jSessionId) {
        getDelegate().setjSessionId(jSessionId);
    }

    @Override
    default String getjSessionId() {
        return getDelegate().getjSessionId();
    }

    @Override
    default void setTechnicalError(int technicalError) {
        getDelegate().setTechnicalError(technicalError);
    }

    @Override
    default int getTechnicalError() {
        return getDelegate().getTechnicalError();
    }

    @Override
    default boolean wasTechnicalError() {
        return getDelegate().wasTechnicalError();
    }
}
