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
package org.restcomm.imscf.common .util;

//import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for traversing elements of a byte array and accessing them as unsigned values (integer).
 */
// @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification =
// "This is by design a read-through 'view' of the byte array without copying it.")
public class UnsignedBytes {

    private byte[] data;
    private final int offset;

    /**
     * Iterator class for {@link UnsignedBytes}. Note: doesn't implement {@link java.util.Iterator} to avoid integer boxing.
     */
    public class Iterator {
        int ptr = 0;

        public boolean hasNext() {
            return ptr < UnsignedBytes.this.getLength();
        }

        public int next() {
            return UnsignedBytes.this.get(ptr++);
        }

        public void skip(int count) {
            if (count < 0 || count >= UnsignedBytes.this.getLength() - ptr)
                throw new IllegalArgumentException("Cannot skip " + count + " bytes at position " + ptr
                        + " with length " + UnsignedBytes.this.getLength());
            ptr += count;
        }
    }

    public UnsignedBytes(byte[] data) {
        this.data = data;
        this.offset = 0;
    }

    public UnsignedBytes(byte[] data, int startOffset) {
        if (startOffset < 0)
            throw new IllegalArgumentException("Start offset must be >=0 ! (actual: " + startOffset + ")");
        if (startOffset >= data.length)
            throw new IllegalArgumentException("Start offset must be < data length! (actual len: " + data.length
                    + ", ofs: " + startOffset + ")");
        this.data = data;
        this.offset = startOffset;
    }

    public int get(int index) {
        return data[offset + index] & 0xFF;
    }

    public int getLength() {
        return data.length - offset;
    }

    public Iterator iterator() {
        return new Iterator();
    }
}
