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


import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import org.mobicents.protocols.api.Association;

/**
 * Represents an SCTP link (association) and sends notification of association events.
 *
 * @author Balogh Gábor
 *
 */
public class SctpLinkStatus extends NotificationBroadcasterSupport implements SctpLinkStatusMXBean {

    private final AtomicLong notificationSequence;

    private String name;
    private String status;
    private String type;
    private String hostAddress;
    private String hostPort;
    private String extraHostAddresses;
    private String peerAddress;
    private String peerPort;
    private String serverName;

    public SctpLinkStatus(Association association) {
        this.notificationSequence = new AtomicLong(0L);
        updateAssociation(association);
    }

    protected void updateAssociation(Association association) {
        this.name = association.getName();
        this.status = SctpLinkManager.getAssociationStatus(association);
        this.type = association.getAssociationType().toString();
        this.hostAddress = association.getHostAddress();
        this.hostPort = String.valueOf(association.getHostPort());
        this.extraHostAddresses = Arrays.toString(association.getExtraHostAddresses());
        this.peerAddress = association.getPeerAddress();
        this.peerPort = String.valueOf(association.getPeerPort());
        this.serverName = (association.getServerName() == null ? "" : association.getServerName());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getHostAddress() {
        return hostAddress;
    }

    @Override
    public String getHostPort() {
        return hostPort;
    }

    @Override
    public String getExtraHostAddresses() {
        return extraHostAddresses;
    }

    @Override
    public String getPeerAddress() {
        return peerAddress;
    }

    @Override
    public String getPeerPort() {
        return peerPort;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        MBeanNotificationInfo info = new MBeanNotificationInfo(
                new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE },
                AttributeChangeNotification.class.getName(),
                "This notification is emitted when the status of an association is changed.");
        return new MBeanNotificationInfo[] { info };
    }

    private Notification createNotification(String associationName, String event, String oldStatus, String newStatus) {
        AttributeChangeNotification notification = new AttributeChangeNotification(this,
                notificationSequence.incrementAndGet(), System.currentTimeMillis(), "Association= '" + associationName
                        + "': " + event + " event happened, new status is '" + newStatus + "'.", "status",
                "java.lang.String", oldStatus, newStatus);
        return notification;
    }

    public void onAssociationDown(Association association) {
        String oldStatus = SctpLinkManager.getAssociationManagmentStatus(association) + SctpLinkManager.UP_STATUS;
        String newStatus = SctpLinkManager.getAssociationStatus(association);
        sendNotification(createNotification(association.getName(), "onAssociationDown", oldStatus, newStatus));
        this.status = newStatus;
    }

    public void onAssociationStarted(Association association) {
        String oldStatus = SctpLinkManager.STOPPED_STATUS + SctpLinkManager.getAssociationLinkStatus(association);
        String newStatus = SctpLinkManager.getAssociationStatus(association);
        sendNotification(createNotification(association.getName(), "onAssociationStarted", oldStatus, newStatus));
        this.status = newStatus;
    }

    public void onAssociationStopped(Association association) {
        String oldStatus = SctpLinkManager.STARTED_STATUS + SctpLinkManager.getAssociationLinkStatus(association);
        String newStatus = SctpLinkManager.getAssociationStatus(association);
        sendNotification(createNotification(association.getName(), "onAssociationStopped", oldStatus, newStatus));
        this.status = newStatus;
    }

    public void onAssociationUp(Association association) {
        String oldStatus = SctpLinkManager.getAssociationManagmentStatus(association) + SctpLinkManager.DOWN_STATUS;
        String newStatus = SctpLinkManager.getAssociationStatus(association);
        sendNotification(createNotification(association.getName(), "onAssociationUp", oldStatus, newStatus));
        this.status = newStatus;
    }

    @Override
    public String toString() {
        return "SctpLinkStatus [notificationSequence=" + notificationSequence + ", name=" + name + ", status=" + status
                + ", type=" + type + ", hostAddress=" + hostAddress + ", hostPort=" + hostPort
                + ", extraHostAddresses=" + extraHostAddresses + ", peerAddress=" + peerAddress + ", peerPort="
                + peerPort + ", serverName=" + serverName + "]";
    }
}
