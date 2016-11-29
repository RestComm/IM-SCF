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
package org.restcomm.imscf.el.cap.call;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/** Represents a call segment. */
public final class CallSegment {

    /** The network ID of this call segment. */
    private int id;

    /** The state of this call segment. */
    private CallSegmentState state = CallSegmentState.IDLE;

    /** LegIDs in this call segment. A bit is set to true if the leg with that id is present in this call segment. */
    private BitSet legs = new BitSet();

    private CallSegmentListener listener;

    @Override
    public String toString() {
        return getName() + "[" + state + ", legs: " + getLegs() + "]";
    }

    /** Creates a new call segment for the leg. */
    CallSegment(int id, int firstLegId) {
        this.id = id;
        legs.set(firstLegId);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return "CS-" + getId();
    }

    public CallSegmentState getState() {
        return state;
    }

    void setState(CallSegmentState newState) {
        this.state = newState;
        if (listener != null) {
            listener.callSegmentStateChanged(this);
        }
    }

    void setListener(CallSegmentListener listener) {
        this.listener = listener;
    }

    public Set<Integer> getLegs() {
        Set<Integer> set = new HashSet<Integer>();

        for (int i = 0; (i = legs.nextSetBit(i)) != -1; i++) { // CHECKSTYLE:OFF
            set.add(Integer.valueOf(i));
        }
        return set;
    }

    public int getLegCount() {
        return legs.cardinality();
    }

    /** Sets the bits in the target BitSet to true for each leg present in this CallSegment,
     *  while other bits are left unchanged. If the parameter is null, a new BitSet is returned.
     */
    public BitSet getLegs(BitSet legs) {
        if (legs == null)
            return (BitSet) this.legs.clone();

        legs.or(this.legs);
        return legs;
    }

    public boolean containsLeg(int legID) {
        return legs.get(legID);
    }

    CallSegment splitLeg(int legID, int newCallSegmentId) {
        if (!legs.get(legID))
            throw new IllegalStateException("Leg " + legID + " is not present in this call segment!");
        legs.clear(legID);
        return new CallSegment(newCallSegmentId, legID);
    }

    CallSegment moveLeg(int legID, CallSegment toCallSegment) {
        if (!legs.get(legID))
            throw new IllegalStateException("Leg " + legID + " is not present in this call segment!");
        else if (toCallSegment.legs.get(legID))
            throw new IllegalStateException("Leg " + legID + " is already present in the target call segment!");

        legs.clear(legID);
        toCallSegment.legs.set(legID);
        return this;
    }

    public CallSegment eventReportInterrupted() {
        setState(CallSegmentState.WAITING_FOR_INSTRUCTIONS);
        return this;
    }

    public CallSegment connectToResource() {
        setState(CallSegmentState.WAITING_FOR_END_OF_USER_INTERACTION);
        return this;
    }

    public CallSegment disconnectForwardConnection() {
        setState(CallSegmentState.WAITING_FOR_INSTRUCTIONS);
        return this;
    }

    public CallSegment connect() {
        return connectLeg(2);
    }

    public CallSegment connectLeg(int legID) {
        if (legs.get(legID))
            throw new IllegalStateException("Leg " + legID + " is already present in " + this);
        legs.set(legID);
        setState(CallSegmentState.MONITORING);
        return this;
    }

    public CallSegment disconnectLeg(int legID) {
        if (!legs.get(legID))
            throw new IllegalStateException("Leg " + legID + " is not present in " + this);
        if (state == CallSegmentState.WAITING_FOR_END_OF_USER_INTERACTION)
            throw new IllegalStateException("disconnectLeg cannot be sent in " + this
                    + " while user interaction is in progress");
        legs.clear(legID);
        if (legs.isEmpty())
            setState(CallSegmentState.IDLE);
        else
            setState(CallSegmentState.WAITING_FOR_INSTRUCTIONS);
        return this;
    }

    public CallSegment continueCS() {
        setState(CallSegmentState.MONITORING);
        return this;
    }

    public CallSegment release() {
        setState(CallSegmentState.IDLE);
        return this;
    }

    /**
     * gsmSSF state list for a single call segment.
     */
    public static enum CallSegmentState {
        /** Ongoing call, activityTest requests may be sent.*/
        MONITORING,
        /** CallSegment suspended for call processing, resetTimer may be sent.*/
        WAITING_FOR_INSTRUCTIONS,
        /** CallSegment connected to MRF, resetTimer(UI) may be sent. */
        WAITING_FOR_END_OF_USER_INTERACTION,
        /** CallSegment has no armed EDPs or has finished, it is ready to be removed. */
        IDLE;
        // state WAITING_FOR_END_OF_TEMPORARY_CONNECTION is not used
    }

}
