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
package org.restcomm.imscf.el.sip;

import org.restcomm.imscf.util.IteratorStream;

import java.util.Objects;

import javax.servlet.sip.SipApplicationSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immutable named constants for SipApplicationSession attributes with value type information.
 *
 * Note: this is not an enum to allow generic type parameter.
 *
 *  @param <V> Value type
 */
public final class SipApplicationSessionAttributes<V> {
    private static final Logger LOG = LoggerFactory.getLogger(SipApplicationSessionAttributes.class);
    private static final String ATTRIBUTE_NAME_PREFIX = SipApplicationSessionAttributes.class.getName() + ".";

    /** Set if the appsession should be kept alive even without SipSessions present. */
    public static final SipApplicationSessionAttributes<Boolean> TIMER_KEEPS_APPSESSION_ALIVE = new SipApplicationSessionAttributes<Boolean>(
            "TIMER_KEEPS_APPSESSION_ALIVE", Boolean.class);

    /** Set if the SIP module has already determined that this SIP call is finished.
     * This flag is used to enforce the prohibition of further request/dialog creation in such a session. */
    public static final SipApplicationSessionAttributes<Boolean> SIP_CALL_FINISHED = new SipApplicationSessionAttributes<Boolean>(
            "SIP_CALL_FINISHED", Boolean.class);

    private final String name;
    private final String propertyName;
    private final Class<V> valueType;

    private SipApplicationSessionAttributes(String name, Class<V> type) {
        this.name = name;
        this.propertyName = ATTRIBUTE_NAME_PREFIX + name;
        this.valueType = type;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class<V> getValueType() {
        return valueType;
    }

    public void set(SipApplicationSession session, V value) {
        Objects.requireNonNull(session, "SipApplicationSession must not be null");
        if (valueType.isAssignableFrom(value.getClass())) {
            session.setAttribute(propertyName, value);
        } else {
            throw new IllegalArgumentException("Property value for " + name + " must be of type " + valueType);
        }
    }

    public void remove(SipApplicationSession session) {
        Objects.requireNonNull(session, "SipApplicationSession must not be null");
        session.removeAttribute(propertyName);
    }

    @SuppressWarnings("unchecked")
    public V get(SipApplicationSession sas) {
        Objects.requireNonNull(sas, "SipApplicationSession must not be null");
        if (!sas.isValid()) {
            LOG.warn("Tried to get {} attribute of already invalidated SipApplicationSession {}", this, sas.getId());
            return null;
        }
        Object o = sas.getAttribute(propertyName);
        if (o == null || valueType.isAssignableFrom(o.getClass())) {
            return (V) o;
        }
        throw new IllegalArgumentException("Property value for " + name + " must be of type " + valueType);
    }

    public static void removeAll(SipApplicationSession sas) {
        Objects.requireNonNull(sas, "SipApplicationSession must not be null");
        if (!sas.isValid())
            return; // nothing to do

        IteratorStream.of(sas.getAttributeNames()).forEach(a -> {
            if (a.startsWith(ATTRIBUTE_NAME_PREFIX))
                sas.removeAttribute(a);
        });
    }
}
