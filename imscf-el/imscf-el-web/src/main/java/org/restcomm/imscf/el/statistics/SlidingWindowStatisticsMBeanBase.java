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
package org.restcomm.imscf.el.statistics;

import org.restcomm.imscf.common.config.DiameterCounterThresholdNotificationType;
import org.restcomm.imscf.common.config.MapCounterThresholdNotificationType;
import org.restcomm.imscf.common.config.ServiceKeyCounterThresholdNotificationType;
import org.restcomm.imscf.el.config.ConfigBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for statistics with sliding time window in EL MBeans.
 * All counters are stored in list of <code>AtomicLong</code>. Every list element is counter for a second of time.
 * The array has as many elements as the seconds required for the time window plus one.
 * At an increment the following happens:
 * <li>The current second's counter is incremented: inc(counterarray[&lt;time in seconds> mod &lt;seconds required>])</li>
 * <li>The next value is zeroed out: set_to_zero(counterarray[&lt;time in seconds + 1> mod &lt;seconds required>])</li>
 * <li>The slots for the previous counters in which no counter increment has been happened is zeroed out as well</li>
 * @author Miklos Pocsaji
 *
 */
public abstract class SlidingWindowStatisticsMBeanBase extends NotificationBroadcasterSupport {

    private static final Logger LOG = LoggerFactory.getLogger(SlidingWindowStatisticsMBeanBase.class);

