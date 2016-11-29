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
package org.restcomm.imscf.el.sip.failover;

import static org.restcomm.imscf.el.sip.SipApplicationSessionAttributes.TIMER_KEEPS_APPSESSION_ALIVE;
import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.ExecutionLayerServerType;
import org.restcomm.imscf.common.config.HeartbeatConfigType;
import org.restcomm.imscf.common.config.ListenAddressType;
import org.restcomm.imscf.common.config.SipApplicationServerGroupType;
import org.restcomm.imscf.common.config.SipApplicationServerType;
import org.restcomm.imscf.common.config.ImscfConfigType.SipApplicationServers;
import org.restcomm.imscf.el.cap.sip.SipSessionAttributes;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.sip.routing.SipAsRouteAndInterface;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for selecting an available SIP AS for a SIP AS group.
 * This class should be reinitialized after a configuration change.
 */
public final class SipAsLoadBalancer {

    private static final Logger LOG = LoggerFactory.getLogger(SipAsLoadBalancer.class);
    private static volatile SipAsLoadBalancer instance;

    // instance fields
    // read-only after construction, no need for concurrency control
    private final Map<String, AsGroupAvailability> groupAvailabilities;
    private final List<AsAvailability> allHeartbeatEnabledAsEndpoints;
    private final HeartbeatConfigType heartbeatConfig;
    // modified on start/stop heartbeats
    private String heartbeatAppSessionId;
    private Map<String, ListenAddressType> outboundInterfaceMap;

