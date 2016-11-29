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
package org.restcomm.imscf.el.cap.sip;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Implementation class for SIP quoted-string manipulation. */
public final class QuotedString {
    // quoted-string = SWS DQUOTE *(qdtext / quoted-pair ) DQUOTE

    // qdtext = LWS / %x21 / %x23-5B / %x5D-7E / UTF8-NONIMSCFII; printable IMSCFII or non IMSCFII UTF-8 (start or
    // continuation)
    // 0x22 is ", 0x5C is \
    static final String NO_QUOTE_CHARS = "\\x21\\x23-\\x5B\\x5D-\\x7E\\x80-\\xFD";
    // LWS / qdtext characters
    static final Pattern NO_QUOTE = Pattern.compile("(?:(?:[ \t]*\\r\\n)?[ \t]+|[" + NO_QUOTE_CHARS + "])");

    // quoted-pair = "\" (%x00-09 / %x0B-0C / %x0E-7F) ; IMSCFII and not CR or LF
    static final String QUOTABLE_CHARS = "\\x00-\\x09\\x0B-\\x0C\\x0E-\\x7F";
    static final Pattern MUST_QUOTE = Pattern.compile("[" + QUOTABLE_CHARS + "&&[^" + NO_QUOTE_CHARS + "]]");

    static final Pattern VALIDATOR = Pattern.compile("\"(?:" + NO_QUOTE + "|\\\\[" + QUOTABLE_CHARS + "])*\"");
    static final Pattern ESCAPER = Pattern.compile("(?<dontquote>" + NO_QUOTE + ")|(?<quote>" + MUST_QUOTE + ")");
    static final Pattern UNESCAPER = Pattern.compile("\\\\([" + QUOTABLE_CHARS + "])");

    private QuotedString() {
    }

    /**
     * Returns true if value is a valid quoted-string according to RFC3261.
     */
    public static boolean isValidQuotedString(String input) {
        return input != null && VALIDATOR.matcher(input).matches();
    }

    /**
     * Encodes value as a quoted-string if possible.
     * @param value the string to be encoded
     * @param checkAlreadyValid if true and the value is already in valid quoted-string format, it is kept as is.
     *  Otherwise, the value is forcefully encoded, possibly resulting in double escapes.
     * @throws IllegalArgumentException if the value contains content that cannot be encoded. */
    public static String toQuotedString(String value, boolean checkAlreadyValid) {
        return appendAsQuotedString(new StringBuffer(), value, checkAlreadyValid).toString();
    }

    /**
     * Same as {@link #toQuotedString(String, boolean)}, but appends the result to a StringBuffer.
     * @param ret the StringBuffer to append to.*/
    public static StringBuffer appendAsQuotedString(StringBuffer ret, String value, boolean checkAlreadyValid) {
        if (checkAlreadyValid && isValidQuotedString(value))
            return ret.append(value);
        return appendAsEscapedQuotedString(ret, value);
    }

    /** Encodes the value as a quoted string, regardless of whether it is already a valid quoted string, and appends the result to
     * the provided StringBuffer.*/
    public static StringBuffer appendAsEscapedQuotedString(StringBuffer ret, String input) {
        Objects.requireNonNull(input, "quoted-string input cannot be null");
        ret.append('"');
        Matcher m = ESCAPER.matcher(input);
        while (true) {
            if (m.lookingAt()) {
                String g = m.group("quote");
                if (g != null)
                    ret.append('\\');
                else
                    g = m.group("dontquote");
                ret.append(g);
                m.region(m.end(), m.regionEnd());
            } else if (m.hitEnd()) {
                break;
            } else {
                throw new IllegalArgumentException(
                        "Cannot escape quoted-string: found illegal content starting at character "
                                + (m.regionStart() + 1) + " with codepoint "
                                + Character.codePointAt(input, m.regionStart()) + "  in input string: " + input);
            }
        }
        ret.append('"');
        return ret;
    }

    /** Tries to unescape a quoted-string into its actual value.
     * @param checkValid if true, the method first checks if the value is a valid quoted-string. Otherwise, a blind unescape occurs with no checks.
     * @throws IllegalArgumentException if checkValid is true and the check fails. */
    public static String toUnescapedValue(String quotedString, boolean checkValid) {
        if (checkValid && !isValidQuotedString(quotedString)) {
            throw new IllegalArgumentException("Invalid quoted-string value: " + quotedString);
        }
        // remove begin/end quotes, unescape the rest
        return UNESCAPER.matcher(quotedString.substring(1, quotedString.length() - 1)).replaceAll("$1");
    }
}
