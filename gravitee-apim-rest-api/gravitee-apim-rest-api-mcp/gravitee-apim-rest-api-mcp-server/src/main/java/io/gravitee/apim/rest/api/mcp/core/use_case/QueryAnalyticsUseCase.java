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
package io.gravitee.apim.rest.api.mcp.core.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.apim.rest.api.mcp.core.ApiResolver;
import io.gravitee.apim.rest.api.mcp.core.EnvironmentResolver;
import io.gravitee.apim.rest.api.mcp.core.Names;
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.AnalyticsQuery;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.model.Interval;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.port.AnalyticsQueries;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.common.utils.TimeProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One query to the analytics engine, in its vocabulary, over a window; the same query over the preceding range of
 * equal length when asked. The numbers come back as they are; what they mean is the caller's call.
 */
@UseCase
public class QueryAnalyticsUseCase {

    private final EnvironmentDirectory environments;
    private final ApiDirectory apis;
    private final AnalyticsQueries analytics;

    public QueryAnalyticsUseCase(EnvironmentDirectory environments, ApiDirectory apis, AnalyticsQueries analytics) {
        this.environments = environments;
        this.apis = apis;
        this.analytics = analytics;
    }

    public enum Mode {
        MEASURES,
        FACETS,
        TIME_SERIES,
    }

    /**
     * @param interval the length of one point, {@code 30s}/{@code 5m}/{@code 1h}/{@code 1d} or milliseconds; time series only
     * @param compareWith {@code previous} to run the same query over the preceding range of equal length
     */
    public record Input(
        McpCaller caller,
        String environment,
        String mode,
        List<AnalyticsQuery.Metric> metrics,
        List<AnalyticsQuery.Filter> filters,
        List<String> by,
        Object interval,
        Integer limit,
        List<AnalyticsQuery.Range> ranges,
        String window,
        String until,
        String compareWith
    ) {}

    /**
     * @param previous the same query over {@code previousWindow}; null unless asked
     */
    public record Output(
        String summary,
        EnvRef environment,
        String mode,
        TimeWindow window,
        TimeWindow previousWindow,
        List<MetricResult> current,
        List<MetricResult> previous
    ) {}

    public record MetricResult(String metric, String unit, Map<String, Number> measures, List<Bucket> buckets) {}

    /**
     * @param at the start of the point, time series only
     * @param name the display name of the key when the engine knows one: an API or application name, a status label
     */
    public record Bucket(String key, String name, Instant at, Map<String, Number> measures, List<Bucket> buckets) {}

    public Output execute(Input input) {
        var mode = parseMode(input.mode());
        var window = TimeWindow.parse(input.window(), input.until(), TimeProvider.instantNow());
        var scope = EnvironmentResolver.resolve(environments, apis, input.caller(), input.environment(), null);
        var compare = input.compareWith() != null && !input.compareWith().isBlank();
        if (compare && !"previous".equalsIgnoreCase(input.compareWith().trim())) {
            throw new TriageException("Invalid compareWith '" + input.compareWith() + "'. The only comparison is 'previous'.");
        }
        if (input.metrics() == null || input.metrics().isEmpty()) {
            throw new TriageException("metrics is required: at least one metric with its measures. Call describe_metrics to list them.");
        }

        var current = run(mode, scope.auditInfo(), window, input);
        var previous = compare ? run(mode, scope.auditInfo(), window.baseline(), input) : null;
        var summary =
            mode.name().toLowerCase(Locale.ROOT) +
            " of " +
            current.size() +
            " metric(s) over " +
            window.length().toMinutes() +
            " min" +
            (compare ? ", and over the " + window.length().toMinutes() + " min before" : "") +
            ".";
        return new Output(summary, scope.environment(), mode.name(), window, compare ? window.baseline() : null, current, previous);
    }

    private List<MetricResult> run(Mode mode, AuditInfo caller, TimeWindow window, Input input) {
        var range = new TimeRange(window.from(), window.to());
        var filters = filters(caller, input.filters());
        try {
            return switch (mode) {
                case MEASURES -> {
                    var metrics = input
                        .metrics()
                        .stream()
                        .map(metric ->
                            new MetricMeasuresRequest(Names.metric(metric.name()), measures(metric), filters(caller, metric.filters()))
                        )
                        .toList();
                    yield analytics
                        .measures(caller, new MeasuresRequest(range, filters, metrics))
                        .metrics()
                        .stream()
                        .map(metric -> new MetricResult(metric.name().name(), unit(metric.unit()), measures(metric.measures()), List.of()))
                        .toList();
                }
                case FACETS -> {
                    var request = new FacetsRequest(
                        range,
                        filters,
                        facetMetrics(caller, input),
                        facets(input.by()),
                        input.limit(),
                        ranges(input.ranges())
                    );
                    yield analytics
                        .facets(caller, request)
                        .metrics()
                        .stream()
                        .map(metric -> new MetricResult(metric.metric().name(), unit(metric.unit()), Map.of(), buckets(metric.buckets())))
                        .toList();
                }
                case TIME_SERIES -> {
                    if (input.interval() == null) {
                        throw new TriageException("interval is required for time_series: the length of one point, for example 1m or 5m.");
                    }
                    var request = new TimeSeriesRequest(
                        range,
                        Interval.parse(input.interval()).toMillis(),
                        filters,
                        facetMetrics(caller, input),
                        facets(input.by()),
                        input.limit(),
                        ranges(input.ranges())
                    );
                    yield analytics
                        .timeSeries(caller, request)
                        .metrics()
                        .stream()
                        .map(metric -> new MetricResult(metric.name().name(), unit(metric.unit()), Map.of(), points(metric.buckets())))
                        .toList();
                }
            };
        } catch (ValidationDomainException e) {
            throw new TriageException(e.getMessage() + " Call describe_metrics with metric=<name> for what it takes.");
        }
    }

