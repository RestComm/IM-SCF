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
package org.restcomm.imscf.sl.mgmt.sctp;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.api.ManagementEventListener;
import org.mobicents.protocols.api.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LinkManager implements an MXBean interface which allows to manage SCTP association through JMX.
 * It can manage multiple SCTP stack but the name of the associations must be unique.
 * Management can be registered before MXBean registration.
 * NOTE: using simple reentrantLock for thread safety (performance is not an issue).
 */
@SuppressWarnings("PMD.GodClass")
public final class SctpLinkManager implements SctpLinkManagerMXBean {

    private static final Logger LOG = LoggerFactory.getLogger(SctpLinkManager.class);
    private static SctpLinkManager instance;
    static final String UP_STATUS = "[UP]";
    static final String DOWN_STATUS = "[DOWN]";
    static final String STARTED_STATUS = "[STARTED]";
    static final String STOPPED_STATUS = "[STOPPED]";

    private ArrayList<Management> pendingRegistrations;
    private HashMap<String, SctpLink> links;
    private HashMap<Management, SctpManagementListener> listeners;
    private String sctpLinkManagerMBeanName;
    private String sctpLinkStatusMBeanName;
    private boolean registered;
    private final Lock registerLock = new ReentrantLock();

    public void stopManagements(Predicate<Management> tester) {
        for (Management m: listeners.keySet()) {
            if (tester.test(m)) {
                try {
                    LOG.debug("Trying to stop SCTP management: {}", m.getName());
                    m.stop();
                    LOG.debug("SCTP management: {} is stopped", m.getName());
                } catch (Exception e) {
                    LOG.warn("Error while stopping SCTP management: {} - {}", m.getName(), e.getMessage());
                }
            }
        }
    }

    static String getAssociationManagmentStatus(Association association) {
        if (association == null) {
            return "";
        }
        return association.isStarted() ? STARTED_STATUS : STOPPED_STATUS;
    }

    static String getAssociationLinkStatus(Association association) {
        if (association == null) {
            return "";
        }
        return association.isUp() ? UP_STATUS : DOWN_STATUS;
    }

    static String getAssociationStatus(Association association) {
        if (association == null) {
            return "";
        }
        return getAssociationManagmentStatus(association) + getAssociationLinkStatus(association);
    }

    public static synchronized SctpLinkManager getInstance() {
        if (instance == null) {
            instance = new SctpLinkManager();
        }
        return instance;
    }

    private SctpLinkManager() {
        this.registered = false;
        this.pendingRegistrations = new ArrayList<Management>();
        this.links = new HashMap<String, SctpLink>();
        this.listeners = new HashMap<Management, SctpManagementListener>();
    }

    public void registerManagement(Management sctpManagement) {
        if (sctpManagement == null) {
            return;
        }
        registerLock.lock();
        try {
            if (!registered) {
                this.pendingRegistrations.add(sctpManagement);
                return;
            }
            SctpManagementListener listener = new SctpManagementListener(sctpManagement);
            listeners.put(sctpManagement, listener);
            sctpManagement.addManagementEventListener(listener);
            Map<String, Association> assocs = sctpManagement.getAssociations();
            if (assocs != null && !assocs.isEmpty()) {
                for (Association association : assocs.values()) {
                    addLink(new SctpLink(sctpManagement, association));
                }
            }
        } finally {
            registerLock.unlock();
        }
    }

    public void unregisterManagement(Management sctpManagement) {
        if (sctpManagement == null) {
            return;
        }
        registerLock.lock();
        try {
            this.pendingRegistrations.remove(sctpManagement);
            sctpManagement.removeManagementEventListener(listeners.get(sctpManagement));
            for (SctpLink link : links.values()) {
                if (link.getManagement() == sctpManagement) {
                    removeLink(link.getName(), sctpManagement);
                }
            }
            listeners.remove(sctpManagement);
        } finally {
            registerLock.unlock();
        }
    }

