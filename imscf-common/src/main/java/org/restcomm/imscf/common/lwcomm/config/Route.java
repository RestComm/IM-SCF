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
package org.restcomm.imscf.common.lwcomm.config;

import java.util.List;
import java.util.Set;

/**
 * Represents a message route.
 * A route has:
 * <li>An unique(!) name</li>
 * <li>Possible sources</li>
 * <li>An ordered list of destinations</li>
 * <li>A mode: has sense only if there are more that one destinations</li>
 * <li>A message retransmit pattern</li>
 * <li>A default target Queue for the messages</li>
 *
 * The list of sources are the nodes from this route can be used.
 * The destinations are the target nodes of the messages to be sent.
 * The mode tells how the destination node should be chosen from the destination list:
 * <li>LOADBALANCE - The destination node will be chosen randomly from the available destinations</li>
 * <li>FAILOVER - The message will be sent to the first available destination</li>
 * <li>MULTICAST - The message will be sent to all available destinations</li>
 * If there is only one destination defined, the value of mode is invariant.
 * The retransmit pattern tells how the UDP messages to a single destination should be retransmitted
 * when no acknowledgement is received. The pattern is a comma-separated list of integers. The integers represent
 * timeouts and mean the elapsed time in milliseconds when a retransmit should be sent after the first emission
 * of the message. The last element of the pattern denotes a time instant when the node is considered down, so there will
 * be no more retransmits to this node, if there is a next node in the target list, it is probed.
 * E.g. the retransmit pattern "200,500,1000" means that if there is no ACK received, then a
 * retransmitted message will be sent after 200ms and 500ms after the first emission and if there is no
 * answer at 1 second after the first message the node will be considered as down. The pattern
 * cannot be empty, there must be at least one timeout defined (in this case there will be no retransmissions).
 * <b>Important: Route objects are equal if their name is equal!</b>
 * @author Miklos Pocsaji
 *
 */
public abstract class Route {
    /**
     * Deliver mode of the route.
     * @See Route
     * @author Miklos Pocsaji
     */
    public enum Mode {
        LOADBALANCE, FAILOVER, MULTICAST
    }

    public abstract String getName();

    public abstract Set<Node> getPossibleSources();

    public abstract List<Node> getDestinations();

    public abstract Mode getMode();

    public abstract List<Integer> getRetransmitPattern();

    public abstract String getDefaultQueue();

    //
    // java.lang.Object overrides
    //

    @Override
    public String toString() {
        return "Route [getName()=" + getName() + ", getPossibleSources()=" + getPossibleSources()
                + ", getDestinations()=" + getDestinations() + ", getMode()=" + getMode() + ", getRetransmitPattern()="
                + getRetransmitPattern() + ", getDefaultQueue()=" + getDefaultQueue() + "]";
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Route other = (Route) obj;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!getName().equals(other.getName()))
            return false;
        return true;
    }

}
