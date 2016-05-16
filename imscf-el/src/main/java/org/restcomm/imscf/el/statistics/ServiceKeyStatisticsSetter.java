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

/**
 * Provides an interface for caller to increment the required attribute for a service key.
 * @author Miklos Pocsaji
 *
 */
public interface ServiceKeyStatisticsSetter extends TcapStatisticsSetter {
    void incActivityTestRequestCount();

    void incActivityTestResponseCount();

    void incApplyChargingCount();

    void incApplyChargingReportCount();

    void incCancelCount();

    void incConnectCount();

    void incConnectToResourceCount();

    void incContinueCount();

    void incContinueWithArgumentCount();

    void incDisconnectForwardConnectionCount();

    void incDisconnectForwardConnectionWithArgumentCount();

    void incDisconnectLegCount();

    void incEventReportBcsmCount();

    void incFurnishChargingInformationCount();

    void incInitialDpCount();

    void incInitiateCallAttemptRequestCount();

    void incInitiateCallAttemptResponseCount();

    void incMoveLegRequestCount();

    void incMoveLegResponseCount();

    void incPlayAnnouncementCount();

    void incPromptAndCollectUserInformationCount();

    void incPromptAndCollectUserInformationResultCount();

    void incReleaseCallCount();

    void incRequestReportBcsmEventCount();

    void incResetTimerCount();

    void incSpecializedResourceReportCount();

    void incSplitLegCount();

    void incSplitLegResponseCount();

}
