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
package org.restcomm.imscf.common.util.logging;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Common code for initializing logback config.
 *
 */
public final class LogConfigurator {

    private LogConfigurator() {
    }

    public static void initLogging(String logbackXml) {

        LoggerContext lctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        lctx.addListener(new ApplicationPropertiesPopulatingLoggerContextListener());
        lctx.reset();

        JoranConfigurator joranConfigurator = new JoranConfigurator();
        joranConfigurator.setContext(lctx);
        try {
            joranConfigurator.doConfigure(logbackXml);
        } catch (JoranException e) {
            throw new RuntimeException(
                    "Error configuring logging. Exception is written to stdout of managed server(s).", e);
        }
    }

    public static void shutdownLogging() {
        LoggerContext lctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        // this takes care of everything, i.e. stops all appenders as well
        lctx.stop();
    }
}
