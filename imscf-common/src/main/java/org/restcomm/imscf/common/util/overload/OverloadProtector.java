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
package org.restcomm.imscf.common.util.overload;

import org.restcomm.imscf.common.util.overload.OverloadProtectorParameters.NonHeapOverloadCheckPolicy;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overload protector service.
 * It is a singleton instance, the init() method must be called prior calling getInstance().
 * <b>The implementation works only for Java8 with G1 garbage collector!</b>
 * @author Miklos Pocsaji
 *
 */
public final class OverloadProtector {
    private static OverloadProtector instance;
    private static final Logger LOGGER = LoggerFactory.getLogger(OverloadProtector.class);

    private OverloadProtectorParameters parameters;
    private OverloadState currentState;
    private List<OverloadListener> listeners;

    private volatile boolean quit = false;
    private volatile Object semaphor = new Object();

    private OverloadProtector() {

    }

    public static synchronized void init(OverloadProtectorParameters parameters) {
        LOGGER.info("OverloadProtector.init() BEGIN");
        if (instance != null) {
            LOGGER.info("There is an active instance, shut down first.");
            shutdown();
        }
        OverloadProtector tmpInstance = new OverloadProtector();
        tmpInstance.currentState = parameters.getNonHeapOverloadCheckPolicy() == NonHeapOverloadCheckPolicy.PERCENT ? OverloadState
                .createOverloadStateWithNonHeapPercent(0, false, 0, false, 0, false) : OverloadState
                .createOverloadStateWithNonHeapAmount(0, false, 0, false, 0, false);
        tmpInstance.parameters = parameters.copy();
        tmpInstance.listeners = new ArrayList<>();
        tmpInstance.new OverloadProtectorThread().start();

        instance = tmpInstance;
        LOGGER.info("OverloadProtector.init() END");
    }