    public static SipAsLoadBalancer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SipAsLoadBalancer should be initialized first");
        }
        return instance;
    }

    // there's no synchronization here, as getInstance does not initialize automatically, and this method is only called
    // when a configuration change occurs
    public static void initialize(ImscfConfigType config) {
        LOG.trace("SipAsLoadBalancer initializing...");
        SipAsLoadBalancer newInstance = new SipAsLoadBalancer(config);
        // construction succeeded, kill previous instance
        if (instance != null) {
            instance.stopHeartbeats();
        }
        instance = newInstance;
        LOG.trace("Done");
    }

    private SipAsLoadBalancer(ImscfConfigType config) {
        SipApplicationServers sipAsConfig = config.getSipApplicationServers();
        if (sipAsConfig == null) {
            LOG.info("No SIP AS configuration");
            // no SIP AS configured, no heartbeat config either.
            groupAvailabilities = Collections.emptyMap();
            allHeartbeatEnabledAsEndpoints = Collections.emptyList();
            heartbeatConfig = null;
            return;
        }

        initOutboundInterfaceMap(config);

        // SIP AS config present, but SIP AS list may still be empty.
        // groupAvailabilities = Collections.unmodifiableMap(sipAsConfig.getSipApplicationServerGroups().stream()
        //        .collect(Collectors.toMap(SipApplicationServerGroupType::getName, AsGroupAvailability::new)));
        groupAvailabilities = Collections.unmodifiableMap(sipAsConfig
                .getSipApplicationServerGroups()
                .stream()
                .collect(
                        Collectors.toMap(SipApplicationServerGroupType::getName, sasg -> new AsGroupAvailability(sasg,
                                this.outboundInterfaceMap))));

        allHeartbeatEnabledAsEndpoints = Collections.unmodifiableList(groupAvailabilities.values().stream()
                .flatMap(g -> g.getAll().stream()).filter(a -> a.getServer().isHeartbeatEnabled())
                .collect(Collectors.toList()));

        HeartbeatConfigType heartbeatConfig = sipAsConfig.getHeartbeatConfiguration();

        if (heartbeatConfig.getActiveIntervalSec() <= 0 && heartbeatConfig.getInactiveIntervalSec() <= 0) {
            // HB off
            if (!allHeartbeatEnabledAsEndpoints.isEmpty())
                throw new IllegalStateException(
                        "SIP AS heartbeat disabled (interval <= 0), but some server instances require heartbeat!");
            else
                // valid, but disabled
                this.heartbeatConfig = null;
        } else if (heartbeatConfig.getActiveIntervalSec() <= 0 || heartbeatConfig.getInactiveIntervalSec() <= 0) {
            throw new IllegalStateException(
                    "Invalid heartbeat configuration: activeIntervalSec and inactiveIntervalSec must both be enabled or disabled!");
        } else if (heartbeatConfig.getTimeoutSec() > 0
                && heartbeatConfig.getTimeoutSec() >= Math.max(heartbeatConfig.getActiveIntervalSec(),
                        heartbeatConfig.getInactiveIntervalSec())) {
            throw new IllegalStateException("SIP AS heartbeat timeout must be less than both heartbeat intervals!");
        } else {
            // valid and enabled
            this.heartbeatConfig = heartbeatConfig;
        }
        LOG.info("SIP AS load balancer configured.");
    }

    public synchronized void startHeartbeats() {
        if (heartbeatConfig == null) {
            LOG.info("SIP AS heartbeat disabled");
            return;
        }
        if (getAllHeartbeatEnabledEndpoints().isEmpty()) {
            LOG.info("No heartbeat enabled SIP AS instances");
            return;
        }

        LOG.info("Starting heartbeat messages for enabled SIP AS instances");
        SipApplicationSession sas = SipServletResources.getSipFactory().createApplicationSession();
        sas.setExpires(0);
        sas.setInvalidateWhenReady(true);
        TIMER_KEEPS_APPSESSION_ALIVE.set(sas, true);
        heartbeatAppSessionId = sas.getId();
        HeartbeatSenderTimerListener listener = new HeartbeatSenderTimerListener(heartbeatAppSessionId);

        long activePeriod = heartbeatConfig.getActiveIntervalSec() * 1000;
        long timeoutDelay = heartbeatConfig.getTimeoutSec() * 1000;
        long inactivePeriod = heartbeatConfig.getInactiveIntervalSec() * 1000;
        // using fixedDelay=false, otherwise the relative delay between the timers drifts away randomly
        // 0 initial delay: even if the period is high, we should get the first status update right on startup
        SipServletResources.createAppTimer(sas, 0, activePeriod, false,
                HeartbeatSenderTimerListener.TimeoutType.ACTIVE_PING, listener);
        SipServletResources.createAppTimer(sas, timeoutDelay, activePeriod, false,
                HeartbeatSenderTimerListener.TimeoutType.ACTIVE_TIMEOUT, listener);
        SipServletResources.createAppTimer(sas, 0, inactivePeriod, false,
                HeartbeatSenderTimerListener.TimeoutType.INACTIVE_PING, listener);
        SipServletResources.createAppTimer(sas, timeoutDelay, inactivePeriod, false,
                HeartbeatSenderTimerListener.TimeoutType.INACTIVE_TIMEOUT, listener);
    }

    public synchronized void stopHeartbeats() {
        if (heartbeatConfig == null || heartbeatAppSessionId == null)
            return;
        SipApplicationSession sas = SipServletResources.getSipSessionsUtil().getApplicationSessionById(
                heartbeatAppSessionId);
        if (sas.isValid())
            sas.invalidate();
        heartbeatAppSessionId = null;
    }

    public Collection<AsAvailability> getAllHeartbeatEnabledEndpoints() {
        return allHeartbeatEnabledAsEndpoints;
    }

    public FailoverContext newContext(String groupName) {
        return groupAvailabilities.get(groupName).newContext();
    }

    public SipApplicationServerType getNextAvailableAs(String groupName, FailoverContext ctx) {
        return groupAvailabilities.get(groupName).getNextAvailableServer(ctx);
    }

    /*
     * public SipURI getNextAvailableAsURI(String groupName, FailoverContext ctx) { return
     * groupAvailabilities.get(groupName).getNextAvailableServerURI(ctx); }
     */

    public SipAsRouteAndInterface getNextAvailableAsRouteAndInterface(String groupName, FailoverContext ctx) {
        return groupAvailabilities.get(groupName).getNextAvailableAsRouteAndInterface(ctx);
    }

    public void setAsUnavailable(String groupName, String serverName) {
        Optional<AsAvailability> oAva = Optional.ofNullable(groupAvailabilities.get(groupName)).map(
                ga -> ga.get(serverName));
        if (!oAva.isPresent()) {
            LOG.warn("No such AS: {}/{}", groupName, serverName);
            return;
        }
        AsAvailability ava = oAva.get();
        if (ava.getServer().isHeartbeatEnabled()) {
            ava.setAvailable(false);
            LOG.trace("setAsUnavailable({},{}): OK (until next heartbeat)", groupName, serverName);
        } else {
            LOG.trace("setAsUnavailable({},{}): ignored (heartbeat disabled)", groupName, serverName);
        }
    }

    AsAvailability getAsAvailability(SipSession s) {
        String groupName = SipSessionAttributes.SIP_AS_GROUP.get(s, String.class);
        String asName = SipSessionAttributes.SIP_AS_NAME.get(s, String.class);
        return groupAvailabilities.get(groupName).get(asName);
    }

    SipServletRequest createHeartbeatMessage(AsAvailability ava) throws ServletException {
        SipFactory sf = SipServletResources.getSipFactory();
        SipApplicationSession sas = SipServletResources.getSipSessionsUtil().getApplicationSessionById(
                heartbeatAppSessionId);
        SipServletRequest ret = sf.createRequest(sas, "OPTIONS", sf.createSipURI("heartbeat", "imscf"), ava.getUri());
        ret.getSession().setHandler("HeartbeatServlet");
        SipSessionAttributes.SIP_AS_GROUP.set(ret.getSession(), ava.getAsGroupName());
        SipSessionAttributes.SIP_AS_NAME.set(ret.getSession(), ava.getServer().getName());
        return ret;
    }

    public void processHeartbeatResponse(SipServletResponse resp) {
        SipSession s = resp.getSession(false);
        if (s == null || !s.isValid()) {
            LOG.warn("Cannot find SIP AS for HB response in nonexistent/invalidated SipSession. Call-ID: {}",
                    resp.getCallId());
            return;
        }
        AsAvailability ava = getAsAvailability(resp.getSession());
        LOG.trace("{}/{} HB response: {}", ava.getAsGroupName(), ava.getServer().getName(), resp.getStatus());
        ava.setAvailable(SipServletResponse.SC_OK == resp.getStatus());
    }

    private void initOutboundInterfaceMap(ImscfConfigType config) {
        LOG.debug("Initializing the outbound interface map...");
        this.outboundInterfaceMap = new HashMap<String, ListenAddressType>();
        SipApplicationServers sipAsConfig = config.getSipApplicationServers();
        if (sipAsConfig != null) {
            for (SipApplicationServerGroupType sasg : sipAsConfig.getSipApplicationServerGroups()) {
                for (SipApplicationServerType sas : sasg.getSipApplicationServer()) {
                    if (!outboundInterfaceMap.containsKey(sas.getHost())) {
                        // so far, there is no outbound interface for this address, so... now it has one

                        // this should match one of the EL listenAddresses
                        String interfaceAddressForDestination = getInterfaceAddressForDestination(sas.getHost());
                        LOG.debug("Found potential interface for host {} : {}", sas.getHost(),
                                interfaceAddressForDestination);

                        ListenAddressType listenAddressTypeForAddress = getListenAddressTypeForAddress(config,
                                interfaceAddressForDestination);

                        if (listenAddressTypeForAddress == null) {
                            throw new IllegalStateException(
                                    "Could not find a configured outbound interface for the following destination: "
                                            + sas.getHost());
                        }
                        outboundInterfaceMap.put(sas.getHost(), listenAddressTypeForAddress);
                        LOG.debug("Added to the Outbound Interface Map: key={}, value={}", sas.getHost(),
                                listenAddressTypeForAddress);
                    }
                }
            }
        }
    }

    private ListenAddressType getListenAddressTypeForAddress(ImscfConfigType config, String address) {
        for (ExecutionLayerServerType el : config.getServers().getExecutionLayerServers()) {
            if (el.getName().equals(ConfigBean.SERVER_NAME)) {
                for (ListenAddressType sla : el.getConnectivity().getSipListenAddresses()) {
                    if (sla.getHost().equals(address)) {
                        return sla;
                    }
                }
            }
        }
        return null;
    }

    private String getInterfaceAddressForDestination(String address) {
        String retVal = null;
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            ds.connect(InetAddress.getByName(address), 0);
            retVal = ds.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException ex) {
            throw new RuntimeException("Could not find outbound network for " + address); // NOPMD
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
        return retVal;
    }

    /** Opaque interface to hold failover state for a context, i.e. a call.*/
    public interface FailoverContext {
    }
}
