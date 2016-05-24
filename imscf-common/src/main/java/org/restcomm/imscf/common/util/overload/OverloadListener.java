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
package org.restcomm.imscf.common.util.overload;

/**
 * Notification interface to OverloadProtector.
 * @author Miklos Pocsaji
 *
 */
public interface OverloadListener {

    /**
     * This method is invoked whenever a change in the system overloaded state occurs.
     * Note that the method is invoked from OverloadProtector's own thread in which the
     * system examination is done as well. So if anything blocks this method from return,
     * overload protection will not work.
     * @param previous The state of the system before this event in the overload point of view.
     * @param actual The current state of the system in the overload point of view.
     */
    void overloadStateChanged(OverloadState previous, OverloadState actual);
}