    private List<FacetMetricMeasuresRequest> facetMetrics(AuditInfo caller, Input input) {
        return input
            .metrics()
            .stream()
            .map(metric ->
                new FacetMetricMeasuresRequest(
                    Names.metric(metric.name()),
                    measures(metric),
                    filters(caller, metric.filters()),
                    sorts(metric.sorts())
                )
            )
            .toList();
    }

    private static List<MetricSpec.Measure> measures(AnalyticsQuery.Metric metric) {
        if (metric.measures() == null || metric.measures().isEmpty()) {
            throw new TriageException(
                "Metric " + metric.name() + " needs at least one measure. Call describe_metrics with metric=" + metric.name() + "."
            );
        }
        return metric.measures().stream().map(Names::measure).toList();
    }

    private static List<FacetMetricMeasuresRequest.Sort> sorts(List<AnalyticsQuery.Sort> sorts) {
        return sorts == null
            ? List.of()
            : sorts
                .stream()
                .map(sort -> new FacetMetricMeasuresRequest.Sort(Names.measure(sort.measure()), Names.order(sort.order())))
                .toList();
    }

    private static List<FacetSpec.Name> facets(List<String> by) {
        return by == null ? List.of() : by.stream().map(Names::facet).toList();
    }

    private static List<NumberRange> ranges(List<AnalyticsQuery.Range> ranges) {
        return ranges == null
            ? List.of()
            : ranges
                .stream()
                .map(range -> new NumberRange(range.from(), range.to()))
                .toList();
    }

    /** API names are accepted where the engine wants ids. */
    private List<Filter> filters(AuditInfo caller, List<AnalyticsQuery.Filter> filters) {
        if (filters == null) {
            return List.of();
        }
        var result = new ArrayList<Filter>();
        for (var filter : filters) {
            var name = Names.filter(filter.name());
            var operator = Names.operator(filter.operator());
            Object value = switch (operator) {
                case IN -> values(filter)
                    .stream()
                    .map(v -> name == FilterSpec.Name.API ? apiId(caller, v) : v)
                    .toList();
                case LTE, GTE -> number(filter);
                default -> name == FilterSpec.Name.API ? apiId(caller, text(filter)) : text(filter);
            };
            result.add(new Filter(name, operator, value));
        }
        return result;
    }

    private String apiId(AuditInfo caller, String idOrName) {
        return ApiResolver.resolve(apis, caller, idOrName).id();
    }

    private static List<String> values(AnalyticsQuery.Filter filter) {
        if (filter.value() instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(text(filter));
    }

    private static String text(AnalyticsQuery.Filter filter) {
        if (filter.value() == null) {
            throw new TriageException("Filter " + filter.name() + " needs a value.");
        }
        return String.valueOf(filter.value());
    }

    private static Number number(AnalyticsQuery.Filter filter) {
        if (filter.value() instanceof Number n) {
            return n;
        }
        try {
            return Double.valueOf(text(filter));
        } catch (NumberFormatException e) {
            throw new TriageException(
                "Filter " + filter.name() + " with " + filter.operator() + " needs a number, not '" + filter.value() + "'."
            );
        }
    }

    private static Mode parseMode(String mode) {
        if (mode != null) {
            for (var candidate : Mode.values()) {
                if (candidate.name().equalsIgnoreCase(mode.trim())) {
                    return candidate;
                }
            }
        }
        throw new TriageException("Invalid mode '" + mode + "'. Use measures, facets or time_series.");
    }

    private static String unit(MetricSpec.Unit unit) {
        return unit == null ? null : unit.name();
    }

    private static Map<String, Number> measures(List<Measure> measures) {
        var result = new LinkedHashMap<String, Number>();
        if (measures != null) {
            measures.forEach(measure -> result.put(measure.name().name(), measure.value()));
        }
        return result;
    }

    private static List<Bucket> buckets(List<FacetBucketResponse> buckets) {
        return buckets == null
            ? List.of()
            : buckets
                .stream()
                .map(bucket -> new Bucket(bucket.key(), bucket.name(), null, measures(bucket.measures()), buckets(bucket.buckets())))
                .toList();
    }

    private static List<Bucket> points(List<TimeSeriesBucketResponse> buckets) {
        return buckets == null
            ? List.of()
            : buckets
                .stream()
                .map(bucket ->
                    new Bucket(
                        bucket.key(),
                        null,
                        bucket.timestamp() == null ? null : Instant.ofEpochMilli(bucket.timestamp()),
                        measures(bucket.measures()),
                        buckets(bucket.buckets())
                    )
                )
                .toList();
    }
}