    public static OverloadProtector getInstance() {
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public OverloadState getCurrentState() {
        return currentState;
    }

    public void addListener(OverloadListener listener) {
        listeners.add(listener);
        LOGGER.debug("Added overload listener {}, listeners are: {}", listener, listeners);
    }

    public boolean removeListener(OverloadListener listener) {
        boolean ret = listeners.remove(listener);
        LOGGER.debug("Removed overload listener {}, listeners are: {}", listener, listeners);
        return ret;
    }

    public static synchronized void shutdown() {
        LOGGER.info("OverloadProtector.shutdown() BEGIN");
        OverloadProtector oldInstance = instance;
        instance = null;
        oldInstance.quit = true;
        synchronized (oldInstance.semaphor) {
            oldInstance.semaphor.notifyAll();
        }
        LOGGER.info("OverloadProtector.shutdown() END");
    }

    /**
     * The thread which checks if the system is overloaded and notifies the listener of the events.
     */
    private final class OverloadProtectorThread extends Thread {

        private static final String HEAP_MEMORY_POOL_NAME = "G1 Old Gen";
        private static final String NONHEAP_MEMORY_POOL_NAME = "Metaspace";
        private static final String OPERATING_SYSTEM_MBEAN_NAME = "java.lang:type=OperatingSystem";
        private static final String SYSTEM_CPU_LOAD_ATTRIBUTE_NAME = "SystemCpuLoad";

        private List<Integer> cpuUsageHistory;
        private int cpuUsageHistoryIndex;
        private MemoryPoolMXBean heapMemoryBean;
        private MemoryPoolMXBean nonHeapMemoryBean;

        public OverloadProtectorThread() {
            super("Overload Protector Thread");
            cpuUsageHistory = new ArrayList<>();
            for (int i = 0; i < parameters.getCpuMeasurementWindow(); i++)
                cpuUsageHistory.add(0);
            for (MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
                if (bean.getName().equals(HEAP_MEMORY_POOL_NAME)) {
                    heapMemoryBean = bean;
                } else if (bean.getName().equals(NONHEAP_MEMORY_POOL_NAME)) {
                    nonHeapMemoryBean = bean;
                }
            }
            LOGGER.debug("heapMemoryBean found: {}", heapMemoryBean);
            LOGGER.debug("nonHeapMemoryBean found: {}", nonHeapMemoryBean);
            if (heapMemoryBean == null) {
                LOGGER.error("Cannot find MemoryPoolMXBean with name '{}'. Heap overload checking will be disabled.",
                        HEAP_MEMORY_POOL_NAME);
            }
            if (nonHeapMemoryBean == null) {
                LOGGER.warn(
                        "Cannot find MemoryPoolMXBean with name '{}'. Non-heap (metaspace) overload checking will be disabled.",
                        NONHEAP_MEMORY_POOL_NAME);
            }
        }

        @Override
        public void run() {
            while (!quit) {
                synchronized (semaphor) {
                    try {
                        semaphor.wait(parameters.getDataCollectionPeriodSec() * 1000);
                    } catch (InterruptedException e) {
                        LOGGER.warn("OverloadProtectorThread wait() interrupted");
                    }
                    if (quit)
                        break;
                    int cpuPercent = updateAndAverageCpuPercent();
                    int heapPercent = getHeapUsagePercent();
                    boolean cpuOverloaded = cpuPercent >= parameters.getCpuOverloadThresholdPercent();
                    boolean heapOverloaded = heapPercent >= parameters.getHeapOverloadThresholdPercent();
                    OverloadState newState;
                    if (parameters.getNonHeapOverloadCheckPolicy() == NonHeapOverloadCheckPolicy.PERCENT) {
                        int nonHeapPercent = getNonHeapUsagePercent();
                        boolean nonHeapOverloaded = nonHeapPercent >= parameters.getNonHeapOverloadThresholdPercent();
                        newState = OverloadState.createOverloadStateWithNonHeapPercent(cpuPercent, cpuOverloaded,
                                heapPercent, heapOverloaded, nonHeapPercent, nonHeapOverloaded);
                        LOGGER.trace("Checking overloaded state. CPU: {}%, heap: {}%, non-heap (metaspace): {}%",
                                cpuPercent, heapPercent, nonHeapPercent);
                        LOGGER.trace("Thresholds: CPU: {}%, heap: {}%, non-heap (metaspace): {}%",
                                parameters.getCpuOverloadThresholdPercent(),
                                parameters.getHeapOverloadThresholdPercent(),
                                parameters.getNonHeapOverloadThresholdPercent());
                    } else {
                        int nonHeapAmount = getNonHeapUsageAmountMegabytes();
                        boolean nonHeapOverloaded = nonHeapAmount >= parameters.getNonHeapOverloadThresholdAmount();
                        newState = OverloadState.createOverloadStateWithNonHeapAmount(cpuPercent, cpuOverloaded,
                                heapPercent, heapOverloaded, nonHeapAmount, nonHeapOverloaded);
                        LOGGER.trace("Checking overloaded state. CPU: {}%, heap: {}%, non-heap (metaspace): {}MB",
                                cpuPercent, heapPercent, nonHeapAmount);
                        LOGGER.trace("Thresholds: CPU: {}%, heap: {}%, non-heap (metaspace): {}MB",
                                parameters.getCpuOverloadThresholdPercent(),
                                parameters.getHeapOverloadThresholdPercent(),
                                parameters.getNonHeapOverloadThresholdAmount());
                    }
                    LOGGER.trace("Actual overloaded state: {}", currentState);
                    LOGGER.trace("New overloaded state: {}", newState);
                    if (!newState.equals(currentState)) {
                        LOGGER.info("System overload state changed. New state: {}", newState);
                        OverloadState oldState = currentState;
                        currentState = newState;
                        for (OverloadListener listener : listeners) {
                            listener.overloadStateChanged(oldState, newState);
                        }
                    }
                }
            }
            LOGGER.info("OverloadProtectorThread exits.");
        }

        private int getCpuPercentage() {
            MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            Object o = null;
            try {
                ObjectName on = new ObjectName(OPERATING_SYSTEM_MBEAN_NAME);
                o = mbeanServer.getAttribute(on, SYSTEM_CPU_LOAD_ATTRIBUTE_NAME);
            } catch (Exception e) {
                LOGGER.warn("Error getting attribute of operating system MBean", e);
                return 0;
            }
            if (o instanceof Number) {
                return (int) Math.round(((Number) o).floatValue() * 100.0);
            } else {
                LOGGER.warn("CPU Load from attribute '{}' of MBean {} is not a number: '{}'",
                        SYSTEM_CPU_LOAD_ATTRIBUTE_NAME, OPERATING_SYSTEM_MBEAN_NAME, o);
                return 0;
            }
        }

        private int getHeapUsagePercent() {
            if (heapMemoryBean != null) {
                LOGGER.trace(
                        "heapMemoryBean.getCollectionUsage().getUsed(): {} - heapMemoryBean.getCollectionUsage().getMax(): {}",
                        heapMemoryBean.getCollectionUsage().getUsed(), heapMemoryBean.getCollectionUsage().getMax());
                LOGGER.trace("heapMemoryBean.getUsage().getUsed(): {} - heapMemoryBean.getUsage().getMax(): {}",
                        heapMemoryBean.getUsage().getUsed(), heapMemoryBean.getUsage().getMax());
                return (int) Math.round((double) heapMemoryBean.getUsage().getUsed()
                        / (double) heapMemoryBean.getUsage().getMax() * 100.0d);
            } else {
                return 0;
            }
        }

        private int getNonHeapUsagePercent() {
            if (nonHeapMemoryBean != null) {
                LOGGER.trace("nonHeapMemoryBean.getUsage().getUsed(): {} - nonHeapMemoryBean.getUsage().getMax(): {}",
                        nonHeapMemoryBean.getUsage().getUsed(), nonHeapMemoryBean.getUsage().getMax());
                return (int) Math.round((double) nonHeapMemoryBean.getUsage().getUsed()
                        / (double) nonHeapMemoryBean.getUsage().getMax() * 100d);
            } else {
                return 0;
            }
        }

        private int getNonHeapUsageAmountMegabytes() {
            if (nonHeapMemoryBean != null) {
                return (int) (nonHeapMemoryBean.getUsage().getUsed() / (1024 * 1024));
            } else {
                return 0;
            }
        }

        private int updateAndAverageCpuPercent() {
            int currentCpu = getCpuPercentage();
            cpuUsageHistory.set(cpuUsageHistoryIndex++ % parameters.getCpuMeasurementWindow(), currentCpu);
            return (int) Math.round(cpuUsageHistory.stream().mapToInt(i -> i).average().getAsDouble());
        }
    }

}
