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
package org.restcomm.imscf.common.ss7.tcap;

import org.restcomm.imscf.common.config.ApplicationContextType;

import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;

/**
 * Imscf utility class to map between Imscf specific and JSs7 TCAP classes and support TCAP features.
 *
 * @author Balogh Gábor
 *
 */
public final class ImscfTCAPUtil {

    private static final String CAP_STACK_PREFIX = "CAP_";
    private static final String MAP_STACK_PREFIX = "MAP_";

    /**
    * To avoid being instantiated.
    */
    private ImscfTCAPUtil() {

    }

    public static String getCapStackNameForSsn(int ssn) {
        return CAP_STACK_PREFIX + ssn;
    }

    public static String getMapStackNameForSsn(int ssn) {
        return MAP_STACK_PREFIX + ssn;
    }

    public static boolean isCapMessage(ApplicationContextType act) {
        if (act == ApplicationContextType.CAP_2
             || act == ApplicationContextType.CAP_3
             || act == ApplicationContextType.CAP_3_SMS
             || act == ApplicationContextType.CAP_4
             || act == ApplicationContextType.CAP_4_SMS) {
            return true;
        }
        return false;
    }

    public static boolean isMapMessage(ApplicationContextType act) {
        if (act == ApplicationContextType.MAP) {
            return true;
        }
        return false;
    }

    public static boolean isMapStack(String name) {
        if (name == null) {
            return false;
        }
        if (name.startsWith(MAP_STACK_PREFIX)) {
            return true;
        }
        return false;
    }

    public static boolean isCapStack(String name) {
        if (name == null) {
            return false;
        }
        if (name.startsWith(CAP_STACK_PREFIX)) {
            return true;
        }
        return false;
    }

    public static ApplicationContextType getApplicationContext(long[] oid) {
        ApplicationContextType ret = null;
        CAPApplicationContext capac = CAPApplicationContext.getInstance(oid);
        if (capac != null) {
            switch (capac) {
                case CapV2_gsmSSF_to_gsmSCF:
                case CapV2_assistGsmSSF_to_gsmSCF:
                case CapV2_gsmSRF_to_gsmSCF:
                    ret = ApplicationContextType.CAP_2;
                    break;
                case CapV3_gsmSSF_scfGeneric:
                case CapV3_gsmSSF_scfAssistHandoff:
                case CapV3_gsmSRF_gsmSCF:
                case CapV3_gprsSSF_gsmSCF:
                case CapV3_gsmSCF_gprsSSF:
                    ret = ApplicationContextType.CAP_3;
                    break;
                case CapV3_cap3_sms:
                    ret = ApplicationContextType.CAP_3_SMS;
                    break;
                case CapV4_gsmSSF_scfGeneric:
                case CapV4_gsmSSF_scfAssistHandoff:
                case CapV4_scf_gsmSSFGeneric:
                case CapV4_gsmSRF_gsmSCF:
                    ret = ApplicationContextType.CAP_4;
                    break;
                case CapV4_cap4_sms:
                    ret = ApplicationContextType.CAP_4_SMS;
                    break;
                default: ret = null;
                    break;
            }
        } else {
            MAPApplicationContext mapac = MAPApplicationContext.getInstance(oid);
            if (mapac != null) {
                ret = ApplicationContextType.MAP;
            }
        }
        return ret;
    }

}
