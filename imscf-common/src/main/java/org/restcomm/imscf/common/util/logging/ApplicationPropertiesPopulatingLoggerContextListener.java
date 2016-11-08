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
package org.restcomm.imscf.common.util.logging;

import org.restcomm.imscf.common.util.ApplicationProperties;

import java.util.Properties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;

/**
 * LoggerContext listener to populate the context with application properties after a context reset.
 * A context reset occurs when the configuration file changes and is processed again.
 */
public class ApplicationPropertiesPopulatingLoggerContextListener implements LoggerContextListener {

    Properties propsToAdd;

    public ApplicationPropertiesPopulatingLoggerContextListener() {
        ApplicationProperties ap = ApplicationProperties.newInstance();
        propsToAdd = ap.getAllProperties();
    }

    @Override
    public void onStop(LoggerContext ctx) {
        System.out.println("LoggerContext (" + ctx + ") stopped.");
    }

    @Override
    public void onStart(LoggerContext ctx) {
        System.out.println("LoggerContext (" + ctx + ") started.");
        System.out.println("LoggerContext properties: " + ctx.getCopyOfPropertyMap());
    }

    @Override
    public void onReset(LoggerContext ctx) {
        for (String prop : propsToAdd.stringPropertyNames()) {
            ctx.putProperty(prop, propsToAdd.getProperty(prop));
        }
        System.out.println("LoggerContext reset, current properties: " + ctx.getCopyOfPropertyMap());
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
        // nothing to do here
    }

    @Override
    public boolean isResetResistant() {
        // this listener is supposed to be added once, after which it survives resets - that's the point.
        return true;
    }

}
