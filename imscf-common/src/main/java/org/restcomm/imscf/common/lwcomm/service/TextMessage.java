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
package org.restcomm.imscf.common.lwcomm.service;

import java.util.Objects;

/**
 * API representation of the text message.
 * @author Miklos Pocsaji
 * @author Tamas Gyorgyey
 */
public class TextMessage {

    protected String payload;
    protected String targetQueue;
    protected String groupId;
    protected String tag;

    protected TextMessage() {
        // allow subclasses
    }

    public String getPayload() {
        return payload;
    }

    public String getTargetQueue() {
        return targetQueue;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return "TextMessage [payload=" + payload + ", targetQueue=" + targetQueue + ", groupId=" + groupId + ", tag="
                + tag + "]";
    }

    /**
     * Returns a builder for a message with the specified payload (mandatory parameter).
     * Group ID, JMS target queue and user Tag will be null unless set through the builder.
     */
    public static TextMessageBuilder<TextMessageBuilder<?, TextMessage>, TextMessage> builder(String payload) {
        return new TextMessageBuilder<TextMessageBuilder<?, TextMessage>, TextMessage>(TextMessage.class, payload);
    }

    /** Builder for creating and manipulating an otherwise immutable TextMessage object. */
    public static class TextMessageBuilder<T extends TextMessageBuilder<?, TM>, TM extends TextMessage> {
        private TM message;

        protected TextMessageBuilder(Class<TM> cls, String payload) {
            try {
                this.message = cls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new AssertionError("", e);
            }
            message.payload = Objects.requireNonNull(payload, "payload cannot be null");
        }

        protected TM getInstance() {
            return message;
        }

        /** Sets the group ID of the message. */
        public TextMessageBuilder<T, TM> setGroupId(String groupId) {
            Objects.requireNonNull(message, "TextMessageBuilder cannot be reused after a call to create()!");
            message.groupId = groupId;
            return this;
        }

        /** Sets the target JMS queue of the message. */
        public TextMessageBuilder<T, TM> setTargetQueue(String targetQueue) {
            Objects.requireNonNull(message, "TextMessageBuilder cannot be reused after a call to create()!");
            message.targetQueue = targetQueue;
            return this;
        }

        /** Sets the user tag of the message. */
        public TextMessageBuilder<T, TM> setTag(String tag) {
            Objects.requireNonNull(message, "TextMessageBuilder cannot be reused after a call to create()!");
            message.tag = tag;
            return this;
        }

        /** Returns the resulting TextMessage. */
        public TM create() {
            TM ret = message;
            message = null; // disable post-create changes by releasing
            return ret;
        }
    }

}
