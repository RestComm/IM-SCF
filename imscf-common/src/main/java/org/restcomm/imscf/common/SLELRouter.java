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
package org.restcomm.imscf.common ;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.ImscfConfigType.Sccp;
import org.restcomm.imscf.common.config.SccpRemoteProfileType;
import org.restcomm.imscf.common.config.TcapTransactionIdRangeType;
import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.config.Route;
import org.restcomm.imscf.common.lwcomm.config.Route.Mode;
import org.restcomm.imscf.common.lwcomm.service.LwCommService;
import org.restcomm.imscf.common.lwcomm.service.LwCommServiceProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapping between SCCP+TCAP dialog identifiers and serving SL/EL node on the other side of the SL-EL communication.
 * @param <Data> the type of data stored in the mappings.
 */
public class SLELRouter<Data> {
    private static final Logger LOG = LoggerFactory.getLogger(SLELRouter.class);

    // Each TCAP dialog is handled by a single EL node. The TCAP dialog is identified by its local and remote
    // Transaction IDs, appearing as OTID and DTID in the TCAP messages, depending on the direction. However, a TCAP
    // dialog identifier is unique only within a communication channel between a pair of SCCP subsystems. An SCCP
    // subsystem is identified by its GT and SSN.
    // For example, the following communications could all take place at the same time:
    // * GT_MSS0+SSN_CAP, TID_1 <-> TID_1, GT_IMSCF+SSN_CAP => EL1
    // * GT_MSS0+SSN_MAP, TID_1 <-> TID_1, GT_IMSCF+SSN_MAP => EL1 //same GT, diff SSN, same TID
    // * GT_MSS1+SSN_CAP, TID_1 <-> TID_2, GT_IMSCF+SSN_CAP => EL1 //diff GT, same SSN, same remote TID
    // * GT_MSS2+SSN_CAP, TID_1 <-> TID_3, GT_IMSCF+SSN_CAP => EL2
    //
    // Note:
    // EL servers have non-overlapping TCAP id ranges, so a local SSN + TCAP TID (if present) always uniquely identifies
    // the EL node.

    /** Stores mapping separated by local SSN only. Dialogs will only be stored here if they have a local TID, which is always unique. */
    private SccpLevelMapping<Data> localOnlyMapping;
    /** Stores mapping separated by remoteGT+remoteSSN+localSSN. */
    private SccpLevelMapping<Data> mapping;
    private int expectedConcurrentTcapDialogCount;

    /** Route looked up once and stored here, as it never changes due to the immutable nature of the LwComm configuration, unless
     * the service is reinitialized. */
    private Route loadbalanceRouteToOtherLayer;
    private LwCommService lwcommService; // stored only to detect a need to compute the above again

