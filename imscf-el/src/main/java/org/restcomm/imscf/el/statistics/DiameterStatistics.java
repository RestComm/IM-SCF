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

import org.restcomm.imscf.common.config.DiameterCounterThresholdNotificationType;
import org.restcomm.imscf.common.el.statistics.DiameterStatisticsMBean;
import java.util.List;

/**
 * MBean implementation for Diameter statistics for an alias.
 * @see SlidingWindowStatisticsMBeanBase
 *
 */
public class DiameterStatistics extends SlidingWindowStatisticsMBeanBase implements DiameterStatisticsMBean,
        DiameterStatisticsSetter {

    private String alias;
    private String moduleName;

    /**
     * The possible counters in this MBean.
     *
     */
    enum Counter {
        balanceQueryReceivedCount, balanceQueryAnsweredCount, debitQueryReceivedCount, debitQueryAnsweredCount
    }

    public DiameterStatistics(String alias, String moduleName, int windowSeconds,
            List<DiameterCounterThresholdNotificationType> notifications) {
        super(windowSeconds, SlidingWindowStatisticsMBeanBase.convertFromDiameterThresholdNotifications(notifications));
        this.alias = alias;
        this.moduleName = moduleName;
        for (Counter c : Counter.values()) {
            addCounter(c.toString());
        }
    }

    @Override
    protected String resolveNotificationVariable(String variable) {
        if ("serviceIdentifier".equals(variable)) {
            return alias;
        }
        return null;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public long getBalanceQueryReceivedCount() {
        return getCounter(Counter.balanceQueryReceivedCount.name());
    }

    @Override
    public long getBalanceQueryAnsweredCount() {
        return getCounter(Counter.balanceQueryAnsweredCount.name());
    }

    @Override
    public long getDebitQueryReceivedCount() {
        return getCounter(Counter.debitQueryReceivedCount.name());
    }

    @Override
    public long getDebitQueryAnsweredCount() {
        return getCounter(Counter.debitQueryAnsweredCount.name());
    }

    @Override
    public void incBalanceQueryReceivedCount() {
        incCounter(Counter.balanceQueryReceivedCount.name());
    }

    @Override
    public void incBalanceQueryAnsweredCount() {
        incCounter(Counter.balanceQueryAnsweredCount.name());
    }

    @Override
    public void incDebitQueryReceivedCount() {
        incCounter(Counter.debitQueryReceivedCount.name());
    }

    @Override
    public void incDebitQueryAnsweredCount() {
        incCounter(Counter.debitQueryAnsweredCount.name());
    }

}
