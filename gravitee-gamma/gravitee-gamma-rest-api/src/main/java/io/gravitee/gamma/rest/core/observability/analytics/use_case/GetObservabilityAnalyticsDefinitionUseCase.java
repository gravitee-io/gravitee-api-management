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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.AdditionalMetricCatalogDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDiscovery;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.BuiltInAdditionalMetricsPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.TrafficReadPermissionPort;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Describes what the widget editor offers for some API kinds in an environment: the built-in metrics, each once,
 * grouped by family, then the environment's additional metric fields holding a number under the {@code ADDITIONAL}
 * family, and the facets those metrics can be grouped by.
 *
 * <p>The additional metrics are the exposed records as {@code DECLARED} entries and, for a caller who reads the
 * environment's traffic, the discovered keys as {@code DISCOVERED} entries, each for the kinds its namespace ties it
 * to (see {@link AdditionalMetricCatalogDomainService#analyticsCatalog}). They come only when a requested kind is one
 * the engine serves them for; no discovery runs otherwise. The environment's keyword and bool keys come along as
 * facets: of the built-in every additional metric takes its facets from, and of every additional metric.
 *
 * <p>{@code origins} narrows the built definition to the entries of those origins; {@code truncated} keeps describing
 * the discovery pass whatever origins are kept.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetObservabilityAnalyticsDefinitionUseCase {

    private final AnalyticsDefinitionPort analyticsDefinitionPort;
    private final AdditionalMetricDefinitionRepository metricDefinitions;
    private final BuiltInAdditionalMetricsPort builtInAdditionalMetrics;
    private final AdditionalMetricDiscoveryPort discovery;
    private final TrafficReadPermissionPort trafficReadPermission;
    private final AdditionalMetricCatalogDomainService catalog;

    public record Input(String organizationId, String environmentId, Set<ApiType> apiTypes, Set<CatalogOrigin> origins) {
        /** Every origin. */
        public Input(String organizationId, String environmentId, Set<ApiType> apiTypes) {
            this(organizationId, environmentId, apiTypes, Set.of());
        }
    }

    /**
     * @param truncated {@code true} when the discovery behind the {@code DISCOVERED} entries was cut, by the cap or because
     *                  it failed, and the request admits such entries; never for a caller who may not read the traffic
     */
    public record Output(AnalyticsDefinition definition, boolean truncated) {}

    public Output execute(Input input) {
        if (input.apiTypes() == null || input.apiTypes().isEmpty()) {
            throw InvalidObservabilityQueryException.missingApiTypes();
        }
        // Kind order, not request order: the same kinds always get the same answer, and the same ETag.
        var kinds = EnumSet.copyOf(input.apiTypes());
        Map<String, MetricDefinition> metrics = new LinkedHashMap<>();
        for (var kind : kinds) {
            if (!ApiType.API_KINDS.contains(kind)) {
                throw InvalidObservabilityQueryException.notAnApiKind(kind.name());
            }
            analyticsDefinitionPort.metricsOf(kind).forEach(metric -> metrics.putIfAbsent(metric.name(), metric));
        }
        var support = analyticsDefinitionPort.additionalMetricSupport();
        if (!support.servesAnyOf(kinds)) {
            return new Output(AnalyticsDefinition.of(metrics.values()).narrowedTo(input.origins()), false);
        }
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
        var additional = catalog.analyticsCatalog(builtInNamesByRawKey, records, discovered, support, kinds);
        metrics.computeIfPresent(support.dimensionsOf(), (name, metric) -> metric.groupedAlsoBy(additional.facetsFor(metric.apiTypes())));
        additional.metrics().forEach(metric -> metrics.putIfAbsent(metric.name(), metric));
        return new Output(
            AnalyticsDefinition.of(metrics.values()).narrowedTo(input.origins()),
            discovered.map(AdditionalMetricDiscovery::truncated).orElse(false)
        );
    }
}
