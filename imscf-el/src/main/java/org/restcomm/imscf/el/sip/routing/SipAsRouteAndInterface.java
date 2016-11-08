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
package org.restcomm.imscf.el.sip.routing;

import javax.servlet.sip.SipURI;

/** Class for holding together the asRoute and the corresponding outbound interface. */
public class SipAsRouteAndInterface {

    private SipURI asRoute;
    private String outboundInterfaceHost;
    private int outboundInterfacePort;

    public SipAsRouteAndInterface(SipURI asRoute, String outboundInterfaceHost, int outboundInterfacePort) {
        super();
        this.asRoute = asRoute;
        this.outboundInterfaceHost = outboundInterfaceHost;
        this.outboundInterfacePort = outboundInterfacePort;
    }

    public SipURI getAsRoute() {
        return asRoute;
    }

    public void setAsRoute(SipURI asRoute) {
        this.asRoute = asRoute;
    }

    public String getOutboundInterfaceHost() {
        return outboundInterfaceHost;
    }

    public void setOutboundInterfaceHost(String outboundInterfaceHost) {
        this.outboundInterfaceHost = outboundInterfaceHost;
    }

    public int getOutboundInterfacePort() {
        return outboundInterfacePort;
    }

    public void setOutboundInterfacePort(int outboundInterfacePort) {
        this.outboundInterfacePort = outboundInterfacePort;
    }

}
