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
package org.restcomm.imscf.el.statistics;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for one-shot setters.
 * @author Miklos Pocsaji
 *
 */
public abstract class StatisticsSetterBase {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsSetterBase.class);

    private CountDownLatch cdl;
    private String counterName;

    public StatisticsSetterBase() {
        cdl = new CountDownLatch(1);
    }

    protected void setCounterName(String counterName) {
        if (this.counterName != null) {
            LOG.error("Counters cannot be increased more than once in a one-shot setter!"
                    + " Please call the appropriate method of ElStatistics to get a new"
                    + " one-shot setter! Counter already increased: '{}', now got: '{}'", this.counterName, counterName);
            return;
        }
        this.counterName = counterName;
        ready();
    }

    public String getCounterName() {
        return counterName;
    }

    final void applyOnMBean(SlidingWindowStatisticsMBeanBase mbean) {
        mbean.incCounter(counterName);
    }

    final void waitForReady() throws InterruptedException {
        cdl.await();
    }

    /**
     * Waits for the API user to call one of the inc... methods.
     * @param timeout amount of time.
     * @param unit unit of parameter timeout.
     * @return true if the API user has called one of the inc.. methods. False, when the timeout elapsed.
     * @throws InterruptedException
     */
    final boolean waitForReady(long timeout, TimeUnit unit) throws InterruptedException {
        return cdl.await(timeout, unit);
    }

    private void ready() {
        cdl.countDown();
    }

    abstract String getMBeanName();

    abstract SlidingWindowStatisticsMBeanBase createNewMBeanInstance();

}
