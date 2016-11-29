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
package org.restcomm.imscf.common.util.history;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores a call history.
 * @author Miklos Pocsaji
 */
public class CallHistory {
    private static final boolean AUDITLOG_ENABLED = "enabled".equals(System.getProperty("imscf.audit.log"));

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";

    private static final ThreadLocal<SimpleDateFormat> SDF = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        }
    };

    private static final Logger AUDITLOG = LoggerFactory.getLogger("audit");

    private static final String SEPARATOR = "|";

    private long startMillis;
    private long lastEventMillis;
    private String imscfCallId;
    private List<String> events;

    public CallHistory(String imscfCallId) {
        long now = System.currentTimeMillis();
        startMillis = now;
        lastEventMillis = now;
        this.imscfCallId = imscfCallId;
        events = Collections.synchronizedList(new LinkedList<String>());
    }

    public void addEvent(String event) {
        if (AUDITLOG_ENABLED) {
            events.add(createEventString(event));
        }
    }

    public void log() {
        if (AUDITLOG_ENABLED) {
            AUDITLOG.info(createCallHistoryString());
        }
    }

    private String createCallHistoryString() {
        String start = String.format("%s - %20s", formatStart(), imscfCallId);
        StringBuilder ret = new StringBuilder(start);
        synchronized (events) {
            for (String event : events) {
                ret.append(" ").append(SEPARATOR).append(" ").append(event);
            }
		}
        int firstToLast = (int) (lastEventMillis - startMillis);
        int firstToPrint = (int) (System.currentTimeMillis() - startMillis);
        ret.append(" ").append(SEPARATOR).append(" to-last:").append(firstToLast).append(", to-print:")
                .append(firstToPrint);
        return ret.toString();
    }

    private String createEventString(String event) {
        long now = System.currentTimeMillis();
        int millisDelta = (int) (now - lastEventMillis);
        lastEventMillis = now;
        return String.format("+%d %s", millisDelta, event);
    }

    private String formatStart() {
        return SDF.get().format(new Date(startMillis));
    }

}
