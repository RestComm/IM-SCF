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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for manipulating Multipart objects. */
public final class MultipartUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MultipartUtil.class);

    private MultipartUtil() {
        // no instances
    }

    /**
     * Returns true if the two Multiparts contain the same parts. If strictOrder is true, the matching parts must appear
     * in the same position in the second Multipart as they appear in the first. Otherwise, parts are found and matched
     * based on their content type, which allows the parts to be present in a different order.
     * If allowExtraContent is true, extra body parts may appear in the second Multipart which are not present in the first.
     * Otherwise the same number of body parts must be present in both.
     * Pairs of BodyParts are considered a match according to the provided predicate.
     */
    public static boolean compareMultiParts(Multipart mp, Multipart other, boolean strictOrder,
            boolean allowExtraContent, BiPredicate<BodyPart, BodyPart> bodyPartMatcher) {
        LOG.trace("compareMultiParts using strict order: {}", strictOrder);

        if (mp == null && other == null)
            return true;
        else if (mp == null || other == null)
            return false;

        try {
            if (!allowExtraContent && mp.getCount() != other.getCount()) {
                LOG.trace("Differing part count");
                return false;
            }

            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart thisBP = mp.getBodyPart(i);
                String ct = thisBP.getContentType();
                LOG.trace("Comparing BodyPart {} with Content-Type {}", i, thisBP.getContentType());
                if (strictOrder) {
                    BodyPart otherBP = other.getBodyPart(i);
                    if (!bodyPartMatcher.test(thisBP, otherBP)) {
                        LOG.trace("Other part doesn't match");
                        return false;
                    }
                } else {
                    List<BodyPart> otherBPs = findBodyParts(other, ct);
                    if (otherBPs.isEmpty()) {
                        LOG.trace("No matching body part found");
                        return false;
                    } else if (!otherBPs.stream().anyMatch(otherBP -> bodyPartMatcher.test(thisBP, otherBP))) {
                        LOG.trace("Found {} parts of the same ContentType, but none of them match", otherBPs.size());
                        return false;
                    }
                }
            }
            return true;

        } catch (MessagingException e) {
            LOG.warn("Failed to parse Multipart", e);
            return false;
        }

    }

    public static ContentType toContentType(String ct) {
        if (ct == null)
            return null;
        try {
            return new ContentType(ct);
        } catch (ParseException e) {
            LOG.warn("Cannot parse ContentType {}", ct, e);
            return null;
        }
    }

    /**
     * @see #findBodyPart(Multipart, ContentType)
     */
    public static BodyPart findBodyPart(Multipart mp, String ct) {
        return findBodyPart(mp, toContentType(ct));
    }

    /** Returns the first BodyPart in this Multipart that matches the given ContentType, null otherwise. */
    public static BodyPart findBodyPart(Multipart mp, ContentType ct) {
        List<BodyPart> matches = findBodyParts(mp, ct);
        if (!matches.isEmpty())
            return matches.get(0);
        else
            return null;
    }

    /** @see #findBodyParts(Multipart, ContentType) */
    public static List<BodyPart> findBodyParts(Multipart mp, String ct) {
        return findBodyParts(mp, toContentType(ct));
    }

    /** Returns the list of BodyParts in this Multipart that match the given ContentType, or an empty list if there
     * is no such part. */
    public static List<BodyPart> findBodyParts(Multipart mp, ContentType ct) {
        if (mp == null || ct == null)
            return Collections.emptyList();
        List<BodyPart> ret = new ArrayList<>();
        try {
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (ct.match(bp.getContentType()))
                    ret.add(bp);
            }
        } catch (MessagingException e) {
            LOG.warn("Failed to parse Multipart", e);
        }
        return ret;
    }

    /** Compares two BodyParts by comparing the content types and the contents of each ignoring leading/trailing whitespace.
     * Other part headers are ignored.*/
    public static boolean compareBodyParts(BodyPart part, BodyPart other, BiPredicate<Object, Object> contentMatcher) {
        if (part == null && other == null)
            return true;
        else if (part == null || other == null)
            return false;

        try {
            if (!part.isMimeType(other.getContentType()))
                return false;
            if (!contentMatcher.test(part.getContent(), other.getContent()))
                return false;

            return true;
        } catch (MessagingException | IOException e) {
            LOG.warn("Failed to parse BodyPart", e);
        }

        return false;
    }

}
