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

import java.io.InputStream;
import java.io.Reader;

import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLReferenceResolver;
import javolution.xml.stream.XMLStreamException;

/**
 * XMLObjectReader marked with AutoClosable, no added functionality.
 */
public class ImscfXMLObjectReader extends XMLObjectReader implements AutoCloseable {

    @Override
    public ImscfXMLObjectReader setInput(InputStream in) throws XMLStreamException {
        return (ImscfXMLObjectReader) super.setInput(in);
    }

    @Override
    public ImscfXMLObjectReader setInput(InputStream in, String encoding) throws XMLStreamException {
        return (ImscfXMLObjectReader) super.setInput(in, encoding);
    }

    @Override
    public ImscfXMLObjectReader setInput(Reader in) throws XMLStreamException {
        return (ImscfXMLObjectReader) super.setInput(in);
    }

    @Override
    public ImscfXMLObjectReader setBinding(XMLBinding binding) {
        return (ImscfXMLObjectReader) super.setBinding(binding);
    }

    @Override
    public ImscfXMLObjectReader setReferenceResolver(XMLReferenceResolver referenceResolver) {
        return (ImscfXMLObjectReader) super.setReferenceResolver(referenceResolver);
    }

}
