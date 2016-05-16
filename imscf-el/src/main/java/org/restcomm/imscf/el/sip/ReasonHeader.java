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

import org.restcomm.imscf.el.sip.servlets.SipServletResources;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for parsing SIP Reason headers. */
public class ReasonHeader {

    private static final Logger LOG = LoggerFactory.getLogger(ReasonHeader.class);

    private String protocol;
    private Integer cause;
    private String text;

    protected void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    protected void setCause(Integer cause) {
        this.cause = cause;
    }

    protected void setText(String text) {
        this.text = text;
    }

    public String getProtocol() {
        return protocol;
    }

    public Integer getCause() {
        return cause;
    }

    public String getText() {
        return text;
    }

    protected ReasonHeader() {
        // restricted to subclasses
    }

    public ReasonHeader(String protocol, Integer cause, String text) {
        this.protocol = protocol;
        this.cause = cause;
        this.text = text;
    }

    public static <T extends ReasonHeader> T parse(SipServletMessage msg, Function<String, Boolean> protocolAcceptor,
            Supplier<T> constructor) throws ServletParseException {
        T ret;
        for (Iterator<String> i = msg.getHeaders("Reason"); i.hasNext();) {
            // mobicents stack says "Reason header is not parameterable !" when in fact it is,
            // so we create one manually
            Parameterable p = SipServletResources.getSipFactory().createParameterable(i.next());
            ret = parse(p, protocolAcceptor, constructor);
            if (ret != null)
                return ret;
        }
        return null;
    }

    public static <T extends ReasonHeader> T parse(Parameterable header, Function<String, Boolean> protocolAcceptor,
            Supplier<T> constructor) {
        String protocol = header.getValue();
        if (protocolAcceptor.apply(protocol)) {
            T ret = constructor.get();
            ret.setProtocol(protocol);
            String causeParam = header.getParameter("cause");
            try {
                ret.setCause(causeParam == null ? null : Integer.valueOf(Integer.parseInt(causeParam)));
            } catch (NumberFormatException e) {
                LOG.warn("Invalid Reason header cause parameter: {}", causeParam, e);
                return null;
            }
            ret.setText(header.getParameter("text"));
            return ret;
        }
        return null;
    }

    public void insertAsHeader(SipServletMessage msg) {
        String reason = protocol;
        if (cause != null)
            reason += ";cause=" + cause;
        if (text != null)
            reason += ";text=\"" + text + "\"";
        msg.addHeader("Reason", reason);
    }
}
