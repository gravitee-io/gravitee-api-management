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
package io.gravitee.apim.core.dashboard.model;

import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.utils.CollectionUtils;
import java.util.List;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class DashboardWidgetRequestMapper {

    private DashboardWidgetRequestMapper() {}

    public static Object toAnalyticsRequest(DashboardWidget.Request request) {
        if (request == null) {
            throw new InvalidQueryException("Widget request cannot be null");
        }
        var type = request.getType();
        if (type == null || type.isBlank()) {
            throw new InvalidQueryException("Widget request type is required");
        }
        return switch (type) {
            case "measures" -> toMeasuresRequest(request);
            case "facets" -> toFacetsRequest(request);
            case "time-series" -> toTimeSeriesRequest(request);
            default -> throw new InvalidQueryException("Unknown widget request type: " + type);
        };
    }

    public static MeasuresRequest toMeasuresRequest(DashboardWidget.Request request) {
        var timeRange = toTimeRange(request.getTimeRange());
        var metrics = toMetricMeasuresRequests(request.getMetrics());
        return new MeasuresRequest(timeRange, List.of(), metrics);
    }

    public static FacetsRequest toFacetsRequest(DashboardWidget.Request request) {
        var timeRange = toTimeRange(request.getTimeRange());
        var metrics = toFacetMetricMeasuresRequests(request.getMetrics());
        var facets = toFacetNames(request.getBy());
        return new FacetsRequest(timeRange, List.of(), metrics, facets, request.getLimit(), List.of());
    }

    public static TimeSeriesRequest toTimeSeriesRequest(DashboardWidget.Request request) {
        var interval = request.getInterval();
        if (interval == null || interval <= 0) {
            throw InvalidQueryException.forZeroOrNegativeInterval(interval);
        }
        var timeRange = toTimeRange(request.getTimeRange());
        var metrics = toFacetMetricMeasuresRequests(request.getMetrics());
        var facets = request.getBy() != null ? toFacetNames(request.getBy()) : List.<FacetSpec.Name>of();
        return new TimeSeriesRequest(timeRange, interval, List.of(), metrics, facets, request.getLimit(), List.of());
    }

    private static TimeRange toTimeRange(DashboardWidget.TimeRange timeRange) {
        if (timeRange == null || timeRange.getFrom() == null || timeRange.getTo() == null) {
            throw new InvalidQueryException("Widget request timeRange with from and to is required");
        }
        return new TimeRange(timeRange.getFrom(), timeRange.getTo());
    }

    private static List<MetricMeasuresRequest> toMetricMeasuresRequests(List<DashboardWidget.MetricRequest> metrics) {
        if (CollectionUtils.isEmpty(metrics)) {
            throw new InvalidQueryException("Widget request must provide at least one metric");
        }
        return metrics.stream().map(DashboardWidgetRequestMapper::toMetricMeasuresRequest).toList();
    }

    private static MetricMeasuresRequest toMetricMeasuresRequest(DashboardWidget.MetricRequest m) {
        if (m == null || m.getName() == null || m.getName().isBlank()) {
            throw new InvalidQueryException("Metric name is required");
        }
        var name = parseMetricName(m.getName());
        var measures = parseMeasures(m.getMeasures(), name.name());
        return new MetricMeasuresRequest(name, measures);
    }

    private static List<FacetMetricMeasuresRequest> toFacetMetricMeasuresRequests(List<DashboardWidget.MetricRequest> metrics) {
        if (CollectionUtils.isEmpty(metrics)) {
            throw new InvalidQueryException("Widget request must provide at least one metric");
        }
        return metrics.stream().map(DashboardWidgetRequestMapper::toFacetMetricMeasuresRequest).toList();
    }

    private static FacetMetricMeasuresRequest toFacetMetricMeasuresRequest(DashboardWidget.MetricRequest m) {
        if (m == null || m.getName() == null || m.getName().isBlank()) {
            throw new InvalidQueryException("Metric name is required");
        }
        var name = parseMetricName(m.getName());
        var measures = parseMeasures(m.getMeasures(), name.name());
        return new FacetMetricMeasuresRequest(name, measures, null);
    }

    private static List<FacetSpec.Name> toFacetNames(List<String> by) {
        if (CollectionUtils.isEmpty(by)) {
            return List.of();
        }
        return by.stream().map(DashboardWidgetRequestMapper::parseFacetName).toList();
    }

    private static MetricSpec.Name parseMetricName(String value) {
        try {
            return MetricSpec.Name.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidQueryException("Unknown metric name: " + value);
        }
    }

    private static List<MetricSpec.Measure> parseMeasures(List<String> values, String metricName) {
        if (CollectionUtils.isEmpty(values)) {
            throw new InvalidQueryException("At least one measure is required for metric " + metricName);
        }
        return values.stream().map(DashboardWidgetRequestMapper::parseMeasure).toList();
    }

    private static MetricSpec.Measure parseMeasure(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidQueryException("Measure name cannot be empty");
        }
        try {
            return MetricSpec.Measure.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidQueryException("Unknown measure name: " + value);
        }
    }

    private static FacetSpec.Name parseFacetName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidQueryException("Facet name cannot be empty");
        }
        try {
            return FacetSpec.Name.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidQueryException("Unknown facet name: " + value);
        }
    }
}
