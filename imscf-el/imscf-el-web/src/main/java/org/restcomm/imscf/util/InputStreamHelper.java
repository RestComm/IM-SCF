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
package org.restcomm.imscf.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;

/** Helper class for handling InputStreams. */
public final class InputStreamHelper {

    private static final Logger LOG = LoggerFactory.getLogger(InputStreamHelper.class);

    private InputStreamHelper() {
        // no instances
    }

    public static String readFullyAsString(InputStream in, Charset charset) {
        try {
            return CharStreams.toString(new InputStreamReader(in, charset));
        } catch (IOException e) {
            LOG.warn("Error reading InputStream", e);
        }
        return null;
    }

    public static boolean compareAsStrings(InputStream in1, InputStream in2, Charset charset, boolean trim) {
        String s1 = readFullyAsString(in1, charset);
        String s2 = readFullyAsString(in2, charset);
        if (s1 == null || s2 == null)
            return false;
        return s1.trim().equals(s2.trim());
    }
}