    /** Simple Wrapper to format a timestamp for logging. */
    private static class TimeStampFormatter {
        private static final ThreadLocal<SimpleDateFormat> DATEFORMAT = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            };
        };
        Date date;

        public TimeStampFormatter(long timestamp) {
            this.date = new Date(timestamp);
        }

        @Override
        public String toString() {
            return DATEFORMAT.get().format(date);
        }
    }

    /** Data stored with a node mapping. */
    static class NodeMapping<Data> {
        long timeOfMapping;
        Data userData;

        @Override
        public String toString() {
            return "[" + userData + " @ " + new TimeStampFormatter(timeOfMapping).toString() + "]";
        }
    }

    /** Fixed-type ConcurrentHashMap. */
    private static class SccpLevelMapping<Data> extends ConcurrentHashMap<SccpDialogId, TcapLevelMapping<Data>> {
        private static final long serialVersionUID = 1L;

        public SccpLevelMapping(int expectedConcurrentSccpConnectionCount) {
            super(expectedConcurrentSccpConnectionCount);
        }
    }

    /**Internal class for finding the location of a mapping record. */
    private static class Storage<Data> {
        TcapLevelMapping<Data> tcapLevelMapping;
        NodeMapping<Data> nodeMapping;

        public Storage(TcapLevelMapping<Data> tcapLevelMapping, NodeMapping<Data> nodeMapping) {
            this.tcapLevelMapping = tcapLevelMapping;
            this.nodeMapping = nodeMapping;
        }

    }

    private Storage<Data> findCurrentStorage(SccpDialogId sdid, TcapDialogId tdid) {

        TcapLevelMapping<Data> tm;
        NodeMapping<Data> m;

        // look in local side first, but don't fail if not found
        tm = localOnlyMapping.get(sdid.localSideOnly());
        if (tm != null) {
            m = tm.get(tdid);
            if (m != null) {
                LOG.trace("Found mapping in local only storage");
                return new Storage<>(tm, m);
            }
        }

        tm = mapping.get(sdid);
        if (tm == null) {
            LOG.trace("No node mapping exists for {}", sdid);
            return null;
        }
        m = tm.get(tdid);
        if (m == null) {
            LOG.trace("No node mapping exists for {}", tdid);
            return null;
        }
        LOG.trace("Found mapping in main storage");
        return new Storage<>(tm, m);
    }

    /**
     * Returns the mapped data for the given key.
     * <h1>Concurrency</h1> May be called concurrently for unrelated key pairs. */
    public Data getMappingData(SccpDialogId sdid, TcapDialogId tdid) {
        Storage<Data> currentStorage = findCurrentStorage(sdid, tdid);
        if (currentStorage == null) {
            LOG.trace("Lookup failed for {}/{}.", sdid, tdid);
            return null;
        }
        Data ret = currentStorage.nodeMapping.userData;
        LOG.debug("Found node mapping: {} / {} -> {}", sdid, tdid, ret);
        return ret;
    }

    // note: TcapLevelMapping objects "leak" as they are never removed, but their number is defined
    // by the number of SccpDialogId-s in use, which is limited by the actual connections of the SL,
    // which is determined by the number of MSS, HLR, etc. connections and doesn't increase with load.
    /**
     * Stores the mapping for the selected node, returns the value of the previous node.
     * <h1>Concurrency</h1>
     * May be called concurrently for unrelated key pairs.
     */
    public Data setMappingData(SccpDialogId sdid, TcapDialogId tdid, Data userData) {
        Storage<Data> currentStorage = findCurrentStorage(sdid, tdid);
        TcapLevelMapping<Data> tm;
        NodeMapping<Data> m, oldm = null;

        if (currentStorage == null) { // first store of this mapping
            if (userData != null) {
                if (tdid.isLocalTIDSet() && !tdid.isRemoteTIDSet()) {
                    LOG.trace("Storing in local only storage");
                    tm = localOnlyMapping.computeIfAbsent(sdid.localSideOnly(), (ignored) -> new TcapLevelMapping<>(
                            expectedConcurrentTcapDialogCount));
                } else {
                    LOG.trace("Storing in main storage");
                    tm = mapping.computeIfAbsent(sdid, (ignored) -> new TcapLevelMapping<>(
                            expectedConcurrentTcapDialogCount));
                }
            } else {
                LOG.warn("Tried to clean nonexistent node mapping for {} / {}!", sdid, tdid);
                return null;
            }
        } else { // currentStorage !=null : there is a previous mapping
            tm = currentStorage.tcapLevelMapping;
            oldm = currentStorage.nodeMapping;
        }

        assert tm != null;

        if (userData == null) {
            // delete mapping
            tm.remove(tdid);

            if (oldm == null) {
                LOG.warn("Tried to clean nonexistent node mapping for {} / {}! (No mappings for this TCAP dialog id)",
                        sdid, tdid);
                LOG.trace("Current mappings: {}", tm);
                return null;
            } else {
                LOG.debug("Cleaned node mapping: {} / {} (was: {} mapped at {})", sdid, tdid, oldm.userData,
                        new TimeStampFormatter(oldm.timeOfMapping));
                LOG.trace("Current mappings: {}", tm);
                return oldm.userData;
            }
        } else {
            long now = System.currentTimeMillis();
            m = new NodeMapping<Data>();
            m.userData = userData;
            m.timeOfMapping = now;
            // replace
            oldm = tm.put(tdid, m);
            if (oldm == null) {
                LOG.debug("Added node mapping: {} / {} -> {}", sdid, tdid, userData);
                LOG.trace("Current mappings: {}", tm);
                return null;
            } else {
                LOG.debug("Replaced node mapping: {} / {} -> {} (was: {} mapped at {})", sdid, tdid, userData,
                        oldm.userData, new TimeStampFormatter(oldm.timeOfMapping));
                LOG.trace("Current mappings: {}", tm);
                return oldm.userData;
            }
        }
    }

    protected void init(ImscfConfigType config) {
        int sccpCount = 1;
        Optional<SccpRemoteProfileType> oRemote = Optional.ofNullable(config.getSccp()).map(Sccp::getSccpRemoteProfile);
        if (oRemote.isPresent()) {
            SccpRemoteProfileType remote = oRemote.get();
            sccpCount += remote.getRemoteGtAddresses().size() + remote.getRemoteSubSystemPointCodeAddresses().size();
        }

        // we expect the load to be shared evenly among the EL servers, so just check the first one
        TcapTransactionIdRangeType range = config.getServers().getExecutionLayerServers().iterator().next()
                .getTcapTransactionIdRange();

        long tcapPerMapping = (range.getMaxInclusive() - range.getMinInclusive()) / sccpCount;
        expectedConcurrentTcapDialogCount = tcapPerMapping > Integer.MAX_VALUE ? Integer.MAX_VALUE
                : (int) tcapPerMapping;
        LOG.debug("Expected concurrent SCCP connection count set to: {}", sccpCount);
        LOG.debug("Expected concurrent TCAP dialog count set to: {}", expectedConcurrentTcapDialogCount);
        mapping = new SccpLevelMapping<Data>(sccpCount);
        localOnlyMapping = new SccpLevelMapping<>(sccpCount);
    }

    protected void deinit() {
        mapping = null;
        localOnlyMapping = null;
    }

    /**
     * Returns the single target LwComm route name to the specified node.
     * <h1>Concurrency</h1> May safely be called concurrently with this or other methods. */
    public String getDirectRouteNameTo(String otherNode) {
        return LwCommServiceProvider.getService().getConfiguration().getLocalNodeName() + " -> " + otherNode;
    }

    /**
     * Returns the load-balanced LwComm route to any node in the other IMSCF layer.
     * <h1>Concurrency</h1> May safely be called concurrently with this or other methods. */
    public Route getRouteToAnyNode() {
        // initialize if null or lwcomm service has changed. otherwise, the value never changes due to an immutable
        // lwcomm config
        if (LwCommServiceProvider.isServiceInitialized()
                && (loadbalanceRouteToOtherLayer == null || lwcommService != LwCommServiceProvider.getService())) {
            Node localNode = LwCommServiceProvider.getService().getConfiguration().getLocalNode();
            for (Route r : LwCommServiceProvider.getService().getConfiguration().getAllRoutes()) {
                if (r.getPossibleSources().contains(localNode) && r.getDestinations().size() > 0
                        && r.getMode() == Mode.LOADBALANCE) {
                    loadbalanceRouteToOtherLayer = r;
                    break;
                }
            }
        }
        return loadbalanceRouteToOtherLayer;
    }
}
