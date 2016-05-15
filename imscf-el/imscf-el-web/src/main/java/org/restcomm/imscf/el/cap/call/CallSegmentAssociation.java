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
package org.restcomm.imscf.el.cap.call;

import org.restcomm.imscf.el.cap.call.CallSegment.CallSegmentState;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/** Represents a CSA for a call. */
public class CallSegmentAssociation implements CallSegmentListener {

    List<CallSegment> callSegments = new ArrayList<CallSegment>(2); // usually CS1 only, sometimes one more
    List<CallSegmentAssociationListener> listeners = new ArrayList<CallSegmentAssociationListener>(1);

    public CallSegmentAssociation() {
        // nothing to do
    }

    @Override
    public String toString() {
        return "CSA[" + callSegments + "]";
    }

    public int getCallSegmentCount() {
        return callSegments.size();
    }

    public List<CallSegment> getCallSegments() {
        return Collections.unmodifiableList(callSegments);
    }

    public CallSegment getOnlyCallSegment() {
        if (callSegments.size() == 1)
            return callSegments.get(0);
        throw new IllegalStateException("Multiple call segments present");
    }

    public CallSegment getCallSegment(int csID) {
        for (CallSegment cs : callSegments)
            if (cs.getId() == csID)
                return cs;
        return null;
    }

    public CallSegment getCallSegmentOfLeg(int legID) {
        for (CallSegment cs : callSegments)
            if (cs.containsLeg(legID))
                return cs;
        return null;
    }

    public int getLowestAvailableCSID() {
        // csID range is [1,127], which falls within the allowed index range of [0,127]
        BitSet used = new BitSet(128);
        for (CallSegment cs : callSegments)
            used.set(cs.getId());
        return used.nextClearBit(1); // start from 1, there is no CS-0
    }

    public int getLowestAvailableIcaLegID() {
        // legID range is [1,255], which falls within the allowed index range of [0,255]
        BitSet used = new BitSet(256);
        for (CallSegment cs : callSegments)
            cs.getLegs(used);
        // LegID > 2 shall always refer to a Called Party, more specifically a party in the call created as a result of
        // the InitiateCallAttempt operation, followed by the ContinueWithArgument operation.
        return used.nextClearBit(3);
    }

    /** Creates and returns the new call segment (CS-1) created as a result of an initialDP operation. */
    public CallSegment initialDP() {
        if (getCallSegment(1) != null)
            throw new IllegalStateException("Call segment 1 already present!");
        CallSegment cs1 = new CallSegment(1, 1);
        cs1.setListener(this);
        callSegments.add(cs1);

        // create new list, to avoid ConcurrentModificationException if a listener modifies the list during callback
        for (CallSegmentAssociationListener listener : new ArrayList<>(listeners)) {
            listener.callSegmentCreated(cs1);
        }

        cs1.setState(CallSegmentState.WAITING_FOR_INSTRUCTIONS);
        return cs1;
    }

    /** Creates and returns the new call segment created as a result of an initiateCallAttempt operation. */
    public CallSegment initiateCallAttempt(int legID, int csID) {
        if (getCallSegment(csID) != null)
            throw new IllegalStateException("Call segment " + csID + " already in use!");
        if (getCallSegmentOfLeg(legID) != null)
            throw new IllegalStateException("Leg ID " + legID + " already in use!");
        CallSegment cs = new CallSegment(csID, legID);
        cs.setListener(this);
        callSegments.add(cs);

        // create new list, to avoid ConcurrentModificationException if a listener modifies the list during callback
        for (CallSegmentAssociationListener listener : new ArrayList<>(listeners)) {
            listener.callSegmentCreated(cs);
        }

        cs.setState(CallSegmentState.WAITING_FOR_INSTRUCTIONS);
        return cs;
    }

