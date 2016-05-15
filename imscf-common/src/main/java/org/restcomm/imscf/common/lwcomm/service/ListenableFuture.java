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
package org.restcomm.imscf.common.lwcomm.service;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * A special future interface which allows listeners to be defined.
 * The listeners will be invoked when the result is ready. If the result is ready
 * when addListener is called, the listener method will be invoked immediately.
 * @author Miklos Pocsaji
 *
 * @param <V> The future result parameter class
 */
public interface ListenableFuture<V> extends Future<V> {

    /**
     * Adds a listener to this Future object.
     * A listener can only be added once to a ListenableFuture, so two calls
     * to addListener with the same listener as the first parameter will not result
     * in calling the listener twice. The latter call's executor paramter will be set.
     * @param listener The listener which will be notified when the result is ready.
     * @param executor Optional executor for use to callback. If this executor is null
     * then the listener method will be invoked from the underlying library's thread.
     */
    void addListener(FutureListener<V> listener, Executor executor);

    /**
     * Removes the listener from this Future object.
     * @param listener The listener to remove.
     */
    void removeListener(FutureListener<V> listener);
}
