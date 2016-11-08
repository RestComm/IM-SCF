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
package org.restcomm.imscf.sl.statistics;

import org.restcomm.imscf.sl.config.ImscfSigtranStack;
import org.restcomm.imscf.sl.config.ConfigBean;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.mobicents.protocols.ss7.m3ua.As;
import org.mobicents.protocols.ss7.m3ua.Asp;
import org.mobicents.protocols.ss7.m3ua.AspFactory;
import org.mobicents.protocols.ss7.m3ua.M3UAManagementEventListener;
import org.mobicents.protocols.ss7.m3ua.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to maintain the MBean statistics of this SL instance.
 * @author Miklos Pocsaji
 *
 */
public final class SlStatistics {

    private static final Logger LOG = LoggerFactory.getLogger(SlStatistics.class);
    private static SlStatistics instance;
    private Map<String, M3uaAsp> m3uaAspMap;
    private Map<Integer, M3uaAs> m3uaAsPcMap;
    private Map<String, M3uaAs> m3uaAsNameMap;
    private M3uaManagementEventListener listener;

    private static final String AS_MBEAN_BASENAME = ConfigBean.SL_MBEAN_DOMAIN + ":type=M3uaAs";
    private static final String ASP_MBEAN_BASENAME = ConfigBean.SL_MBEAN_DOMAIN + ":type=M3uaAsp";

    private SlStatistics() {
        // empty constructor
    }

    /**
     * Initializes the statistics MBeans for this signaling layer server instance.
     * @param sigtranStack The initialized stack.
     */
    public static void initializeStatistics(ImscfSigtranStack sigtranStack) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        instance = new SlStatistics();
        instance.m3uaAsPcMap = new HashMap<Integer, M3uaAs>();
        instance.m3uaAsNameMap = new HashMap<String, M3uaAs>();
        instance.m3uaAspMap = new HashMap<String, M3uaAsp>();

