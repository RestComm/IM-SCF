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
package org.restcomm.imscf.el.modules.routing;

import org.restcomm.imscf.common.config.ApplicationContextType;
import org.restcomm.imscf.common.config.RoutingCriteriaType;

import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.cap.api.CAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;

/** Class for matching a routing criteria entry. */
public class RoutingCriteriaMatcher {
    private ApplicationContextType appCtx;
    private UnsignedNumberRangeList skList;

    public RoutingCriteriaMatcher(RoutingCriteriaType config) {
        this.appCtx = config.getApplicationContext();
        // empty sklist allowed, it will always match
        this.skList = UnsignedNumberRangeList.parse(config.getServiceKeyRangeList());
    }

    public boolean matches(CAPApplicationContext capAppCtx, int serviceKey) {
        // matches if all configured criteria match. A null (non-configured) criterion is a match.
        return (appCtx == null || matches(appCtx, capAppCtx)) && (skList == null || skList.matches(serviceKey));
    }

    // note that servicekey doesn't make much sense for MAP matching...
    public boolean matches(MAPApplicationContext mapAppCtx, int serviceKey) {
        // matches if all configured criteria match. A null (non-configured) criterion is a match.
        return (appCtx == null || matches(appCtx, mapAppCtx)) && (skList == null || skList.matches(serviceKey));
    }

    private boolean matches(ApplicationContextType configAC, CAPApplicationContext capAC) {
        switch (configAC) {
        case CAP_2:
            return CAPApplicationContextVersion.version2.equals(capAC.getVersion());
        case CAP_3_SMS:
            return CAPApplicationContext.CapV3_cap3_sms.equals(capAC);
        case CAP_3:
            return CAPApplicationContextVersion.version3.equals(capAC.getVersion())
                    && !CAPApplicationContext.CapV3_cap3_sms.equals(capAC);
        case CAP_4_SMS:
            return CAPApplicationContext.CapV4_cap4_sms.equals(capAC);
        case CAP_4:
            return CAPApplicationContextVersion.version4.equals(capAC.getVersion())
                    && !CAPApplicationContext.CapV4_cap4_sms.equals(capAC);
        default:
            return false;
        }
    }

    private boolean matches(ApplicationContextType configAC, MAPApplicationContext mapAC) {
        return ApplicationContextType.MAP.equals(configAC) && mapAC != null;
    }
}
