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

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

/** Common constants to be used with SIP. */
public final class SipConstants {

    public static final String SUPPORTED_100REL = "100rel";
    public static final String CONTENTTYPE_CAP_XML_STRING = "application/cap+xml";
    public static final String CONTENTTYPE_MAP_XML_STRING = "application/map-phase3+xml";
    public static final String CONTENTTYPE_PIDF_XML_STRING = "application/pidf+xml";
    public static final String CONTENTTYPE_SDP_STRING = "application/sdp";
    public static final String CONTENTTYPE_MULTIPART_MIXED_STRING = "multipart/mixed";

    public static final ContentType CONTENTTYPE_CAP_XML;
    public static final ContentType CONTENTTYPE_MAP_XML;
    public static final ContentType CONTENTTYPE_PIDF_XML;
    public static final ContentType CONTENTTYPE_SDP;
    public static final ContentType CONTENTTYPE_MULTIPART_MIXED;

    public static final String HEADER_MAP_METHOD = "Subject";
    public static final String HEADER_ICA_ROUTE = "x-imscf-route";
    public static final String HEADER_IMSCF_SERVICE_IDENTIFIER = "x-imscf-service-identifier";
    public static final String HEADER_AUTOMATIC_CALL_PROCESSING_SUSPENSION = "x-imscf-cps";
    public static final String HVALUE_CPS_START = "start";
    public static final String HVALUE_CPS_STOP = "stop";

    public static final String DOMAIN_IMSCF = "imscf.restcomm.org";
    public static final String WARNING_HEADER_VALUE_START = "399 " + DOMAIN_IMSCF + " ";
    public static final String CAP_MODULE_NAME_PARAM = "capmodule";

    static {
        try {
            CONTENTTYPE_CAP_XML = new ContentType(CONTENTTYPE_CAP_XML_STRING);
            CONTENTTYPE_MAP_XML = new ContentType(CONTENTTYPE_MAP_XML_STRING);
            CONTENTTYPE_PIDF_XML = new ContentType(CONTENTTYPE_PIDF_XML_STRING);
            CONTENTTYPE_SDP = new ContentType(CONTENTTYPE_SDP_STRING);
            CONTENTTYPE_MULTIPART_MIXED = new ContentType(CONTENTTYPE_MULTIPART_MIXED_STRING);
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }

    private SipConstants() {
        // no instances
    }
}
