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

import org.restcomm.imscf.common .util.TCAPMessageInfo;

/** TCAP dialog identifier. */
public final class TcapDialogId {
    private final Long remoteTcapTID;
    private final Long localTcapTID;

    public TcapDialogId(Long remoteTcapTID, Long localTcapTID) {
        this.remoteTcapTID = remoteTcapTID;
        this.localTcapTID = localTcapTID;
    }

    public static TcapDialogId extractFromTCAPMessageInfo(TCAPMessageInfo info, boolean incoming) {
        Long remoteTcapTID = incoming ? info.getOtid() : info.getDtid();
        Long localTcapTID = incoming ? info.getDtid() : info.getOtid();
        return new TcapDialogId(remoteTcapTID, localTcapTID);
    }

    public boolean isRemoteTIDSet() {
        return remoteTcapTID != null;
    }

    public boolean isLocalTIDSet() {
        return localTcapTID != null;
    }

    public Long getLocalTcapTID() {
        return localTcapTID;
    }

    public Long getRemoteTcapTID() {
        return remoteTcapTID;
    }

    /** Merges set values from the two IDs into a combined ID. The value in <code>one</code> take precedence for values set in both. */
    public static TcapDialogId merge(TcapDialogId one, TcapDialogId other) {
        if (!other.isLocalTIDSet() && !other.isRemoteTIDSet())
            return one;
        Long local = one.isLocalTIDSet() ? one.localTcapTID : other.localTcapTID;
        Long remote = one.isRemoteTIDSet() ? one.remoteTcapTID : other.remoteTcapTID;
        return new TcapDialogId(remote, local);
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("hashCode() must not be used on " + getClass().getSimpleName());
    }

    /**
     * Relaxed equals that allows unset values. Two dialogs are equal if all set values match.
     * Rationale: a TC_ABORT or TC_END message only has a DTID, while a TC_BEGIN only has OTID. This
     * equals implementation allows for example matching a TC_CONTINUE with a set OTID (remote side)
     * and DTID (local side) to match a previous TC_BEGIN with only a set OTID (local side).
     * <p>
     * This implementation is <i>reflexive, symmetric</i> and <i>consistent</i>, but not <i>transitive</i>.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TcapDialogId other = (TcapDialogId) obj;
        if (localTcapTID != null && other.localTcapTID != null && !localTcapTID.equals(other.localTcapTID))
            return false;
        if (remoteTcapTID != null && other.remoteTcapTID != null && !remoteTcapTID.equals(other.remoteTcapTID))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[rTID:" + (remoteTcapTID != null ? "0x" + Long.toHexString(remoteTcapTID) : "N/A") + ", lTID:"
                + (localTcapTID != null ? "0x" + Long.toHexString(localTcapTID) : "N/A") + "]";
    }
}
