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
package io.gravitee.apim.core.analytics_engine.domain_service;

import static io.gravitee.apim.core.analytics_engine.exception.InternalDefinitionException.forUnknownMetric;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.NumberRange;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.utils.CollectionUtils;
import java.util.List;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
public class AnalyticsQueryValidator {

    private static final int MAX_FACETS_QUERY_FACETS_SIZE = 3;
    private static final int MAX_TIME_SERIES_QUERY_FACETS_SIZE = 2;

    private final AnalyticsDefinitionQueryService definition;

    public AnalyticsQueryValidator(AnalyticsDefinitionQueryService analyticsDefinitionQueryService) {
        this.definition = analyticsDefinitionQueryService;
    }

    public void validateMeasuresRequest(MeasuresRequest request) {
        validateTimeRange(request.timeRange());
        validateMetricsAndMeasures(request.metrics());
    }

    public void validateFacetsRequest(FacetsRequest request) {
        validateTimeRange(request.timeRange());
        validateFacetsSize(request);
        validateFacetMetricsAndMeasures(request.metrics());
        validateFacets(request.facets(), request.metrics());
        validateRanges(request.facets(), request.ranges());
        validateSorts(request.metrics());
    }

    public void validateTimeSeriesRequest(TimeSeriesRequest request) {
        validateTimeRange(request.timeRange());
        validateFacetsSize(request);
        validateFacetMetricsAndMeasures(request.metrics());
        validateFacets(request.facets(), request.metrics());
        validateRanges(request.facets(), request.ranges());
        validateSorts(request.metrics());
        validateInterval(request.interval());
    }

    private void validateInterval(Long interval) {
        if (interval <= 0) {
            throw InvalidQueryException.forZeroOrNegativeInterval(interval);
        }
    }

    private void validateSorts(List<FacetMetricMeasuresRequest> metrics) {
        for (var metric : metrics) {
            if (metric.sorts() != null && !metric.sorts().isEmpty()) {
                for (var sort : metric.sorts()) {
                    if (!metric.measures().contains(sort.measure())) {
                        throw InvalidQueryException.forInvalidSort(sort.measure().name());
                    }
                }
            }
        }
    }

    private void validateFacets(List<FacetSpec.Name> facets, List<FacetMetricMeasuresRequest> metrics) {
        for (var metric : metrics) {
            var metricSpec = definition.findMetric(metric.name()).orElseThrow(() -> forUnknownMetric(metric.name().name()));
            for (var facet : facets) {
                if (!metricSpec.facets().contains(facet)) {
                    throw InvalidQueryException.forIncompatibleFacet(facet.name(), metric.name().name());
                }
            }
        }
    }

    private void validateRanges(List<FacetSpec.Name> facets, List<NumberRange> ranges) throws InvalidQueryException {
        if (CollectionUtils.isEmpty(facets)) {
            return;
        }
        var rangedFacet = facets.getLast();
        if (rangedFacet == FacetSpec.Name.HTTP_STATUS_CODE_GROUP && CollectionUtils.isNotEmpty(ranges)) {
            throw InvalidQueryException.forForbiddenRanges(rangedFacet.name());
        }
    }

    private void validateFacetsSize(FacetsRequest request) {
        if (CollectionUtils.isEmpty(request.facets())) {
            throw InvalidQueryException.forEmptyFacets();
        }
        if (request.facets().size() > MAX_FACETS_QUERY_FACETS_SIZE) {
            throw InvalidQueryException.forMaximumFacetsExceeded(MAX_FACETS_QUERY_FACETS_SIZE, request.facets().size());
        }
    }

    private void validateFacetsSize(TimeSeriesRequest request) {
        if (request.facets().size() > MAX_TIME_SERIES_QUERY_FACETS_SIZE) {
            throw InvalidQueryException.forMaximumFacetsExceeded(MAX_TIME_SERIES_QUERY_FACETS_SIZE, request.facets().size());
        }
    }

    private void validateTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            throw new InvalidQueryException("Time range cannot be null");
        }
        if (timeRange.from() == null) {
            throw new InvalidQueryException("Time range lower bound cannot be null");
        }
        if (timeRange.to() == null) {
            throw new InvalidQueryException("Time range upper bound cannot be null");
        }
        if (timeRange.from().isAfter(timeRange.to())) {
            throw InvalidQueryException.forInvalidTimeRangeBounds();
        }
    }

    private void validateFacetMetricsAndMeasures(List<FacetMetricMeasuresRequest> queries) {
        validateMetricsAndMeasures(
            queries
                .stream()
                .map(query -> new MetricMeasuresRequest(query.name(), query.measures()))
                .toList()
        );
    }

    private void validateMetricsAndMeasures(List<MetricMeasuresRequest> queries) {
        for (var query : queries) {
            var metric = query.name();
            var metricSpec = definition.findMetric(metric).orElseThrow(() -> forUnknownMetric(metric.name()));

            for (var measure : query.measures()) {
                if (!metricSpec.measures().contains(measure)) {
                    throw InvalidQueryException.forInvalidMeasure(metric.name(), measure.name());
                }
            }
        }
    }
}
