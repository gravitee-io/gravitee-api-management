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

import java.util.Map;
import java.util.Objects;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author GraviteeSource Team
 */
public class MonitoringCPU {

    @JsonProperty("percent_use")
    private int percentUse;
    @JsonProperty("load_average")
    private Map<String, ? super Number> loadAverage;

    public int getPercentUse() {
        return percentUse;
    }

    public void setPercentUse(int percentUse) {
        this.percentUse = percentUse;
    }

    public Map<String, ? super Number> getLoadAverage() {
        return loadAverage;
    }

    public void setLoadAverage(Map<String, ? super Number> loadAverage) {
        this.loadAverage = loadAverage;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonitoringCPU)) return false;
        MonitoringCPU that = (MonitoringCPU) o;
        return percentUse == that.percentUse &&
                Objects.equals(loadAverage, that.loadAverage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(percentUse, loadAverage);
    }

    @Override
    public String toString() {
        return "MonitoringCPU{" +
                "percentUse=" + percentUse +
                ", loadAverage=" + loadAverage +
                '}';
    }
}
