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
package io.gravitee.apim.core.analytics_engine.service_provider;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.exception.UnsupportedMetricException;
import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricName;
import io.gravitee.apim.core.analytics_engine.model.FacetName;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.observability.model.AdditionalMetricField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
public class AnalyticsQueryContextProvider {

    private final Map<MetricSpec.Name, AnalyticsEngineQueryService> services = new HashMap<>();

    public AnalyticsQueryContextProvider(List<AnalyticsEngineQueryService> impl) {
        for (var service : impl) {
            for (var metric : service.metrics()) {
                services.put(metric, service);
            }
        }
    }

    public Map<AnalyticsEngineQueryService, MeasuresRequest> resolve(MeasuresRequest measureRequest) {
        var context = new HashMap<AnalyticsEngineQueryService, MeasuresRequest>();
        for (var metric : measureRequest.metrics()) {
            var service = resolve(metric.name());
            var request = context.computeIfAbsent(service, s -> measureRequest.emptyMetrics());
            request.metrics().add(metric);
        }
        return context;
    }

    public Map<AnalyticsEngineQueryService, FacetsRequest> resolve(FacetsRequest facetsRequest) {
        var context = new HashMap<AnalyticsEngineQueryService, FacetsRequest>();
        for (var metric : facetsRequest.metrics()) {
            var service = resolve(metric.name(), facetsRequest.facets());
            var request = context.computeIfAbsent(service, s -> facetsRequest.emptyMetrics());
            request.metrics().add(metric);
        }
        return context;
    }

    public Map<AnalyticsEngineQueryService, TimeSeriesRequest> resolve(TimeSeriesRequest timeSeriesRequest) {
        var context = new HashMap<AnalyticsEngineQueryService, TimeSeriesRequest>();
        for (var metric : timeSeriesRequest.metrics()) {
            var service = resolve(metric.name(), timeSeriesRequest.facets());
            var request = context.computeIfAbsent(service, s -> timeSeriesRequest.emptyMetrics());
            request.metrics().add(metric);
        }
        return context;
    }

    /**
     * Whether a query naming the field reads the calls themselves: when the key holds the values of a built-in
     * identifying a caller, naming the field must not open them with a lesser right (OBS-70, the built-in wins on its
     * own key).
     */
    public boolean identifiesCaller(AdditionalMetricField field) {
        return services
            .values()
            .stream()
            .distinct()
            .flatMap(service -> service.builtInsStoredAs(field).stream())
            .anyMatch(FilterSpec.Name::isIdentifying);
    }

    /** See {@link AnalyticsEngineQueryService#narrowsBy}, asked of the service the metric is routed to. */
    public boolean narrowsBy(MetricSpec.Name metric, FilterSpec.Name filter) {
        return resolve(metric).narrowsBy(metric, filter);
    }

    /**
     * The service of a metric grouped by these facets. An additional metric field is in no metric's declaration, so
     * the validator cannot tell which metrics it groups: the service the metric is routed to does.
     */
    private AnalyticsEngineQueryService resolve(MetricSpec.Name metric, List<FacetName> facets) {
        var service = resolve(metric);
        Stream.ofNullable(facets)
            .flatMap(List::stream)
            .filter(AdditionalMetricName.class::isInstance)
            .filter(facet -> !service.groupsByAdditionalMetrics(metric))
            .findFirst()
            .ifPresent(facet -> {
                throw InvalidQueryException.forIncompatibleFacet(facet.name(), metric.name());
            });
        return service;
    }

    public AnalyticsEngineQueryService resolve(MetricSpec.Name metric) {
        return Optional.ofNullable(services.get(metric)).orElseThrow(() -> new UnsupportedMetricException(metric.name()));
    }
}
