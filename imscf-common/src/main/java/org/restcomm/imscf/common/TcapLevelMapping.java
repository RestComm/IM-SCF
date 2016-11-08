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
package org.restcomm.imscf.common ;

import org.restcomm.imscf.common .SLELRouter.NodeMapping;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * TCAP dialog mapping. The put, get and remove operations allow concurrent access for different keys.
 * For equal keys, get and remove allow full concurrency with other methods, while put operations may behave unexpectedly (see {@link #put(TcapDialogId, NodeMapping)}).
 * <p>
 * Due to the nature of mutable keys in TCAP ID mapping however, a mapping should always be {@link #remove(Object)}-ed
 * first before reusing the key in a {@link #put(TcapDialogId, NodeMapping)} for a different call. (The new call's BEGIN will contain a single TID,
 * which will update the appropriate map, but the value will still be linked to the other side's TID of the previous call, unless removed explicitly.)
 *
 * @param <Data> The type of data stored in the mapping.
 */
public class TcapLevelMapping<Data> implements ConcurrentMap<TcapDialogId, NodeMapping<Data>> {

    private ConcurrentHashMap<Long, TcapLevelMapping<Data>.NodeMappingWrapper> byRemoteTcapTID;
    private ConcurrentHashMap<Long, TcapLevelMapping<Data>.NodeMappingWrapper> byLocalTcapTID;

    /** Wrapper for a NodeMapping containing the keys for both maps. */
    private class NodeMappingWrapper {
        TcapDialogId key;
        NodeMapping<Data> nm;
        boolean deleted;

        @Override
        public String toString() {
            return "[rTID:" + key.getRemoteTcapTID() + " lTID:" + key.getLocalTcapTID() + " nm:" + nm + "]";
        }

        /** Updates the key of this wrapper and updates the map with the new key (calls {@link #store()}).
         * <b>Thread must hold the monitor of this object when calling this method.</b>*/
        NodeMappingWrapper update(TcapDialogId key) {
            assert Thread.holdsLock(this);
            // Updates can only add a new OTID/DTID to the key. All common values in the two keys must match.
            if (!this.key.equals(key))
                throw new IllegalArgumentException("Provided key " + key + " is not an extension of this key "
                        + this.key);

            this.key = TcapDialogId.merge(this.key, key);
            return store();
        }

        /** Updates the map with the current keys of this wrapper.
         * <b>Thread must hold the monitor of this object when calling this method.</b>*/
        NodeMappingWrapper store() {
            assert Thread.holdsLock(this);
            // caller is synchronized on this wrapper, but the maps are still accessed concurrently for unrelated calls.
            if (key.isLocalTIDSet()) {
                byLocalTcapTID.put(key.getLocalTcapTID(), this);
            }
            if (key.isRemoteTIDSet()) {
                byRemoteTcapTID.put(key.getRemoteTcapTID(), this);
            }
            return this;
        }

        /** Deletes the wrapper from the map.
         * <b>Thread must hold the monitor of this object when calling this method.</b>*/
        NodeMappingWrapper delete() {
            assert Thread.holdsLock(this);
            deleted = true;
            // caller is synchronized on this wrapper, but the maps are still accessed concurrently for unrelated calls.
            if (key.isLocalTIDSet()) {
                byLocalTcapTID.remove(key.getLocalTcapTID(), this);
            }
            if (key.isRemoteTIDSet()) {
                byRemoteTcapTID.remove(key.getRemoteTcapTID(), this);
            }
            return this;
        }
    }

    public TcapLevelMapping(int expectedConcurrentTcapDialogCount) {
        byRemoteTcapTID = new ConcurrentHashMap<Long, TcapLevelMapping<Data>.NodeMappingWrapper>(
                expectedConcurrentTcapDialogCount, 0.8f);
        byLocalTcapTID = new ConcurrentHashMap<Long, TcapLevelMapping<Data>.NodeMappingWrapper>(
                expectedConcurrentTcapDialogCount, 0.8f);
    }

    private NodeMappingWrapper findWrapper(TcapDialogId key) {
        NodeMappingWrapper w = null;
        if (key.isRemoteTIDSet())
            w = byRemoteTcapTID.get(key.getRemoteTcapTID());
        if (w == null && key.isLocalTIDSet())
            w = byLocalTcapTID.get(key.getLocalTcapTID());
        if (w == null)
            return null;

        if (!w.key.equals(key))
            throw new IllegalStateException("Found mismatched mapping entry " + w.key + " for key " + key);

        return w;
    }

    /**
     * Concurrency note: If multiple concurrent put operations are invoked for equal keys
     * for which no mapping existed in the map prior to these invocations, all invocations
     * may return null, or some invocations may return values put in the map by the other invocations.
     */
    @Override
    public NodeMapping<Data> put(TcapDialogId key, NodeMapping<Data> value) {
        NodeMapping<Data> ret;
        // findWrapper runs concurrently for unrelated IDs
        NodeMappingWrapper w = findWrapper(key);
        if (w == null) {
            w = new NodeMappingWrapper();
            w.key = key; // no copy needed as TcapDialogId is immutable
        }

        synchronized (w) { // newly created or existing wrapper
            if (w.deleted) // a concurrent remove() finished before us
                ret = null;
            else
                ret = w.nm; // null if we just created it, stored value otherwise

            // concurrent put() with no initially present value not handled, both threads might return null and the map
            // will contain one of the values, which should actually be identical
            // (same TID at the same time -> same call)
            // concurrent put() with already present value would have simply updated this same wrapper

            w.nm = value;
            w.update(key);
        }

        return ret;
    }

    @Override
    public NodeMapping<Data> get(Object objKey) {
        if (!(objKey instanceof TcapDialogId)) {
            throw new IllegalArgumentException("Key must be non-null and of type TcapDialogId");
        }
        TcapDialogId key = (TcapDialogId) objKey;
        // findWrapper runs concurrently for unrelated IDs
        NodeMappingWrapper w = findWrapper(key);
        if (w == null)
            return null;
        // concurrent put/remove with the same key will be reflected in the same wrapper if it was present
        synchronized (w) {
            if (w.deleted) // don't put it back if a concurrent remove() deleted it after we found it
                return null;
            else
                return w.update(key).nm;
        }
    }

    @Override
    public NodeMapping<Data> remove(Object objKey) {
        if (!(objKey instanceof TcapDialogId)) {
            throw new IllegalArgumentException("Key must be non-null and of type TcapDialogId");
        }
        TcapDialogId key = (TcapDialogId) objKey;
        // findWrapper runs concurrently for unrelated IDs
        NodeMappingWrapper w = findWrapper(key);
        if (w == null)
            return null;
        // concurrent remove with the same key will be reflected in the same wrapper if it was present
        synchronized (w) {
            if (w.deleted)
                return null;
            else
                return w.delete().nm;
        }
    }

    private String printMap1PerLine(Map<?, ?> m, long maxEntries) {
        long extra = m.size() - maxEntries;
        String suffix = extra > 0 ? "\n " + extra + " other entries not shown\n]" : "\n]";
        return m.entrySet().stream().limit(maxEntries).map(e -> e.getKey() + " -> " + e.getValue())
                .collect(Collectors.joining("\n", "[\n", suffix));
    }

    @Override
    public String toString() {
        long max = 10; // fixed limit for now
        return "TcapLevelMapping [\nbyRemoteTcapTID=" + printMap1PerLine(byRemoteTcapTID, max) + ",\nbyLocalTcapTID="
                + printMap1PerLine(byLocalTcapTID, max) + "]";
    }

    // unsupported methods, marked deprecated to generate compiler warnings

    @Deprecated
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void putAll(Map<? extends TcapDialogId, ? extends NodeMapping<Data>> m) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Set<TcapDialogId> keySet() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Collection<NodeMapping<Data>> values() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Set<Entry<TcapDialogId, NodeMapping<Data>>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public NodeMapping<Data> putIfAbsent(TcapDialogId key, NodeMapping<Data> value) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean replace(TcapDialogId key, NodeMapping<Data> oldValue, NodeMapping<Data> newValue) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public NodeMapping<Data> replace(TcapDialogId key, NodeMapping<Data> value) {
        throw new UnsupportedOperationException();
    }

}
