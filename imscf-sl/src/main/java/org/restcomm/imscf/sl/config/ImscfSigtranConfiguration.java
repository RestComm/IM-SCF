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
package org.restcomm.imscf.sl.config;

import org.restcomm.imscf.common.config.ImscfConfigType;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.xml.sax.SAXException;

/**
 * Utility class for handling sigtran configuration loading.
 *
 * @author Balogh Gábor
 *
 */
public final class ImscfSigtranConfiguration {

    /**
     * To avoid being instantiated.
     */
    private ImscfSigtranConfiguration() {

    }

    @SuppressWarnings("unchecked")
    public static ImscfConfigType load(File configXmlFile) throws JAXBException, SAXException {
        JAXBContext context = JAXBContext.newInstance("org.restcomm.imscf.common.config");
        Unmarshaller um = context.createUnmarshaller();
        return ((JAXBElement<ImscfConfigType>) um.unmarshal(configXmlFile)).getValue();
    }
}
