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
package org.restcomm.imscf.common.lwcomm.service.messages;

import org.restcomm.imscf.common.lwcomm.service.impl.LwCommServiceImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores MessageSenders by outgoing message id.
 * @author Miklos Pocsaji
 *
 */
public class MessageSenderStore {

    private Map<String, MessageSender> store = new ConcurrentHashMap<String, MessageSender>();

    public void registerMessageSender(MessageSender ms) {
        // TODO limit number of stored objects!
        store.put(ms.getMessageId(), ms);
        LwCommServiceImpl.getServiceImpl().getStatistics().setMessageSenderStoreSize(store.size());
        LwCommServiceImpl.LOGGER.debug("MessageSender with id {} (hashcode: {}) registered, store size is {}",
                ms.getMessageId(), ms.getMessageId().hashCode(), store.size());
    }

    public void unregisterMessageSender(MessageSender ms) {
        MessageSender removed = store.remove(ms.getMessageId());
        LwCommServiceImpl.getServiceImpl().getStatistics().setMessageSenderStoreSize(store.size());
        LwCommServiceImpl.LOGGER.debug("MessageSender with key {} removed from store: {}, store size is {}",
                ms.getMessageId(), removed == null ? "failure" : "success", store.size());
    }

    public MessageSender getMessageSender(String messageId) {
        return store.get(messageId);
    }
}
