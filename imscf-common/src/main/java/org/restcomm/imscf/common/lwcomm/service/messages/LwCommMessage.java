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
package org.restcomm.imscf.common.lwcomm.service.messages;

import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.service.IncomingTextMessage;
import org.restcomm.imscf.common.lwcomm.service.impl.LwCommServiceImpl;
import org.restcomm.imscf.common.lwcomm.service.impl.LwCommUtil;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.Charset;

import org.slf4j.MDC;

/**
 * Internal representation of a message (normal, ACK, NACK and HEARTBEAT as well).
 * Can be created by parsing plain text and can be converted to plain text (for sending).
 * @author Miklos Pocsaji
 * @author Tamas Gyorgyey
 */
@SuppressWarnings("PMD.GodClass")
public class LwCommMessage {

    /**
     * The possible types of a message.
     * @author Miklos Pocsaji
     */
    public enum Type {
        NORMAL, ACK, NACK, HEARTBEAT, INVALID
    }

    private static final String DOUBLE_LINETERMINATOR = "\n\n";

    protected String id;
    protected Type type;
    protected Node from;
    protected String targetQueue;
    protected int retransmitCount;
    protected String payload;
    protected int payloadBytes;
    protected boolean failover;
    protected String targetRoute;
    protected String groupId;
    protected String userTag;

    public static final String ACK = "ACK";
    public static final String HEARTBEAT = "HEARTBEAT";
    public static final String HEADER_FROM = "From";
    public static final String HEADER_TARGET_QUEUE = "Target-Queue";
    public static final String HEADER_RETRANSMIT_COUNT = "Retransmit-Count";
    public static final String HEADER_PAYLOAD_BYTES = "Payload-Bytes";
    public static final String HEADER_FAILOVER = "Failover";
    public static final String HEADER_TARGET_ROUTE = "Target-Route";
    public static final String HEADER_GROUP_ID = "Group-Id";
    public static final String HEADER_USER_TAG = "Tag";

    protected LwCommMessage() {
        // Constructor is empty to allow only subclasses and self static methods to instantiate.
    }

