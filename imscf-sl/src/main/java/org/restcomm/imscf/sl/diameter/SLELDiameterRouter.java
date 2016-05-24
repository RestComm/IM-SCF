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
package org.restcomm.imscf.sl.diameter;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.lwcomm.config.Node;
import org.restcomm.imscf.common.lwcomm.config.Route;
import org.restcomm.imscf.common.lwcomm.config.Route.Mode;
import org.restcomm.imscf.common.lwcomm.service.LwCommServiceProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapping between diameter dialog identifiers and serving SL/EL node on the other side of the SL-EL communication.
 * @param <Data> the type of data stored in the mappings.
 */
public class SLELDiameterRouter<Data> {
    private static final Logger LOG = LoggerFactory.getLogger(SLELDiameterRouter.class);

    private DiameterLevelMapping<Data> diameterMap;

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
    private static class DiameterLevelMapping<Data> extends ConcurrentHashMap<DiameterDialogId, NodeMapping<Data>> {
        private static final long serialVersionUID = 1L;

        public DiameterLevelMapping() {
            super();
        }
    }

    /**Internal class for finding the location of a mapping record. */
    private static class Storage<Data> {
        NodeMapping<Data> nodeMapping;

        public Storage(NodeMapping<Data> nodeMapping) {
            this.nodeMapping = nodeMapping;
        }
    }

    public boolean isInCurrentStorage(DiameterDialogId diamId) {
        if (diameterMap.get(diamId) == null) {
            return false;
        }
        return true;
    }

    private Storage<Data> findCurrentStorage(DiameterDialogId diamId) {
        NodeMapping<Data> m;

        m = diameterMap.get(diamId);
        if (m == null) {
            LOG.trace("No node mapping exists for {}", diamId);
            return null;
        }
        LOG.trace("Found mapping in main storage");
        return new Storage<>(m);
    }

    // TODO check concurrency correctness
    public Data getMappingData(DiameterDialogId diameterId) {
        Storage<Data> currentStorage = findCurrentStorage(diameterId);
        if (currentStorage == null) {
            LOG.trace("Lookup failed for {}.", diameterId);
            return null;
        }
        Data ret = currentStorage.nodeMapping.userData;
        LOG.debug("Found node mapping: {} -> {}", diameterId, ret);
        return ret;
    }

    // TODO check concurrency correctness
    /**
     * Stores the mapping for the selected node, returns the value of the previous node.
     */
    public Data setMappingData(DiameterDialogId diameterId, Data userData) {
        NodeMapping<Data> m, oldm = null;

        if (userData == null) {
            oldm = diameterMap.remove(diameterId);

            if (oldm == null) {
                LOG.warn("Tried to clean nonexistent node mapping for {} ! (No mappings for this Diameter dialog id)",
                        diameterId);
                LOG.trace("Current mappings: {}", diameterMap);
                return null;
            } else {
                LOG.debug("Cleaned node mapping: {} (was: {} mapped at {})", diameterId, oldm.userData,
                        new TimeStampFormatter(oldm.timeOfMapping));
                LOG.trace("Current mappings: {}", diameterMap);
                return oldm.userData;
            }
        } else {
            long now = System.currentTimeMillis();
            m = new NodeMapping<Data>();
            m.userData = userData;
            m.timeOfMapping = now;

            oldm = diameterMap.put(diameterId, m);
            if (oldm == null) {
                LOG.debug("Added node mapping: {} -> {}", diameterId, userData);
                LOG.trace("Current mappings: {}", diameterMap);
                return null;
            } else {
                LOG.trace("Replaced node mapping: {} -> {} (was: {} mapped at {})", diameterId, userData,
                        oldm.userData, new TimeStampFormatter(oldm.timeOfMapping));
                LOG.debug("Replaced node mapping: {} -> {} (was: {} mapped at {})", diameterId, userData,
                        oldm.userData, new TimeStampFormatter(oldm.timeOfMapping));
                LOG.trace("Current mappings: {}", diameterMap);
                return oldm.userData;
            }
        }
    }

    protected void init(ImscfConfigType config) {
        diameterMap = new DiameterLevelMapping<>();
    }

    protected void deinit() {
        diameterMap = null;
    }

    public String getDirectRouteNameTo(String otherNode) {
        return LwCommServiceProvider.getService().getConfiguration().getLocalNodeName() + " -> " + otherNode;
    }

    public Route getRouteToAnyNode() {
        // FIXME this is not too effective...
        Node localNode = LwCommServiceProvider.getService().getConfiguration().getLocalNode();
        for (Route r : LwCommServiceProvider.getService().getConfiguration().getAllRoutes()) {
            if (r.getPossibleSources().contains(localNode) && r.getDestinations().size() > 0
                    && r.getMode() == Mode.LOADBALANCE) {
                return r;
            }
        }
        return null;
    }

}
