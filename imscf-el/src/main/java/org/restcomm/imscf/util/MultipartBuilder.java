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

import java.nio.charset.StandardCharsets;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

/**
 * Builder class for easily building multipart contents with method call chaining.
 * This implementation is NOT thread safe and should be used by a single thread at a time.
 * An instance should not be reused.
 */
public final class MultipartBuilder {
    private MimeMultipart multipart;
    private InternetHeaders currentHeaders;

    /** Creates an instance with default "multipart/mixed" type. */
    public MultipartBuilder() {
        this("mixed");
    }

    /** Creates an instance with the specified multipart/&lt;subType>. */
    public MultipartBuilder(String subType) {
        multipart = new MimeMultipart(subType);
        currentHeaders = new InternetHeaders();
    }

    /** Adds a complete header line to the current content part. */
    public MultipartBuilder addPartHeaderLine(String headerLine) {
        currentHeaders.addHeaderLine(headerLine);
        return this;
    }

    /** Adds a header to the current content part. */
    public MultipartBuilder addPartHeader(String name, String value) {
        currentHeaders.addHeader(name, value);
        return this;
    }

    /** Adds a content part body with default UTF-8 encoding. */
    public MultipartBuilder addPartBody(String contentType, String body) throws MessagingException {
        // TODO specify charset as Content-Type parameter?
        return addPartBody(contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    /** Adds a content part with the headers added so far and the specified Content-Type. */
    public MultipartBuilder addPartBody(String contentType, byte[] body) throws MessagingException {
        currentHeaders.addHeader("Content-Type", contentType);
        multipart.addBodyPart(new MimeBodyPart(currentHeaders, body));
        currentHeaders = new InternetHeaders(); // for the next body part
        return this;
    }

    public MimeMultipart getResult() {
        MimeMultipart result = multipart;
        // disable reuse
        multipart = null;
        currentHeaders = null;
        return result;
    }
}
