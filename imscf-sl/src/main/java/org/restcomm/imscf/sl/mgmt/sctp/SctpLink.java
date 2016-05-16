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
package org.restcomm.imscf.sl.mgmt.sctp;


import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.Management;

/**
 * Manageable SCTP link.
 *
 */
public class SctpLink extends SctpLinkStatus {

    protected final Management management;
    protected Association association;

    public SctpLink(Management management, Association association) {
        super(association);
        this.management = management;
        this.association = association;
    }

    public void start() throws Exception {
        management.startAssociation(association.getName());
    }

    public void stop() throws Exception {
        management.stopAssociation(association.getName());
    }

    public Association getAssociation() {
        return this.association;
    }

    public Management getManagement() {
        return this.management;
    }

    public void updateAssociation(Association association) {
        if (association != null  && association != this.association) {
            super.updateAssociation(association);
            this.association = association;
        }
    }

    @Override
    public String toString() {
        return "SctpLink [management=" + management + ", association=" + association + "SctpLinkStatus="
                + super.toString() + "]";
    }
}
