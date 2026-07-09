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
package io.gravitee.definition.model.v4.agent;

import io.gravitee.definition.model.v4.analytics.tracing.Tracing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Analytics configuration for an agent API. Unlike the generic
 * {@link io.gravitee.definition.model.v4.analytics.Analytics} (sampling, logging, …), an agent only
 * exposes OpenTelemetry tracing for now — mirroring
 * {@link io.gravitee.definition.model.v4.nativeapi.NativeAnalytics}.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AgentAnalytics {

    /** Per-API OpenTelemetry tracing toggle (enabled + verbose). */
    private Tracing tracing;
}
