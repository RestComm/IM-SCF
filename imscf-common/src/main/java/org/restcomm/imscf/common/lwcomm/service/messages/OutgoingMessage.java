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
package org.restcomm.imscf.common.lwcomm.service.messages;

import org.restcomm.imscf.common.lwcomm.config.Route;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;
import org.restcomm.imscf.common.lwcomm.service.impl.LwCommServiceImpl;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Internal representation of an outgoing message.
 * @author Miklos Pocsaji
 *
 */
public class OutgoingMessage extends LwCommMessage {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static OutgoingMessage createAck(LwCommMessage msg) {
        OutgoingMessage ret = new OutgoingMessage();
        ret.type = Type.ACK;
        ret.id = msg.getId();
        ret.targetRoute = msg.getTargetRoute();
        ret.groupId = msg.getGroupId();
        ret.from = LwCommServiceImpl.getServiceImpl().getConfiguration().getLocalNode();
        return ret;
    }

    public static OutgoingMessage createNack(LwCommMessage msg) {
        OutgoingMessage ret = new OutgoingMessage();
        ret.type = Type.NACK;
        ret.id = msg.getId();
        ret.targetRoute = msg.getTargetRoute();
        ret.groupId = msg.getGroupId();
        ret.from = LwCommServiceImpl.getServiceImpl().getConfiguration().getLocalNode();
        return ret;
    }

    public static OutgoingMessage createHeartbeat() {
        OutgoingMessage ret = new OutgoingMessage();
        ret.type = Type.HEARTBEAT;
        ret.id = "N/A";
        ret.from = LwCommServiceImpl.getServiceImpl().getConfiguration().getLocalNode();
        return ret;
    }

    public static OutgoingMessage create(Route targetRoute, TextMessage message) {
        OutgoingMessage ret = new OutgoingMessage();
        ret.type = Type.NORMAL;
        ret.id = generateNewMessageId();
        ret.payload = message.getPayload();
        ret.targetQueue = message.getTargetQueue() == null ? targetRoute.getDefaultQueue() : message.getTargetQueue();
        ret.targetRoute = targetRoute.getName();
        ret.groupId = message.getGroupId();
        ret.userTag = message.getTag();
        ret.from = LwCommServiceImpl.getServiceImpl().getConfiguration().getLocalNode();
        ret.payloadBytes = ret.getCalculatedPayloadBytes();
        return ret;
    }

    public void setRetransmitCount(int retransmitCount) {
        this.retransmitCount = retransmitCount;
    }

    public void setFailover(boolean failover) {
        this.failover = failover;
    }

    private static String generateNewMessageId() {
        byte[] buf = new byte[6];
        RANDOM.nextBytes(buf);
        return Base64.getEncoder().encodeToString(buf);
    }

    @Override
    public String toString() {
        return "OutgoingMessage [id=" + id + ", type=" + type + ", from=" + from + ", targetQueue=" + targetQueue
                + ", retransmitCount=" + retransmitCount + ", payload=" + payload + ", failover=" + failover
                + ", targetRoute=" + targetRoute + ", groupId=" + groupId + ", userTag=" + userTag + "]";
    }

}
