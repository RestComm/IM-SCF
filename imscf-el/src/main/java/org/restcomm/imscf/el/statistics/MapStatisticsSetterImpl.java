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

import org.restcomm.imscf.common.config.MapCounterThresholdNotificationType;
import org.restcomm.imscf.util.MBeanHelper;

/**
 * One-shot setter implementation for map statistics.
 * @author Miklos Pocsaji
 *
 */
public class MapStatisticsSetterImpl extends TcapStatisticsSetterBase implements MapStatisticsSetter {

    private static final String MBEAN_BASE_NAME = MBeanHelper.EL_MBEAN_DOMAIN + ":type=MapStatistics";

    private String alias;
    private int windowSeconds;
    private List<MapCounterThresholdNotificationType> notifications;

    public MapStatisticsSetterImpl(String alias, int windowSeconds,
            List<MapCounterThresholdNotificationType> notifications) {
        super();
        this.alias = alias;
        this.windowSeconds = windowSeconds;
        this.notifications = notifications;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    String getMBeanName() {
        return MBEAN_BASE_NAME + ",alias=" + alias;
    }

    @Override
    SlidingWindowStatisticsMBeanBase createNewMBeanInstance() {
        return new MapStatistics(alias, windowSeconds, notifications);
    }

    @Override
    public void incAnyTimeInterrogationCount() {
        setCounterName(MapStatistics.Counter.anyTimeInterrogationCount.name());
    }

    @Override
    public void incAnyTimeInterrogationResultCount() {
        setCounterName(MapStatistics.Counter.anyTimeInterrogationResultCount.name());
    }

}
