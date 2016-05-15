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
package org.restcomm.imscf.sl.history;

import org.restcomm.imscf.common.util.ImscfCallId;
import org.restcomm.imscf.common.util.history.CallHistory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores call history for an ImscfCallId.
 * @author Miklos Pocsaji
 *
 */
public class SlCallHistoryStore {

    private Map<ImscfCallId, CallHistory> callHistoryMap = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(SlCallHistoryStore.class);

    public SlCallHistoryStore() {
        // Empty constructor
    }

    public void registerEvent(ImscfCallId imscfCallId, Event event, String... parameters) {
        CallHistory ch = callHistoryMap.get(imscfCallId);
        if (ch == null) {
            ch = new CallHistory(imscfCallId.toString());
            callHistoryMap.put(imscfCallId, ch);
        }
        ch.addEvent(event.toEventString(parameters));
    }

    public void logAndRemoveCallHistory(ImscfCallId imscfCallId) {
        CallHistory ch = callHistoryMap.remove(imscfCallId);
        if (ch == null) {
            LOG.warn("No call history found for imscf call id {}", imscfCallId);
        } else {
            ch.log();
        }
    }

}
