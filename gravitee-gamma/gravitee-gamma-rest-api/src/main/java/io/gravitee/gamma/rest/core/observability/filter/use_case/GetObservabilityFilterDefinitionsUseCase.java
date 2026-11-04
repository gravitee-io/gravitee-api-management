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
package io.gravitee.gamma.rest.core.observability.filter.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.AdditionalMetricCatalogDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDiscovery;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.BuiltInAdditionalMetricsPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.TrafficReadPermissionPort;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Returns the self-describing catalog of observability filters, narrowed on the two independent
 * discovery axes ({@link Signal} and {@link ApiType}). The UI calls this once per panel to build its
 * filter chip palette dynamically and to know which filters it may apply to which signal — without
 * hardcoded value→label maps (each {@code ENUM} spec carries its labelled values).
 *
 * <p>The built-in specs reading an additional metric carry its key and value type; the environment's exposed records
 * follow as {@code DECLARED} entries and, for a caller who reads the environment's traffic, its discovered keys as
 * {@code DISCOVERED} entries, with every {@code lastSeen} (see
 * {@link AdditionalMetricCatalogDomainService#filterCatalog}). Exposure is a catalog concern only: the query
 * validators do not read records.
 *
 * <p>Both axes are optional: a {@code null}/empty set on an axis is unconstrained, so an empty input
 * on both yields the full catalog. The registry keeps a spec when its own non-empty axis intersects
 * the requested one (see {@link FilterRegistry#getFilters(Set, Set)}).
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetObservabilityFilterDefinitionsUseCase {

    private final FilterRegistry filterRegistry;
    private final AdditionalMetricDefinitionRepository metricDefinitions;
    private final BuiltInAdditionalMetricsPort builtInAdditionalMetrics;
    private final AdditionalMetricDiscoveryPort discovery;
    private final TrafficReadPermissionPort trafficReadPermission;
    private final AdditionalMetricCatalogDomainService catalog;

    public record Input(String organizationId, String environmentId, Set<Signal> signals, Set<ApiType> apiTypes) {}

    /**
     * @param truncated {@code true} when the discovery behind the {@code DISCOVERED} entries was cut, by the cap or because
     *                  it failed, and the request admits such entries; never for a caller who may not read the traffic
     */
    public record Output(List<FilterSpec> filters, boolean truncated) {}

    public Output execute(Input input) {
        var builtInNamesByRawKey = builtInAdditionalMetrics.builtInNamesByRawKey();
        var records = metricDefinitions.findByEnvironmentId(input.environmentId());
        Optional<AdditionalMetricDiscovery> discovered = trafficReadPermission.readsTraffic(input.organizationId(), input.environmentId())
            ? Optional.of(
                discovery.discover(
                    input.organizationId(),
                    input.environmentId(),
                    catalog.keysListedElsewhere(builtInNamesByRawKey, records)
                )
            )
            : Optional.empty();
        var filters = catalog.filterCatalog(
            filterRegistry.getFilters(input.signals(), input.apiTypes()),
            builtInNamesByRawKey,
            records,
            discovered,
            input.signals(),
            input.apiTypes()
        );
        var truncated =
            discovered.map(AdditionalMetricDiscovery::truncated).orElse(false) &&
            catalog.admitsDynamicEntries(input.signals(), input.apiTypes());
        return new Output(filters, truncated);
    }
}
