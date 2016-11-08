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
package org.restcomm.imscf.el.modules.routing;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for parsing and matching an unsigned number range list and matching an unsigned number against it.
 */
public final class UnsignedNumberRangeList {
    private UnsignedNumberRange[] ranges;

    private UnsignedNumberRangeList(String config) {
        ranges = Arrays.stream(config.trim().split("\\s*,\\s*")).map(UnsignedNumberRange::parse)
                .toArray(UnsignedNumberRange[]::new);
    }

    /** Returns true if the number matches one of the ranges, or if the list of ranges is empty. */
    public boolean matches(int num) {
        return ranges.length == 0 || Arrays.stream(ranges).anyMatch(r -> r.matches(num));
    }

    public boolean matches(String num) {
        int number = Integer.parseInt(num);
        return matches(number);
    }

    @Override
    public String toString() {
        return Arrays.toString(ranges);
    }

    /** Creates a new instance by parsing the specified string.
     * @return the created instance or null if the parameter was null. */
    public static UnsignedNumberRangeList parse(String rangeList) {
        if (rangeList == null || rangeList.trim().isEmpty())
            return null;
        return new UnsignedNumberRangeList(rangeList);
    }
}

/** A min-max inclusive range.*/
class UnsignedNumberRange {
    private static final Pattern PATTERN = Pattern.compile("(?<low>[0-9]+)(?:\\s*-\\s*(?<high>[0-9]+))?");
    int low, high;

    public UnsignedNumberRange(int low, int high) {
        if (low < 0 || high < low)
            throw new IllegalArgumentException("0 <= low <= high must be true! Actual values are: low " + low
                    + ", high: " + high);
        this.low = low;
        this.high = high;
    }

    public boolean matches(int value) {
        return low <= value && value <= high;
    }

    @Override
    public String toString() {
        return "[" + low + "-" + high + "]";
    }

    public static UnsignedNumberRange parse(String range) {
        Matcher m = PATTERN.matcher(range);
        if (m.matches()) {
            int low = Integer.parseInt(m.group("low")); // must be present
            int high = Optional.ofNullable(m.group("high")).map(Integer::parseInt).orElse(low);
            return new UnsignedNumberRange(low, high);
        } else {
            throw new IllegalArgumentException("The provided string does not match a number range: " + range);
        }
    }

}
