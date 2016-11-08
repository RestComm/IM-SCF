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
package org.restcomm.imscf.el.statistics;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.DiameterCounterThresholdNotificationType;
import org.restcomm.imscf.common.config.MapCounterThresholdNotificationType;
import org.restcomm.imscf.common.config.ServiceKeyCounterThresholdNotificationType;
import org.restcomm.imscf.common.config.SipApplicationServerGroupType;
import org.restcomm.imscf.common.config.SipApplicationServerType;
import org.restcomm.imscf.util.MBeanHelper;

/**
 * Helper class for execution layer server statistics.
 * @author Miklos Pocsaji
 *
 */
public final class ElStatistics {

    private static final Logger LOG = LoggerFactory.getLogger(ElStatistics.class);

    private static volatile ElStatistics instance;

    /** SIP AS groupName --> (SIP AS instanceName --> statistics MBean). */
    private Map<String, Map<String, SipAs>> sipAsMap;

    private StatisticsThread statisticsThread;

    private int windowSeconds;

    private List<ServiceKeyCounterThresholdNotificationType> serviceKeyNotifications;
    private List<MapCounterThresholdNotificationType> mapNotifications;
    private List<DiameterCounterThresholdNotificationType> diameterNotifications;

    private ElStatistics() {
        // empty constructor
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static void initializeStatistics(ImscfConfigType imscfConfig) {
        if (instance != null) {
            LOG.warn("Statistics subsystem seems to be initialized. Trying to shut down first...");
            shutdownStatistics();
        }
        LOG.info("Initializing EL statistics...");
        ElStatistics tmpInstance = new ElStatistics();
        tmpInstance.sipAsMap = new HashMap<>();
        tmpInstance.windowSeconds = imscfConfig.getStatistics().getStatisticsTimeWindowSec();
        tmpInstance.serviceKeyNotifications = imscfConfig.getNotificationConfiguration()
                .getServiceKeyCounterThresholdNotifications();
        tmpInstance.mapNotifications = imscfConfig.getNotificationConfiguration().getMapCounterThresholdNotifications();
        tmpInstance.diameterNotifications = imscfConfig.getNotificationConfiguration()
                .getDiameterCounterThresholdNotifications();
        LOG.info("Service key notifications: {}", tmpInstance.serviceKeyNotifications);
        LOG.info("MAP notifications: {}", tmpInstance.mapNotifications);
        LOG.info("Diameter notifications: {}", tmpInstance.diameterNotifications);

        for (SipApplicationServerGroupType sag : Optional.ofNullable(imscfConfig.getSipApplicationServers())
                .map(s -> s.getSipApplicationServerGroups()).orElse(Collections.emptyList())) {
            String sagName = sag.getName();
            tmpInstance.sipAsMap.put(sagName, new HashMap<>());

            for (SipApplicationServerType sas : sag.getSipApplicationServer()) {
                SipAs sipAs = new SipAs(sas.getName(), sagName, sas.getHost(), sas.getPort(), sas.isHeartbeatEnabled());
                tmpInstance.sipAsMap.get(sagName).put(sas.getName(), sipAs);
                String s = constructSipAsMBeanName(sas.getName(), sagName);
                LOG.info("Registering MBean {}", s);
                MBeanHelper.registerMBean(sipAs, s);
            }
        }

        LOG.info("Starting statistics thread...");
        tmpInstance.statisticsThread = new StatisticsThread();
        tmpInstance.statisticsThread.start();
        LOG.info("EL statistics initialized.");
        instance = tmpInstance;
    }

    public static void shutdownStatistics() {
        LOG.info("Shutting down EL statistics...");
        if (instance == null) {
            LOG.error("ElStatistics is not initialized");
            return;
        }

        instance.sipAsMap.values().stream().flatMap(gMap -> gMap.values().stream()).forEach(sipAs -> {
            String s = constructSipAsMBeanName(sipAs.getName(), sipAs.getGroupName());
            MBeanHelper.unregisterMBean(s);
        });

        instance.statisticsThread.quit = true;
        try {
            // Wait at most twice as much the thread is sleeping
            boolean success = instance.statisticsThread.shutdownCdl.await(StatisticsThread.WAIT_MS * 2,
                    TimeUnit.MILLISECONDS);
            if (!success) {
                LOG.warn("Shutting down StatisticsThread timed out. (Waited {} ms.)", StatisticsThread.WAIT_MS * 2);
            }
        } catch (InterruptedException e) {
            LOG.error("Error while waiting for StatisticsThread to shut down.", e);
        }

        instance = null;
        LOG.info("EL statistics shut down.");
    }

    public static SipAs getSipAs(String groupName, String asName) {
        if (instance == null) {
            LOG.error("ElStatistics is not initialized");
            return null;
        }

        return instance.sipAsMap.getOrDefault(groupName, Collections.emptyMap()).get(asName);
    }

    public static void setSipAsReachable(String groupName, String asName, boolean reachable) {
        SipAs sipAs = getSipAs(groupName, asName);
        if (sipAs != null) {
            sipAs.setReachable(reachable);
        }
    }

    public static MapStatisticsSetter createOneShotMapStatisticsSetter(String alias) {
        if (instance == null) {
            LOG.error("ElStatistics is not initialized");
            return null;
        }
        MapStatisticsSetterImpl ret = new MapStatisticsSetterImpl(alias, instance.windowSeconds,
                instance.mapNotifications);
        instance.statisticsThread.setterQueue.add(ret);
        return ret;
    }

    public static ServiceKeyStatisticsSetter createOneShotServiceKeyStatisticsSetter(String serviceIdentifier) {
        if (instance == null) {
            LOG.error("ElStatistics is not initialized");
            return null;
        }
        ServiceKeyStatisticsSetterImpl ret = new ServiceKeyStatisticsSetterImpl(serviceIdentifier,
                instance.windowSeconds, instance.serviceKeyNotifications);
        instance.statisticsThread.setterQueue.add(ret);
        return ret;
    }

    public static DiameterStatisticsSetter createOneShotDiameterStatisticsSetter(String alias, String moduleName) {
        if (instance == null) {
            LOG.error("ElStatistics is not initialized");
            return null;
        }
        DiameterStatisticsSetterImpl ret = new DiameterStatisticsSetterImpl(alias, moduleName, instance.windowSeconds,
                instance.diameterNotifications);
        instance.statisticsThread.setterQueue.add(ret);
        return ret;
    }

    private static String constructSipAsMBeanName(String asName, String asGroupName) {
        return MBeanHelper.EL_MBEAN_DOMAIN + ":type=SipAs,group=" + asGroupName + ",name=" + asName;
    }

    /**
     * The thread which actually increments the counters, periodically does a cleanup and checks for notifications
     * Registers the MBeans if needed.
     * Only one instance of this thread should be running.
     * @author Miklos Pocsaji
     *
     */
    private static class StatisticsThread extends Thread {
        private volatile boolean quit;
        private BlockingQueue<StatisticsSetterBase> setterQueue = new LinkedBlockingQueue<>();
        private Map<String, SlidingWindowStatisticsMBeanBase> mbeanMap = new HashMap<String, SlidingWindowStatisticsMBeanBase>();
        private CountDownLatch shutdownCdl = new CountDownLatch(1);
        private long lastCleanupTimestamp = System.currentTimeMillis();
        private long lastCheckNotificationsTimestamp = System.currentTimeMillis();

        private static final int WAIT_MS = 1000;
        private static final int CLEANUP_FREQUENCY_MS = 30000;
        private static final int CHECK_NOTIFICATIONS_FREQUENCY_MS = 10000;

        public StatisticsThread() {
            super("IMSCF EL statistics thread");
        }

        @Override
        public void run() {
            while (!quit) {
                StatisticsSetterBase ssb = null;
                try {
                    ssb = setterQueue.poll(WAIT_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    LOG.warn("Wait interrupted, shutting down.", e);
                    quit = true;
                    break;
                }
                long now = System.currentTimeMillis();
                if (lastCleanupTimestamp + CLEANUP_FREQUENCY_MS < now) {
                    mbeanMap.values().stream().forEach(bean -> bean.cleanupCounters());
                    lastCleanupTimestamp = now;
                }
                if (lastCheckNotificationsTimestamp + CHECK_NOTIFICATIONS_FREQUENCY_MS < now) {
                    mbeanMap.values().stream().forEach(bean -> bean.checkCountersAndSendNotifications());
                    lastCheckNotificationsTimestamp = now;
                }
                if (ssb == null)
                    continue;
                // Wait for the "user" to call one of the inc.. methods on the setter
                try {
                    boolean b = ssb.waitForReady(WAIT_MS, TimeUnit.MILLISECONDS);
                    if (!b) {
                        // timeout occured
                        LOG.error(
                                "No call to one of the incXXX() methods has been issued after {} milliseconds of getting the one-shot setter! This counter increase will not be done: {}",
                                WAIT_MS, ssb.getCounterName());
                        continue;
                    }
                } catch (InterruptedException e) {
                    LOG.warn("Wait interrupted, shutting down.", e);
                    quit = true;
                    break;
                }
                // At this time we have the info which counter should be increased.
                String mbeanName = ssb.getMBeanName();
                SlidingWindowStatisticsMBeanBase theMBean = mbeanMap.get(mbeanName);
                if (theMBean == null) {
                    // The MBean is not yet registered
                    LOG.info("Creating new MBean: {}", mbeanName);
                    theMBean = ssb.createNewMBeanInstance();
                    MBeanHelper.registerMBean(theMBean, mbeanName);
                    mbeanMap.put(mbeanName, theMBean);
                }
                ssb.applyOnMBean(theMBean);
            }
            LOG.info("ElStatistics.StatisticsThread loop ended. Unregistering MBeans");
            for (String name : mbeanMap.keySet()) {
                MBeanHelper.unregisterMBean(name);
            }
            shutdownCdl.countDown();
        }
    }
}
