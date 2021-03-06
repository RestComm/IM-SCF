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
package org.restcomm.imscf.common.lwcomm.service;

/**
 * Interface the method of which is called when receiving a message in the LwComm subsystem
 *
 * Threading information:
 * Current implementation calls this method in a thread from a pool with receiveWorkerPool
 * threads in it set from the configuration.
 * @author Miklos Pocsaji
 *
 */
public interface MessageReceiver {

    /**
     * Called when a message arrvies.
     * @param message The message arrived.
     */
    void onMessage(IncomingTextMessage message);
}
