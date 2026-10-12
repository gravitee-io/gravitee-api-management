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
package io.gravitee.apim.rest.api.mcp.infra;

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
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.rest.api.mcp.core.model.Dimension;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TimelinePoint;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.port.TrafficAnalytics;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads traffic from the analytics engine, which restricts every query to the APIs the caller can see.
 */
public class ComputeTrafficAnalytics implements TrafficAnalytics {

    /** The engine keeps the ten busiest values of a facet that is further split by status group. */
    private static final int MAX_VALUES = 10;

    private static final List<MetricSpec.Measure> COUNT = List.of(MetricSpec.Measure.COUNT);
    private static final List<MetricSpec.Measure> P95 = List.of(MetricSpec.Measure.P95);
    private static final List<FacetMetricMeasuresRequest> REQUESTS = List.of(
        new FacetMetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, COUNT, List.of())
    );
    private static final List<FacetMetricMeasuresRequest> RESPONSE_TIMES = List.of(
        new FacetMetricMeasuresRequest(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, P95, List.of()),
        new FacetMetricMeasuresRequest(MetricSpec.Name.HTTP_ENDPOINT_RESPONSE_TIME, P95, List.of())
    );

    private final ComputeMeasuresUseCase measures;
    private final ComputeFacetsUseCase facets;
    private final ComputeTimeSeriesUseCase timeSeries;

    public ComputeTrafficAnalytics(ComputeMeasuresUseCase measures, ComputeFacetsUseCase facets, ComputeTimeSeriesUseCase timeSeries) {
        this.measures = measures;
        this.facets = facets;
        this.timeSeries = timeSeries;
    }

    @Override
    public TrafficStats total(AuditInfo caller, TimeWindow window, TrafficScope scope) {
        var counts = new StatusCounts();
        facets(caller, window, scope, REQUESTS, List.of(FacetSpec.Name.HTTP_STATUS_CODE_GROUP), null).forEach(counts::addGroup);

        var request = new MeasuresRequest(
            range(window),
            filters(scope),
            List.of(
                new MetricMeasuresRequest(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, P95),
                new MetricMeasuresRequest(MetricSpec.Name.HTTP_ENDPOINT_RESPONSE_TIME, P95)
            )
        );
        var responseTimes = new HashMap<MetricSpec.Name, Double>();
        var measured = ApimContext.run(caller, () -> measures.execute(new ComputeMeasuresUseCase.Input(caller, request)));
        for (var metric : measured.response().metrics()) {
            responseTimes.put(metric.name(), valueOf(metric.measures()));
        }
        return counts.toStats(
            responseTimes.get(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME),
            responseTimes.get(MetricSpec.Name.HTTP_ENDPOINT_RESPONSE_TIME)
        );
    }

    @Override
    public List<TrafficSample> by(AuditInfo caller, TimeWindow window, TrafficScope scope, Dimension dimension) {
        if (dimension == Dimension.STATUS) {
            return byStatus(caller, window, scope);
        }
        var facet = facetOf(dimension);
        var responseTimes = new HashMap<String, Map<MetricSpec.Name, Double>>();
        var responseTimesRequest = new FacetsRequest(range(window), filters(scope), RESPONSE_TIMES, List.of(facet), MAX_VALUES, List.of());
        var response = ApimContext.run(caller, () ->
            facets.execute(new ComputeFacetsUseCase.Input(caller, responseTimesRequest))
        ).response();
        for (var metric : response.metrics()) {
            for (var bucket : metric.buckets()) {
                responseTimes.computeIfAbsent(bucket.key(), key -> new HashMap<>()).put(metric.metric(), valueOf(bucket.measures()));
            }
        }

        var samples = new ArrayList<TrafficSample>();
        for (var bucket : facets(caller, window, scope, REQUESTS, List.of(facet, FacetSpec.Name.HTTP_STATUS_CODE_GROUP), null)) {
            var counts = new StatusCounts();
            if (bucket.buckets() != null) {
                bucket.buckets().forEach(counts::addGroup);
            }
            var times = responseTimes.getOrDefault(bucket.key(), Map.of());
            samples.add(
                new TrafficSample(
                    bucket.key(),
                    nameOf(bucket),
                    counts.toStats(
                        times.get(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME),
                        times.get(MetricSpec.Name.HTTP_ENDPOINT_RESPONSE_TIME)
                    )
                )
            );
        }
        return samples;
    }

    @Override
    public List<TimelinePoint> timeline(AuditInfo caller, TimeWindow window, Duration interval, TrafficScope scope) {
        var responseTimes = new HashMap<Long, Map<MetricSpec.Name, Double>>();
        for (var metric : timeSeries(caller, window, interval, scope, RESPONSE_TIMES, List.of())) {
            for (var bucket : metric.buckets()) {
                responseTimes.computeIfAbsent(bucket.timestamp(), at -> new HashMap<>()).put(metric.name(), valueOf(bucket.measures()));
            }
        }

        var points = new ArrayList<TimelinePoint>();
        for (var metric : timeSeries(caller, window, interval, scope, REQUESTS, List.of(FacetSpec.Name.HTTP_STATUS_CODE_GROUP))) {
            for (TimeSeriesBucketResponse bucket : metric.buckets()) {
                var counts = new StatusCounts();
                if (bucket.buckets() != null) {
                    bucket.buckets().forEach(counts::addGroup);
                }
                var times = responseTimes.getOrDefault(bucket.timestamp(), Map.of());
                points.add(
                    new TimelinePoint(
                        Instant.ofEpochMilli(bucket.timestamp()),
                        counts.toStats(
                            times.get(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME),
                            times.get(MetricSpec.Name.HTTP_ENDPOINT_RESPONSE_TIME)
                        )
                    )
                );
            }
        }
        return points;
    }

    private List<TrafficSample> byStatus(AuditInfo caller, TimeWindow window, TrafficScope scope) {
        var sorted = List.of(
            new FacetMetricMeasuresRequest(
                MetricSpec.Name.HTTP_REQUESTS,
                COUNT,
                List.of(new FacetMetricMeasuresRequest.Sort(MetricSpec.Measure.COUNT, FacetMetricMeasuresRequest.Sort.Order.DESC))
            )
        );
        var samples = new ArrayList<TrafficSample>();
        for (var bucket : facets(caller, window, scope, sorted, List.of(FacetSpec.Name.HTTP_STATUS), MAX_VALUES)) {
            var counts = new StatusCounts();
            counts.add(bucket.key(), countOf(bucket));
            samples.add(new TrafficSample(bucket.key(), nameOf(bucket), counts.toStats(null, null)));
        }
        return samples;
    }

    private List<FacetBucketResponse> facets(
        AuditInfo caller,
        TimeWindow window,
        TrafficScope scope,
        List<FacetMetricMeasuresRequest> metrics,
        List<FacetSpec.Name> by,
        Integer limit
    ) {
        var request = new FacetsRequest(range(window), filters(scope), metrics, by, limit, List.of());
        return ApimContext.run(caller, () -> facets.execute(new ComputeFacetsUseCase.Input(caller, request)))
            .response()
            .metrics()
            .stream()
            .flatMap(metric -> metric.buckets().stream())
            .toList();
    }

    private List<io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse> timeSeries(
        AuditInfo caller,
        TimeWindow window,
        Duration interval,
        TrafficScope scope,
        List<FacetMetricMeasuresRequest> metrics,
        List<FacetSpec.Name> by
    ) {
        var request = new TimeSeriesRequest(range(window), interval.toMillis(), filters(scope), metrics, by, null, List.of());
        return ApimContext.run(caller, () -> timeSeries.execute(new ComputeTimeSeriesUseCase.Input(caller, request)))
            .response()
            .metrics();
    }

    private static TimeRange range(TimeWindow window) {
        return new TimeRange(window.from(), window.to());
    }

    private static List<Filter> filters(TrafficScope scope) {
        var filters = new ArrayList<Filter>();
        if (scope.apiId() != null) {
            filters.add(new Filter(FilterSpec.Name.API, FilterOperator.EQ, scope.apiId()));
        }
        if (scope.statusGroup() != null) {
            filters.add(new Filter(FilterSpec.Name.HTTP_STATUS_CODE_GROUP, FilterOperator.EQ, scope.statusGroup().label()));
        }
        return filters;
    }

    private static FacetSpec.Name facetOf(Dimension dimension) {
        return switch (dimension) {
            case API -> FacetSpec.Name.API;
            case APPLICATION -> FacetSpec.Name.APPLICATION;
            case PLAN -> FacetSpec.Name.PLAN;
            case GATEWAY -> FacetSpec.Name.GATEWAY;
            case STATUS -> FacetSpec.Name.HTTP_STATUS;
            case PATH -> FacetSpec.Name.HTTP_PATH;
            case METHOD -> FacetSpec.Name.HTTP_METHOD;
            case HOST -> FacetSpec.Name.HOST;
            case CONSUMER_IP -> FacetSpec.Name.CONSUMER_IP;
        };
    }

    private static String nameOf(FacetBucketResponse bucket) {
        return bucket.name() == null ? bucket.key() : bucket.name();
    }

    private static long countOf(FacetBucketResponse bucket) {
        var value = valueOf(bucket.measures());
        return value == null ? 0 : Math.round(value);
    }

    private static Double valueOf(List<Measure> measures) {
        if (measures == null || measures.isEmpty() || measures.getFirst().value() == null) {
            return null;
        }
        return measures.getFirst().value().doubleValue();
    }

    /** Request counts split by status class, read from buckets keyed by a status code or by a status range. */
    private static final class StatusCounts {

        private long requests;
        private long clientErrors;
        private long serverErrors;

        void addGroup(FacetBucketResponse bucket) {
            add(bucket.key(), countOf(bucket));
        }

        void add(String statusOrRange, long count) {
            requests += count;
            if (statusOrRange != null && statusOrRange.startsWith("4")) {
                clientErrors += count;
            } else if (statusOrRange != null && statusOrRange.startsWith("5")) {
                serverErrors += count;
            }
        }

        TrafficStats toStats(Double responseTimeP95Ms, Double endpointResponseTimeP95Ms) {
            return TrafficStats.of(
                requests,
                clientErrors,
                serverErrors,
                requests == 0 ? null : responseTimeP95Ms,
                requests == 0 ? null : endpointResponseTimeP95Ms
            );
        }
    }
}
