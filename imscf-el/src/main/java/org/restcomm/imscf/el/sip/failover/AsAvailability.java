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
package org.restcomm.imscf.el.sip.failover;

import org.restcomm.imscf.common.config.SipApplicationServerType;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.sip.ImmutableSipURI;
import org.restcomm.imscf.el.statistics.ElStatistics;

import java.util.Objects;

import javax.servlet.sip.SipURI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Availability wrapper for a single AS, containing status and cached uri. */
class AsAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(AsAvailability.class);

    private final SipApplicationServerType server;
    private final String asGroupName;
    private final SipURI uri;

    private boolean available; // maybe store last available timestamp instead, or both

    AsAvailability(String asGroupName, SipApplicationServerType server) {
        this.asGroupName = Objects.requireNonNull(asGroupName);
        this.server = Objects.requireNonNull(server);
        this.uri = new ImmutableSipURI(SipUtil.createAppServerRoutingAddress(server));
        // if heartbeat is enabled, the AS is considered unavailable until a successful heartbeat
        // otherwise, it is considered always available
        setAvailable(server.isHeartbeatEnabled() ? false : true);
    }

    SipApplicationServerType getServer() {
        return server;
    }

    SipURI getUri() {
        return uri;
    }

    boolean isAvailable() {
        return available;
    }

    String getAsGroupName() {
        return asGroupName;
    }

    void setAvailable(boolean available) {
        if (this.available != available) {
            if (available) {
                LOG.info("{}/{} is now available", asGroupName, server.getName());
            } else {
                LOG.warn("{}/{} is now unavailable", asGroupName, server.getName());
            }
        }
        this.available = available;
        ElStatistics.setSipAsReachable(asGroupName, server.getName(), available);
    }

    @Override
    public String toString() {
        return "AsAvailability [" + asGroupName + "/" + server.getName() + "/"
                + (available ? "available" : "unavailable") + "]";
    }

}