    /**
     * Creates a new message parsed from the raw data received.
     * @param rawMessage The String data received from the network.
     */
    public LwCommMessage(String rawMessage) {
        // read message line by line
        BufferedReader reader = new BufferedReader(new StringReader(rawMessage));

        try {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                LwCommServiceImpl.LOGGER.error("Incoming message is empty.");
                type = Type.INVALID;
                return;
            }
            // For ACK and NACK, a space is included in the pattern, because a normal message id could start with these
            // strings, but it cannot contain a space.
            if (firstLine.startsWith("ACK ")) {
                type = Type.ACK;
                id = firstLine.split(" ")[1];
            } else if (firstLine.startsWith("NACK ")) {
                type = Type.NACK;
                id = firstLine.split(" ")[1];
            } else if (firstLine.startsWith("HEARTBEAT")) {
                type = Type.HEARTBEAT;
                // Next line MUST be the "From: "
                parseLine(reader.readLine());
                id = "N/A";
            } else {
                type = Type.NORMAL;
                id = firstLine;
            }
            String line = reader.readLine();
            while (line != null) {
                boolean inTheBody = parseLine(line);
                if (inTheBody)
                    break;
                line = reader.readLine();
            }
            int payloadStart = rawMessage.indexOf(DOUBLE_LINETERMINATOR);
            if (payloadStart > 0) {
                payload = rawMessage.substring(payloadStart + DOUBLE_LINETERMINATOR.length());
            }
            reader.close();
        } catch (Exception ex) {
            LwCommServiceImpl.LOGGER.error("Error while parsing message", ex);
            type = Type.INVALID;
        }
    }

    private boolean parseLine(String line) {
        if (line.startsWith(HEADER_FROM)) {
            from = LwCommServiceImpl.getServiceImpl().getConfiguration().getNodeByName(line.split(" ")[1]);
        } else if (line.startsWith(HEADER_TARGET_QUEUE)) {
            targetQueue = line.split(" ")[1];
        } else if (line.startsWith(HEADER_RETRANSMIT_COUNT)) {
            retransmitCount = Integer.parseInt(line.split(" ")[1]);
        } else if (line.startsWith(HEADER_PAYLOAD_BYTES)) {
            payloadBytes = Integer.parseInt(line.split(" ")[1]);
        } else if (line.startsWith(HEADER_FAILOVER)) {
            failover = Boolean.valueOf(line.split(" ")[1]);
        } else if (line.startsWith(HEADER_TARGET_ROUTE)) {
            targetRoute = line.split(" ")[1];
        } else if (line.startsWith(HEADER_GROUP_ID)) {
            groupId = line.split(" ")[1];
        } else if (line.startsWith(HEADER_USER_TAG)) {
            userTag = line.split(" ")[1];
        } else if ("".equals(line)) {
            // body starts
            return true;
        } else {
            LwCommServiceImpl.LOGGER.warn("Unparseable message line: '{}'", line);
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public Node getFrom() {
        return from;
    }

    public Type getType() {
        return type;
    }

    public String getTargetQueue() {
        return targetQueue;
    }

    public int getRetransmitCount() {
        return retransmitCount;
    }

    public int getPayloadBytes() {
        return payloadBytes;
    }

    public boolean isFailover() {
        return failover;
    }

    public String getTargetRoute() {
        return targetRoute;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getUserTag() {
        return userTag;
    }

    public String toRawMessage() {
        MDC.put(LwCommUtil.LOGGER_MDC_MSGID_KEY, id);
        if (type == Type.INVALID) {
            LwCommServiceImpl.LOGGER.warn("Trying to get raw string from message with type INVALID (id={})", id);
            return null;
        }
        if (from == null) {
            LwCommServiceImpl.LOGGER.warn("Trying to get raw string from message with missing 'From' (id={})", id);
            return null;
        }
        StringBuilder ret = new StringBuilder();
        switch (type) {
        case ACK:
            ret.append("ACK ").append(id).append("\n");
            break;
        case NACK:
            ret.append("NACK ").append(id).append("\n");
            break;
        case HEARTBEAT:
            ret.append("HEARTBEAT\n");
            break;
        case NORMAL:
            ret.append(id).append("\n");
            break;
        default:
            LwCommServiceImpl.LOGGER.error("Unexpected message type: {}", type);
            return null;
        }

        ret.append(HEADER_FROM).append(": ").append(from.getName()).append("\n");
        if (targetQueue != null) {
            ret.append(HEADER_TARGET_QUEUE).append(": ").append(targetQueue).append("\n");
        }
        if (retransmitCount > 0) {
            ret.append(HEADER_RETRANSMIT_COUNT).append(": ").append(String.valueOf(retransmitCount)).append("\n");
        }
        if (failover) {
            ret.append(HEADER_FAILOVER).append(": ").append("true").append("\n");
        }
        if (targetRoute != null && !targetRoute.equals("")) {
            ret.append(HEADER_TARGET_ROUTE).append(": ").append(targetRoute).append("\n");
        } else {
            ret.append(HEADER_TARGET_ROUTE).append(": N/A\n");
        }
        if (groupId != null && !groupId.equals("")) {
            ret.append(HEADER_GROUP_ID).append(": ").append(groupId).append("\n");
        }

        if (userTag != null && !userTag.trim().equals("")) {
            ret.append(HEADER_USER_TAG).append(": ").append(userTag).append("\n");
        }

        if (payload != null) {
            ret.append(HEADER_PAYLOAD_BYTES).append(": ").append(String.valueOf(payloadBytes)).append("\n");
            ret.append("\n");
            ret.append(payload);
        } else {
            ret.append("\n");
        }

        return ret.toString();
    }

    public int getCalculatedPayloadBytes() {
        return payload.getBytes(Charset.forName("UTF-8")).length;
    }

    public IncomingTextMessage asIncomingTextMessage() {
        return IncomingTextMessage.builder(payload, from, id).setGroupId(groupId).setTargetQueue(targetQueue)
                .setTag(userTag).create();
    }

    @Override
    public String toString() {
        return "LwCommMessage [id=" + id + ", type=" + type + ", from=" + from + ", targetQueue=" + targetQueue
                + ", retransmitCount=" + retransmitCount + ", payload=" + payload + ", failover=" + failover
                + ", targetRoute=" + targetRoute + ", groupId=" + groupId + ", userTag=" + userTag + "]";
    }

}
