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
package io.gravitee.gamma.rest.core.observability.catalog.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.AdditionalMetricCatalogDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalog;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.BuiltInAdditionalMetricsPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.TrafficReadPermissionPort;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Lists the environment's additional metrics: built-in, declared and, for a caller who reads the environment's
 * traffic, discovered, each with its {@code lastSeen}.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class ListMetricDefinitionsUseCase {

    private final AdditionalMetricDefinitionRepository repository;
    private final BuiltInAdditionalMetricsPort builtInAdditionalMetrics;
    private final AdditionalMetricDiscoveryPort discovery;
    private final TrafficReadPermissionPort trafficReadPermission;
    private final FilterRegistry filterRegistry;
    private final AnalyticsDefinitionPort analyticsDefinition;
    private final AdditionalMetricCatalogDomainService catalog;

    public record Input(String organizationId, String environmentId) {}

    public record Output(AdditionalMetricCatalog catalog) {}

    public Output execute(Input input) {
        return new Output(
            catalog.merge(
                builtInAdditionalMetrics.builtInNamesByRawKey(),
                filterRegistry.getFilters(Set.of(), Set.of()),
                builtInMetrics().values(),
                repository.findByEnvironmentId(input.environmentId()),
                discovery.discover(input.organizationId(), input.environmentId()),
                trafficReadPermission.readsTraffic(input.organizationId(), input.environmentId())
            )
        );
    }

    private Map<String, MetricDefinition> builtInMetrics() {
        Map<String, MetricDefinition> metrics = new LinkedHashMap<>();
        for (ApiType kind : EnumSet.copyOf(ApiType.API_KINDS)) {
            analyticsDefinition.metricsOf(kind).forEach(metric -> metrics.putIfAbsent(metric.name(), metric));
        }
        return metrics;
    }
}
