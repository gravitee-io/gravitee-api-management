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
public class MonitoringProcess {

    @JsonProperty("cpu_percent")
    private int cpuPercent;
    @JsonProperty("open_file_descriptors")
    private int openFileDescriptors;
    @JsonProperty("max_file_descriptors")
    private int maxFileDescriptors;

    public int getCpuPercent() {
        return cpuPercent;
    }

    public void setCpuPercent(int cpuPercent) {
        this.cpuPercent = cpuPercent;
    }

    public int getOpenFileDescriptors() {
        return openFileDescriptors;
    }

    public void setOpenFileDescriptors(int openFileDescriptors) {
        this.openFileDescriptors = openFileDescriptors;
    }

    public int getMaxFileDescriptors() {
        return maxFileDescriptors;
    }

    public void setMaxFileDescriptors(int maxFileDescriptors) {
        this.maxFileDescriptors = maxFileDescriptors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonitoringProcess)) return false;
        MonitoringProcess that = (MonitoringProcess) o;
        return openFileDescriptors == that.openFileDescriptors &&
                maxFileDescriptors == that.maxFileDescriptors;
    }

    @Override
    public int hashCode() {
        return Objects.hash(openFileDescriptors, maxFileDescriptors);
    }

    @Override
    public String toString() {
        return "MonitoringProcess{" +
                "openFileDescriptors=" + openFileDescriptors +
                ", maxFileDescriptors=" + maxFileDescriptors +
                '}';
    }
}
