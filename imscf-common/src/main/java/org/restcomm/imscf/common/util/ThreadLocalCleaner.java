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
package org.restcomm.imscf.common.util;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to clean up thread locals in all threads.
 * Useful on application undeploy where objects of classes owned by the application reside on
 * thread locals in application server's threads. By leaving these objects in the thread local
 * structures of those threads a memory leak is introduced
 * @author Miklos Pocsaji
 *
 */
public final class ThreadLocalCleaner {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadLocalCleaner.class);

    private ThreadLocalCleaner() {
        // This class cannot be instantiated from the outside.
    }

    /**
     * Cleans up all threads' threadlocals.
     * Iterates through all threads in the JVM and uses reflection to eliminate those
     * threadlocal entries where the value's class matches with one of the regular
     * expressions given in the parameter.
     * @param classesRegex Regular expressions to match against the value's class.
     */
    public static void cleanThreadLocals(String... classesRegex) {

        // WARN: this is far from CPU optimal, but simple.
        // See: http://stackoverflow.com/questions/1323408/get-a-list-of-all-threads-currently-running-in-java
        Set<Thread> allThreads = Thread.getAllStackTraces().keySet();

        LOG.info("ThreadLocalCleaner.cleanThreadLocals({})", Arrays.asList(classesRegex));
        for (Thread t : allThreads) {
            try {
                cleanThreadLocals(t, classesRegex);
            } catch (Exception ex) {
                LOG.info("Exception while cleaning ThreadLocals.", ex);
            }
        }
    }

    private static void cleanThreadLocals(Thread thread, String... classesRegex) throws NoSuchFieldException,
            ClassNotFoundException, IllegalArgumentException, IllegalAccessException, SecurityException,
            NoSuchMethodException, InvocationTargetException {

        // LOG.debug("Checking thread {}", thread.getName());
        Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
        threadLocalsField.setAccessible(true);

        Class<?> threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
        Field tableField = threadLocalMapClass.getDeclaredField("table");
        tableField.setAccessible(true);

        Object threadLocalMap = threadLocalsField.get(thread);
        if (threadLocalMap == null) {
            return;
        }

        Object table = tableField.get(threadLocalMap);
        int threadLocalCount = Array.getLength(table);

        // LOG.debug("Thread {} has {} threadlocals", thread.getName(), threadLocalCount);

        for (int i = 0; i < threadLocalCount; i++) {
            Object entry = Array.get(table, i);
            if (entry == null)
                continue;
            Field valueField = entry.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            Object value = valueField.get(entry);
            if (value == null)
                continue;
            String valueClass = value.getClass().getName();
            // LOG.trace("Found non-null value {} of type {}", value, valueClass);
            for (String regex : classesRegex) {
                Pattern p = Pattern.compile(regex);
                if (p.matcher(valueClass).matches()) {
                    LOG.info("Found value with class {} matching regex {} at index {}. Cleaning from thread {}.",
                            valueClass, regex, i, thread.getName());
                    // valueField.set(entry, null);
                    clearThreadLocal(threadLocalMap, i);
                    break;
                }
            }
        }

    }

    /**
     * Clears the value at given index from the given ThreadLocalMap.
     * Clones the code at ThreadLocal$ThreadLocalMap.remove()
     * @param threadLocalMap The ThreadLocal$ThreadLocalMap instance to remove from
     * @param index The index to remove
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    // Since the method to clone uses reference equality we have to use it as well but this annoys PMD
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static void clearThreadLocal(Object threadLocalMap, int index) throws ClassNotFoundException,
            NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Class<?> threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");

        // Get the "private Entry[] table" in threadLocalMap:
        Field tableField = threadLocalMapClass.getDeclaredField("table");
        tableField.setAccessible(true);
        // The "table" object is an array of java.lang.ThreadLocal$ThreadLocalMap$Entry objects:
        Object table = tableField.get(threadLocalMap);

        // Get the "private int expungeStaleEntry(int staleSlot)" method in threadLocalMap
        Method expungeStaleEntry = threadLocalMapClass.getDeclaredMethod("expungeStaleEntry", int.class);
        expungeStaleEntry.setAccessible(true);

        // The java.lang.ThreadLocal$ThreadLocalMap$Entry class is a Reference:
        Object key = ((Reference<?>) Array.get(table, index)).get();

        int len = Array.getLength(table);
        int indexToRemove = index;
        // Copying the for loop from ThreadLocal$ThreadLocalMap.remove()
        for (Object entry = Array.get(table, indexToRemove); entry != null; indexToRemove = (indexToRemove + 1) % len, entry = Array
                .get(table, indexToRemove)) {
            Object actualKey = ((Reference<?>) entry).get();
            if (actualKey == key) {
                LOG.trace("Clearing ThreadLocalMap entry. Entry: {}, index: {}, key: {}", entry, index, actualKey);
                ((Reference<?>) entry).clear();
                expungeStaleEntry.invoke(threadLocalMap, index);
            }
        }
    }
}
