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
package org.restcomm.imscf.el.sip.servlets;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.annotation.SipListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** App level SipServletListener. */
@SipListener
public class SipServletListenerImpl implements SipServletListener {

    private static final Logger LOG = LoggerFactory.getLogger(SipServletListenerImpl.class);

    private static CountDownLatch servletContextInitialized = new CountDownLatch(1);

    public static boolean waitForSipContext(long maxTime, TimeUnit timeUnit) {
        try {
            return servletContextInitialized.await(maxTime, timeUnit);
        } catch (InterruptedException e) {
            LOG.warn("Thread interrupted while waiting for SIP servlet context to initialize.");
            return false;
        }
    }

    @Override
    public void servletInitialized(SipServletContextEvent ce) {
        LOG.debug("servletInitialized: {}/{}", ce.getServletContext().getContextPath(), ce.getSipServlet()
                .getServletName());

        if (ce.getSipServlet() instanceof MainServlet) {
            LOG.info("SIP initialized for outgoing messages");
            servletContextInitialized.countDown();
        }
    }
}
