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
package org.restcomm.imscf.el.sip.failover;

/**
 * Simple load balancer implementation that uses a round-robin strategy, i.e. indices are returned sequentially
 * and wrapped at the maximum index (e.g. 0,1,2,3,0,1,2,3,0... for an index count of 4) as if by the implementation:
 *
 * <pre>
 * private int index = -1;
 * public int next(){
 *  return index = (++index) % max;
 * }
 * </pre>
 *
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private int current;
    private final int max;

    public RoundRobinLoadBalancer(int numberOfIndices) {
        this(numberOfIndices, 0);
    }

    public RoundRobinLoadBalancer(int numberOfIndices, int startIndex) {
        if (startIndex < 0 || startIndex >= numberOfIndices)
            throw new IndexOutOfBoundsException("startIndex " + startIndex + " not in [0," + (numberOfIndices - 1)
                    + "]");
        max = numberOfIndices;
        current = startIndex - 1;
    }

    @Override
    public int next() {
        return ++current == max ? current = 0 : current; // CHECKSTYLE:IGNORE InnerAssignment
    }
}
