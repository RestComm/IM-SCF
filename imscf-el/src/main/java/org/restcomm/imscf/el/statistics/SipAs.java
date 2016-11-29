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

import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;
import org.restcomm.imscf.common.el.statistics.SipAsMBean;

/**
 * SIP application server MBean implementation.
 * @author Miklos Pocsaji
 *
 */
public class SipAs extends NotificationBroadcasterSupport implements SipAsMBean {

    private final String name;
    private final String groupName;
    private final String ip;
    private final int port;
    private final boolean heartbeatEnabled;
    private boolean reachable;
    private AtomicLong notificationSequence = new AtomicLong(1);

    public SipAs(String name, String groupName, String ip, int port, boolean heartbeatEnabled) {
        this.name = name;
        this.groupName = groupName;
        this.ip = ip;
        this.port = port;
        this.heartbeatEnabled = heartbeatEnabled;
    }

    @Override
    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        boolean prevReachable = this.reachable;
        this.reachable = reachable;
        if (prevReachable != reachable) {
            sendNotification(new AttributeChangeNotification(this, notificationSequence.getAndIncrement(),
                    System.currentTimeMillis(), "Application server '" + groupName + "/" + name
                            + "' reachable status changed to: " + reachable, "reachable", "java.lang.Boolean",
                    prevReachable, reachable));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[] { new MBeanNotificationInfo(
                new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE },
                AttributeChangeNotification.class.getName(),
                "This notification is sent when the reachable status of the SIP AS changes.") };
    }
}
