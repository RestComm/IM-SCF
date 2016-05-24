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

import org.restcomm.imscf.common.lwcomm.config.impl.NodeImpl;

/**
 * Represents a node in the configuration.
 * <b>Important: two nodes are equal if their name is equal!</b>
 * @author Miklos Pocsaji
 *
 */

public abstract class Node {

    /**
     * The name of the node. Must conform to the node name in the application server.
     * @return The name of the node
     */
    public abstract String getName();

    /**
     * The hostname of the node.
     * @return The hostname of the node
     */
    public abstract String getHost();

    /**
     * The UDP port of the node to connect to.
     * @return The UDP port of the node.
     */
    public abstract int getPort();

    @Override
    public final String toString() {
        return "Node [name=" + getName() + ", host=" + getHost() + ", port=" + getPort() + "]";
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
        NodeImpl other = (NodeImpl) obj;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!getName().equals(other.getName()))
            return false;
        return true;
    }

}
