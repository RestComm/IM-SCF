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
package org.restcomm.imscf.common .util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimalistic parser for peeking into TCAP content in SCCP messages to determine message type and transaction ids.
 */
public class TCAPMessageInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(TCAPMessageInfo.class);

    /**
     * Enum class for TCAP message types.
     */
    public static enum MessageType {
        // TC_UNI not used in IN
        TC_BEGIN(0x62), TC_END(0x64), TC_CONTINUE(0x65), TC_ABORT(0x67);

        private final int encodedTagByte;

        public int getEncodedTagByte() {
            return encodedTagByte;
        }

        private MessageType(int encodedTagByte) {
            this.encodedTagByte = encodedTagByte;
        }

        public static MessageType forByte(int ubyte) {
            for (MessageType m : values())
                if (m.getEncodedTagByte() == ubyte)
                    return m;
            throw new IllegalArgumentException("Unrecognized TCAP message type tag " + ubyte);
        }
    }

    /** Constants for TCAP transaction id tags. */
    private static enum TIDType {
        OTID(0x48), DTID(0x49);

        private final int encodedTagByte;

        public int getEncodedTagByte() {
            return encodedTagByte;
        }

        private TIDType(int encodedTagByte) {
            this.encodedTagByte = encodedTagByte;
        }

        // public static TIDType forByte(int ubyte) {
        // for (TIDType m : values())
        // if (m.getEncodedTagByte() == ubyte)
        // return m;
        // throw new IllegalArgumentException("Unrecognized Transaction ID tag "
        // + ubyte);
        // }
    }

    MessageType messageType;
    private Long otid;
    private Long dtid;

    // application-context, etc. if needed

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Long getOtid() {
        return otid;
    }

    public void setOtid(Long otid) {
        this.otid = otid;
    }

    public Long getDtid() {
        return dtid;
    }

    public void setDtid(Long dtid) {
        this.dtid = dtid;
    }

    @Override
    public String toString() {
        return messageType + "[otid: " + (otid == null ? "N/A" : "0x" + Long.toHexString(otid)) + ", dtid: "
                + (dtid == null ? "N/A" : "0x" + Long.toHexString(dtid)) + "]";
    }

    public static TCAPMessageInfo parse(byte[] data) {
        return parse(new UnsignedBytes(data));
    }

    public static TCAPMessageInfo parse(byte[] data, int offset) {
        return parse(new UnsignedBytes(data, offset));
    }

    public static TCAPMessageInfo parse(UnsignedBytes ubytes) {
        TCAPMessageInfo info = new TCAPMessageInfo();
        UnsignedBytes.Iterator u = ubytes.iterator();

        // first: tag for message type
        info.setMessageType(MessageType.forByte(u.next()));

        // second: length of message in short or long form
        int lenFirstByte = u.next();
        // short: if length < 127, bit 8 is 0 and bits 7-1 contain length
        // long : if length > 127, bit 8 is 1, and bits 7-1 mean additional byte
        // count. those additional bytes contain the length
        // we don't actually need the length, so just skip it
        if ((lenFirstByte & 0b1000_0000) > 0)
            u.skip(lenFirstByte & 0b0111_1111);

        // third: dialogue portion with OTID and DTID values
        switch (info.getMessageType()) {
        case TC_BEGIN:
        case TC_CONTINUE:
            // OTID should be present
            if (u.next() == TIDType.OTID.getEncodedTagByte()) {
                info.otid = parseUnsignedWithLength(u);
            } else {
                LOGGER.error("No OTID tag found in " + info.getMessageType());
                return info;
            }
            if (info.getMessageType() == MessageType.TC_BEGIN)
                break; // no DTID
            //$FALL-THROUGH$ for TC_CONTINUE
        case TC_END:
        case TC_ABORT:
            // DTID should be present
            if (u.next() == TIDType.DTID.getEncodedTagByte()) {
                info.dtid = parseUnsignedWithLength(u);
            } else {
                LOGGER.error("No DTID tag found in " + info.getMessageType());
                return info;
            }
            break;
        default:
            throw new RuntimeException("Unknown TCAP message type: " + info.getMessageType());
        }

        return info;
    }

    private static long parseUnsignedWithLength(UnsignedBytes.Iterator u) {
        int len = u.next(); // note that len > 8 wouldn't fit into a long...
        // parse tid from next len bytes
        long tid = 0;
        for (int j = len - 1; j >= 0; j--) {
            tid |= (u.next() & 0xFFL) << (8 * j);
        }
        return tid;
    }
}
