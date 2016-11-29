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
package org.restcomm.imscf.util;

import java.nio.charset.Charset;

/** Utility class for arrays. */
public final class ArrayUtil {
    private ArrayUtil() {
        // NOOP
    }

    public static int indexOf(final byte[] haystack, final byte[] needle) {
        int maxh = haystack.length - needle.length;
        for (int h = 0; h < maxh; h++) {
            while (haystack[h] != needle[0] && ++h < maxh)
                ; // NOPMD; CHECKSTYLE:IGNORE EmptyStatement, logic in loop header.
            if (h < maxh) {
                int n;
                for (n = 0; n < needle.length && haystack[h + n] == needle[n]; n++)
                    ; // CHECKSTYLE:IGNORE EmptyStatement, logic in loop header

                if (n == needle.length)
                    return h;
            }
        }
        return -1;
    }

    public static boolean contains(byte[] haystack, byte[] needle) {
        return indexOf(haystack, needle) >= 0;
    }

    public static boolean contains(byte[] haystack, String needle, Charset charset) {
        return contains(haystack, needle.getBytes(charset));
    }

}