    /** Moves the leg to CS1. Returns CS1. */
    public CallSegment moveLeg(int legID) {
        CallSegment from = getCallSegmentOfLeg(legID);
        if (from == null)
            throw new IllegalStateException("Leg " + legID + " not found in any call segment!");
        CallSegment to = getCallSegment(1);
        if (to == null)
            throw new IllegalStateException("Missing call segment 1!");

        from.moveLeg(legID, to);

        from.setState(CallSegmentState.IDLE); // terminates
        to.setState(CallSegmentState.WAITING_FOR_INSTRUCTIONS);
        return to;
    }

    /** Splits the leg to a new call segment. */
    public void splitLeg(int legID, int newCallSegmentID) {
        if (getCallSegment(newCallSegmentID) != null)
            throw new IllegalStateException("Call segment " + newCallSegmentID + " exists already!");
        CallSegment from = getCallSegmentOfLeg(legID);
        if (from == null)
            throw new IllegalStateException("No call segment that contains leg " + legID);

        CallSegment to = from.splitLeg(legID, newCallSegmentID);
        to.setListener(this);
        callSegments.add(to);

        // create new list, to avoid ConcurrentModificationException if a listener modifies the list during callback
        for (CallSegmentAssociationListener listener : new ArrayList<>(listeners)) {
            listener.callSegmentCreated(to);
        }

        from.setState(CallSegmentState.WAITING_FOR_INSTRUCTIONS);
        to.setState(CallSegmentState.WAITING_FOR_INSTRUCTIONS);
    }

    public void registerListener(CallSegmentAssociationListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void unregisterListener(CallSegmentAssociationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void callSegmentStateChanged(CallSegment cs) {
        // create new list, to avoid ConcurrentModificationException if a listener modifies the list during callback
        for (CallSegmentAssociationListener listener : new ArrayList<>(listeners)) {
            listener.callSegmentStateChanged(cs);
        }
        if (cs.getState() == CallSegmentState.IDLE) {
            callSegments.remove(cs);
            // create new list, to avoid ConcurrentModificationException if a listener modifies the list during callback
            for (CallSegmentAssociationListener listener : new ArrayList<>(listeners)) {
                listener.callSegmentDestroyed(cs);
            }
        }
    }

    public static void main(String[] args) {
        CallSegmentAssociation csa = new CallSegmentAssociation();

        CallSegmentAssociationListener listener = new CallSegmentAssociationListener() {
            @Override
            public void callSegmentCreated(CallSegment cs) {
                System.out.println("Created: " + cs + "\n\tAll: " + csa.getCallSegments());
            }

            @Override
            public void callSegmentDestroyed(CallSegment cs) {
                System.out.println("Destroyed: " + cs + "\n\tAll: " + csa.getCallSegments());
            }

            @Override
            public void callSegmentStateChanged(CallSegment cs) {
                System.out.println("State changed: " + cs);
            }
        };

        csa.registerListener(listener);

        System.out.println("\n:::IDP + connect");
        csa.initialDP().connect();
        System.out.println("\n:::ERBCSM leg 1 + continue");
        csa.getCallSegmentOfLeg(2).eventReportInterrupted().continueCS();
        System.out.println("\n:::ERBCSM leg 2 (disconnect) + CTR");
        csa.getCallSegmentOfLeg(1).eventReportInterrupted().disconnectLeg(2).connectToResource();
        System.out.println("\n:::DFC + continue");
        csa.getCallSegmentOfLeg(1).disconnectForwardConnection().continueCS();
        System.out.println("\n:::ICA(leg 3, cs 2)  + continue");
        csa.initiateCallAttempt(3, 2).continueCS();
        System.out.println("\n:::ERBCSM(leg 3)");
        csa.getCallSegmentOfLeg(3).eventReportInterrupted();
        System.out.println("\n:::moveleg(3) + continue");
        csa.moveLeg(3).continueCS();
        System.out.println("\n:::splitleg(leg 1 -> cs 2)");
        csa.splitLeg(1, 2);
        System.out.println("\n:::continue (cs(leg 1))");
        csa.getCallSegmentOfLeg(1).continueCS();
        System.out.println("\n:::moveleg(1) + continue");
        csa.moveLeg(1).continueCS();

    }
}
