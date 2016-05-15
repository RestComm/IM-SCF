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

import java.io.OutputStream;

import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

/**
 * XMLObjectWriter marked with AutoClosable, no added functionality.
 */
public class ImscfXMLObjectWriter extends XMLObjectWriter implements AutoCloseable {
    @Override
    public ImscfXMLObjectWriter setOutput(OutputStream out) throws XMLStreamException {
        return (ImscfXMLObjectWriter) super.setOutput(out);
    }

    @Override
    public ImscfXMLObjectWriter setIndentation(String indentation) {
        return (ImscfXMLObjectWriter) super.setIndentation(indentation);
    }

    @Override
    public ImscfXMLObjectWriter setBinding(XMLBinding binding) {
        return (ImscfXMLObjectWriter) super.setBinding(binding);
    }
}
