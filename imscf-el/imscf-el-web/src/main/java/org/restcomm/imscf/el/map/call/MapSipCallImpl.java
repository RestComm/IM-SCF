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
package org.restcomm.imscf.el.map.call;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.protocols.ss7.map.api.MAPDialog;

import org.restcomm.imscf.el.call.impl.TCAPSIPCallBase;
import org.restcomm.imscf.el.map.MAPModule;
import org.restcomm.imscf.el.map.scenarios.MapIncomingRequestScenario;
import org.restcomm.imscf.el.map.scenarios.MapOutgoingRequestScenario;
import org.restcomm.imscf.el.sip.SipModule;

/**
 * MAP-SIP call implementation.
 * @author Miklos Pocsaji
 *
 */
public class MapSipCallImpl extends TCAPSIPCallBase implements MAPSIPCall {

    private SipModule sipModule;
    private MAPDialog mapDialog;
    private MAPModule mapModule;
    private MapMethod mapMethod;
    private AtiRequest atiRequest;
    private List<MapIncomingRequestScenario<?>> mapIncomingRequestScenarios = new ArrayList<>();
    private List<MapOutgoingRequestScenario> mapOutgoingRequestScenarios = new ArrayList<>();

    @Override
    public long getMaxAge() {
        return getMapModule().getModuleConfiguration().getMapTimeoutSec() * 1000L;
    }

    @Override
    public SipModule getSipModule() {
        return sipModule;
    }

    @Override
    public void setSipModule(SipModule sipModule) {
        this.sipModule = sipModule;
    }

    @Override
    public MAPDialog getMAPDialog() {
        return mapDialog;
    }

    @Override
    public void setMAPDialog(MAPDialog mapDialog) {
        this.mapDialog = mapDialog;
    }

    @Override
    public MAPModule getMapModule() {
        return mapModule;
    }

    @Override
    public void setMapModule(MAPModule mapModule) {
        this.mapModule = mapModule;
    }

    @Override
    public MapMethod getMapMethod() {
        return mapMethod;
    }

    @Override
    public void setMapMethod(MapMethod mapMethod) {
        this.mapMethod = mapMethod;
    }

    @Override
    public AtiRequest getAtiRequest() {
        return atiRequest;
    }

    @Override
    public void setAtiRequest(AtiRequest atiRequest) {
        this.atiRequest = atiRequest;
    }

    @Override
    public List<MapIncomingRequestScenario<?>> getMapIncomingRequestScenarios() {
        return mapIncomingRequestScenarios;
    }

    @Override
    public List<MapOutgoingRequestScenario> getMapOutgoingRequestScenarios() {
        return mapOutgoingRequestScenarios;
    }

    @Override
    public String toString() {
        return "MAP [" + mapMethod + ", imscfCallId=" + getImscfCallId() + ", sipAppSessionId=" + sipAppSessionId
                + ", remoteTcapTrId=" + getRemoteTcapTrId() + ", localTcapTrId=" + getLocalTcapTrId()
                + ", sipScenarioCount=" + sipScenarios.size() + ", mapScenarioCount=("
                + mapIncomingRequestScenarios.size() + " IN, " + mapOutgoingRequestScenarios.size() + " OUT)]";
    }
}
