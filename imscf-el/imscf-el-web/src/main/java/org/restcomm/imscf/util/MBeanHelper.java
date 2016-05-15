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

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for MBean operations. */
public final class MBeanHelper {
    public static final String EL_MBEAN_DOMAIN = "org.restcomm.imscf.el";

    private static final Logger LOG = LoggerFactory.getLogger(MBeanHelper.class);

    private MBeanHelper() {
    }

    public static void registerMBean(Object o, String s) {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName on = new ObjectName(s);
            if (mbeanServer.isRegistered(on)) {
                try {
                    mbeanServer.unregisterMBean(on);
                } catch (Exception ex) {
                    LOG.warn("Exception while unregistering previous MBean instance with name {}",
                            on.getCanonicalName(), ex);
                }
            }
            mbeanServer.registerMBean(o, on);
        } catch (Exception ex) {
            LOG.warn("Exception while registering MBean with name {} ", s, ex);
        }
    }

    public static void unregisterMBean(String s) {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName on = new ObjectName(s);
            if (mbeanServer.isRegistered(on)) {
                mbeanServer.unregisterMBean(on);
            } else {
                LOG.warn("No MBean registered with name {}", s);
            }
        } catch (Exception ex) {
            LOG.warn("Unable to unregister MBean with name {}", s);
        }
    }
}
