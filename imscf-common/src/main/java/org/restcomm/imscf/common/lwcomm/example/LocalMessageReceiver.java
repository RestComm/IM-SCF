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
package org.restcomm.imscf.common.lwcomm.example;

import org.restcomm.imscf.common.lwcomm.service.IncomingTextMessage;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;
import org.restcomm.imscf.common.lwcomm.service.impl.LwCommServiceImpl;

/**
 * Example receiver for local testing.
 * Just writes the arrived message into stdout.
 * @author Miklos Pocsaji
 *
 */
public class LocalMessageReceiver implements MessageReceiver {

    @Override
    public void onMessage(IncomingTextMessage message) {
        LwCommServiceImpl.LOGGER.debug("Message arrived: {}", message);
        // try {
        // Thread.sleep(1100);
        // } catch (InterruptedException e) {
        // LwCommServiceImpl.LOGGER.error("Sleep interrupted:", e);
        // }
    }

}