        for (Entry<String, As[]> e : sigtranStack.getM3uaManagement().getRoute().entrySet()) {
            int pointCode = extractPointCodeFromKey(e.getKey());
            for (As as : e.getValue()) {
                LOG.info("route: {}, point code: {}, M3UA AS: {}", e.getKey(), pointCode, as);
                if (as == null)
                    continue;
                M3uaAs statAs = new M3uaAs(as.getName(), pointCode);
                statAs.setStatus(as.getState().getName());
                String asMBeanName = constructAsMBeanName(as.getName());
                instance.m3uaAsPcMap.put(pointCode, statAs);
                instance.m3uaAsNameMap.put(as.getName(), statAs);
                try {
                    ObjectName on = new ObjectName(asMBeanName);
                    try {
                        server.unregisterMBean(on);
                    } catch (Exception ex) {
                        LOG.trace("Unable to unregister MBean {}, it does not exist yet.", asMBeanName);
                    }
                    server.registerMBean(statAs, on);
                } catch (Exception ex) {
                    LOG.error("Error registering MBean for M3UA AS ({})", asMBeanName, ex);
                }

                for (Asp asp : as.getAspList()) {
                    M3uaAsp statAsp = new M3uaAsp(asp.getName(), asp.getAspFactory().getAssociation().getPeerAddress(),
                            asp.getAspFactory().getAssociation().getPeerPort());
                    statAsp.setStatus(asp.getState().getName());
                    String aspMBeanName = constructAspMBeanName(asp.getName());
                    instance.m3uaAspMap.put(statAsp.getName(), statAsp);
                    try {
                        ObjectName on = new ObjectName(aspMBeanName);
                        try {
                            server.unregisterMBean(on);
                        } catch (Exception ex) {
                            LOG.trace("Unable to unregister MBean {}, it does not exist yet.", aspMBeanName);
                        }
                        server.registerMBean(statAsp, on);
                    } catch (Exception ex) {
                        LOG.error("Error registering MBean M3UA ASP ({})", aspMBeanName, ex);
                    }
                }
            }
        }
        instance.listener = new M3uaManagementEventListener();
        sigtranStack.getM3uaManagement().addM3UAManagementEventListener(instance.listener);
    }

    public static void shutdownStatistics(ImscfSigtranStack sigtranStack) {
        sigtranStack.getM3uaManagement().removeM3UAManagementEventListener(instance.listener);
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        for (String s : instance.m3uaAsNameMap.keySet()) {
            try {
                ObjectName on = new ObjectName(constructAsMBeanName(s));
                LOG.info("Unregistering MBean: {}", s);
                server.unregisterMBean(on);
            } catch (Exception ex) {
                LOG.error("Error unregistering MBean ({})", s, ex);
            }
        }
        for (String s : instance.m3uaAspMap.keySet()) {
            try {
                ObjectName on = new ObjectName(constructAspMBeanName(s));
                LOG.info("Unregistering MBean: {}", s);
                server.unregisterMBean(on);
            } catch (Exception ex) {
                LOG.error("Error unregistering MBean ({})", s, ex);
            }
        }
        instance = null;
    }

    public static void incMessagesSentToPc(int pointCode) {
        M3uaAs m3uaAs = instance.m3uaAsPcMap.get(pointCode);
        if (m3uaAs != null) {
            m3uaAs.incMessagesSent();
        } else {
            LOG.warn("No MBean found for point code {}", pointCode);
        }
    }

    public static void incMessagesReceivedFromPc(int pointCode) {
        M3uaAs m3uaAs = instance.m3uaAsPcMap.get(pointCode);
        if (m3uaAs != null) {
            m3uaAs.incMessagesReceived();
        } else {
            LOG.warn("No MBean found for point code {}", pointCode);
        }
    }

    private static String constructAsMBeanName(String asName) {
        return AS_MBEAN_BASENAME + ",name=" + asName;
    }

    private static String constructAspMBeanName(String aspName) {
        return ASP_MBEAN_BASENAME + ",name=" + aspName;
    }

    /**
     * Extracts the destination point code from a jSS7 stack key.
     * M3UA stack hashses the application servers with a key in the following form:
     * <destination pc>:<source pc>
     * @param key The key to extract from
     * @return The integer value before the colon
     */
    private static int extractPointCodeFromKey(String key) {
        return Integer.parseInt(key.substring(0, key.indexOf(":")));
    }

    /**
     * Listener which is notified when an M3UA ASP/AS changes.
     * @author Miklos Pocsaji
     *
     */
    private static class M3uaManagementEventListener implements M3UAManagementEventListener {
        @Override
        public void onAsActive(As as, State state) {
            LOG.info("onAsActive({}, {})", as, state);
            instance.m3uaAsNameMap.get(as.getName()).setStatus(as.getState().getName());
        }

        @Override
        public void onAsCreated(As as) {
            LOG.info("onAsCreated({})", as);
            instance.m3uaAsNameMap.get(as.getName()).setStatus(as.getState().getName());
        }

        @Override
        public void onAsDestroyed(As as) {
            LOG.info("onAsDestroyed({})", as);
            instance.m3uaAsNameMap.get(as.getName()).setStatus(as.getState().getName());
        }

        @Override
        public void onAsDown(As as, State state) {
            LOG.info("onAsDown({}, {})", as, state);
            instance.m3uaAsNameMap.get(as.getName()).setStatus(as.getState().getName());
        }

        @Override
        public void onAsInactive(As as, State state) {
            LOG.info("onAsInactive({}, {})", as, state);
            instance.m3uaAsNameMap.get(as.getName()).setStatus(as.getState().getName());
        }

        @Override
        public void onAsPending(As as, State state) {
            LOG.info("onAsPending({}, {})", as, state);
            instance.m3uaAsNameMap.get(as.getName()).setStatus(as.getState().getName());
        }

        @Override
        public void onAspActive(Asp asp, State state) {
            LOG.info("onAspActive({}, {})", asp, state);
            instance.m3uaAspMap.get(asp.getName()).setStatus(asp.getState().getName());
        }

        @Override
        public void onAspAssignedToAs(As as, Asp asp) {
            LOG.info("onAspAssignedToAs({}, {})", as, asp);
        }

        @Override
        public void onAspDown(Asp asp, State state) {
            LOG.info("onAspDown({}, {})", asp, state);
            instance.m3uaAspMap.get(asp.getName()).setStatus(asp.getState().getName());
        }

        @Override
        public void onAspFactoryCreated(AspFactory aspFactory) {
            LOG.info("onAspFactoryCreated({})", aspFactory);
        }

        @Override
        public void onAspFactoryDestroyed(AspFactory aspFactory) {
            LOG.info("onAspFactoryDestroyed({})", aspFactory);
        }

        @Override
        public void onAspFactoryStarted(AspFactory aspFactory) {
            LOG.info("onAspFactoryStarted({})", aspFactory);
        }

        @Override
        public void onAspFactoryStopped(AspFactory aspFactory) {
            LOG.info("onAspFactoryStopped({})", aspFactory);
        }

        @Override
        public void onAspInactive(Asp asp, State state) {
            LOG.info("onAspInactive({}, {})", asp, state);
            instance.m3uaAspMap.get(asp.getName()).setStatus(asp.getState().getName());
        }

        @Override
        public void onAspUnassignedFromAs(As as, Asp asp) {
            LOG.info("onAspUnassignedFromAs({}, {})", as, asp);
        }

        @Override
        public void onRemoveAllResources() {
            LOG.info("onRemoveAllResources()");
        }

        @Override
        public void onServiceStarted() {
            LOG.info("onServiceStarted()");
        }

        @Override
        public void onServiceStopped() {
            LOG.info("onServiceStopped()");
        }

    }
}
