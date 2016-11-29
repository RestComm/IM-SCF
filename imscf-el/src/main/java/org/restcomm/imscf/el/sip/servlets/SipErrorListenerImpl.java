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
package org.restcomm.imscf.el.sip.servlets;

import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.stack.CallContext;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;

import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.servlet.sip.SipErrorEvent;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.annotation.SipListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** App level SipSession error listener. */
@SipListener
public class SipErrorListenerImpl implements SipErrorListener {

    private static final Logger LOG = LoggerFactory.getLogger(SipErrorListenerImpl.class);

    @EJB
    private transient CallStore callStore;
    @EJB
    private transient CallFactoryBean callFactory;

    @EJB
    private transient ConfigBean config;

    @PostConstruct
    private void init() {
        Objects.requireNonNull(callStore, "CallStore not found");
        Objects.requireNonNull(config, "ConfigBean not found");
        Objects.requireNonNull(callFactory, "CallFactory not found");
    }

    @Override
    public void noAckReceived(SipErrorEvent arg0) {
        try (ContextLayer cl = CallContext.with(callStore, config, callFactory)) {
            try (IMSCFCall call = CallContext.getCallStore().getSipCall(arg0.getRequest())) {
                LOG.warn("No ACK received in {} for response:\n{}\n to the request:\n{}", call, arg0.getResponse(),
                        arg0.getRequest());
                // TODO: handle if not for an error response?
            }
        }
    }

    @Override
    public void noPrackReceived(SipErrorEvent arg0) {
        try (ContextLayer cl = CallContext.with(callStore, config, callFactory)) {
            try (IMSCFCall call = CallContext.getCallStore().getSipCall(arg0.getRequest())) {
                LOG.warn("No PRACK received in {} for response:\n{}\n to the request:\n{}", call, arg0.getResponse(),
                        arg0.getRequest());
                // TODO error response, release?
            }
        }
    }

}
