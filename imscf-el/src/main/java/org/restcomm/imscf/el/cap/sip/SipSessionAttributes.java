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
package org.restcomm.imscf.el.cap.sip;

import java.util.Objects;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Named constants for SipSession attributes with value type information.
 */
public enum SipSessionAttributes {
    LEG_ID(String.class), INITIAL_REQUEST(SipServletRequest.class), INITIAL_RESPONSE(SipServletResponse.class), MRF_ALIAS(
            String.class), MRF_CS_ID(Integer.class),
    /** Set to true if a CANCEL or BYE request was sent as UAC for this session. */
    UAC_DISCONNECTED(Object.class),
    /** SIP AS ID stored for AS heartbeat messages. */
    SIP_AS_GROUP(String.class),
    /** SIP AS ID stored for AS heartbeat messages. */
    SIP_AS_NAME(String.class);

    private static final Logger LOG = LoggerFactory.getLogger(SipSessionAttributes.class);

    private final String propertyName;
    private final Class<?> valueType;

    private SipSessionAttributes(Class<?> type) {
        this.propertyName = SipSessionAttributes.class.getName() + "." + name();
        this.valueType = type;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public <T> void set(SipSession session, T value) {
        Objects.requireNonNull(session, "SipSession must not be null");
        if (valueType.isAssignableFrom(value.getClass())) {
            session.setAttribute(propertyName, value);
        } else {
            throw new IllegalArgumentException("Property value for " + name() + " must be of type " + valueType);
        }
    }

    public void remove(SipSession session) {
        Objects.requireNonNull(session, "SipSession must not be null");
        session.removeAttribute(propertyName);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(SipSession session, Class<T> type) {
        Objects.requireNonNull(session, "SipSession must not be null");
        if (!session.isValid()) {
            LOG.warn("Tried to get {} attribute of already invalidated session {}", this, session.getId());
            return null;
        }
        Object o = session.getAttribute(propertyName);
        if (o == null || type.isAssignableFrom(o.getClass())) {
            return (T) o;
        }
        throw new IllegalArgumentException("Property value for " + name() + " must be of type " + valueType);
    }

    public static void removeAll(SipSession session) {
        Objects.requireNonNull(session, "SipSession must not be null");
        if (!session.isValid())
            return; // nothing to do

        for (SipSessionAttributes a : values()) {
            a.remove(session);
        }
    }
}
