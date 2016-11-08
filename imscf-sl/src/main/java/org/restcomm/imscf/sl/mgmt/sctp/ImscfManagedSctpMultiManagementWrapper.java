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
package org.restcomm.imscf.sl.mgmt.sctp;


import java.io.IOException;

import org.mobicents.protocols.sctp.multiclient.MultiManagementImpl;

/**
 * MultiManagamentImpl wrapper which implements the ImscfManagedSctpManagementWrapper abstract class.
 * Its main feature is to register the MultiManagementImpl object to the IMSCF LinkManager object in its constructor.
 * Useful when it is dynamically instantiated by 3rd party components like jdiameter stack.
 * NOTE: it could be as simple as a super class of MultiManagementImpl but wrapper pattern seemed to
 * be more future proof.
 *
 */
public class ImscfManagedSctpMultiManagementWrapper extends ImscfManagedSctpManagementWrapper {

    public ImscfManagedSctpMultiManagementWrapper(MultiManagementImpl delegatedManagement) {
        super(delegatedManagement);
    }

    public ImscfManagedSctpMultiManagementWrapper(String sctpStackName) throws IOException {
        this(new MultiManagementImpl(sctpStackName));
        SctpLinkManager.getInstance().registerManagement(this);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        if (iface == null) {
            return false;
        }
        if (iface == MultiManagementImpl.class) {
            return true;
        }
        if (MultiManagementImpl.class.isAssignableFrom(iface)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) {
        if (iface == null) {
            return null;
        }
        if (iface == MultiManagementImpl.class) {
            return (T) this.delegatedManagement;
        }
        if (MultiManagementImpl.class.isAssignableFrom(iface)) {
            return (T) this.delegatedManagement;
        }
        return null;
    }

}
