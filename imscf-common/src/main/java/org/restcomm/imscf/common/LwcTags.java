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
package org.restcomm.imscf.common ;

/** LwComm message tag constants used in SL-EL communication.
 * These tags form the basis of message filtering in the overload protection mechanism.
 * They represent IMSCF level SL-EL concepts and are transparently handled by lwcomm.
 */
public final class LwcTags {
    /** Accepting this message will create a new session.*/
    public static final String NEW_SESSION = "NEW_SESSION";
    /** Accepting this message will update an existing session.*/
    public static final String IN_SESSION = "IN_SESSION";

    public static String[] getAll() {
        return new String[] { NEW_SESSION, IN_SESSION };
    }

    private LwcTags() {
    }
}
