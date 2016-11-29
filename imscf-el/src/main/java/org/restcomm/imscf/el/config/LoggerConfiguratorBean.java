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

import org.restcomm.imscf.common.util.logging.LogConfigurator;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Singleton bean for configuring the app logger.
 */
@Startup
@Singleton
public class LoggerConfiguratorBean {
    public static final String IMSCF_LOGBACK_FILE = System.getProperty("imscf.logback.file.name", "logback.xml");

    protected LoggerConfiguratorBean() {
        // only here to allow bean subclass
    }

    @PostConstruct
    private void initLogging() {
        LogConfigurator.initLogging(new File(ConfigBean.CONFIG_DIR, IMSCF_LOGBACK_FILE).getAbsolutePath());
    }

    // TOOD: move to some application lifecycle listener?
    @PreDestroy
    private void shutdownLogging() {
        LogConfigurator.shutdownLogging();
    }
}
