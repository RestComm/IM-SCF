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
package org.restcomm.imscf.common.lwcomm.service;

import org.restcomm.imscf.common.lwcomm.config.Configuration;
import org.restcomm.imscf.common.lwcomm.service.TextMessage.TextMessageBuilder; // checkstyle:ignore UnusedImports, it is used in javadoc

/**
 * Lightweight Communication Service.
 * Obtain an instance using LwCommServiceProvider.
 * @author Miklos Pocsaji
 * @author Tamas Gyorgyey
 */
public interface LwCommService {

    /** This enum contains constants for dynamically modifying the runtime behavior of LwComm after initial configuration. */
    public enum AcceptMode {
        /** The message is accepted and answered with ACK. This is the default mode. */
        ACCEPT,
        /** The message is silently ignored and no ACK is sent.
         *  <p>Note: this causes the sender to retransmit the message until
         *  <br/>a) this node changes to ACCEPT mode again and sends an ACK to a retransmit; or
         *  <br/>b) the failover timer kicks in and the sender moves on to the next target
         *  </p> */
        DROP,
        /** The message is rejected with a NACK response.
         *  <p>Note: this causes an immediate sender side failover to the next target, if there is one.</p> */
        REJECT,
    }


    /**
     * Sends the message to the given target route.
     * @param targetRoute The name of the route in the configuration.
     * @param message The message to send.
     * @return A ListenableFuture object. The client can wait for the result using get() or
     * request callback using addListener()
     */
    SendResultFuture<SendResult> send(String targetRoute, TextMessage message);

    /**
     * Call this method to shut the communication subsystem down.
     */
    void shutdown();

    /**
     * Gets the configuration of the initialized subsystem.
     * @return A Configuration object.
     */
    Configuration getConfiguration();


    /**
     * Changes the message accept mode of the local node for the specified tags.
     * If the tags parameter is null or empty, the specified mode applies to all
     * previously configured tags and also becomes the default value for unconfigured tags.
     * The initial default mode is {@link AcceptMode#ACCEPT}.
     * @see TextMessageBuilder#setTag(String)
     */
    void setAcceptMode(AcceptMode mode, String... tags);
}