    public boolean registerLinkManager(String sctpLinkManagerMBeanName, String sctpLinkStatusMBeanName) {
        registerLock.lock();
        try {

            this.sctpLinkManagerMBeanName = sctpLinkManagerMBeanName;
            this.sctpLinkStatusMBeanName = sctpLinkStatusMBeanName;
            this.registered = false;

            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            try {
                ObjectName on = new ObjectName(sctpLinkManagerMBeanName);
                try {
                    server.unregisterMBean(on);
                } catch (Exception ex) {
                    LOG.trace("Unable to unregister MBean {}, it does not exist yet.", sctpLinkManagerMBeanName);
                }
                server.registerMBean(this, on);
                this.registered = true;
            } catch (Exception ex) {
                LOG.error("Error registering MBean ({})", sctpLinkManagerMBeanName, ex);
                return registered;
            }
            for (SctpLink link : links.values()) {
                registerLinkStatusBean(link);
            }
            for (Management m : pendingRegistrations) {
                registerManagement(m);
            }
            return registered;
        } finally {
            registerLock.unlock();
        }
    }

    public void unregisterLinkManager() {
        registerLock.lock();
        try {
            if (!registered) {
                return;
            }
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            try {
                ObjectName on = new ObjectName(sctpLinkManagerMBeanName);
                server.unregisterMBean(on);
            } catch (Exception ex) {
                LOG.trace("Unable to unregister MBean {}, it does not exist yet.", sctpLinkManagerMBeanName);
            }
            for (SctpLinkStatus link : links.values()) {
                unregisterLinkStatusBean(link);
            }
        } finally {
            registerLock.unlock();
        }
    }

    private void registerLinkStatusBean(SctpLinkStatus link) {
        registerLock.lock();
        try {
            if (!registered || link == null) {
                return;
            }
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            String mbeanName = this.sctpLinkStatusMBeanName + ",name=" + link.getName();
            try {
                ObjectName on = new ObjectName(mbeanName);
                try {
                    server.unregisterMBean(on);
                } catch (Exception ex) {
                    LOG.trace("Unable to unregister MBean {}, it does not exist yet.", mbeanName);
                }
                server.registerMBean(link, on);
            } catch (Exception ex) {
                LOG.error("Error registering MBean ({})", mbeanName, ex);
            }
        } finally {
            registerLock.unlock();
        }
    }

    private void unregisterLinkStatusBean(SctpLinkStatus link) {
        registerLock.lock();
        try {
            if (!registered || link == null) {
                return;
            }
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            String mbeanName = this.sctpLinkManagerMBeanName + ",name=" + link.getName();
            try {
                ObjectName on = new ObjectName(mbeanName);
                try {
                    server.unregisterMBean(on);
                } catch (Exception ex) {
                    LOG.trace("Unable to unregister MBean {}, it does not exist yet.", mbeanName);
                }
            } catch (Exception ex) {
                LOG.error("Error registering MBean ({})", mbeanName, ex);
            }
        } finally {
            registerLock.unlock();
        }
    };

    public List<String> getAssociations() {
        registerLock.lock();
        try {
            if (links.isEmpty()) {
                return new ArrayList<String>();
            }
            ArrayList<String> ret = new ArrayList<String>();
            links.values().forEach(i -> ret.add(i.getName() + " - " + getAssociationStatus(i.getAssociation())));
            ret.sort(null);
            return ret;
        } finally {
            registerLock.unlock();
        }
    }

    @Override
    public void stopAssociation(String name) {
        registerLock.lock();
        try {
            SctpLink link = links.get(name);
            if (link == null) {
                LOG.warn("Can not find link by name=[{}]", name);
                return;
            }
            try {
                link.stop();
            } catch (Throwable t) {
                LOG.warn("Can not stop association name=[{}]: {}", name, t.getMessage());
            }
        } finally {
            registerLock.unlock();
        }
    }

    @Override
    public void startAssociation(String name) {
        registerLock.lock();
        try {
            SctpLink link = links.get(name);
            if (link == null) {
                LOG.warn("Can not find link by name=[{}]", name);
                return;
            }
            try {
                link.start();
            } catch (Throwable t) {
                LOG.warn("Can not start association name=[{}]: {}", name, t.getMessage());
            }
        } finally {
            registerLock.unlock();
        }
    }

