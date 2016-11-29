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
package org.restcomm.imscf.el.sip;

import org.restcomm.imscf.util.ArrayUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.servlet.sip.SipServletMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Parameterable detector for messages containing a certain string or matching a regex pattern.
 * The message only matches if it has a single String part as body. */
public class SipBodyContentDetector implements SipMessageDetector {

    private static final Logger LOG = LoggerFactory.getLogger(SipBodyContentDetector.class);

    private String contains;
    private Pattern matches;

    /** Accept only messages containing this string.*/
    public SipBodyContentDetector(String contains) {
        this.contains = Objects.requireNonNull(contains);
    }

    /** Accept only messages matching this regex.*/
    public SipBodyContentDetector(Pattern matches) {
        this.matches = Objects.requireNonNull(matches);
    }

    @Override
    public boolean accept(SipServletMessage msg) {
        if (msg.getContentType() == null) { // no content
            return false;
        }
        try {
            Object content = msg.getContent();
            if (content instanceof String) {
                if (matches != null) {
                    return matches.matcher((String) content).matches();
                } else {
                    return ((String) content).contains(contains);
                }
            } else {
                if (matches != null) {
                    LOG.trace("Ignoring Pattern matching on body type {}", content.getClass());
                    // don't try to match binary content with a String regex
                    return false;
                } else {
                    return ArrayUtil.contains(msg.getRawContent(), contains, StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read SIP message body", e);
        }
    }
}
