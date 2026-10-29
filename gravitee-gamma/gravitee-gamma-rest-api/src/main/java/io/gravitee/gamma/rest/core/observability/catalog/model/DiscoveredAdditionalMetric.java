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
import java.util.Objects;

/**
 * A key found in the environment's documents by a discovery pass.
 *
 * @param lastSeen most recent time a document of the environment carried the key within the discovery window; a key
 *                 without one was not found and is not reported.
 *
 * @author GraviteeSource Team
 */
public record DiscoveredAdditionalMetric(String rawKey, AdditionalMetricValueType valueType, Instant lastSeen) {
    public DiscoveredAdditionalMetric {
        Objects.requireNonNull(rawKey, "A discovered additional metric needs its key");
        Objects.requireNonNull(valueType, "A discovered additional metric needs its value type");
        Objects.requireNonNull(lastSeen, "A discovered additional metric needs the time it was last seen");
    }
}
