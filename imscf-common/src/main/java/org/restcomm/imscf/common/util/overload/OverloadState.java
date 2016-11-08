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
package org.restcomm.imscf.common.util.overload;

/**
 * Defines the system state in terms of overload.
 * Note that equals() and hashCode() methods are overriddent and take into accound only the
 * overloaded flags. Concrete usage percentages are irrelevant when comparing two OverloadState objects.
 * @author Miklos Pocsaji
 *
 */
public final class OverloadState {

    private int cpuPercent;
    private boolean cpuOverloaded;
    private int heapPercent;
    private boolean heapOverloaded;
    private int nonHeap;
    private boolean nonHeapInPercent;
    private boolean nonHeapOverloaded;

    private OverloadState(int cpuPercent, boolean cpuOverloaded, int heapPercent, boolean heapOverloaded, int nonHeap,
            boolean nonHeapOverloaded) {
        this.cpuPercent = cpuPercent;
        this.cpuOverloaded = cpuOverloaded;
        this.heapPercent = heapPercent;
        this.heapOverloaded = heapOverloaded;
        this.nonHeap = nonHeap;
        this.nonHeapOverloaded = nonHeapOverloaded;
    }

    public static OverloadState createOverloadStateWithNonHeapPercent(int cpuPercent, boolean cpuOverloaded,
            int heapPercent, boolean heapOverloaded, int nonHeapPercent, boolean nonHeapOverloaded) {
        OverloadState ret = new OverloadState(cpuPercent, cpuOverloaded, heapPercent, heapOverloaded, nonHeapPercent,
                nonHeapOverloaded);
        ret.nonHeapInPercent = true;
        return ret;
    }

    public static OverloadState createOverloadStateWithNonHeapAmount(int cpuPercent, boolean cpuOverloaded,
            int heapPercent, boolean heapOverloaded, int nonHeapMegabytes, boolean nonHeapOverloaded) {
        OverloadState ret = new OverloadState(cpuPercent, cpuOverloaded, heapPercent, heapOverloaded, nonHeapMegabytes,
                nonHeapOverloaded);
        ret.nonHeapInPercent = false;
        return ret;
    }

    public int getCpuPercent() {
        return cpuPercent;
    }

    public boolean isCpuOverloaded() {
        return cpuOverloaded;
    }

    public int getHeapPercent() {
        return heapPercent;
    }

    public boolean isHeapOverloaded() {
        return heapOverloaded;
    }

    public int getNonHeap() {
        return nonHeap;
    }

    public boolean isNonHeapOverloaded() {
        return nonHeapOverloaded;
    }

    public boolean isAnythingOverloaded() {
        return isCpuOverloaded() || isHeapOverloaded() || isNonHeapOverloaded();
    }

    public boolean isCpuOrHeapOverloaded() {
        return isCpuOverloaded() || isHeapOverloaded();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (cpuOverloaded ? 1231 : 1237);
        result = prime * result + (heapOverloaded ? 1231 : 1237);
        result = prime * result + (nonHeapOverloaded ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OverloadState other = (OverloadState) obj;
        if (cpuOverloaded != other.cpuOverloaded)
            return false;
        if (heapOverloaded != other.heapOverloaded)
            return false;
        if (nonHeapOverloaded != other.nonHeapOverloaded)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "OverloadState [cpuPercent=" + cpuPercent + "%, cpuOverloaded=" + cpuOverloaded + ", heapPercent="
                + heapPercent + "%, heapOverloaded=" + heapOverloaded + ", nonHeap=" + nonHeap
                + (nonHeapInPercent ? "%" : "MB") + ", nonHeapOverloaded=" + nonHeapOverloaded + "]";
    }

}
