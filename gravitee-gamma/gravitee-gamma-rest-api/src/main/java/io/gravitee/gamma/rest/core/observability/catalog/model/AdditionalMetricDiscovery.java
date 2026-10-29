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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import java.time.Instant;
import java.util.List;

/**
 * Result of the discovery pass over the environment's documents.
 *
 * @param fields      the additional metric keys found over the last 30 days, the most recently seen first. Built-in
 *                    and declared keys are included, so their {@code lastSeen} can be served.
 * @param refreshedAt when the pass that produced {@code fields} ran.
 * @param truncated   {@code true} when the pass left keys out (cap or timeout).
 *
 * @author GraviteeSource Team
 */
public record AdditionalMetricDiscovery(List<DiscoveredAdditionalMetric> fields, Instant refreshedAt, boolean truncated) {
    public AdditionalMetricDiscovery {
        fields = List.copyOf(fields);
    }

    public static AdditionalMetricDiscovery empty(Instant refreshedAt) {
        return new AdditionalMetricDiscovery(List.of(), refreshedAt, false);
    }
}
