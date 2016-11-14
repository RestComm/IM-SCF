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

public class CAPTimerDefault {

    private static final int _Timer_CircuitSwitchedCallControl_Short = 6000; // 1 - 10 sec
    private static final int _Timer_CircuitSwitchedCallControl_Medium = 30000; // 1 - 60 sec
    private static final int _Timer_CircuitSwitchedCallControl_Long = 300000; // 1 s - 30 minutes
    private static final int _Timer_Sms_Short = 10000; // 1 - 20 sec
    private static final int _Timer_Gprs_Short = 10000;

    private final int timerCircuitSwitchedCallControlShort;
    private final int timerCircuitSwitchedCallControlMedium;
    private final int timerCircuitSwitchedCallControlLong;
    private final int timerSmsShort;
    private final int timerGprsShort;

    public static CAPTimerDefault getDefaultInstance() {
        return new CAPTimerDefault(_Timer_CircuitSwitchedCallControl_Short,
                                   _Timer_CircuitSwitchedCallControl_Medium,
                                   _Timer_CircuitSwitchedCallControl_Long,
                                   _Timer_Sms_Short,
                                   _Timer_Gprs_Short);
    }

    public CAPTimerDefault(int timerCircuitSwitchedCallControlShort,
            int timerCircuitSwitchedCallControlMedium,
            int timerCircuitSwitchedCallControlLong,
            int timerSmsShort,
            int timerGprsShort) {
        super();
        this.timerCircuitSwitchedCallControlShort = timerCircuitSwitchedCallControlShort;
        this.timerCircuitSwitchedCallControlMedium = timerCircuitSwitchedCallControlMedium;
        this.timerCircuitSwitchedCallControlLong = timerCircuitSwitchedCallControlLong;
        this.timerSmsShort = timerSmsShort;
        this.timerGprsShort = timerGprsShort;
    }

    public int getTimerCircuitSwitchedCallControlShort() {
        return timerCircuitSwitchedCallControlShort;
    }
    public int getTimerCircuitSwitchedCallControlMedium() {
        return timerCircuitSwitchedCallControlMedium;
    }
    public int getTimerCircuitSwitchedCallControlLong() {
        return timerCircuitSwitchedCallControlLong;
    }
    public int getTimerSmsShort() {
        return timerSmsShort;
    }
    public int getTimerGprsShort() {
        return timerGprsShort;
    }
}
