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
package org.restcomm.imscf.el.call;

/**Listener interface for IMSCF call life cycle events.*/
public interface ImscfCallLifeCycleListener {
    /**
     * This method is called when the state of a call changes. The new state is available through {@link IMSCFCall#getImscfState() call.getImscfState()}.
     * @param call the call on which the state change occurred.
     * @param oldState the previous state of the call.
     */
    default void imscfCallStateChanged(IMSCFCall call, ImscfCallLifeCycleState oldState) {
        imscfCallStateChanged(call);
    }

    /** Same as the other method, but old state is ignored.*/
    void imscfCallStateChanged(IMSCFCall call);
}
