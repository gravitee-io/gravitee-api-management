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
public class MonitoringThread {

    private int count;
    @JsonProperty("peak_count")
    private int peakCount;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getPeakCount() {
        return peakCount;
    }

    public void setPeakCount(int peakCount) {
        this.peakCount = peakCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonitoringThread)) return false;
        MonitoringThread that = (MonitoringThread) o;
        return count == that.count &&
                peakCount == that.peakCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, peakCount);
    }

    @Override
    public String toString() {
        return "MonitoringThread{" +
                "count=" + count +
                ", peakCount=" + peakCount +
                '}';
    }
}
