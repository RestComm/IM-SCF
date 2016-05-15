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
package org.restcomm.imscf.common.util.overload;

/**
 * Parameters for setting up OverloadProtector.
 * @author Miklos Pocsaji
 *
 */
public final class OverloadProtectorParameters {

    public static final int DEFAULT_CPU_OVERLOAD_THRESHOLD_PERCENT = 80;
    public static final int DEFAULT_CPU_MEASUREMENT_WINDOW = 10;
    public static final int DEFAULT_DATA_COLLECTION_PERIOD_SEC = 3;
    public static final int DEFAULT_HEAP_OVERLOAD_THRESHOLD_PERCENT = 90;
    public static final int DEFAULT_NONHEAP_OVERLOAD_THRESHOLD_PERCENT = 90;

    /** Type for specifying what method is used for checking non-heap memory overload.
     * In case of PERCENT, the attribute nonHeapOverloadThresholdPercent must be set to a value between 1-100.
     * In case of AMOUNT, the attribute nonHeapOverloadThresholdAmount must be set to the value
     * above what the used non-heap space size is considered overloaded.
     * @author Miklos Pocsaji
     *
     */
    public enum NonHeapOverloadCheckPolicy {
        PERCENT, AMOUNT
    }

    private int cpuOverloadThresholdPercent = DEFAULT_CPU_OVERLOAD_THRESHOLD_PERCENT;
    private int cpuMeasurementWindow = DEFAULT_CPU_MEASUREMENT_WINDOW;
    private int dataCollectionPeriodSec = DEFAULT_DATA_COLLECTION_PERIOD_SEC;
    private int heapOverloadThresholdPercent = DEFAULT_HEAP_OVERLOAD_THRESHOLD_PERCENT;
    private int nonHeapOverloadThresholdPercent = DEFAULT_HEAP_OVERLOAD_THRESHOLD_PERCENT;
    private int nonHeapOverloadThresholdAmount;
    private NonHeapOverloadCheckPolicy nonHeapOverloadCheckPolicy = NonHeapOverloadCheckPolicy.PERCENT;

    public OverloadProtectorParameters() {
        // Empty constructor
    }

    private OverloadProtectorParameters(OverloadProtectorParameters other) {
        this.cpuOverloadThresholdPercent = other.cpuOverloadThresholdPercent;
        this.cpuMeasurementWindow = other.cpuMeasurementWindow;
        this.dataCollectionPeriodSec = other.dataCollectionPeriodSec;
        this.heapOverloadThresholdPercent = other.heapOverloadThresholdPercent;
        this.nonHeapOverloadThresholdPercent = other.nonHeapOverloadThresholdPercent;
        this.nonHeapOverloadThresholdAmount = other.nonHeapOverloadThresholdAmount;
        this.nonHeapOverloadCheckPolicy = other.nonHeapOverloadCheckPolicy;
    }

    OverloadProtectorParameters copy() {
        return new OverloadProtectorParameters(this);
    }

    public int getCpuOverloadThresholdPercent() {
        return cpuOverloadThresholdPercent;
    }

    public void setCpuOverloadThresholdPercent(int cpuOverloadThresholdPercent) {
        this.cpuOverloadThresholdPercent = cpuOverloadThresholdPercent;
    }

    public int getCpuMeasurementWindow() {
        return cpuMeasurementWindow;
    }

    public void setCpuMeasurementWindow(int cpuMeasurementWindow) {
        this.cpuMeasurementWindow = cpuMeasurementWindow;
    }

    public int getDataCollectionPeriodSec() {
        return dataCollectionPeriodSec;
    }

    public void setDataCollectionPeriodSec(int dataCollectionPeriodSec) {
        this.dataCollectionPeriodSec = dataCollectionPeriodSec;
    }

    public int getHeapOverloadThresholdPercent() {
        return heapOverloadThresholdPercent;
    }

    public void setHeapOverloadThresholdPercent(int heapOverloadThresholdPercent) {
        this.heapOverloadThresholdPercent = heapOverloadThresholdPercent;
    }

    public int getNonHeapOverloadThresholdPercent() {
        return nonHeapOverloadThresholdPercent;
    }

    public void setNonHeapOverloadThresholdPercent(int nonHeapOverloadThresholdPercent) {
        this.nonHeapOverloadThresholdPercent = nonHeapOverloadThresholdPercent;
    }

    public int getNonHeapOverloadThresholdAmount() {
        return nonHeapOverloadThresholdAmount;
    }

    public void setNonHeapOverloadThresholdAmount(int nonHeapOverloadThresholdAmount) {
        this.nonHeapOverloadThresholdAmount = nonHeapOverloadThresholdAmount;
    }

    public NonHeapOverloadCheckPolicy getNonHeapOverloadCheckPolicy() {
        return nonHeapOverloadCheckPolicy;
    }

    public void setNonHeapOverloadCheckPolicy(NonHeapOverloadCheckPolicy nonHeapOverloadCheckPolicy) {
        this.nonHeapOverloadCheckPolicy = nonHeapOverloadCheckPolicy;
    }
}
