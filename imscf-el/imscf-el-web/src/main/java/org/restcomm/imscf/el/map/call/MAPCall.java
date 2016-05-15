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

import java.util.List;

import javax.servlet.sip.SipServletRequest;

import org.restcomm.imscf.el.cap.sip.SipConstants;
import org.restcomm.imscf.el.map.MAPModule;
import org.restcomm.imscf.el.map.scenarios.MapIncomingRequestScenario;
import org.restcomm.imscf.el.map.scenarios.MapOutgoingRequestScenario;
import org.restcomm.imscf.el.tcap.call.TCAPCall;

import org.mobicents.protocols.ss7.map.api.MAPDialog;

/**
 * Call interface for MAP calls. Used both for ATI/FNR and Mobility Management calls.
 */
public interface MAPCall extends TCAPCall {

    void setMAPDialog(MAPDialog dialog);

    MAPDialog getMAPDialog();

    void setMapModule(MAPModule mapModule);

    MAPModule getMapModule();

    MapMethod getMapMethod();

    void setMapMethod(MapMethod mapMethod);

    AtiRequest getAtiRequest();

    void setAtiRequest(AtiRequest atiRequest);

    List<MapIncomingRequestScenario<?>> getMapIncomingRequestScenarios();

    List<MapOutgoingRequestScenario> getMapOutgoingRequestScenarios();

    @Override
    default String getServiceIdentifier() {
        switch (getMapMethod()) {
        case AnyTimeInterrogation:
            return getAtiRequest().getTargetRemoteSystem();
        case SendRoutingInfoForSM: // TODO
        case ProvideSubscriberInfo: // TODO
        default:
            return "UNDEFINED";
        }
    }

    /**
     * The type of the call.
     * @author Miklos Pocsaji
     */
    public enum MapMethod {
        AnyTimeInterrogation, SendRoutingInfoForSM, ProvideSubscriberInfo;

        public static MapMethod fromSubscribe(SipServletRequest req) {
            String method = req.getHeader(SipConstants.HEADER_MAP_METHOD);
            MapMethod ret;
            try {
                ret = valueOf(method);
            } catch (IllegalArgumentException | NullPointerException ex) {
                ret = null;
            }
            return ret;
        }
    }
}
