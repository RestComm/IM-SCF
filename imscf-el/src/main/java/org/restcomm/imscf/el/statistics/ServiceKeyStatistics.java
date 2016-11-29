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

import org.restcomm.imscf.common.config.ServiceKeyCounterThresholdNotificationType;
import org.restcomm.imscf.common.el.statistics.ServiceKeyStatisticsMBean;

/**
 * MBean implementation of statistics counters for a service key.
 * @see SlidingWindowStatisticsMBeanBase
 * @author Miklos Pocsaji
 *
 */
public class ServiceKeyStatistics extends TcapStatisticsMBeanBase implements ServiceKeyStatisticsMBean,
        ServiceKeyStatisticsSetter {

    private String serviceIdentifier;

    /**
     * The counter names for a service key.
     * @author Miklos Pocsaji
     *
     */
    enum Counter {
        activityTestRequestCount,

        activityTestResponseCount,

        applyChargingCount,

        applyChargingReportCount,

        cancelCount,

        connectCount,

        connectToResourceCount,

        continueCount,

        continueWithArgumentCount,

        disconnectForwardConnectionCount,

        disconnectForwardConnectionWithArgumentCount,

        disconnectLegCount,

        eventReportBcsmCount,

        furnishChargingInformationCount,

        initialDpCount,

        initiateCallAttemptRequestCount,

        initiateCallAttemptResponseCount,

        moveLegRequestCount,

        moveLegResponseCount,

        playAnnouncementCount,

        promptAndCollectUserInformationCount,

        promptAndCollectUserInformationResultCount,

        releaseCallCount,

        requestReportBcsmEventCount,

        resetTimerCount,

        specializedResourceReportCount,

        splitLegCount,

        splitLegResponseCount,

    }

    public ServiceKeyStatistics(String serviceIdentifier, int windowSeconds,
            List<ServiceKeyCounterThresholdNotificationType> notifications) {
        super(windowSeconds, SlidingWindowStatisticsMBeanBase
                .convertFromServiceKeyThresholdNotifications(notifications));
        this.serviceIdentifier = serviceIdentifier;
        for (Counter c : Counter.values()) {
            addCounter(c.toString());
        }
    }

    @Override
    protected String resolveNotificationVariable(String variable) {
        if ("serviceIdentifier".equals(variable)) {
            return String.valueOf(serviceIdentifier);
        }
        return null;
    }

    @Override
    public long getActivityTestRequestCount() {
        return getCounter(Counter.activityTestRequestCount.name());
    }

    @Override
    public long getActivityTestResponseCount() {
        return getCounter(Counter.activityTestResponseCount.name());
    }

    @Override
    public long getApplyChargingCount() {
        return getCounter(Counter.applyChargingCount.name());
    }

    @Override
    public long getApplyChargingReportCount() {
        return getCounter(Counter.applyChargingReportCount.name());
    }

    @Override
    public long getCancelCount() {
        return getCounter(Counter.cancelCount.name());
    }

    @Override
    public long getConnectCount() {
        return getCounter(Counter.connectCount.name());
    }

    @Override
    public long getConnectToResourceCount() {
        return getCounter(Counter.connectToResourceCount.name());
    }

    @Override
    public long getContinueCount() {
        return getCounter(Counter.continueCount.name());
    }

    @Override
    public long getContinueWithArgumentCount() {
        return getCounter(Counter.continueWithArgumentCount.name());
    }

    @Override
    public long getDisconnectForwardConnectionCount() {
        return getCounter(Counter.disconnectForwardConnectionCount.name());
    }

    @Override
    public long getDisconnectForwardConnectionWithArgumentCount() {
        return getCounter(Counter.disconnectForwardConnectionWithArgumentCount.name());
    }

    @Override
    public long getDisconnectLegCount() {
        return getCounter(Counter.disconnectLegCount.name());
    }

    @Override
    public long getEventReportBcsmCount() {
        return getCounter(Counter.eventReportBcsmCount.name());
    }

    @Override
    public long getFurnishChargingInformationCount() {
        return getCounter(Counter.furnishChargingInformationCount.name());
    }

    @Override
    public long getInitialDpCount() {
        return getCounter(Counter.initialDpCount.name());
    }

    @Override
    public long getInitiateCallAttemptRequestCount() {
        return getCounter(Counter.initiateCallAttemptRequestCount.name());
    }

    @Override
    public long getInitiateCallAttemptResponseCount() {
        return getCounter(Counter.initiateCallAttemptResponseCount.name());
    }

    @Override
    public long getMoveLegRequestCount() {
        return getCounter(Counter.moveLegRequestCount.name());
    }

    @Override
    public long getMoveLegResponseCount() {
        return getCounter(Counter.moveLegResponseCount.name());
    }

    @Override
    public long getPlayAnnouncementCount() {
        return getCounter(Counter.playAnnouncementCount.name());
    }

    @Override
    public long getPromptAndCollectUserInformationCount() {
        return getCounter(Counter.promptAndCollectUserInformationCount.name());
    }

    @Override
    public long getPromptAndCollectUserInformationResultCount() {
        return getCounter(Counter.promptAndCollectUserInformationResultCount.name());
    }

    @Override
    public long getReleaseCallCount() {
        return getCounter(Counter.releaseCallCount.name());
    }

    @Override
    public long getRequestReportBcsmEventCount() {
        return getCounter(Counter.requestReportBcsmEventCount.name());
    }

    @Override
    public long getResetTimerCount() {
        return getCounter(Counter.resetTimerCount.name());
    }

    @Override
    public long getSpecializedResourceReportCount() {
        return getCounter(Counter.specializedResourceReportCount.name());
    }

    @Override
    public long getSplitLegCount() {
        return getCounter(Counter.splitLegCount.name());
    }

    @Override
    public long getSplitLegResponseCount() {
        return getCounter(Counter.splitLegResponseCount.name());
    }

    @Override
    public void reset() {
        resetAllCounters();
    }

    @Override
    public void incRequestReportBcsmEventCount() {
        incCounter(Counter.requestReportBcsmEventCount.name());
    }

    @Override
    public void incActivityTestRequestCount() {
        incCounter(Counter.activityTestRequestCount.name());
    }

    @Override
    public void incActivityTestResponseCount() {
        incCounter(Counter.activityTestResponseCount.name());
    }

    @Override
    public void incApplyChargingCount() {
        incCounter(Counter.applyChargingCount.name());
    }

    @Override
    public void incApplyChargingReportCount() {
        incCounter(Counter.applyChargingReportCount.name());
    }

    @Override
    public void incCancelCount() {
        incCounter(Counter.cancelCount.name());
    }

    @Override
    public void incConnectCount() {
        incCounter(Counter.connectCount.name());
    }

    @Override
    public void incConnectToResourceCount() {
        incCounter(Counter.connectToResourceCount.name());
    }

    @Override
    public void incContinueCount() {
        incCounter(Counter.continueCount.name());
    }

    @Override
    public void incContinueWithArgumentCount() {
        incCounter(Counter.continueWithArgumentCount.name());
    }

    @Override
    public void incDisconnectForwardConnectionCount() {
        incCounter(Counter.disconnectForwardConnectionCount.name());
    }

    @Override
    public void incDisconnectForwardConnectionWithArgumentCount() {
        incCounter(Counter.disconnectForwardConnectionWithArgumentCount.name());
    }

    @Override
    public void incDisconnectLegCount() {
        incCounter(Counter.disconnectLegCount.name());
    }

    @Override
    public void incEventReportBcsmCount() {
        incCounter(Counter.eventReportBcsmCount.name());
    }

    @Override
    public void incFurnishChargingInformationCount() {
        incCounter(Counter.furnishChargingInformationCount.name());
    }

    @Override
    public void incInitialDpCount() {
        incCounter(Counter.initialDpCount.name());
    }

    @Override
    public void incInitiateCallAttemptRequestCount() {
        incCounter(Counter.initiateCallAttemptRequestCount.name());
    }

    @Override
    public void incInitiateCallAttemptResponseCount() {
        incCounter(Counter.initiateCallAttemptResponseCount.name());
    }

    @Override
    public void incMoveLegRequestCount() {
        incCounter(Counter.moveLegRequestCount.name());
    }

    @Override
    public void incMoveLegResponseCount() {
        incCounter(Counter.moveLegResponseCount.name());
    }

    @Override
    public void incPlayAnnouncementCount() {
        incCounter(Counter.playAnnouncementCount.name());
    }

    @Override
    public void incPromptAndCollectUserInformationCount() {
        incCounter(Counter.promptAndCollectUserInformationCount.name());
    }

    @Override
    public void incPromptAndCollectUserInformationResultCount() {
        incCounter(Counter.promptAndCollectUserInformationResultCount.name());
    }

    @Override
    public void incReleaseCallCount() {
        incCounter(Counter.releaseCallCount.name());
    }

    @Override
    public void incResetTimerCount() {
        incCounter(Counter.resetTimerCount.name());
    }

    @Override
    public void incSpecializedResourceReportCount() {
        incCounter(Counter.specializedResourceReportCount.name());
    }

    @Override
    public void incSplitLegCount() {
        incCounter(Counter.splitLegCount.name());
    }

    @Override
    public void incSplitLegResponseCount() {
        incCounter(Counter.splitLegResponseCount.name());
    }

}
