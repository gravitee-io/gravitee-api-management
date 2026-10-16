/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gamma.rest.resources.observability.capabilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.gamma.rest.core.observability.capabilities.model.ObservabilityCapabilities;

/** Keyed like the capabilities of gamma-lib-observability, so a UI hands the answer over as is. */
public record ObservabilityCapabilitiesDto(
    @JsonProperty("observability.logs.read") boolean logsRead,
    @JsonProperty("observability.traces.read") boolean tracesRead,
    @JsonProperty("observability.dashboards.read") boolean dashboardsRead,
    @JsonProperty("observability.dashboards.write") boolean dashboardsWrite
) {
    public static ObservabilityCapabilitiesDto from(ObservabilityCapabilities capabilities) {
        return new ObservabilityCapabilitiesDto(
            capabilities.logsRead(),
            capabilities.tracesRead(),
            capabilities.dashboardsRead(),
            capabilities.dashboardsWrite()
        );
    }
}
