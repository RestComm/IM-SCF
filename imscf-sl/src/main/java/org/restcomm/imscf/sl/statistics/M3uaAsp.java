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
package org.restcomm.imscf.sl.statistics;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;
import org.restcomm.imscf.common.sl.statistics.M3uaAspMBean;

/**
 * MBean implementation of an M3UA ASP.
 * @author Miklos Pocsaji
 *
 */
public class M3uaAsp extends NotificationBroadcasterSupport implements M3uaAspMBean {

    private String name;
    private String ipAddress;
    private int port;
    private String status;
    private AtomicLong statusChangeSequence = new AtomicLong();

    public M3uaAsp(String name, String ipAddress, int port) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    @Override
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        String oldStatus = this.status;
        this.status = status;
        AttributeChangeNotification notification = new AttributeChangeNotification(this,
                statusChangeSequence.incrementAndGet(), System.currentTimeMillis(), "M3UA ASP '" + name
                        + "' status changed from '" + oldStatus + "' to '" + status + "'", "status",
                "java.lang.String", oldStatus, status);
        sendNotification(notification);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        MBeanNotificationInfo statusChange = new MBeanNotificationInfo(
                new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE },
                AttributeChangeNotification.class.getName(),
                "This notification is emitted when the status of the M3UA ASP changes.");
        return new MBeanNotificationInfo[] { statusChange };
    }

}