    private SctpLink getLink(String linkName, Management management) {
        registerLock.lock();
        try {
            SctpLink link = links.get(linkName);
            if (link != null && link.getManagement() == management) {
                return link;
            }
            return null;
        } finally {
            registerLock.unlock();
        }
    }

    private boolean addLink(SctpLink link) {
        registerLock.lock();
        try {
            if (!links.containsKey(link.getName())) {
                links.put(link.getName(), link);
                registerLinkStatusBean(link);
                LOG.info("Link={} is successfully registered to LinkManager", link);
                return true;
            }
            LOG.info("Link={} is failed to register to LinkManager", link);
            return false;
        } finally {
            registerLock.unlock();
        }
    }

    private boolean removeLink(String linkName, Management management) {
        registerLock.lock();
        try {
            SctpLink linkToDelete = getLink(linkName, management);
            if (linkToDelete != null) {
                unregisterLinkStatusBean(linkToDelete);
                links.remove(linkToDelete.getName());
                LOG.info("Link={} is successfully unregistered from LinkManager", linkToDelete);
                return true;
            }
            LOG.info("Link={} is failed to unregister from LinkManager", linkToDelete);
            return false;
        } finally {
            registerLock.unlock();
        }
    }

    private void reattachLink(SctpLink link, Association association) {
        if (link != null) {
            link.updateAssociation(association);
        }
    }

    /**
     * SCTP ManagementEvent listener implementation.
     * Logs ManagementEvent and sends notification of association level events.
     *
     */
    class SctpManagementListener implements ManagementEventListener {

        private final Management management;

        public SctpManagementListener(Management management) {
            this.management = management;
        }


        @Override
        public void onAssociationAdded(Association association) {
            LOG.info("onAssociationAdded association={}", association);
            //add or reattach a link to the association
            SctpLink link = getLink(association.getName(), management);
            if (link == null) {
                LOG.debug("Adding new SctpLink for assoctiation={} to LinkManager", association);
                addLink(new SctpLink(management, association));
            } else {
                LOG.debug("Reattaching existing SctpLink for association={}", association);
                reattachLink(link, association);
            }
        }

        @Override
        public void onAssociationDown(Association association) {
            LOG.info("onAssociationDown association={}", association);
            SctpLinkStatus link = getLink(association.getName(), management);
            if (link != null) {
                link.onAssociationDown(association);
            }
        }

        @Override
        public void onAssociationRemoved(Association association) {
            LOG.info("onAssociationRemoved association={}", association);
            //we don't remove links
            //removeLink(association.getName(), management);
        }

        @Override
        public void onAssociationStarted(Association association) {
            LOG.info("onAssociationStarted association={}", association);
            SctpLinkStatus link = getLink(association.getName(), management);
            if (link != null) {
                link.onAssociationStarted(association);
            }
        }

        @Override
        public void onAssociationStopped(Association association) {
            LOG.info("onAssociationStopped association={}", association);
            SctpLinkStatus link = getLink(association.getName(), management);
            if (link != null) {
                link.onAssociationStopped(association);
            }
        }

        @Override
        public void onAssociationUp(Association association) {
            LOG.info("onAssociationUp association={}", association);
            SctpLinkStatus link = getLink(association.getName(), management);
            if (link != null) {
                link.onAssociationUp(association);
            }
        }

        @Override
        public void onRemoveAllResources() {
            LOG.info("onRemoveAllResources");
        }

        @Override
        public void onServerAdded(Server server) {
            LOG.info("onServerRemoved server={}", server);
        }

        @Override
        public void onServerRemoved(Server server) {
            LOG.info("onServerRemoved server={}", server);
        }

        @Override
        public void onServiceStarted() {
            LOG.info("onServiceStarted");
        }

        @Override
        public void onServiceStopped() {
            LOG.info("onServiceStopped");
        }
    }
}
