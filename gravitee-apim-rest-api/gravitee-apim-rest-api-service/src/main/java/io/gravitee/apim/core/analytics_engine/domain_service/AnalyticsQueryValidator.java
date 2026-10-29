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
import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricName;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetName;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterName;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.apim.core.observability.model.Signal;
import io.gravitee.apim.core.utils.CollectionUtils;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
        validateFilters(request.filters());
        validateMetricsAndMeasures(request.metrics());
    }

    public void validateFacetsRequest(FacetsRequest request) {
        validateTimeRange(request.timeRange());
        validateFilters(request.filters());
        validateFacetsSize(request);
        validateFacetMetricsAndMeasures(request.metrics());
        validateFacets(request.facets(), request.metrics());
        validateRanges(request.facets(), request.ranges());
        validateSorts(request.metrics());
    }

    public void validateTimeSeriesRequest(TimeSeriesRequest request) {
        validateTimeRange(request.timeRange());
        validateFilters(request.filters());
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

    /**
     * A built-in facet must be one the metric declares. An additional metric field is not in any declaration: its
     * value type alone says whether it groups, and the engine the metric is routed to whether it reads the field
     * (see {@code AnalyticsQueryContextProvider}).
     */
    private void validateFacets(List<FacetName> facets, List<FacetMetricMeasuresRequest> metrics) {
        for (var metric : metrics) {
            var metricSpec = definition.findMetric(metric.name()).orElseThrow(() -> forUnknownMetric(metric.name().name()));
            for (var level = 0; level < facets.size(); level++) {
                var facet = facets.get(level);
                switch (facet) {
                    case FacetSpec.Name builtIn -> {
                        if (!metricSpec.facets().contains(builtIn)) {
                            throw InvalidQueryException.forIncompatibleFacet(builtIn.name(), metric.name().name());
                        }
                    }
                    case AdditionalMetricName additionalMetric -> validateGrouping(additionalMetric, level == facets.size() - 1);
                }
            }
        }
    }

    /** A keyword or a boolean groups by its values at any level; a number by the request's ranges, as the last facet. */
    private static void validateGrouping(AdditionalMetricName facet, boolean last) {
        var valueType = facet.field().valueType();
        if (valueType.groupsByTerms()) {
            return;
        }
        if (!valueType.groupsByRanges()) {
            throw InvalidQueryException.forUngroupableFacet(facet.name());
        }
        if (!last) {
            throw InvalidQueryException.forRangesOnlyFacet(facet.name());
        }
    }

    private void validateRanges(List<FacetName> facets, List<NumberRange> ranges) throws InvalidQueryException {
        if (CollectionUtils.isEmpty(facets)) {
            return;
        }
        var rangedFacet = facets.getLast();
        var ranged = CollectionUtils.isNotEmpty(ranges);
        if (ranged && ranges.stream().anyMatch(range -> range.from() == null || range.to() == null)) {
            throw InvalidQueryException.forOpenRange(rangedFacet.name());
        }
        switch (rangedFacet) {
            case FacetSpec.Name builtIn -> {
                if (builtIn == FacetSpec.Name.HTTP_STATUS_CODE_GROUP && ranged) {
                    throw InvalidQueryException.forForbiddenRanges(builtIn.name());
                }
            }
            case AdditionalMetricName additionalMetric -> {
                var groupsByRanges = additionalMetric.field().valueType().groupsByRanges();
                if (groupsByRanges && !ranged) {
                    throw InvalidQueryException.forRangesOnlyFacet(additionalMetric.name());
                }
                if (!groupsByRanges && ranged) {
                    throw InvalidQueryException.forForbiddenRanges(additionalMetric.name());
                }
            }
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

    private void validateFilters(List<Filter> filters) {
        if (filters == null) {
            return;
        }
        for (var filter : filters) {
            if (filter.name() == null) {
                throw new InvalidQueryException("Filter name cannot be null");
            }
            if (!supportsAnalytics(filter.name())) {
                throw InvalidQueryException.forUnsupportedAnalyticsFilter(filter.name().name());
            }
            if (filter.operator() != null && !supportsAnalytics(filter.name(), filter.operator())) {
                throw InvalidQueryException.forUnsupportedAnalyticsOperator(filter.operator().name(), filter.name().name());
            }
            if (filter.value() == null) {
                throw InvalidQueryException.forNullFilterValue(filter.name().name());
            }
        }
    }

    /** The operators {@code AnalyticsMeasuresAdapter.toFilterOperator} translates; pinned by AnalyticsQueryValidatorTest. */
    private static final Set<FilterOperator> ANALYTICS_OPERATORS = EnumSet.of(
        FilterOperator.EQ,
        FilterOperator.IN,
        FilterOperator.LTE,
        FilterOperator.GTE
    );

    /**
     * Whether the analytics engine can evaluate this filter, read from the catalog's signal axis rather than
     * from a list kept in step with it by hand.
     *
     * <p>A name the catalog does not describe at all is accepted, as it was before the axis existed: the
     * catalog omits a few names the engines still know ({@code MESSAGE_CONNECTOR_ID} and the edge dimensions, pinned by
     * AnalyticsDefinitionYAMLQueryServiceTest for their owners to decide), and rejecting those here would be a
     * behaviour change rather than a refactor. An additional metric field is in no catalog: its value type
     * decides, through the operators it takes.
     */
    private boolean supportsAnalytics(FilterName name) {
        return switch (name) {
            case FilterSpec.Name builtIn -> definition
                .findFilter(builtIn)
                .map(spec -> spec.appliesTo(Signal.ANALYTICS))
                .orElse(true);
            case AdditionalMetricName _ -> true;
        };
    }

    /** An additional metric field takes the operators of its value type: none for a detail-only one. */
    private static boolean supportsAnalytics(FilterName name, FilterOperator operator) {
        if (!ANALYTICS_OPERATORS.contains(operator)) {
            return false;
        }
        return switch (name) {
            case FilterSpec.Name _ -> true;
            case AdditionalMetricName additionalMetric -> additionalMetric.field().valueType().operators().contains(operator);
        };
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
        for (var query : queries) {
            validateFilters(query.filters());
        }
        validateMetricsAndMeasures(
            queries
                .stream()
                .map(query -> new MetricMeasuresRequest(query.name(), query.measures()))
                .toList()
        );
    }

    private void validateMetricsAndMeasures(List<MetricMeasuresRequest> queries) {
        for (var query : queries) {
            // Needed for the direct MeasuresRequest path; no-op when called via validateFacetMetricsAndMeasures
            // (which projects to MetricMeasuresRequest with empty filters).
            validateFilters(query.filters());
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
