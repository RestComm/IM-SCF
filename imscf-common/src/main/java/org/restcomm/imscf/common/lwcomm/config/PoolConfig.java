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
package org.restcomm.imscf.common.lwcomm.config;

/**
 * Configuration for a thread pool.
 * A thread pool can be configured in two ways:
 * <li>type == NATIVE: By setting exactly the maximum number of threads and forcing LwComm to use its own thread pools.</li>
 * <li>type == REFERENCE: By specifying a reference to the application server's thread pool</li>
 * @author Miklos Pocsaji
 *
 */
public final class PoolConfig {

    /**
     * Type of the config.
     * @see PoolConfig
     * @author Miklos Pocsaji
     *
     */
    public enum Type {
        NATIVE, REFERENCE
    }

    private Type type;
    private int maxThreads;
    private String threadPoolRef;

    public PoolConfig(int maxThreads) {
        this.maxThreads = maxThreads;
        this.type = Type.NATIVE;
    }

    public PoolConfig(String threadPoolRef) {
        this.threadPoolRef = threadPoolRef;
        this.type = Type.REFERENCE;
    }

    public Type getType() {
        return type;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public String getThreadPoolRef() {
        return threadPoolRef;
    }

    @Override
    public String toString() {
        return "PoolConfig [type=" + type + ", maxThreads=" + maxThreads + ", threadPoolRef=" + threadPoolRef + "]";
    }

}
