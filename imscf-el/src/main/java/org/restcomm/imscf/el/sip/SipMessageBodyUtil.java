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
package org.restcomm.imscf.el.sip;

import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.util.InputStreamHelper;
import org.restcomm.imscf.util.MultipartUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMultipart;
import javax.servlet.sip.SipServletMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for manipulating SIP message bodies. */
public final class SipMessageBodyUtil {

    public static final Logger LOG = LoggerFactory.getLogger(SipMessageBodyUtil.class);

    private SipMessageBodyUtil() {
        // NOOP
    }

    /** Collects a list of all body part contents that match the specified Content-Type. */
    public static List<Object> findContent(SipServletMessage msg, ContentType contentType) {
        List<Object> ret = new ArrayList<>();
        String ctHeader = msg.getContentType();
        if (ctHeader == null) {
            return ret;
        }

        try {
            if (contentType.match(ctHeader)) {
                Object content = msg.getContent();
                ret.add(content);
                LOG.trace("Found {} of type {}", contentType, content.getClass());
            } else if (SipConstants.CONTENTTYPE_MULTIPART_MIXED.match(ctHeader)) {
                for (BodyPart bp : MultipartUtil.findBodyParts((MimeMultipart) msg.getContent(), contentType)) {
                    Object content = bp.getContent();
                    ret.add(content);
                    LOG.trace("Found {} of type {}", contentType, content.getClass());
                }
            }
        } catch (IOException | MessagingException e) {
            LOG.warn("Failed to read message body", e);
        }

        return ret;
    }

    /**
     * Compares two content objects. Strings are compared with leading/trailing whitespace ignored,
     * byte arrays are compared byte per byte, and MultiPart types compared recursively.
     */
    public static boolean compareContents(Object one, Object other, boolean strictOrder, boolean allowExtraContent) {
        LOG.trace("compareContents using strict order: {}, allow extra content: {}", strictOrder, allowExtraContent);
        if (one instanceof String && other instanceof String) {
            LOG.trace("Comparing String bodies");
            return ((String) one).trim().equals(((String) other).trim());
        } else if (one instanceof byte[] && other instanceof byte[]) {
            LOG.trace("Comparing byte[] bodies");
            return Arrays.equals((byte[]) one, (byte[]) other);
        } else if (one instanceof InputStream && other instanceof InputStream) {
            LOG.trace("Comparing InputStream bodies");
            // TODO: note: binary streams are not handled, but shouldn't be present in this application
            // TODO: should somehow use actual charset, e.g. US-IMSCFII (or as specified...) for SDP, as specified in
            // preamble for XML... IMSCFII is valid UTF-8, so at least that's not a problem.
            return InputStreamHelper.compareAsStrings((InputStream) one, (InputStream) other, StandardCharsets.UTF_8,
                    true);
        } else if (one instanceof Multipart && other instanceof Multipart) {
            LOG.trace("Comparing MimeMultipart bodies");
            return MultipartUtil.compareMultiParts((Multipart) one, (Multipart) other, strictOrder, allowExtraContent,
                    SipMessageBodyUtil::compareBodyParts);
        } else if (one != null && other != null) {
            LOG.trace("Comparing unknown type bodies ({} vs {})", one.getClass(), other.getClass());
            return one.equals(other);
        } else {
            return one == null && other == null;
        }
    }

    /**
     * Using default comparator {@link #compareContents(Object, Object)}.
     * @see MultipartUtil#compareBodyParts(BodyPart, BodyPart, BiPredicate)
     */
    public static boolean compareBodyParts(BodyPart part, BodyPart other) {
        return MultipartUtil.compareBodyParts(part, other,
                (a, b) -> SipMessageBodyUtil.compareContents(a, b, true, false));
    }

    /**
     * Tries to convert to content object to a String.
     */
    public static String convertToString(Object obj) {
        LOG.trace("converting {} to String", obj.getClass());
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof byte[]) {
            return new String((byte[]) obj, StandardCharsets.UTF_8);
        } else if (obj instanceof InputStream) {
            // TODO: note: binary streams are not handled, but shouldn't be present in this application
            // TODO: should somehow use actual charset, e.g. US-IMSCFII (or as specified...) for SDP, as specified in
            // preamble for XML... IMSCFII is valid UTF-8, so at least that's not a problem.
            return InputStreamHelper.readFullyAsString((InputStream) obj, StandardCharsets.UTF_8);
        } else {
            return String.valueOf(obj);
        }
    }

    /** Converts the entire message content to a String object.*/
    public static String getBodyAsString(SipServletMessage msg) {
        if (msg.getContentType() == null)
            return null;
        try {
            return convertToString(msg.getRawContent());
        } catch (IOException e) {
            LOG.warn("Failed to read message content", e);
            return null;
        }
    }
}
