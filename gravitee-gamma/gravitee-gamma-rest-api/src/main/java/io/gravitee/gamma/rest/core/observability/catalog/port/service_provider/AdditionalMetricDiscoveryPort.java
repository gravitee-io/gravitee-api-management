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
package io.gravitee.gamma.rest.core.observability.catalog.port.service_provider;

import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDiscovery;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;

/**
 * The additional metric keys the environment's documents carry, found by a discovery pass over the last 30 days.
 * Implementations may serve the result of a recent pass rather than run one per call: {@code refreshedAt} says when
 * it ran. Every field carries its key, its value type and a {@code lastSeen} (see {@link DiscoveredAdditionalMetric}).
 *
 * <p>The list calls it once per request, next to {@code TrafficReadPermissionPort}, which loads the caller's readable
 * APIs: an implementation should not load them again.
 *
 * @author GraviteeSource Team
 */
public interface AdditionalMetricDiscoveryPort {
    AdditionalMetricDiscovery discover(String organizationId, String environmentId);
}
