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

import org.restcomm.imscf.common.ss7.tcap.ImscfTCAPUtil;

import org.mobicents.protocols.ss7.map.api.MAPStack;
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;
import org.mobicents.ss7.congestion.CongestionListener;
import org.mobicents.ss7.congestion.CongestionTicket;

/**
 * Wrapper class to extends MAPStackImpl class functionality with IMSCF specific features.
 *
 *
 * @author Balogh Gábor
 *
 */
public class MAPStackImplImscfWrapper implements MAPStack, CongestionListener {

    protected MAPProviderImplImscfWrapper mapProvider = null;

    private State state = State.IDLE;

    private final String name;

    public MAPStackImplImscfWrapper(int subSystemNumber, TCAPProvider tcapProvider) {
        this(ImscfTCAPUtil.getMapStackNameForSsn(subSystemNumber), tcapProvider);
    }

    protected MAPStackImplImscfWrapper(String name, TCAPProvider tcapProvider) {
        this.name = name;
        this.state = State.CONFIGURED;
        mapProvider = new MAPProviderImplImscfWrapper(name, tcapProvider);
    }

    @Override
    public String getName() {
        return name;
    }

    public MAPProvider getMAPProvider() {
        return this.mapProvider;
    }

    public void start() throws Exception {
        if (state != State.CONFIGURED) {
            throw new IllegalStateException("Stack has not been configured or is already running!");
        }
        this.mapProvider.start();
        this.state = State.RUNNING;
    }

    public void stop() {
        if (state != State.RUNNING) {
            throw new IllegalStateException("Stack is not running!");
        }
        this.mapProvider.stop();
        this.state = State.CONFIGURED;
    }

    public TCAPStack getTCAPStack() {
        return null;
    }

    public void onCongestionStart(CongestionTicket ticket) {
        this.mapProvider.onCongestionStart(ticket);
    }

    public void onCongestionFinish(CongestionTicket ticket) {
        this.mapProvider.onCongestionFinish(ticket);
    }

    private enum State {
        IDLE, CONFIGURED, RUNNING;
    }

}

