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
package org.restcomm.imscf.el.map.call;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

/**
 * Represents an AnyTimeInterrogation request.
 * @author Miklos Pocsaji
 *
 */
public class AtiRequest {

    private String targetNumber;
    private String targetRemoteSystem;
    private String subscribeFrom;
    private String subscribeContact;
    private String sipSessionId;

    public AtiRequest(SipServletRequest subscribeRequest) {
        // TODO
        subscribeFrom = subscribeRequest.getFrom().toString();
        subscribeContact = subscribeRequest.getHeader("Contact");

        targetNumber = "N/A";
        targetRemoteSystem = "N/A";
        URI touri = subscribeRequest.getTo().getURI();
        if (touri instanceof SipURI) {
            SipURI tosipuri = (SipURI) touri;
            targetNumber = tosipuri.getUser().replace("+", "");
            targetRemoteSystem = tosipuri.getHost();
        }
        sipSessionId = subscribeRequest.getSession().getId();
    }

    public String getTargetNumber() {
        return targetNumber;
    }

    public String getTargetRemoteSystem() {
        return targetRemoteSystem;
    }

    public String getSubscribeFrom() {
        return subscribeFrom;
    }

    public String getSubscribeContact() {
        return subscribeContact;
    }

    public String getSipSessionId() {
        return sipSessionId;
    }

    @Override
    public String toString() {
        return "AtiRequest [targetNumber=" + targetNumber + ", targetRemoteSystem=" + targetRemoteSystem
                + ", subscribeFrom=" + subscribeFrom + ", subscribeContact=" + subscribeContact + "]";
    }

}
