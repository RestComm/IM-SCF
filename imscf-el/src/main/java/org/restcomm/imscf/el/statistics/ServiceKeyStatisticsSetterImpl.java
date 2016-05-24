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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.restcomm.imscf.common.config.ServiceKeyCounterThresholdNotificationType;
import org.restcomm.imscf.util.MBeanHelper;

/**
 * One-shot setter implementation for service key statistics.
 * @author Miklos Pocsaji
 *
 */
public class ServiceKeyStatisticsSetterImpl extends TcapStatisticsSetterBase implements ServiceKeyStatisticsSetter {

    private static final String MBEAN_BASE_NAME = MBeanHelper.EL_MBEAN_DOMAIN + ":type=ServiceKeyStatistics";
    private static final Logger LOG = LoggerFactory.getLogger(ServiceKeyStatisticsSetterImpl.class);
    private String serviceIdentifier;
    private int windowSeconds;
    private List<ServiceKeyCounterThresholdNotificationType> notifications;

    public ServiceKeyStatisticsSetterImpl(String serviceIdentifier, int windowSeconds,
            List<ServiceKeyCounterThresholdNotificationType> notifications) {
        super();
        this.serviceIdentifier = serviceIdentifier;
        this.windowSeconds = windowSeconds;
        this.notifications = notifications;
    }

    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    @Override
    String getMBeanName() {
        return MBEAN_BASE_NAME + ",serviceIdentifier=" + serviceIdentifier;
    }

    @Override
    SlidingWindowStatisticsMBeanBase createNewMBeanInstance() {
        LOG.info("Creating new MBean ({}), notifications: {}", getMBeanName(), notifications);
        return new ServiceKeyStatistics(serviceIdentifier, windowSeconds, notifications);
    }

    @Override
    public void incRequestReportBcsmEventCount() {
        setCounterName(ServiceKeyStatistics.Counter.requestReportBcsmEventCount.name());
    }

    @Override
    public void incActivityTestRequestCount() {
        setCounterName(ServiceKeyStatistics.Counter.activityTestRequestCount.name());
    }

    @Override
    public void incActivityTestResponseCount() {
        setCounterName(ServiceKeyStatistics.Counter.activityTestResponseCount.name());
    }

    @Override
    public void incApplyChargingCount() {
        setCounterName(ServiceKeyStatistics.Counter.applyChargingCount.name());
    }

    @Override
    public void incApplyChargingReportCount() {
        setCounterName(ServiceKeyStatistics.Counter.applyChargingReportCount.name());
    }

    @Override
    public void incCancelCount() {
        setCounterName(ServiceKeyStatistics.Counter.cancelCount.name());
    }

    @Override
    public void incConnectCount() {
        setCounterName(ServiceKeyStatistics.Counter.connectCount.name());
    }

    @Override
    public void incConnectToResourceCount() {
        setCounterName(ServiceKeyStatistics.Counter.connectToResourceCount.name());
    }

    @Override
    public void incContinueCount() {
        setCounterName(ServiceKeyStatistics.Counter.continueCount.name());
    }

    @Override
    public void incContinueWithArgumentCount() {
        setCounterName(ServiceKeyStatistics.Counter.continueWithArgumentCount.name());
    }

    @Override
    public void incDisconnectForwardConnectionCount() {
        setCounterName(ServiceKeyStatistics.Counter.disconnectForwardConnectionCount.name());
    }

    @Override
    public void incDisconnectForwardConnectionWithArgumentCount() {
        setCounterName(ServiceKeyStatistics.Counter.disconnectForwardConnectionWithArgumentCount.name());
    }

    @Override
    public void incDisconnectLegCount() {
        setCounterName(ServiceKeyStatistics.Counter.disconnectLegCount.name());
    }

    @Override
    public void incEventReportBcsmCount() {
        setCounterName(ServiceKeyStatistics.Counter.eventReportBcsmCount.name());
    }

    @Override
    public void incFurnishChargingInformationCount() {
        setCounterName(ServiceKeyStatistics.Counter.furnishChargingInformationCount.name());
    }

    @Override
    public void incInitialDpCount() {
        setCounterName(ServiceKeyStatistics.Counter.initialDpCount.name());
    }

    @Override
    public void incInitiateCallAttemptRequestCount() {
        setCounterName(ServiceKeyStatistics.Counter.initiateCallAttemptRequestCount.name());
    }

    @Override
    public void incInitiateCallAttemptResponseCount() {
        setCounterName(ServiceKeyStatistics.Counter.initiateCallAttemptResponseCount.name());
    }

    @Override
    public void incMoveLegRequestCount() {
        setCounterName(ServiceKeyStatistics.Counter.moveLegRequestCount.name());
    }

    @Override
    public void incMoveLegResponseCount() {
        setCounterName(ServiceKeyStatistics.Counter.moveLegResponseCount.name());
    }

    @Override
    public void incPlayAnnouncementCount() {
        setCounterName(ServiceKeyStatistics.Counter.playAnnouncementCount.name());
    }

    @Override
    public void incPromptAndCollectUserInformationCount() {
        setCounterName(ServiceKeyStatistics.Counter.promptAndCollectUserInformationCount.name());
    }

    @Override
    public void incPromptAndCollectUserInformationResultCount() {
        setCounterName(ServiceKeyStatistics.Counter.promptAndCollectUserInformationResultCount.name());
    }

    @Override
    public void incReleaseCallCount() {
        setCounterName(ServiceKeyStatistics.Counter.releaseCallCount.name());
    }

    @Override
    public void incResetTimerCount() {
        setCounterName(ServiceKeyStatistics.Counter.resetTimerCount.name());
    }

    @Override
    public void incSpecializedResourceReportCount() {
        setCounterName(ServiceKeyStatistics.Counter.specializedResourceReportCount.name());
    }

    @Override
    public void incSplitLegCount() {
        setCounterName(ServiceKeyStatistics.Counter.splitLegCount.name());
    }

    @Override
    public void incSplitLegResponseCount() {
        setCounterName(ServiceKeyStatistics.Counter.splitLegResponseCount.name());
    }

}
