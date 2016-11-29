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
package org.restcomm.imscf.el.sccp;

import java.io.IOException;

import org.restcomm.imscf.common.config.GtAddressType;
import org.restcomm.imscf.common.config.RemoteGtAddressType;
import org.restcomm.imscf.common.config.RemoteSubSystemPointCodeType;
import org.restcomm.imscf.common.config.SubSystemType;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.sccp.impl.parameter.DefaultEncodingScheme;
import org.mobicents.protocols.ss7.sccp.impl.parameter.GlobalTitle0011Impl;

/** Utility class for mapping IMSCF config types to SCCP types. */
public final class SccpPrimitiveMapper {

    private static final int PC_FILLED_BY_SL = 0xC000;

    private SccpPrimitiveMapper() {
        // no instances
    }

    public static NumberingPlan getGtNumberingPlan(GtAddressType address) {
        return NumberingPlan.valueOf(address.getGtNumberingPlan());
    }

    public static NatureOfAddress getGtNatureOfAddress(GtAddressType address) {
        return NatureOfAddress.valueOf(address.getGtNoa());
    }

    public static SccpAddress createSccpAddress(GtAddressType address, ParameterFactory factory) {
        // note: address.getGtIndicator() is ignored. AddressIndicator is calculated by the factory based on the
        // provided parameters
        GlobalTitle gt = new GlobalTitle0011Impl(address.getGlobalTitle(), address.getGtTranslationType(),
                        new DefaultEncodingScheme(), getGtNumberingPlan(address));
        return factory.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0,
                address.getSubSystemNumber());
    }

    public static SccpAddress createSccpAddress(RemoteGtAddressType address, ParameterFactory factory, boolean addPc) {
        // note: address.getGtIndicator() is ignored. AddressIndicator is calculated by the factory based on the
        // provided parameters
        GlobalTitle gt = new GlobalTitle0011Impl(address.getGlobalTitle(), address.getGtTranslationType(),
                        new DefaultEncodingScheme(), getGtNumberingPlan(address));
        int pc = 0;
        if (addPc) {
            pc = address.getPointCode();
        }
        return factory.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, pc,
                address.getSubSystemNumber());
    }

    public static SccpAddress createSccpAddress(SubSystemType address, ParameterFactory factory) {
        return factory.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN, null, 0,
                address.getSubSystemNumber());
    }

    /**
     * Creates an SCCP address which is resolved by point code and subsystem number.
     * As the value of the point code a special number is set (0xC000) which is recognized by the SL
     * and will be replaced by the SL's own point code.
     * @param address The subsystem number to set into the address
     * @param factory The parameter factory of the stack
     * @return A new SccpAddress instance
     */
    public static SccpAddress createSccpAddressPcFilledBySl(SubSystemType address, ParameterFactory factory) {
        return factory.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN, null, PC_FILLED_BY_SL,
                address.getSubSystemNumber());
    }

    public static SccpAddress createSccpAddress(RemoteSubSystemPointCodeType address, ParameterFactory factory) {
        return factory.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN, null, address.getPointCode(),
                address.getSubSystemNumber());
    }
}
