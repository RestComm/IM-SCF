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
package org.restcomm.imscf.el.diameter.call;

import org.restcomm.imscf.el.call.impl.IMSCFCallBase;
import org.restcomm.imscf.el.diameter.DiameterModule;

/**
 * Class for.
 */
public class DiameterHttpCallImpl extends IMSCFCallBase implements DiameterHttpCall {

    protected String appSessionId;
    protected String sessionId;
    protected String serviceContextId;
    protected DiameterModule module;
    protected String jSessionId = null;
    protected int inInstanceIndex = -1;
    protected int technicalError = -1;

    @Override
    public String getDiameterSessionId() {
        return sessionId;
    }

    @Override
    public void setDiameterSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void setServiceContextId(String serviceContextId) {
        this.serviceContextId = serviceContextId;
    }

    @Override
    public void setDiameterModule(DiameterModule module) {
        this.module = module;
    }

    @Override
    public DiameterModule getDiameterModule() {
        return module;
    }

    @Override
    public String getjSessionId() {
        return jSessionId;
    }

    @Override
    public void setjSessionId(String jSessionId) {
        this.jSessionId = jSessionId;
    }

    @Override
    public int getInInstanceIndex() {
        return inInstanceIndex;
    }

    @Override
    public void setInInstanceIndex(int inInstanceIndex) {
        this.inInstanceIndex = inInstanceIndex;
    }

    @Override
    public boolean wasTechnicalError() {
        return technicalError >= 0;
    }

    @Override
    public int getTechnicalError() {
        return technicalError;
    }

    @Override
    public void setTechnicalError(int technicalError) {
        this.technicalError = technicalError;
    }

    @Override
    public String getAppSessionId() {
        return appSessionId;
    }

    @Override
    public void setAppSessionId(String appSessionId) {
        this.appSessionId = appSessionId;
    }

    @Override
    public long getMaxAge() {
        return getDiameterModule().getModuleConfiguration().getSessionTimeoutSec() * 1000L;
    }

    @Override
    public String getServiceIdentifier() {
        return serviceContextId;
    }
}
