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
package org.restcomm.imscf.common.lwcomm.service;

import org.restcomm.imscf.common.lwcomm.config.Node;

/**
 * Result of sending a message.
 * The type of the result can be
 * <li>SUCCESS - The delivery was successful. The actualDestination attribute holds the Node which received the message</li>
 * <li>CANCELLED - The delivery has been cancelled by the user.</li>
 * <li>FAILURE - No ACK message has been received for the message.</li>
 *
 * Note that in case of CANCELLED and FAILURE there is still a chance that the message has been received by a node.
 * @author Miklos Pocsaji
 *
 */
public class SendResult {
    public static final SendResult CANCELLED = new SendResult(Type.CANCELLED);
    public static final SendResult FAILURE = new SendResult(Type.FAILURE);

    /**
     * Result of the message send.
     * @see SendResult
     * @author Miklos Pocsaji
     *
     */
    public enum Type {
        SUCCESS, CANCELLED, FAILURE
    }

    private Type type;
    private Node actualDestination;

    private SendResult(Type type) {
        this.type = type;
    }

    public SendResult(Node actualDestination) {
        this.type = Type.SUCCESS;
        this.actualDestination = actualDestination;
    }

    public Type getType() {
        return type;
    }

    public Node getActualDestination() {
        return actualDestination;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actualDestination == null) ? 0 : actualDestination.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        SendResult other = (SendResult) obj;
        if (actualDestination == null) {
            if (other.actualDestination != null)
                return false;
        } else if (!actualDestination.equals(other.actualDestination))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SendResult [type=" + type + ", actualDestination=" + actualDestination + "]";
    }

}
