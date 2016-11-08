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
package org.restcomm.imscf.common ;

import java.util.Optional;

import org.mobicents.protocols.ss7.sccp.message.SccpDataMessage;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;

/** SCCP dialog identifier. */
public final class SccpDialogId {
    private String remoteGT;
    private int remoteSSN;
    // String localGT; // no need, it's always the same
    private int localSSN;

    public SccpDialogId(String remoteGT, int remoteSSN, int localSSN) {
        this.remoteGT = remoteGT;
        this.remoteSSN = remoteSSN;
        this.localSSN = localSSN;
    }

    public static SccpDialogId extractFromSccpMessage(SccpDataMessage msg, boolean incoming) {
        String remoteGT;
        int remoteSSN, localSSN;
        if (incoming) {
            // remoteGT may be null in some cases, e.g. ATI request
            remoteGT = Optional.ofNullable(msg.getCallingPartyAddress().getGlobalTitle()).map(GlobalTitle::getDigits)
                    .orElse(null);
            remoteSSN = msg.getCallingPartyAddress().getSubsystemNumber();
            localSSN = msg.getCalledPartyAddress().getSubsystemNumber();
        } else {
            // outgoing
            // remoteGT may be null in some cases, e.g. ATI request
            remoteGT = Optional.ofNullable(msg.getCalledPartyAddress().getGlobalTitle()).map(GlobalTitle::getDigits)
                    .orElse(null);
            remoteSSN = msg.getCalledPartyAddress().getSubsystemNumber();
            localSSN = msg.getCallingPartyAddress().getSubsystemNumber();
        }
        return new SccpDialogId(remoteGT, remoteSSN, localSSN);
    }

    public boolean isRemoteGtPresent() {
        return remoteGT != null;
    }

    /** Returns a clone with the remoteGT ignored and everything else kept. */
    public SccpDialogId noRemoteGT() {
        return new SccpDialogId(null, remoteSSN, localSSN);
    }

    /** Returns a clone with only local SSN/GT kept. */
    public SccpDialogId localSideOnly() {
        return new SccpDialogId(null, -1, localSSN);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + localSSN;
        result = prime * result + ((remoteGT == null) ? 0 : remoteGT.hashCode());
        result = prime * result + remoteSSN;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SccpDialogId other = (SccpDialogId) obj;
        if (localSSN != other.localSSN)
            return false;
        if (remoteGT == null) {
            if (other.remoteGT != null)
                return false;
        } else if (!remoteGT.equals(other.remoteGT))
            return false;
        if (remoteSSN != other.remoteSSN)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[rGT:" + remoteGT + ", rSSN:" + remoteSSN + ", lSSN:" + localSSN + "]";
    }
}
