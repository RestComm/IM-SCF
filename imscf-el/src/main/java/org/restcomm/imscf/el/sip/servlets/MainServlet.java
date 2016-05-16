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

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.ExecutionLayerServerType;
import org.restcomm.imscf.el.cap.sip.SipUtil;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.common.util.ImscfCallId;

import java.io.IOException;
import java.util.Collection;
import java.util.ListIterator;
import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.annotation.SipApplicationKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main SIP servlet of the EL application, responsible for servlet mapping and appsession handling.
 */
@javax.servlet.sip.annotation.SipServlet(loadOnStartup = 1, name = "MainServlet")
public class MainServlet extends SipServlet {

    private static final Logger LOG = LoggerFactory.getLogger(MainServlet.class);
    private static final long serialVersionUID = 1L;
    private static final String SAS_ATTR_APPSESSIONKEY = MainServlet.class.getName() + ".APPSESSIONKEY";
    private static final String ROUTE_PARAM_APPSESSIONKEY = "encodeuri";

    private boolean isSipUsed;

    @EJB
    private transient ConfigBean configBean;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        SipServletResources.init(config.getServletContext());
        if (configBean.isCAPUsed() || configBean.isMAPUsed()) {
            // Optional.ofNullable(configBean.getConfig().getSipApplicationServers())
            // .map(s -> s.getSipApplicationServerGroups()).ifPresent(ignored -> {
            // SipAsLoadBalancer.initialize(configBean.getConfig()); // depends on SipServletResources
            // });
            configBean.setLocalSipURI(findLocalSipURI(config.getServletContext(), configBean.getConfig()));
            isSipUsed = true;
            LOG.info("MainServlet init done");
        } else {
            LOG.info("SIP disabled");
        }
    }

    @Override
    protected void doRequest(SipServletRequest req) throws ServletException, IOException {
        if (!isSipUsed) {
            LOG.warn(
                    "Incoming SIP {} from {} [via: {}], but SIP is disabled because SIP modules are not configured. Responding with 503.",
                    req.getMethod(), req.getInitialRemoteAddr(), req.getHeader("Via"));
            SipUtil.createAndSetWarningHeader(req.createResponse(SipServletResponse.SC_SERVICE_UNAVAILABLE),
                    "No SIP module configured").send();
            return;
        }

        if (req.isInitial()) {
            switch (req.getMethod()) {
            case "OPTIONS":
                LOG.debug("forwarding initial OPTIONS to HeartbeatServlet");
                req.getSession().setHandler("HeartbeatServlet");
                req.getRequestDispatcher("HeartbeatServlet").forward(req, null);
                break;
            case "INVITE":
                LOG.debug("forwarding initial INVITE to CAPServlet");
                req.getSession().setHandler("CAPServlet");
                req.getRequestDispatcher("CAPServlet").forward(req, null);
                break;
            case "SUBSCRIBE":
                LOG.debug("forwarding initial SUBSCRIBE to MAPServlet");
                req.getSession().setHandler("MAPServlet");
                req.getRequestDispatcher("MAPServlet").forward(req, null);
                break;
            default:
                LOG.error("No handler for initial request method {}", req.getMethod());
                SipServletResponse resp = req.createResponse(SipServletResponse.SC_BAD_REQUEST);
                SipUtil.createAndSetWarningHeader(resp, "No handler for initial request.");
                resp.send();
                break;
            }
        } else {
            LOG.error("Request is not initial!\n{}", req);
            SipServletResponse resp = req.createResponse(SipServletResponse.SC_BAD_REQUEST);
            SipUtil.createAndSetWarningHeader(resp, "No handler for non-initial request.");
            resp.send();
        }
    }

    @Override
    protected void doResponse(SipServletResponse resp) throws ServletException, IOException {
        LOG.error("No handler for response:\n{}", resp);
    }

    @SipApplicationKey(applicationName = "imscf-el")
    public static String sipApplicationKey(SipServletRequest req) {
        String appsessionkey = null;
        SipURI uri = getTopMostRouteHeader(req);
        if (uri != null) {
            appsessionkey = uri.getParameter(ROUTE_PARAM_APPSESSIONKEY);
        }

        if (appsessionkey != null) {
            LOG.debug("sipApplicationKey consulted, returning found appsessionkey: [{}]", appsessionkey);
        } else {
            appsessionkey = getAppSessionKey(createAppSession());
            LOG.debug(
                    "sipApplicationKey consulted, no '{}' parameter in Route header. Returning generated new appsession key: [{}]",
                    ROUTE_PARAM_APPSESSIONKEY, appsessionkey);
        }
        return appsessionkey;

    }

    public static SipURI getTopMostRouteHeader(SipServletRequest req) {
        Address route = req.getPoppedRoute();
        if (route == null) {
            try {
                ListIterator<Address> li = req.getAddressHeaders("Route");
                if (li.hasNext()) {
                    route = li.next();
                }
            } catch (ServletParseException ignored) {
                LOG.warn("Error parsing initial message.", ignored);
                return null;
            }
        }
        if (route != null)
            return (SipURI) route.getURI(); // a Route URI can only be SIP
        else
            return null;
    }

    public static SipURI findLocalSipURI(ServletContext ctx) {
        SipURI uri = null;
        @SuppressWarnings("unchecked")
        Collection<SipURI> allOutbound = (Collection<SipURI>) ctx.getAttribute(SipServlet.OUTBOUND_INTERFACES);
        LOG.debug("all outbound interfaces: {}", allOutbound);
        for (SipURI s : allOutbound) {
            if ("udp".equalsIgnoreCase(s.getTransportParam())) {
                uri = s;
                break;
            } else if ("tcp".equalsIgnoreCase(s.getTransportParam())) {
                uri = s;
                // continue to search for udp
            } else if (uri == null) {
                uri = s;
            }
        }
        LOG.debug("Local SIP URI chosen from all interfaces: {}", uri);
        return (SipURI) uri.clone();
    }

    public static SipURI findLocalSipURI(ServletContext ctx, ImscfConfigType config) {
        for (ExecutionLayerServerType el : config.getServers().getExecutionLayerServers()) {
            if (el.getName().equals(ConfigBean.SERVER_NAME)) {
                SipFactory sf = (SipFactory) ctx.getAttribute(SIP_FACTORY);
                SipURI s = sf.createSipURI("", el.getConnectivity().getSipListenAddress().getHost());
                s.setPort(el.getConnectivity().getSipListenAddress().getPort());
                s.setTransportParam("udp");
                LOG.debug("Local SIP URI derived from config: {}", s);
                return s;
            }
        }
        return findLocalSipURI(ctx);
    }

    public static String generateNewAppSessionKey() {
        return ImscfCallId.generate().toString();
    }

    public static SipURI prepareBackRouteURI(SipURI backRoute, String appSessionKey) {
        backRoute.setParameter(ROUTE_PARAM_APPSESSIONKEY, appSessionKey);
        backRoute.setLrParam(true);
        return backRoute;
    }

    public static String getAppSessionKey(SipApplicationSession sas) {
        return (String) sas.getAttribute(SAS_ATTR_APPSESSIONKEY);
    }

    /** Looks up or creates, then sets up a new appsession with the given key. */
    public static SipApplicationSession initAppSession(String key) {
        SipApplicationSession ret = SipServletResources.getSipSessionsUtil().getApplicationSessionByKey(key, true);
        ret.setAttribute(SAS_ATTR_APPSESSIONKEY, key);
        return ret;
    }

    /** Creates and sets up a new appsession. */
    public static SipApplicationSession createAppSession() {
        return initAppSession(generateNewAppSessionKey());
    }

}
