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

import org.mobicents.protocols.ss7.cap.CAPStackImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;
import org.mobicents.protocols.ss7.cap.api.CAPStack;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;
import org.mobicents.protocols.ss7.tcap.api.TCAPStack;


/**
 * Wrapper class to extends CAPStackImpl class functionality with IMSCF specific features.
 *
 *
 * @author Balogh Gábor
 *
 */
public class CAPStackImplImscfWrapper implements CAPStack {

    protected CAPProviderImplImscfWrapper capProvider = null;

    private State state = State.IDLE;

    private final String name;

    public CAPStackImplImscfWrapper(int subSystemNumber, TCAPProvider tcapProvider) {
        this(ImscfTCAPUtil.getCapStackNameForSsn(subSystemNumber), tcapProvider);
    }

    protected CAPStackImplImscfWrapper(String name, TCAPProvider tcapProvider) {
        this.name = name;
        this.state = State.CONFIGURED;
        capProvider = new CAPProviderImplImscfWrapper(name, tcapProvider);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CAPProvider getCAPProvider() {
        return this.capProvider;
    }

    @Override
    public void start() throws Exception {
        if (state != State.CONFIGURED) {
            throw new IllegalStateException("Stack has not been configured or is already running!");
        }
        this.capProvider.start();
        this.state = State.RUNNING;
    }

    @Override
    public void stop() {
        if (state != State.RUNNING) {
            throw new IllegalStateException("Stack is not running!");
        }
        this.capProvider.stop();
        this.state = State.CONFIGURED;
    }

    @Override
    public TCAPStack getTCAPStack() {
        return null;
    }

    public void setCAPTimerDefault(CAPTimerDefault timerDefault) {
        this.capProvider.setCAPTimerDefault(timerDefault);
    }

    private enum State {
        IDLE, CONFIGURED, RUNNING;
    }

}
