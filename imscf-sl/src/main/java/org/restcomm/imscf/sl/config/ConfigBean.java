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

import java.io.File;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.lib.ImscfConfigChecker;

/**
 * Singleton bean storing the SL configuration.
 */
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Singleton
@Startup
@DependsOn(value = { "LoggerConfiguratorBean" })
public class ConfigBean {
    public static final String CONFIG_DIR = System.getProperty("imscf.config.dir",
            System.getProperty("jboss.server.base.dir", ".") + File.separator + "configuration" + File.separator
                    + "imscf");
    public static final String IMSCF_CONFIG_FILE = System.getProperty("imscf.config.file.name", "imscf.xml");
    public static final String SERVER_NAME = System.getProperty("jboss.server.name", "IMSCF");
    public static final String SL_MBEAN_DOMAIN = "org.restcomm.imscf.sl";

    private static final Logger LOG = LoggerFactory.getLogger(ConfigBean.class);

    private ImscfConfigType config;
    private ImscfConfigChecker checker;

    @PostConstruct
    private void init() {
        config = loadConfigurationFile();
        checker = new ImscfConfigChecker(config);
        try {
            checker.checkConfiguration();
        } catch (IllegalStateException e) {
            LOG.error("Invalid configuration: {}", e.getMessage(), e);
        }
    }

    private static ImscfConfigType loadConfigurationFile() {

        File configFile = new File(CONFIG_DIR, IMSCF_CONFIG_FILE);
        LOG.debug("Reading configuration file {}", configFile.getAbsolutePath());
        try {
            JAXBContext context = JAXBContext.newInstance("org.restcomm.imscf.common.config");
            Unmarshaller um = context.createUnmarshaller();

            // validate based on the XSD
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(Thread.currentThread().getContextClassLoader()
                    .getResource("META-INF/imscf-config.xsd"));
            um.setSchema(schema);

            @SuppressWarnings("unchecked")
            JAXBElement<ImscfConfigType> root = (JAXBElement<ImscfConfigType>) um.unmarshal(configFile);
            LOG.debug("Configuration read");
            return root.getValue();
        } catch (JAXBException | SAXException e) {
            throw new RuntimeException("No valid configuration!", e);
        }
    }

    @Lock(LockType.READ)
    public ImscfConfigType getConfig() {
        return config;
    }

    @Lock(LockType.READ)
    public boolean isSigtranStackNeeded() {
        return checker.isSigtranStackNeeded();
    }

    @Lock(LockType.READ)
    public boolean isDiameterStackNeeded() {
        return checker.isDiameterStackNeeded();
    }
}
