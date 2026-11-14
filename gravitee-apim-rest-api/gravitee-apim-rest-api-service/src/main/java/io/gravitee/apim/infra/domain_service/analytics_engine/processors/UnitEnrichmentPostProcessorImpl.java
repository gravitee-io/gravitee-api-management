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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import io.gravitee.apim.core.analytics_engine.domain_service.UnitEnrichmentPostProcessor;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class UnitEnrichmentPostProcessorImpl implements UnitEnrichmentPostProcessor {

    private final AnalyticsDefinitionQueryService definitionQueryService;

    @Override
    public MeasuresResponse enrichUnits(MeasuresResponse response) {
        var enriched = response
            .metrics()
            .stream()
            .map(metric ->
                new MetricMeasuresResponse(
                    metric.name(),
                    MetricSpec.servedUnit(unitOf(metric.name()), metric.measures().stream().map(Measure::name).toList()),
                    metric.measures(),
                    metric.ignoredFilters()
                )
            )
            .toList();
        return new MeasuresResponse(enriched);
    }

    @Override
    public FacetsResponse enrichUnits(FacetsResponse response, List<FacetMetricMeasuresRequest> requestedMetrics) {
        var requested = measuresByMetric(requestedMetrics);
        var enriched = response
            .metrics()
            .stream()
            .map(metric ->
                new MetricFacetsResponse(
                    metric.metric(),
                    MetricSpec.servedUnit(unitOf(metric.metric()), requested.getOrDefault(metric.metric(), Set.of())),
                    metric.buckets(),
                    metric.ignoredFilters()
                )
            )
            .toList();
        return new FacetsResponse(enriched);
    }

    @Override
    public TimeSeriesResponse enrichUnits(TimeSeriesResponse response, List<FacetMetricMeasuresRequest> requestedMetrics) {
        var requested = measuresByMetric(requestedMetrics);
        var enriched = response
            .metrics()
            .stream()
            .map(metric ->
                new TimeSeriesMetricResponse(
                    metric.name(),
                    MetricSpec.servedUnit(unitOf(metric.name()), requested.getOrDefault(metric.name(), Set.of())),
                    metric.buckets(),
                    metric.ignoredFilters()
                )
            )
            .toList();
        return new TimeSeriesResponse(enriched);
    }

    /** A built-in takes the unit its definition declares; an additional metric stays unset: no definition on this path carries one. */
    private MetricSpec.Unit unitOf(MetricName metric) {
        return switch (metric) {
            case MetricSpec.Name builtIn -> definitionQueryService.findMetric(builtIn).map(MetricSpec::unit).orElse(null);
            case AdditionalMetricName _ -> null;
        };
    }

    private static Map<MetricName, Set<MetricSpec.Measure>> measuresByMetric(List<FacetMetricMeasuresRequest> requestedMetrics) {
        return requestedMetrics
            .stream()
            .collect(
                Collectors.groupingBy(
                    FacetMetricMeasuresRequest::name,
                    Collectors.flatMapping(metric -> metric.measures().stream(), Collectors.toSet())
                )
            );
    }
}
