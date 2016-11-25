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
package org.restcomm.imscf.el.config;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.ExecutionLayerServerType;
import org.restcomm.imscf.common.config.lib.ImscfConfigChecker;
import org.restcomm.imscf.util.MBeanHelper;
import org.restcomm.imscf.el.sip.routing.SipURIAndNetmask;
import org.restcomm.imscf.common.el.config.ConfigurationManagerMBean;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
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

/**
 * Singleton bean storing the EL configuration.
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
    public static final String SERVER_NAME = System.getProperty("jboss.server.name");
    private static final Logger LOG = LoggerFactory.getLogger(ConfigBean.class);

    private static final String CONFIG_MBEAN_NAME = MBeanHelper.EL_MBEAN_DOMAIN + ":type=Configuration";

    private int serverIndex = 0;
    private List<SipURIAndNetmask> localSipURIsWithNetmask = new ArrayList<>();
    private ImscfConfigType config;
    private ImscfConfigChecker checker;
    private Set<ConfigurationChangeListener> configListeners = new HashSet<>();

    @PostConstruct
    private void postConstruct() {
        configInit();
        try {
            MBeanHelper.registerMBean(new ConfigurationManager(), CONFIG_MBEAN_NAME);
        } catch (NotCompliantMBeanException e) {
            // it is an implementation error if this happens
            throw new AssertionError(e);
        }
    }

    private void configInit() {
        ImscfConfigType oldConfig = config; // could be null

        LOG.info("Reading configuration...");
        ImscfConfigType config = loadConfigurationFile();
        ImscfConfigChecker checker = new ImscfConfigChecker(config);
        try {
            checker.checkConfiguration();
        } catch (IllegalStateException e) {
            LOG.error("Invalid configuration: {}", e.getMessage(), e);
            return;
        }
        // all OK
        this.config = config;
        this.checker = checker;

        int idx = 0;
        for (Iterator<ExecutionLayerServerType> it = config.getServers().getExecutionLayerServers().iterator(); it
                .hasNext();) {
            ExecutionLayerServerType el = it.next();
            idx++; // use 1-based index
            if (el.getName().equals(SERVER_NAME)) {
                serverIndex = idx;
                break;
            }
        }
        LOG.info("Configuration loaded.");
        configListeners.forEach(l -> {
            l.configurationChanged(oldConfig, config);
        });
        LOG.info("New configuration applied.");
    }

    @PreDestroy
    private void preDestroy() {
        MBeanHelper.unregisterMBean(CONFIG_MBEAN_NAME);
    }

    private static File getConfigFile() {
        return new File(CONFIG_DIR, IMSCF_CONFIG_FILE);
    }

    private static ImscfConfigType loadConfigurationFile() {

        File configFile = getConfigFile();
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
            LOG.trace("Configuration:\n{}", root.getValue());
            return root.getValue();
        } catch (JAXBException | SAXException e) {
            throw new RuntimeException("No valid configuration!", e);
        }
    }

    public void registerListener(ConfigurationChangeListener listener) {
        if (configListeners.add(listener)) {
            LOG.trace("Added new ConfigurationChangeListener {}", listener);
            listener.configurationChanged(null, config);
        }
    }

    @Lock(LockType.READ)
    public ImscfConfigType getConfig() {
        return config;
    }

    @Lock(LockType.READ)
    public List<SipURIAndNetmask> getLocalSipURIsWithNetmask() {
        return localSipURIsWithNetmask;
    }

    @Lock(LockType.WRITE)
    public void setLocalSipURIsWithNetmask(List<SipURIAndNetmask> localSipURIsWithNetmask) {
        this.localSipURIsWithNetmask = localSipURIsWithNetmask;
    }

    @Lock(LockType.READ)
    public int getServerIndex() {
        return serverIndex;
    }

    @Lock(LockType.READ)
    public boolean isSigtranStackNeeded() {
        return isCAPUsed() || isMAPUsed();
    }

    @Lock(LockType.READ)
    public boolean isCAPUsed() {
        return !config.getCapModules().isEmpty();
    }

    @Lock(LockType.READ)
    public boolean isMAPUsed() {
        return !config.getMapModules().isEmpty();
    }

    /** MBean implementation. */
    private class ConfigurationManager extends StandardMBean implements ConfigurationManagerMBean {

        private final String configurationFilePath = getConfigFile().getAbsolutePath();

        ConfigurationManager() throws NotCompliantMBeanException {
            super(ConfigurationManagerMBean.class);
        }

        @Override
        public String getConfigurationFilePath() {
            return configurationFilePath;
        }

        @Override
        public synchronized void reloadConfiguration() {
            LOG.info("JMX request: reloadConfiguration");
            ConfigBean.this.configInit();
        }

        @Override
        protected String getDescription(MBeanInfo info) {
            return "Configuration management MBean";
        }

        @Override
        protected String getDescription(MBeanAttributeInfo info) {
            switch (info.getName()) { // NOPMD TooFewBranchesForASwitchStatement
            case "ConfigurationFilePath":
                return "The resolved file path where IMSCF searches for the configuration file.";
            default:
                return "No description";
            }
        }

        @Override
        protected MBeanConstructorInfo[] getConstructors(MBeanConstructorInfo[] ctors, Object impl) {
            return new MBeanConstructorInfo[0];
        }

        @Override
        protected String getDescription(MBeanOperationInfo info) {
            switch (info.getName()) { // NOPMD TooFewBranchesForASwitchStatement
            case "reloadConfiguration":
                return "Tries to reload the configuration from the file path indicated by the ConfigurationFilePath attribute.";
            default:
                return "No description";
            }
        }

        @Override
        protected int getImpact(MBeanOperationInfo info) {
            switch (info.getName()) { // NOPMD TooFewBranchesForASwitchStatement
            case "reloadConfiguration":
                return MBeanOperationInfo.ACTION;
            default:
                return super.getImpact(info);
            }
        }
    }

}
