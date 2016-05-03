/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.model.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author GraviteeSource Team
 */
public class MonitoringJVM {

    private long timestamp;
    @JsonProperty("uptime_in_millis")
    private long uptimeInMillis;

    // Memory

    @JsonProperty("heap_used_in_bytes")
    private long heapUsedInBytes;
    @JsonProperty("heap_used_percent")
    private int heapUsedPercent;
    @JsonProperty("heap_committed_in_bytes")
    private long heapCommittedInBytes;
    @JsonProperty("heap_max_in_bytes")
    private long heapMaxInBytes;
    @JsonProperty("non_heap_used_in_bytes")
    private long nonHeapUsedInBytes;
    @JsonProperty("non_heap_committed_in_bytes")
    private long nonHeapCommittedInBytes;

    // Pools

    @JsonProperty("young_pool_used_in_bytes")
    private long youngPoolUsedInBytes;
    @JsonProperty("young_pool_max_in_bytes")
    private long youngPoolMaxInBytes;
    @JsonProperty("young_pool_peak_used_in_bytes")
    private long youngPoolPeakUsedInBytes;
    @JsonProperty("young_pool_peak_max_in_bytes")
    private long youngPoolPeakMaxInBytes;

    @JsonProperty("survivor_pool_used_in_bytes")
    private long survivorPoolUsedInBytes;
    @JsonProperty("survivor_pool_max_in_bytes")
    private long survivorPoolMaxInBytes;
    @JsonProperty("survivor_pool_peak_used_in_bytes")
    private long survivorPoolPeakUsedInBytes;
    @JsonProperty("survivor_pool_peak_max_in_bytes")
    private long survivorPoolPeakMaxInBytes;

    @JsonProperty("old_pool_used_in_bytes")
    private long oldPoolUsedInBytes;
    @JsonProperty("old_pool_max_in_bytes")
    private long oldPoolMaxInBytes;
    @JsonProperty("old_pool_peak_used_in_bytes")
    private long oldPoolPeakUsedInBytes;
    @JsonProperty("old_pool_peak_max_in_bytes")
    private long oldPoolPeakMaxInBytes;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getUptimeInMillis() {
        return uptimeInMillis;
    }

    public void setUptimeInMillis(long uptimeInMillis) {
        this.uptimeInMillis = uptimeInMillis;
    }

    public long getHeapUsedInBytes() {
        return heapUsedInBytes;
    }

    public void setHeapUsedInBytes(long heapUsedInBytes) {
        this.heapUsedInBytes = heapUsedInBytes;
    }

    public int getHeapUsedPercent() {
        return heapUsedPercent;
    }

    public void setHeapUsedPercent(int heapUsedPercent) {
        this.heapUsedPercent = heapUsedPercent;
    }

    public long getHeapCommittedInBytes() {
        return heapCommittedInBytes;
    }

    public void setHeapCommittedInBytes(long heapCommittedInBytes) {
        this.heapCommittedInBytes = heapCommittedInBytes;
    }

    public long getHeapMaxInBytes() {
        return heapMaxInBytes;
    }

    public void setHeapMaxInBytes(long heapMaxInBytes) {
        this.heapMaxInBytes = heapMaxInBytes;
    }

    public long getNonHeapUsedInBytes() {
        return nonHeapUsedInBytes;
    }

    public void setNonHeapUsedInBytes(long nonHeapUsedInBytes) {
        this.nonHeapUsedInBytes = nonHeapUsedInBytes;
    }

    public long getNonHeapCommittedInBytes() {
        return nonHeapCommittedInBytes;
    }

    public void setNonHeapCommittedInBytes(long nonHeapCommittedInBytes) {
        this.nonHeapCommittedInBytes = nonHeapCommittedInBytes;
    }

    public long getYoungPoolUsedInBytes() {
        return youngPoolUsedInBytes;
    }

    public void setYoungPoolUsedInBytes(long youngPoolUsedInBytes) {
        this.youngPoolUsedInBytes = youngPoolUsedInBytes;
    }

    public long getYoungPoolMaxInBytes() {
        return youngPoolMaxInBytes;
    }

    public void setYoungPoolMaxInBytes(long youngPoolMaxInBytes) {
        this.youngPoolMaxInBytes = youngPoolMaxInBytes;
    }

    public long getYoungPoolPeakUsedInBytes() {
        return youngPoolPeakUsedInBytes;
    }

    public void setYoungPoolPeakUsedInBytes(long youngPoolPeakUsedInBytes) {
        this.youngPoolPeakUsedInBytes = youngPoolPeakUsedInBytes;
    }

    public long getYoungPoolPeakMaxInBytes() {
        return youngPoolPeakMaxInBytes;
    }

    public void setYoungPoolPeakMaxInBytes(long youngPoolPeakMaxInBytes) {
        this.youngPoolPeakMaxInBytes = youngPoolPeakMaxInBytes;
    }

    public long getSurvivorPoolUsedInBytes() {
        return survivorPoolUsedInBytes;
    }

    public void setSurvivorPoolUsedInBytes(long survivorPoolUsedInBytes) {
        this.survivorPoolUsedInBytes = survivorPoolUsedInBytes;
    }

    public long getSurvivorPoolMaxInBytes() {
        return survivorPoolMaxInBytes;
    }

    public void setSurvivorPoolMaxInBytes(long survivorPoolMaxInBytes) {
        this.survivorPoolMaxInBytes = survivorPoolMaxInBytes;
    }

    public long getSurvivorPoolPeakUsedInBytes() {
        return survivorPoolPeakUsedInBytes;
    }

