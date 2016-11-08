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

import org.restcomm.imscf.common.config.MapCounterThresholdNotificationType;
import org.restcomm.imscf.common.el.statistics.MapStatisticsMBean;

import java.util.List;

/**
 * MBean implementation for MAP statistics for an alias.
 * @see SlidingWindowStatisticsMBeanBase
 * @author Miklos Pocsaji
 *
 */
public class MapStatistics extends TcapStatisticsMBeanBase implements MapStatisticsMBean {

    private String alias;

    /**
     * The possible counters in this MBean.
     * @author Miklos Pocsaji
     *
     */
    enum Counter {
        anyTimeInterrogationCount, anyTimeInterrogationResultCount
    }

    public MapStatistics(String alias, int windowSeconds, List<MapCounterThresholdNotificationType> notifications) {
        super(windowSeconds, SlidingWindowStatisticsMBeanBase.convertFromMapThresholdNotifications(notifications));
        this.alias = alias;
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
    public long getAnyTimeInterrogationCount() {
        return getCounter(Counter.anyTimeInterrogationCount.name());
    }

    @Override
    public long getAnyTimeInterrogationResultCount() {
        return getCounter(Counter.anyTimeInterrogationResultCount.name());
    }

    public void incAnyTimeInterrogationCount() {
        incCounter(Counter.anyTimeInterrogationCount.name());
    }

    public void incAnyTimeInterrogationResultCount() {
        incCounter(Counter.anyTimeInterrogationResultCount.name());
    }

}
