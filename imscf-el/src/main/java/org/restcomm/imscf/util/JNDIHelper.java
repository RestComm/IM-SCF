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

import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.config.ConfigBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for JNDI operations.
 */
public final class JNDIHelper {
    private static final Logger LOG = LoggerFactory.getLogger(JNDIHelper.class);
    private static final String APPNAME = findAppName();
    private static final String JNDI_WEB_BASE = "java:global/" + APPNAME + "/imscf-el-web/";
    private static final String JNDI_CALLSTORE = JNDI_WEB_BASE + "CallStoreBean";
    private static final String JNDI_CALLFACTORY = JNDI_WEB_BASE + "CallFactoryBean";
    private static final String JNDI_CONFIGBEAN = JNDI_WEB_BASE + "ConfigBean";

    private JNDIHelper() {
    }

    public static CallStore getCallStore() {
        try {
            return (CallStore) new InitialContext().lookup(JNDI_CALLSTORE);
        } catch (NamingException e) {
            LOG.warn("Failed JNDI lookup: ", e);
            return null;
        }
    }

    public static CallFactoryBean getCallFactory() {
        try {
            return (CallFactoryBean) new InitialContext().lookup(JNDI_CALLFACTORY);
        } catch (NamingException e) {
            LOG.warn("Failed JNDI lookup: ", e);
            return null;
        }
    }

    public static ConfigBean getConfigBean() {
        try {
            return (ConfigBean) new InitialContext().lookup(JNDI_CONFIGBEAN);
        } catch (NamingException e) {
            LOG.warn("Failed JNDI lookup: ", e);
            return null;
        }
    }

    public static ManagedScheduledExecutorService getManagedScheduledExecutorService() {
        try {
            return InitialContext.doLookup("java:comp/DefaultManagedScheduledExecutorService");
        } catch (NamingException e) {
            LOG.warn("Failed JNDI lookup: ", e);
            return null;
        }
    }

    // note: we wouldn't need this if app/module level lookups worked in the stack...
    // TODO: remove after fixing JNDI tree for SIP module
    private static String findAppName() {
        Supplier<RuntimeException> supplier = () -> new IllegalStateException(
                "Cannot find application name in JNDI tree!");
        try {
            return Collections
                    .list(((Context) new InitialContext().lookup("java:global")).list(""))
                    .stream()
                    .filter(nc -> nc.getName().matches("imscf-el-(\\d)+(_.*)?")
                            && nc.getClassName().equals("javax.naming.Context")).findFirst().orElseThrow(supplier)
                    .getName();
        } catch (NamingException e) {
            throw supplier.get();
        }
    }

    public static void list() throws NamingException {
        for (String prefix : Arrays.asList("", "java:comp", "java:module", "java:app", "java:global")) {
            LOG.trace("{}", prefix);
            Context c = null;
            try {
                c = (Context) new InitialContext().lookup(prefix);
            } catch (NamingException e) {
                LOG.trace("error looking up prefix: {}", prefix);
                continue;
            }
            list(c, "");
        }
    }

    public static void list(Context c, String indent) {
        List<NameClassPair> l;
        try {
            l = Collections.list(c.list(""));
        } catch (NamingException e) {
            LOG.trace("error in list(\"\")", e);
            return;
        }
        if (l.isEmpty()) {
            LOG.trace("{} <empty>", indent);
            return;
        }
        for (NameClassPair nc : l) {
            LOG.trace("{}Name: {}, class: {}", indent, nc.getName(), nc.getClassName());
            Object o;
            try {
                o = c.lookup(nc.getName());
            } catch (NamingException e) {
                LOG.trace("error in child lookup: {}", nc.getName());
                continue;
            }
            if (o instanceof Context) {
                list((Context) o, indent + "  ");
            }
        }
    }
}