    public void setSurvivorPoolPeakUsedInBytes(long survivorPoolPeakUsedInBytes) {
        this.survivorPoolPeakUsedInBytes = survivorPoolPeakUsedInBytes;
    }

    public long getSurvivorPoolPeakMaxInBytes() {
        return survivorPoolPeakMaxInBytes;
    }

    public void setSurvivorPoolPeakMaxInBytes(long survivorPoolPeakMaxInBytes) {
        this.survivorPoolPeakMaxInBytes = survivorPoolPeakMaxInBytes;
    }

    public long getOldPoolUsedInBytes() {
        return oldPoolUsedInBytes;
    }

    public void setOldPoolUsedInBytes(long oldPoolUsedInBytes) {
        this.oldPoolUsedInBytes = oldPoolUsedInBytes;
    }

    public long getOldPoolMaxInBytes() {
        return oldPoolMaxInBytes;
    }

    public void setOldPoolMaxInBytes(long oldPoolMaxInBytes) {
        this.oldPoolMaxInBytes = oldPoolMaxInBytes;
    }

    public long getOldPoolPeakUsedInBytes() {
        return oldPoolPeakUsedInBytes;
    }

    public void setOldPoolPeakUsedInBytes(long oldPoolPeakUsedInBytes) {
        this.oldPoolPeakUsedInBytes = oldPoolPeakUsedInBytes;
    }

    public long getOldPoolPeakMaxInBytes() {
        return oldPoolPeakMaxInBytes;
    }

    public void setOldPoolPeakMaxInBytes(long oldPoolPeakMaxInBytes) {
        this.oldPoolPeakMaxInBytes = oldPoolPeakMaxInBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonitoringJVM)) return false;
        MonitoringJVM that = (MonitoringJVM) o;
        return timestamp == that.timestamp &&
                uptimeInMillis == that.uptimeInMillis &&
                heapUsedInBytes == that.heapUsedInBytes &&
                heapUsedPercent == that.heapUsedPercent &&
                heapCommittedInBytes == that.heapCommittedInBytes &&
                heapMaxInBytes == that.heapMaxInBytes &&
                nonHeapUsedInBytes == that.nonHeapUsedInBytes &&
                nonHeapCommittedInBytes == that.nonHeapCommittedInBytes &&
                youngPoolUsedInBytes == that.youngPoolUsedInBytes &&
                youngPoolMaxInBytes == that.youngPoolMaxInBytes &&
                youngPoolPeakUsedInBytes == that.youngPoolPeakUsedInBytes &&
                youngPoolPeakMaxInBytes == that.youngPoolPeakMaxInBytes &&
                survivorPoolUsedInBytes == that.survivorPoolUsedInBytes &&
                survivorPoolMaxInBytes == that.survivorPoolMaxInBytes &&
                survivorPoolPeakUsedInBytes == that.survivorPoolPeakUsedInBytes &&
                survivorPoolPeakMaxInBytes == that.survivorPoolPeakMaxInBytes &&
                oldPoolUsedInBytes == that.oldPoolUsedInBytes &&
                oldPoolMaxInBytes == that.oldPoolMaxInBytes &&
                oldPoolPeakUsedInBytes == that.oldPoolPeakUsedInBytes &&
                oldPoolPeakMaxInBytes == that.oldPoolPeakMaxInBytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, uptimeInMillis, heapUsedInBytes, heapUsedPercent, heapCommittedInBytes, heapMaxInBytes, nonHeapUsedInBytes, nonHeapCommittedInBytes, youngPoolUsedInBytes, youngPoolMaxInBytes, youngPoolPeakUsedInBytes, youngPoolPeakMaxInBytes, survivorPoolUsedInBytes, survivorPoolMaxInBytes, survivorPoolPeakUsedInBytes, survivorPoolPeakMaxInBytes, oldPoolUsedInBytes, oldPoolMaxInBytes, oldPoolPeakUsedInBytes, oldPoolPeakMaxInBytes);
    }

    @Override
    public String toString() {
        return "MonitoringJVM{" +
                "timestamp=" + timestamp +
                ", uptimeInMillis=" + uptimeInMillis +
                ", heapUsedInBytes=" + heapUsedInBytes +
                ", heapUsedPercent=" + heapUsedPercent +
                ", heapCommittedInBytes=" + heapCommittedInBytes +
                ", heapMaxInBytes=" + heapMaxInBytes +
                ", nonHeapUsedInBytes=" + nonHeapUsedInBytes +
                ", nonHeapCommittedInBytes=" + nonHeapCommittedInBytes +
                ", youngPoolUsedInBytes=" + youngPoolUsedInBytes +
                ", youngPoolMaxInBytes=" + youngPoolMaxInBytes +
                ", youngPoolPeakUsedInBytes=" + youngPoolPeakUsedInBytes +
                ", youngPoolPeakMaxInBytes=" + youngPoolPeakMaxInBytes +
                ", survivorPoolUsedInBytes=" + survivorPoolUsedInBytes +
                ", survivorPoolMaxInBytes=" + survivorPoolMaxInBytes +
                ", survivorPoolPeakUsedInBytes=" + survivorPoolPeakUsedInBytes +
                ", survivorPoolPeakMaxInBytes=" + survivorPoolPeakMaxInBytes +
                ", oldPoolUsedInBytes=" + oldPoolUsedInBytes +
                ", oldPoolMaxInBytes=" + oldPoolMaxInBytes +
                ", oldPoolPeakUsedInBytes=" + oldPoolPeakUsedInBytes +
                ", oldPoolPeakMaxInBytes=" + oldPoolPeakMaxInBytes +
                '}';
    }
}
