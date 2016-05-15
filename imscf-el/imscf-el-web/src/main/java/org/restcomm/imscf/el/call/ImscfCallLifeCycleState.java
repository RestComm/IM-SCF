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
package org.restcomm.imscf.el.call;

/** Generic life cycle states used for all IMSCF calls. */
public enum ImscfCallLifeCycleState {
    /** Immediately after creation, the call is still being set up. */
    INITIAL,
    /** The call is active. */
    ACTIVE,
    /** The call is being released gracefully.
     * A call that has reached its maximum allowed lifetime will automatically enter this state. */
    RELEASING,
    /** The call is finished. No more signaling should occur at this point, and the call will shortly be deleted. */
    FINISHED
}
