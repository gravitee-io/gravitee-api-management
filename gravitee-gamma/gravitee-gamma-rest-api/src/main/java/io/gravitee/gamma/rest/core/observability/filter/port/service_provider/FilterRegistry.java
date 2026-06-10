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
package io.gravitee.gamma.rest.core.observability.filter.port.service_provider;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import java.util.List;
import java.util.Set;

/**
 * Core-side port that returns the aggregated, deduplicated filter specs for observability queries.
 * The infra adapter discovers {@link FilterContributor} implementations (typically via
 * {@link java.util.ServiceLoader}); consumers stay unaware of how contributors are wired.
 *
 * @author GraviteeSource Team
 */
public interface FilterRegistry {
    /**
     * Returns the deduplicated union of all contributor filter specs, narrowed on two independent
     * axes: kept specs are those whose {@link FilterSpec#signals()} intersects {@code signals}
     * <strong>and</strong> whose {@link FilterSpec#apiTypes()} intersects {@code apiTypes}. An axis
     * passed as {@code null} or empty is unconstrained.
     *
     * @param signals  The signal set to filter by; {@code null}/empty → any signal.
     * @param apiTypes The API-kind set to filter by; {@code null}/empty → any API kind.
     */
    List<FilterSpec> getFilters(Set<Signal> signals, Set<ApiType> apiTypes);
}
