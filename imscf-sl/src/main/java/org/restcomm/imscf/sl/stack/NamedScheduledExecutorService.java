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
package org.restcomm.imscf.sl.stack;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Provides a named scheduling interface. */
public class NamedScheduledExecutorService {

    private ScheduledExecutorService delegate;
    private ConcurrentHashMap<String, Ref<ScheduledFuture<?>>> futures = new ConcurrentHashMap<>();

    public NamedScheduledExecutorService(ScheduledExecutorService delegate) {
        this.delegate = delegate;
    }

    /** Schedules a task with the specified unique name. */
    public void scheduleNamedTask(String name, Runnable command, long delay, TimeUnit unit) {
        Objects.requireNonNull(name, "Task name cannot be null");
        Ref<ScheduledFuture<?>> ref = new Ref<ScheduledFuture<?>>();
        Object prev = futures.putIfAbsent(name, ref);
        if (prev != null) {
            // up to this point, everything was effectively a NOOP, no cleanup necessary
            throw new IllegalStateException("A task named '" + name + "' exists already!");
        }

        // OK, spot reserved, schedule away
        ScheduledFuture<?> f = delegate.schedule(() -> {
            try {
                command.run();
            } finally {
                futures.remove(name);
            }
        }, delay, unit);
        // at this point, f could be done already, but that doesn't bother either the code below or the cancel method.
        ref.set(f);
    }

    /** Cancels the task with the given name. */
    public boolean cancelNamedTask(String name) {
        // simply return false if the task is already done
        return Optional.ofNullable(futures.remove(name)).map(Ref::get).map(f -> f.cancel(false)).orElse(false);
    }

    /** Indirect delayed-set reference to an object.*/
    private static class Ref<T> {
        private T ref;

        public void set(T ref) {
            this.ref = ref;
        }

        public T get() {
            return ref;
        }
    }
}