    private Map<String, Counter> counters;
    private int windowSeconds;
    private List<Notification> allNotifications;
    private AtomicLong notificationSequence = new AtomicLong(1);

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_0-9]+)\\}");

    protected SlidingWindowStatisticsMBeanBase(int windowSeconds, List<Notification> notifications) {
        this.windowSeconds = windowSeconds;
        this.counters = new HashMap<String, Counter>();
        this.allNotifications = notifications;
    }

    //
    // NotificationBroadcasterSupport overrides
    //

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[] { new MBeanNotificationInfo(
                new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE }, "Threshold crossed in a counter",
                "This notification is sent when one of the counters cross the predefined thresholds.") };
    }

    /**
     * Adds a counter to the backend map.
     * Should only be called at initialization phase, because the backend map is not synchronized.
     * @param counterName The name of the new counter.
     */
    protected void addCounter(String counterName) {
        List<Notification> cNotifications = new ArrayList<Notification>();
        for (Notification n : allNotifications) {
            // LOG.trace("nofitication counter name: {}, counter name: {}", n.counterName, counterName);
            if (n.counterName.equals(counterName)) {
                cNotifications.add(n);
            }
        }
        Counter c = new Counter(windowSeconds, cNotifications);
        LOG.debug("Counter added with name '{}': {}", counterName, c);
        counters.put(counterName, c);
    }

    void incCounter(String counterName) {
        LOG.trace("Incrementing counter '{}'", counterName);
        Counter c = counters.get(counterName);
        if (c == null) {
            LOG.warn("No counter found with name '{}'.", counterName);
            return;
        }
        List<AtomicLong> longList = c.values;

        long time = getTimeSec();
        int index = (int) (time % windowSeconds);
        int nextIndex = (int) ((time + 1) % windowSeconds);

        // Zero out the seconds where no increment has been happened

        // ^^^ and the code below is unneeded since cleanupCounters is periodically called from StatisticsThread

        // int emptySeconds = (int) (time - c.timestampSec.get());
        c.timestampSec.set(time);
        // LOG.trace("seconds where no increments have been made: {}", emptySeconds);
        // if (emptySeconds >= windowSeconds) {
        // resetAllCounters();
        // } else {
        // for (int i = 1; i < emptySeconds; i++) {
        // int zeroIndex = (int) ((time - i) % windowSeconds);
        // longList.get(zeroIndex).set(0);
        // }
        // }

        longList.get(index).incrementAndGet();
        longList.get(nextIndex).set(0);

    }

    private void checkAndFireNotifications(String counterName, List<Notification> notifications, long prevValue,
            long newValue) {
        if (notifications.isEmpty())
            return;
        for (Notification n : notifications) {
            // LOG.trace("Checking notifications. counter: {}, notification: {}", counterName, n);
            if (prevValue <= n.low && newValue > n.low && n.lowFromBelow) {
                sendNotification(counterName, n.notificationText, n.low, prevValue, newValue, "low", "below");
            } else if (prevValue >= n.low && newValue < n.low && n.lowFromAbove) {
                sendNotification(counterName, n.notificationText, n.low, prevValue, newValue, "low", "above");
            } else if (prevValue <= n.high && newValue > n.high && n.highFromBelow) {
                sendNotification(counterName, n.notificationText, n.high, prevValue, newValue, "high", "below");
            } else if (prevValue >= n.high && newValue < n.high && n.highFromAbove) {
                sendNotification(counterName, n.notificationText, n.high, prevValue, newValue, "high", "above");
            }
        }
    }

    private void sendNotification(String counterName, String rawMsg, long thresholdValue, long prevValue,
            long newValue, String highLow, String aboveBelow) {
        String msg = rawMsg;
        msg = msg.replace("${serverName}", ConfigBean.SERVER_NAME);
        msg = msg.replace("${counterName}", counterName);
        msg = msg.replace("${counterValue}", String.valueOf(newValue));
        msg = msg.replace("${thresholdValue}", String.valueOf(thresholdValue));
        msg = msg.replace("${aboveBelow}", aboveBelow);
        msg = msg.replace("${highLow}", highLow);

        Matcher m = VARIABLE_PATTERN.matcher(msg);
        StringBuffer ret = new StringBuffer();
        while (m.find()) {
            String variable = m.group(1);
            String value = resolveNotificationVariable(variable);
            if (value == null) {
                value = "__UNRESOLVED_" + variable + "__";
            }
            m.appendReplacement(ret, value);
        }
        msg = m.appendTail(ret).toString();

        AttributeChangeNotification n = new AttributeChangeNotification(this, notificationSequence.getAndIncrement(),
                System.currentTimeMillis(), msg, counterName, "java.lang.Long", prevValue, newValue);
        LOG.info("Sending notification: {}", msg);
        sendNotification(n);
    }

    void cleanupCounters() {
        long timeEnd = getTimeSec();
        for (Map.Entry<String, Counter> e : counters.entrySet()) {
            Counter c = e.getValue();
            long zeroOutCount = timeEnd - c.timestampSec.get() - 1;
            if (zeroOutCount > 0) {
                // LOG.trace("Zeroing out last {} second(s) of counter {}", zeroOutCount, e.getKey());
                for (long timeStart = c.timestampSec.get() + 1; timeStart < timeEnd; timeStart++) {
                    c.values.get((int) (timeStart % windowSeconds)).set(0);
                    c.timestampSec.set(timeEnd);
                }
            }
        }
    }

    void checkCountersAndSendNotifications() {
        // LOG.trace("checkCountersAndSendNotifications()");
        for (Map.Entry<String, Counter> e : counters.entrySet()) {
            Counter c = e.getValue();
            String counterName = e.getKey();
            long newValue = getCounter(counterName);

            if (c.previousValue >= 0) {
                checkAndFireNotifications(counterName, c.notifications, c.previousValue, newValue);
            }
            c.previousValue = newValue;
        }
    }

    protected long getCounter(String counterName) {
        Counter c = counters.get(counterName);
        if (c == null) {
            LOG.warn("No counter found with name '{}'", counterName);
            return -1;
        }
        List<AtomicLong> longList = c.values;
        return longList.stream().mapToLong(al -> al.get()).sum();
    }

    protected void resetAllCounters() {
        counters.values().stream().flatMap(c -> c.values.stream()).forEach(al -> al.set(0));
    }

    private static long getTimeSec() {
        return System.currentTimeMillis() / 1000;
    }

    protected abstract String resolveNotificationVariable(String variable);

    /**
     * Holds a counter. Members:
     * <li>List of AtomicLongs, size of the required seconds + 1</li>
     * <li>Timestamp when the
     * @author Miklos Pocsaji
     *
     */
    private static final class Counter {
        private volatile List<AtomicLong> values;
        private volatile AtomicLong timestampSec;
        private volatile List<Notification> notifications;
        private volatile long previousValue = -1;

        public Counter(int secs, List<Notification> notifications) {
            this.notifications = notifications;
            values = new ArrayList<AtomicLong>(secs + 1);
            timestampSec = new AtomicLong(getTimeSec());
            for (int i = 0; i <= secs; i++) {
                values.add(new AtomicLong());
            }
        }

        @Override
        public String toString() {
            return "Counter [timestampSec=" + timestampSec + ", notifications=" + notifications + ", previousValue="
                    + previousValue + "]";
        }

    }

    /**
     * Class which holds a notification configuration for a counter.
     * @author Miklos Pocsaji
     *
     */
    static final class Notification {
        private long low;
        private long high;
        private boolean lowFromBelow;
        private boolean lowFromAbove;
        private boolean highFromBelow;
        private boolean highFromAbove;
        private String counterName;
        private String notificationText;

        public Notification(long low, long high, boolean lowFromBelow, boolean lowFromAbove, boolean highFromBelow,
                boolean highFromAbove, String counterName, String notificationText) {
            this.low = low;
            this.high = high;
            this.lowFromBelow = lowFromBelow;
            this.lowFromAbove = lowFromAbove;
            this.highFromBelow = highFromBelow;
            this.highFromAbove = highFromAbove;
            this.counterName = counterName;
            this.notificationText = notificationText;
        }

        @Override
        public String toString() {
            return "Notification [low=" + low + ", high=" + high + ", lowFromBelow=" + lowFromBelow + ", lowFromAbove="
                    + lowFromAbove + ", highFromBelow=" + highFromBelow + ", highFromAbove=" + highFromAbove
                    + ", counterName=" + counterName + ", notificationText=" + notificationText + "]";
        }

    }

    static List<Notification> convertFromServiceKeyThresholdNotifications(
            List<ServiceKeyCounterThresholdNotificationType> nots) {
        List<Notification> ret = new ArrayList<Notification>(nots.size());
        nots.forEach(n -> ret.add(new Notification(n.getThresholdLow(), n.getThresholdHigh(), n
                .isNotificationWhenLowFromBelow(), n.isNotificationWhenLowFromAbove(), n
                .isNotificationWhenHighFromBelow(), n.isNotificationWhenHighFromAbove(), n.getCounterName().value(), n
                .getNotificationText())));
        return ret;
    }

    static List<Notification> convertFromMapThresholdNotifications(List<MapCounterThresholdNotificationType> nots) {
        List<Notification> ret = new ArrayList<Notification>(nots.size());
        nots.forEach(n -> ret.add(new Notification(n.getThresholdLow(), n.getThresholdHigh(), n
                .isNotificationWhenLowFromBelow(), n.isNotificationWhenLowFromAbove(), n
                .isNotificationWhenHighFromBelow(), n.isNotificationWhenHighFromAbove(), n.getCounterName().value(), n
                .getNotificationText())));
        return ret;
    }

    static List<Notification> convertFromDiameterThresholdNotifications(
            List<DiameterCounterThresholdNotificationType> nots) {
        List<Notification> ret = new ArrayList<Notification>(nots.size());
        nots.forEach(n -> ret.add(new Notification(n.getThresholdLow(), n.getThresholdHigh(), n
                .isNotificationWhenLowFromBelow(), n.isNotificationWhenLowFromAbove(), n
                .isNotificationWhenHighFromBelow(), n.isNotificationWhenHighFromAbove(), n.getCounterName().value(), n
                .getNotificationText())));
        return ret;
    }
}
