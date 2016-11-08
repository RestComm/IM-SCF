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

import java.util.List;

import org.restcomm.imscf.common.config.DiameterCounterThresholdNotificationType;
import org.restcomm.imscf.util.MBeanHelper;

/**
 * One-shot setter implementation for diameter statistics.
 *
 */
public class DiameterStatisticsSetterImpl extends StatisticsSetterBase implements DiameterStatisticsSetter {

    private static final String MBEAN_BASE_NAME = MBeanHelper.EL_MBEAN_DOMAIN + ":type=DiameterStatistics";
    private String alias;
    private String moduleName;
    private int windowSeconds;
    private List<DiameterCounterThresholdNotificationType> notifications;

    public DiameterStatisticsSetterImpl(String alias, String moduleName, int windowSeconds,
            List<DiameterCounterThresholdNotificationType> notifications) {
        super();
        this.alias = alias;
        this.moduleName = moduleName;
        this.windowSeconds = windowSeconds;
        this.notifications = notifications;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    String getMBeanName() {
        return MBEAN_BASE_NAME + ",module=" + moduleName + ",alias=" + alias;
    }

    @Override
    SlidingWindowStatisticsMBeanBase createNewMBeanInstance() {
        return new DiameterStatistics(alias, moduleName, windowSeconds, notifications);
    }

    @Override
    public void incBalanceQueryReceivedCount() {
        setCounterName(DiameterStatistics.Counter.balanceQueryReceivedCount.name());
    }

    @Override
    public void incBalanceQueryAnsweredCount() {
        setCounterName(DiameterStatistics.Counter.balanceQueryAnsweredCount.name());
    }

    @Override
    public void incDebitQueryReceivedCount() {
        setCounterName(DiameterStatistics.Counter.debitQueryReceivedCount.name());
    }

    @Override
    public void incDebitQueryAnsweredCount() {
        setCounterName(DiameterStatistics.Counter.debitQueryAnsweredCount.name());
    }

}
