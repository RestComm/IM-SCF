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
package org.restcomm.imscf.sl.diameter.creditcontrol;

import org.restcomm.imscf.common.diameter.creditcontrol.CCRequestType;

import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;

/**
 * Class for storing diameter CCA sessions and their parameters.
 */
public class DiameterGWCCASessionData {
    protected String sessionId;
    protected CCRequestType ccRequestType;
    protected Long ccRequestNumber;
    protected JCreditControlRequest request;
    protected ServerCCASession session;
    protected boolean technicalError = false;

    protected long startTime;

    public DiameterGWCCASessionData(ServerCCASession session) {
        this.session = session;
        this.startTime = System.currentTimeMillis();
    }

    public void updateSession(DiameterGWCCASessionData newSessionData) {
        this.ccRequestType = newSessionData.ccRequestType;
        this.ccRequestNumber = newSessionData.ccRequestNumber;
        this.request = newSessionData.request;
        this.session = newSessionData.session;
        this.startTime = System.currentTimeMillis();
    }

    public long getStartTime() {
        return startTime;
    }

    public JCreditControlRequest getRequest() {
        return request;
    }

    public void setRequest(JCreditControlRequest request) {
        this.request = request;
    }

    public ServerCCASession getSession() {
        return session;
    }

    public void setSession(ServerCCASession session) {
        this.session = session;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public CCRequestType getCcRequestType() {
        return ccRequestType;
    }

    public void setCcRequestType(CCRequestType ccRequestType) {
        this.ccRequestType = ccRequestType;
    }

    public Long getCcRequestNumber() {
        return ccRequestNumber;
    }

    public void setCcRequestNumber(Long ccRequestNumber) {
        this.ccRequestNumber = ccRequestNumber;
    }

    public boolean wasTechnicalError() {
        return technicalError;
    }

    public void setTechnicalError(boolean technicalError) {
        this.technicalError = technicalError;
    }

}
