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
package io.gravitee.rest.api.model.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author GraviteeSource Team
 */
public class MonitoringMemory {

    @JsonProperty("total_in_bytes")
    private long totalInBytes;
    @JsonProperty("free_in_bytes")
    private long freeInBytes;
    @JsonProperty("used_in_bytes")
    private long usedInBytes;
    @JsonProperty("free_percent")
    private int freePercent;
    @JsonProperty("used_percent")
    private int usedPercent;

    public long getTotalInBytes() {
        return totalInBytes;
    }

    public void setTotalInBytes(long totalInBytes) {
        this.totalInBytes = totalInBytes;
    }

    public long getFreeInBytes() {
        return freeInBytes;
    }

    public void setFreeInBytes(long freeInBytes) {
        this.freeInBytes = freeInBytes;
    }

    public long getUsedInBytes() {
        return usedInBytes;
    }

    public void setUsedInBytes(long usedInBytes) {
        this.usedInBytes = usedInBytes;
    }

    public int getFreePercent() {
        return freePercent;
    }

    public void setFreePercent(int freePercent) {
        this.freePercent = freePercent;
    }

    public int getUsedPercent() {
        return usedPercent;
    }

    public void setUsedPercent(int usedPercent) {
        this.usedPercent = usedPercent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonitoringMemory)) return false;
        MonitoringMemory that = (MonitoringMemory) o;
        return totalInBytes == that.totalInBytes &&
                freeInBytes == that.freeInBytes &&
                usedInBytes == that.usedInBytes &&
                freePercent == that.freePercent &&
                usedPercent == that.usedPercent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalInBytes, freeInBytes, usedInBytes, freePercent, usedPercent);
    }

    @Override
    public String toString() {
        return "MonitoringMemory{" +
                "totalInBytes=" + totalInBytes +
                ", freeInBytes=" + freeInBytes +
                ", usedInBytes=" + usedInBytes +
                ", freePercent=" + freePercent +
                ", usedPercent=" + usedPercent +
                '}';
    }
}
