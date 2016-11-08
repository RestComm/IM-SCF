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
package org.restcomm.imscf.el.map;

import org.restcomm.imscf.common.config.NatureOfAddressType;
import org.restcomm.imscf.common.config.NumberingPlanType;
import org.restcomm.imscf.common.config.Ss7AddressType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;

/** Utility class for mapping IMSCF config types to jss7 MAP types. */
public final class MapPrimitiveMapper {

    private static final Map<NumberingPlanType, NumberingPlan> IMSCF_TO_MAP_API_NP = new EnumMap<>(
            NumberingPlanType.class);
    private static final Map<NatureOfAddressType, AddressNature> IMSCF_TO_MAP_API_AN = new EnumMap<>(
            NatureOfAddressType.class);
    static {
        IMSCF_TO_MAP_API_NP.put(NumberingPlanType.DATA, NumberingPlan.data);
        IMSCF_TO_MAP_API_NP.put(NumberingPlanType.ISDN, NumberingPlan.ISDN);
        IMSCF_TO_MAP_API_NP.put(NumberingPlanType.TELEX, NumberingPlan.telex);
    }
    static {
        IMSCF_TO_MAP_API_AN.put(NatureOfAddressType.INTERNATIONAL, AddressNature.international_number);
        IMSCF_TO_MAP_API_AN.put(NatureOfAddressType.NATIONAL, AddressNature.national_significant_number);
        IMSCF_TO_MAP_API_AN.put(NatureOfAddressType.NETWORK_SPECIFIC, AddressNature.network_specific_number);
        IMSCF_TO_MAP_API_AN.put(NatureOfAddressType.SUBSCRIBER_NUMBER, AddressNature.subscriber_number);
        IMSCF_TO_MAP_API_AN.put(NatureOfAddressType.UNKNOWN, AddressNature.unknown);
    }

    public static NumberingPlan getAsMapApiNumberingPlan(NumberingPlanType np) {
        return IMSCF_TO_MAP_API_NP.get(np);
    }

    public static AddressNature getAsMapApiAddressNature(NatureOfAddressType noa) {
        return IMSCF_TO_MAP_API_AN.get(noa);
    }

    public static ISDNAddressString createISDNAddressString(Ss7AddressType address, MAPParameterFactory factory) {
        Objects.requireNonNull(address, "Address is null");
        Objects.requireNonNull(factory, "MAPParameterFactory is null");
        return factory.createISDNAddressString(getAsMapApiAddressNature(address.getNoa()),
                getAsMapApiNumberingPlan(address.getNumberingPlan()), address.getAddress());
    }

    private MapPrimitiveMapper() {
        // no instances
    }
}
