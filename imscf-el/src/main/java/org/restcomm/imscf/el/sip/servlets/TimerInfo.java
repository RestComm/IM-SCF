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
package org.restcomm.imscf.el.sip.servlets;

import java.io.Serializable;

/**
 * Wrapper encapsulating application data and timer info.
 */
public class TimerInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Type of timer. */
    public static enum TimerType {
        /** Call level timer. */
        CALL,
        /** App level timer. */
        APP
    }

    private TimerInfo.TimerType type;
    private String name;
    private Serializable info;

    public TimerInfo(TimerType type, String name, Serializable info) {
        this.type = type;
        this.name = name;
        this.info = info;
    }

    public TimerInfo.TimerType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Serializable getUserInfo() {
        return info;
    }

    @Override
    public String toString() {
        return "TimerInfo [type=" + type + ", name=" + name + ", info=" + info + "]";
    }

}
