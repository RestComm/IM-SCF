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

import org.mobicents.protocols.ss7.map.api.MAPDialog;

import org.restcomm.imscf.el.map.MAPModule;
import org.restcomm.imscf.el.map.call.AtiRequest;
import org.restcomm.imscf.el.map.call.MAPCall;

/**
 * Delegate interface for MAPCall.
 * @author Miklos Pocsaji
 *
 */
public interface DelegatingMapCall extends MAPCall, DelegatingTcapCall {

    @Override
    default void setMAPDialog(MAPDialog dialog) {
        ((MAPCall) getDelegate()).setMAPDialog(dialog);
    }

    @Override
    default MAPDialog getMAPDialog() {
        return ((MAPCall) getDelegate()).getMAPDialog();
    }

    @Override
    default void setMapModule(MAPModule mapModule) {
        ((MAPCall) getDelegate()).setMapModule(mapModule);
    }

    @Override
    default MAPModule getMapModule() {
        return ((MAPCall) getDelegate()).getMapModule();
    }

    @Override
    default MapMethod getMapMethod() {
        return ((MAPCall) getDelegate()).getMapMethod();
    }

    @Override
    default void setMapMethod(MapMethod mapMethod) {
        ((MAPCall) getDelegate()).setMapMethod(mapMethod);
    }

    @Override
    default AtiRequest getAtiRequest() {
        return ((MAPCall) getDelegate()).getAtiRequest();
    }

    @Override
    default void setAtiRequest(AtiRequest atiRequest) {
        ((MAPCall) getDelegate()).setAtiRequest(atiRequest);
    }

    @Override
    default String getServiceIdentifier() {
        return getDelegate().getServiceIdentifier();
    }
}
