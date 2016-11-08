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
package org.restcomm.imscf.common.ss7.cap;

import org.restcomm.imscf.common.ss7.tcap.ImscfTCAPUtil;

import org.mobicents.protocols.ss7.cap.CAPProviderImpl;
import org.mobicents.protocols.ss7.tcap.api.NamedTCListener;
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;


/**
 * Wrapper class to extends CAPProviderImpl class functionality with IMSCF specific features.
 *
 *
 * @author Balogh Gábor
 *
 */
@SuppressWarnings("serial")
public class CAPProviderImplImscfWrapper extends CAPProviderImpl implements NamedTCListener {

    private String name;

    public CAPProviderImplImscfWrapper(int subSystemNumber, TCAPProvider tcapProvider) {
       this(ImscfTCAPUtil.getCapStackNameForSsn(subSystemNumber), tcapProvider);
    }

    protected CAPProviderImplImscfWrapper(String name, TCAPProvider tcapProvider) {
       super(name, tcapProvider);
       this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
