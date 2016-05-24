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
package org.restcomm.imscf.sl.config;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.GtAddressType;
import org.restcomm.imscf.common.config.M3UaProfileType;
import org.restcomm.imscf.common.config.M3UaRouteType;
import org.restcomm.imscf.common.config.NetworkIndicatorType;
import org.restcomm.imscf.common.config.RemoteSubSystemPointCodeType;
import org.restcomm.imscf.common.config.SctpAssociationRemoteSideType;
import org.restcomm.imscf.common.config.SignalingLayerServerType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class with helper methods for JAXB generated imscf config classes.
 *
 * @author Balogh GÃ¡bor
 *
 */
public final class ImscfConfigUtil {
    /**
    * To avoid being instantiated.
    */
    private ImscfConfigUtil() {

    }

    public static int getNetworkIndicatorIntValue(NetworkIndicatorType ni) {
        int ret = -1;
        switch (ni) {
            case INTERNATIONAL: ret = 0;
                break;
            case INTERNATIONAL_EXTENSION: ret = 1;
                break;
            case NATIONAL: ret = 2;
                break;
            case NATIONAL_EXTENSION: ret = 3;
                break;
            default:
        }

        return ret;
    }

    public static SignalingLayerServerType getServerConfigByName(String serverName, ImscfConfigType config) {
        SignalingLayerServerType ret = null;
        for (SignalingLayerServerType server : config.getServers().getSignalingLayerServers()) {
            if (server.getName().equals(serverName)) {
                ret = server;
                break;
            }
        }
        return ret;
    }

    public static List<SctpAssociationRemoteSideType> getAllSctpRemoteSideByM3uaProfile(String m3uaProfileName,
            ImscfConfigType config) {
        ArrayList<SctpAssociationRemoteSideType> ret = new ArrayList<SctpAssociationRemoteSideType>();
        M3UaProfileType profile = null;
        for (M3UaProfileType p : config.getM3UaProfiles()) {
            if (p.getName().equals(m3uaProfileName)) {
                profile = p;
                break;
            }
        }
        if (profile != null) {
            for (M3UaRouteType route : profile.getM3UaRoutes()) {
                ret.add(route.getPrimaryAssociation());
                if (route.getSecondaryAssociation() != null) {
                    ret.add(route.getSecondaryAssociation());
                }
            }
        }
        return ret;
    }

   public static RemoteSubSystemPointCodeType findRemotePcSsnByName(String name, ImscfConfigType config) {
       for (RemoteSubSystemPointCodeType pcSsn: config.getSccp().getSccpRemoteProfile().getRemoteSubSystemPointCodeAddresses()) {
           if (pcSsn.getAlias().equals(name)) {
               return pcSsn;
           }
       }
       return null;
   }

   public static GtAddressType findRemoteGtAddressByName(String name, ImscfConfigType config) {
       for (GtAddressType gtAddress: config.getSccp().getSccpRemoteProfile().getRemoteGtAddresses()) {
           if (gtAddress.getAlias().equals(name)) {
               return gtAddress;
           }
       }
       return null;
   }
}
