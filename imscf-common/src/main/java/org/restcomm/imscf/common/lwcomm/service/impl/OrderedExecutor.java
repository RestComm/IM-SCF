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
package org.restcomm.imscf.common.lwcomm.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

/**
 * Special executor which can order the tasks if a common key is given.
 * Runnables submitted with non-null key will guaranteed to run in order for the same key.
 * @author Miklos Pocsaji
 *
 */
public class OrderedExecutor {

    private static final Queue<Runnable> EMPTY_QUEUE = new QueueWithHashCodeAndEquals<Runnable>(
            new ConcurrentLinkedQueue<Runnable>());

    private ConcurrentMap<Object, Queue<Runnable>> taskMap = new ConcurrentHashMap<Object, Queue<Runnable>>();
    private Executor delegate;
    private volatile boolean stopped;

    public OrderedExecutor(Executor delegate) {
        this.delegate = delegate;
    }

    public void execute(Runnable runnable, Object key) {
        if (stopped) {
            return;
        }

        if (key == null) {
            delegate.execute(runnable);
            return;
        }

        Queue<Runnable> queueForKey = taskMap.computeIfPresent(key, (k, v) -> {
            v.add(runnable);
            return v;
        });
        if (queueForKey == null) {
            // There was no running task with this key
            Queue<Runnable> newQ = new QueueWithHashCodeAndEquals<Runnable>(new ConcurrentLinkedQueue<Runnable>());
            newQ.add(runnable);
            // Use putIfAbsent because this execute() method can be called concurrently as well
            queueForKey = taskMap.putIfAbsent(key, newQ);
            if (queueForKey != null)
                queueForKey.add(runnable);
            delegate.execute(new InternalRunnable(key));
        }
    }

    public void shutdown() {
        stopped = true;
        taskMap.clear();
    }

    /**
     * Own Runnable used by OrderedExecutor.
     * The runnable is associated with a specific key - the Queue&lt;Runnable> for this
     * key is polled.
     * If the queue is empty, it tries to remove the queue from taskMap.
     * @author Miklos Pocsaji
     *
     */
    private class InternalRunnable implements Runnable {

        private Object key;

        public InternalRunnable(Object key) {
            this.key = key;
        }

        @Override
        public void run() {
            while (true) {
                // There must be at least one task now
                Runnable r = taskMap.get(key).poll();
                while (r != null) {
                    r.run();
                    r = taskMap.get(key).poll();
                }
                // The queue emptied
                // Remove from the map if and only if the queue is really empty
                boolean removed = taskMap.remove(key, EMPTY_QUEUE);
                if (removed) {
                    // The queue has been removed from the map,
                    // if a new task arrives with the same key, a new InternalRunnable
                    // will be created
                    break;
                } // If the queue has not been removed from the map it means that someone put a task into it
                  // so we can safely continue the loop
            }
        }
    }

    /**
     * Special Queue implementation, with equals() and hashCode() methods.
     * By default, Java SE queues use identity equals() and default hashCode() methods.
     * This implementation uses Arrays.equals(Queue::toArray()) and Arrays.hashCode(Queue::toArray()).
     * @author Miklos Pocsaji
     *
     * @param <E> The type of elements in the queue.
     */
    private static class QueueWithHashCodeAndEquals<E> implements Queue<E> {

        private Queue<E> delegate;

        public QueueWithHashCodeAndEquals(Queue<E> delegate) {
            this.delegate = delegate;
        }

        public boolean add(E e) {
            return delegate.add(e);
        }

        public boolean offer(E e) {
            return delegate.offer(e);
        }

        public int size() {
            return delegate.size();
        }

        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        public E remove() {
            return delegate.remove();
        }

        public E poll() {
            return delegate.poll();
        }

        public E element() {
            return delegate.element();
        }

        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        public E peek() {
            return delegate.peek();
        }

        public Object[] toArray() {
            return delegate.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        public boolean addAll(Collection<? extends E> c) {
            return delegate.addAll(c);
        }

        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c);
        }

        public void clear() {
            delegate.clear();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof QueueWithHashCodeAndEquals)) {
                return false;
            }
            QueueWithHashCodeAndEquals<?> other = (QueueWithHashCodeAndEquals<?>) obj;
            return Arrays.equals(toArray(), other.toArray());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(toArray());
        }

    }

}
