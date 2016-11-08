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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javolution.xml.XMLBinding;
import javolution.xml.stream.XMLStreamException;

import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ApplyChargingRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ConnectRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ContinueWithArgumentRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.DisconnectLegRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.FurnishChargingInformationRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.InitialDPRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.InitiateCallAttemptRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.PlayAnnouncementRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.PromptAndCollectUserInformationRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.RequestReportBCSMEventRequestImpl;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.ResetTimerRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for CAP/MAP to XML serialization. */
public final class Jss7ToXml {
    private static final Logger LOG = LoggerFactory.getLogger(Jss7ToXml.class);

    private static final XMLBinding IMSCF_XML_BINDING;

    static {
        IMSCF_XML_BINDING = new XMLBinding();
        IMSCF_XML_BINDING.setAlias(ApplyChargingRequestImpl.class, "applyCharging");
        IMSCF_XML_BINDING.setAlias(ConnectRequestImpl.class, "connect");
        IMSCF_XML_BINDING.setAlias(ContinueWithArgumentRequestImpl.class, "continueWithArgument");
        IMSCF_XML_BINDING.setAlias(DisconnectLegRequestImpl.class, "disconnectLeg");
        IMSCF_XML_BINDING.setAlias(FurnishChargingInformationRequestImpl.class, "furnishChargingInformation");
        IMSCF_XML_BINDING.setAlias(InitialDPRequestImpl.class, "initialDP");
        IMSCF_XML_BINDING.setAlias(InitiateCallAttemptRequestImpl.class, "initiateCallAttempt");
        IMSCF_XML_BINDING.setAlias(PlayAnnouncementRequestImpl.class, "playAnnouncement");
        IMSCF_XML_BINDING.setAlias(PromptAndCollectUserInformationRequestImpl.class, "promptAndCollectUserInformation");
        IMSCF_XML_BINDING.setAlias(RequestReportBCSMEventRequestImpl.class, "requestReportBCSMEvent");
        IMSCF_XML_BINDING.setAlias(ResetTimerRequestImpl.class, "resetTimer");

    }

    private Jss7ToXml() {
        // no instances
    }

    public static String encode(Object jss7Object) {
        return encode(jss7Object, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> String encode(T jss7Object, String name) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2500); // about enough for an IDP

        try (ImscfXMLObjectWriter writer = new ImscfXMLObjectWriter()) {
            writer.setOutput(baos).setBinding(IMSCF_XML_BINDING).setIndentation(" ");
            if (name != null) {
                // we expect the runtime (implementation) class to be the one used for encoding
                writer.write(jss7Object, name, (Class<T>) jss7Object.getClass());
            } else {
                writer.write(jss7Object);
            }
            writer.flush();
            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            LOG.warn("Error serializing JSS7 object {}: {}", jss7Object, e, e);
            return null;
        }
    }

    /** Custom exception type for reading JSS7 XML objects. */
    @SuppressWarnings("serial")
    public static class XmlDecodeException extends Exception {
        public XmlDecodeException(String msg) {
            super(msg);
        }

        public XmlDecodeException(String msg, Throwable t) {
            super(msg, t);
        }
    }

    /**
     * Decode an object of the given type.
     * @return An object of the specified type on successful parse.
     * @throws XmlDecodeException if any other XML type is encountered or the allowed type fails to parse.
     */
    @SuppressWarnings("unchecked")
    public static <T> T decode(Object xml, Class<T> cls) throws XmlDecodeException {
        return (T) decodeAnyOf(xml, cls);
    }

    /**
     * Decode an object of one of the given types.
     * @return An object of one of the specified types on successful parse
     * @throws XmlDecodeException if any other XML type is encountered or one of the allowed types fails to parse or the content cannot be read
     */
    public static Object decodeAnyOf(Object xml, Class<?>... cls) throws XmlDecodeException { // NOPMD reassign
        // all accepted parameters finally become an inputstream: ((String ->) byte[] ->) InputStream
        if (xml instanceof String) {
            xml = ((String) xml).getBytes(StandardCharsets.UTF_8);
        }
        if (xml instanceof byte[]) {
            xml = new ByteArrayInputStream((byte[]) xml);
        }
        if (!(xml instanceof InputStream)) {
            // MultiPart or other types cannot directly contain an xml
            throw new XmlDecodeException("Cannot decode XML from " + (xml == null ? "null" : xml.getClass().getName()));
        }
        InputStream input = (InputStream) xml;

        try (ImscfXMLObjectReader reader = new ImscfXMLObjectReader()) {
            reader.setInput(input);
            reader.setBinding(IMSCF_XML_BINDING);
            if (reader.hasNext()) {
                String name = reader.getStreamReader().getLocalName().toString();
                LOG.trace("Next XML element: {}", name);
                Object ret;
                try {
                    ret = reader.read();
                } catch (Exception e) {
                    throw new XmlDecodeException("Failed to read element: " + name, e);
                }
                for (Class<?> c : cls)
                    if (c.isInstance(ret))
                        return ret;
                throw new XmlDecodeException("Unexpected element: " + name + " of class " + ret.getClass()
                        + ". Allowed: " + Arrays.toString(cls));
            } else {
                throw new XmlDecodeException("No next XML element");
            }
        } catch (XMLStreamException e) {
            throw new XmlDecodeException("Error deserializing JSS7 object", e);
        }
    }

}
