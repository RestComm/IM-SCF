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
package org.restcomm.imscf.common.util;

import static java.lang.Character.MAX_RADIX;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IMSCF Call ID class, sort of a time based UUID tailored to IMSCF needs.
 */
public final class ImscfCallId {
    private static final String NODE = Optional.ofNullable(System.getProperty("jboss.server.name")).orElseThrow(
            () -> new RuntimeException("System property jboss.server.name not found!"));
    private static final Pattern PARSER = Pattern.compile("(.*)[.]([0-9a-zA-Z]+)[.]([0-9a-zA-Z]+)");

    // note: the VM would have to run for millennia for this to overflow at the expected call rate,
    // so we don't handle it
    private static final AtomicLong CALLCOUNTER = new AtomicLong(0);

    // node is stored because the class can also be used to parse ImscfCallIds generated on other nodes.
    private final String node;
    private final long timestamp;
    private final long sequence;
    private final transient String tostring;
    private final transient int hashCode;
    private transient String toHumanReadableString;

    private ImscfCallId(String node, long timestamp, long sequence) {
        this.node = node;
        this.timestamp = timestamp;
        this.sequence = sequence;
        tostring = new StringBuilder().append(this.node).append('.').append(Long.toString(this.timestamp, MAX_RADIX))
                .append('.').append(Long.toString(this.sequence, MAX_RADIX)).toString();

        final int prime = 31;
        int result = 1;
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        result = prime * result + (int) (sequence ^ (sequence >>> 32));
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        hashCode = result;
    }

    /** Returns the originating node ID. */
    public String getNode() {
        return node;
    }

    /** Returns the time of generation. */
    public long getTimestamp() {
        return timestamp;
    }

    /** Returns the call sequence number. */
    public long getSequence() {
        return sequence;
    }

    @Override
    public String toString() {
        return tostring;
    }

    public String toHumanReadableString() {
        if (toHumanReadableString == null)
            toHumanReadableString = new StringBuilder(tostring)
                    .append("[call ")
                    .append(sequence)
                    .append(" @ ")
                    .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS").format(
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())))
                    .append(" ]").toString();
        return toHumanReadableString;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImscfCallId other = (ImscfCallId) obj;
        if (sequence != other.sequence)
            return false;
        if (timestamp != other.timestamp)
            return false;
        if (node == null) {
            return other.node == null;
        } else {
            return node.equals(other.node);
        }
    }

    /**
     * Generates a new IMSCF call id. This method can be called concurrently without external synchronization.
     * <p>
     * The generated ID contains the node id, timestamp and sequence number and is guaranteed to be unique
     * for each invocation on the IMSCF application level.
     * </p>
     * <p>Note that this method is neither cryptographically secure (as the generated IDs are easily predictable),
     *  nor does it provide IDs that are very different from each other (as would be the case with a random UUID).</p>
     */
    public static ImscfCallId generate() {
        return new ImscfCallId(NODE, System.currentTimeMillis(), CALLCOUNTER.incrementAndGet());
    }

    public static ImscfCallId parse(String imscfCallId) {

        Matcher m = PARSER.matcher(imscfCallId);
        if (!m.matches()) {
            return null;
        }
        return new ImscfCallId(m.group(1), Long.parseLong(m.group(2), MAX_RADIX), Long.parseLong(m.group(3), MAX_RADIX));
    }

    public static void main(String[] args) {
        Thread[] ts = new Thread[100];
        for (int i = 0; i < 100; i++) {
            ts[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    for (int j = 0; j < 100; j++) {
                        ImscfCallId id = ImscfCallId.generate();
                        System.out.println(id + "  ->  " + ImscfCallId.parse(id.toString()).toHumanReadableString()
                                + " // " + Thread.currentThread().getName());
                    }
                }
            });
        }
        for (int i = 0; i < ts.length; i++) {
            ts[i].start();
        }
    }

}
