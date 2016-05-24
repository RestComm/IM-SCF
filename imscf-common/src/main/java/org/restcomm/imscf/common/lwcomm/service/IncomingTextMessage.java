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

import java.util.Objects;

/**
 * Class which represents incoming messages.
 * @see TextMessage
 * @author Miklos Pocsaji
 * @author Tamas Gyorgyey
 */
public class IncomingTextMessage extends TextMessage {

    protected Node from;
    protected String id;

    protected IncomingTextMessage() {
        // allow subclasses
    }

    public Node getFrom() {
        return from;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "IncomingTextMessage [from=" + from + ", id=" + id + ", getPayload()=" + getPayload()
                + ", getTargetQueue()=" + getTargetQueue() + ", getGroupId()=" + getGroupId() + "]";
    }

    /**
     * Returns a builder for a message with the specified payload, source node and id (mandatory parameters).
     * Group ID, JMS target queue and user Tag will be null unless set through the builder.
     */
    public static IncomingTextMessageBuilder<IncomingTextMessageBuilder<?, IncomingTextMessage>, IncomingTextMessage> builder(
            String payload, Node from, String id) {
        return new IncomingTextMessageBuilder<IncomingTextMessageBuilder<?, IncomingTextMessage>, IncomingTextMessage>(
                IncomingTextMessage.class, payload, from, id);
    }

    /** Builder for creating and manipulating an otherwise immutable IncomingTextMessage object. */
    public static class IncomingTextMessageBuilder<ITMB extends IncomingTextMessageBuilder<?, ITM>, ITM extends IncomingTextMessage>
            extends TextMessageBuilder<IncomingTextMessageBuilder<ITMB, ITM>, ITM> {

        protected IncomingTextMessageBuilder(Class<ITM> cls, String payload, Node from, String id) {
            super(cls, payload);
            getInstance().from = Objects.requireNonNull(from, "Message source node cannot be null");
            getInstance().id = Objects.requireNonNull(id, "Message ID cannot be null");
        }

    }
}
