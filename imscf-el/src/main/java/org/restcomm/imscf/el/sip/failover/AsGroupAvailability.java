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

import org.restcomm.imscf.common.config.ListenAddressType;
import org.restcomm.imscf.common.config.MessageDistributionType;
import org.restcomm.imscf.common.config.SipApplicationServerGroupType;
import org.restcomm.imscf.common.config.SipApplicationServerType;
import org.restcomm.imscf.el.sip.failover.SipAsLoadBalancer.FailoverContext;
import org.restcomm.imscf.el.sip.routing.SipAsRouteAndInterface;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for keeping track of AS availability in a group and selecting
 * a server based on the load balancing policy of the group.
 */
class AsGroupAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(AsGroupAvailability.class);

    private final MessageDistributionType distributionType;
    private final AsAvailability[] asAvailability;
    private final Map<String, AsAvailability> asAvailabilityAsMap;
    private final ThreadLocal<LoadBalancer> loadBalancer;

    AsGroupAvailability(SipApplicationServerGroupType group, Map<String, ListenAddressType> outboundInterfaces) {
        this.distributionType = group.getDistribution();

        this.asAvailability = group
                .getSipApplicationServer()
                .stream()
                .map(sas -> new AsAvailability(group.getName(), sas, outboundInterfaces.get(sas.getHost()).getHost(),
                        outboundInterfaces.get(sas.getHost()).getPort())).toArray(AsAvailability[]::new);
        // read-only map, no need for ConcurrentMap
        this.asAvailabilityAsMap = Collections.<String, AsAvailability> unmodifiableMap(Stream.of(asAvailability)
                .collect(Collectors.toMap(ava -> ava.getServer().getName(), Function.identity())));

        if (this.distributionType == MessageDistributionType.LOADBALANCE) {
            // starting index is randomly chosen for each thread
            this.loadBalancer = ThreadLocal.withInitial(() -> new RoundRobinLoadBalancer(asAvailability.length,
                    ThreadLocalRandom.current().nextInt(asAvailability.length)));
        } else {
            this.loadBalancer = null;
        }
    }

    private AsAvailability getNextAvailable(FailoverContextImpl ctx) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("getNextAvailable({}, tried {}) for {}", distributionType, ctx, Arrays.toString(asAvailability));
        }

        if (distributionType == MessageDistributionType.FAILOVER) {
            for (int current = 0; current < asAvailability.length; current++) {
                AsAvailability a = asAvailability[current];
                if (a.isAvailable() && !ctx.usedAlready(current)) {
                    ctx.markUsed(current);
                    return a;
                }
            }
            return null;
        } else {
            // getNextAvailableX() -> getNextAvailable() -> {this block} may be called by different threads, but
            // synchronization is not desirable. Instead, a ThreadLocal Loadbalancer is used for storing the last
            // used instance index. Thus each thread will balance on its own with no coordination among them, except
            // for choosing the initial index, which is determined by a shared random generator.
            LoadBalancer threadLocalLB = loadBalancer.get();
            int current = threadLocalLB.next(), initial = current;
            while (!asAvailability[current].isAvailable() || ctx.usedAlready(current)) {
                current = threadLocalLB.next();
                if (current == initial) { // we cycled all the instances
                    LOG.trace("Cycled all starting from {}, none available", initial);
                    return null;
                }
            }
            LOG.trace("Returning {}", current);
            ctx.markUsed(current);
            return asAvailability[current];
        }
    }

    SipApplicationServerType getNextAvailableServer(FailoverContext ctx) {
        AsAvailability ava = getNextAvailable((FailoverContextImpl) ctx);
        return ava != null ? ava.getServer() : null;
    }

    /*
     * SipURI getNextAvailableServerURI(FailoverContext ctx) { AsAvailability ava =
     * getNextAvailable((FailoverContextImpl) ctx); return ava != null ? ava.getUri() : null; }
     */
    SipAsRouteAndInterface getNextAvailableAsRouteAndInterface(FailoverContext ctx) {
        AsAvailability nextAvailable = getNextAvailable((FailoverContextImpl) ctx);
        return nextAvailable != null ? nextAvailable.getSipAsRouteAndInterface() : null;
    }

    List<AsAvailability> getAll() {
        return Collections.unmodifiableList(Arrays.asList(asAvailability));
    }

    AsAvailability get(String name) {
        return asAvailabilityAsMap.get(name);
    }

    FailoverContext newContext() {
        return new FailoverContextImpl(asAvailability.length);
    }

    /** A simple BitSet to hold flags for already used instances. */
    @SuppressWarnings("serial")
    private static final class FailoverContextImpl extends BitSet implements FailoverContext {
        public FailoverContextImpl(int size) {
            super(size);
        }

        public boolean usedAlready(int index) {
            return get(index);
        }

        public void markUsed(int index) {
            set(index);
        }
    }
}
