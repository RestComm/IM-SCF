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
package org.restcomm.imscf.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Utility class for accessing application.properties in the EAR.
 *
 */
public final class ApplicationProperties {

    private static final String PROP_PREFIX_APPLICATION_PROPERTIES = "application.";
    private static final String PROP_PREFIX_MANIFEST_MF = "manifest.";

    Properties p;

    private ApplicationProperties(Properties p) {
        this.p = p;
    }

    public static ApplicationProperties newInstance() {
        return new ApplicationProperties(loadProperties());
    }

    private static Properties loadProperties() {
        ClassLoader cl = getContextClassLoader(); // TODO: is this necessary?
        Properties p = new Properties();

        // application.properties
        try (InputStream is = cl.getResourceAsStream("application.properties")) {
            if (is != null) {
                Properties appProp = new Properties();
                appProp.load(is);
                for (String s : appProp.stringPropertyNames()) {
                    p.put(getApplicationPropertyName(s), appProp.getProperty(s));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties from application.properties!", e);
        }

        // manifest entries
        try (InputStream is = cl.getResourceAsStream("MANIFEST.MF")) {
            if (is != null) {
                Manifest mf = new Manifest(is);
                for (Entry<Object, Object> e : mf.getMainAttributes().entrySet()) {
                    String propKey = getManifestPropertyName(String.valueOf(e.getKey()));
                    p.put(propKey, String.valueOf(e.getValue()));
                }
                for (Entry<String, Attributes> section : mf.getEntries().entrySet()) {
                    for (Entry<Object, Object> e : section.getValue().entrySet()) {
                        String propKey = getManifestPropertyName(String.valueOf(e.getKey()), section.getKey());
                        p.put(propKey, String.valueOf(e.getValue()));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties from MANIFEST.MF!", e);
        }

        return p;
    }

    private static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                ClassLoader cl = null;
                try {
                    cl = Thread.currentThread().getContextClassLoader();
                } catch (SecurityException ex) {
                    return cl;
                }
                return cl;
            }
        });
    }

    public Properties getAllProperties() {
        return new Properties(p);
    }

    public String getProperty(String name) {
        return p.getProperty(name);
    }

    public static String getApplicationPropertyName(String property) {
        return PROP_PREFIX_APPLICATION_PROPERTIES + property;
    }

    public static String getManifestPropertyName(String property, String section) {
        return PROP_PREFIX_MANIFEST_MF + section + "." + property;
    }

    public static String getManifestPropertyName(String property) {
        return getManifestPropertyName(property, "Main");
    }

    public String getApplicationProperty(String property) {
        return p.getProperty(getApplicationPropertyName(property));
    }

    public String getManifestProperty(String property, String section) {
        return p.getProperty(getManifestPropertyName(property, section));
    }

    public String getManifestProperty(String property) {
        return p.getProperty(getManifestPropertyName(property));
    }

}
