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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.NativeOperationDurationBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleMaxBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleSUMBuilder;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Measures over the native Kafka {@code event-metrics} data stream.
 *
 * <p>The index mixes four document shapes (see {@link EventMetricsFieldResolver}), so every metric
 * has to be restricted to the {@code doc-type} that carries its field. The restriction is applied as
 * a {@code filter} sub-aggregation named {@code <METRIC>#__FILTER__} — the same envelope
 * {@link HTTPMeasuresQueryAdapter} uses for per-metric filters, and which {@code AggregationAdapter}
 * already knows how to unwrap on the response side.
 *
 * <p>When every metric of the query targets the same {@code doc-type} — the common case, a widget
 * queries one family — the term is hoisted into the root query instead, which keeps the aggregation
 * tree flat and avoids scanning documents that cannot contribute.
 *
 * <p>Supported measures are deliberately few: {@code SUM} for the accumulated counters, {@code MAX}
 * for the gauges, and {@code AVG} for the three operation durations, which is derived rather than
 * read from a field (see {@link NativeOperationDurationBuilder}).
 */
public class EventMetricsMeasuresQueryAdapter {

    private final EventMetricsFieldResolver fieldResolver = new EventMetricsFieldResolver();

    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);

    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    private final SimpleSUMBuilder sumBuilder = new SimpleSUMBuilder();

    private final SimpleMaxBuilder maxBuilder = new SimpleMaxBuilder();

    private final NativeOperationDurationBuilder durationBuilder = new NativeOperationDurationBuilder();

    public String adapt(MeasuresQuery query) {
        return json(query).toString();
    }

    private JsonObject json(MeasuresQuery query) {
        return new JsonObject().put("size", 0).put("query", adaptQuery(query)).put("aggs", adaptMetrics(query.metrics()));
    }

    /**
     * Root query: the caller filters plus, when unambiguous, the shared {@code doc-type}.
     */
    JsonObject adaptQuery(Query query) {
        var bool = boolAdapter.adaptForEventMetrics(query);
        return sharedDocType(query.metrics())
            .map(docType -> {
                var filters = bool.getJsonObject("bool").getJsonArray("filter");
                filters.add(docTypeTerm(docType));
                return bool;
            })
            .orElse(bool);
    }

    /**
     * The single {@code doc-type} shared by every metric of the query, or empty when they differ (or
     * when there is no metric at all).
     */
    /**
     * Guards the bucketed query kinds, which cannot express a per-metric {@code doc-type}.
     *
     * <p>In a facets or time-series query the {@code terms} (or {@code date_histogram}) bucket sits
     * <em>above</em> the measures, so a per-metric {@code #__FILTER__} envelope would end up
     * <em>inside</em> the bucket. Two things then break silently: {@code terms.order} would point at
     * {@code METRIC#SUM}, which is no longer a direct child of the bucket (Elasticsearch rejects the
     * path), and {@link AggregationAdapter} only unwraps the envelope for plain measure aggregations
     * — a derived duration, nested one level deeper, would read back as 0 with no error at all.
     *
     * <p>Refusing is the honest option until the repository splits such a query into one Elasticsearch
     * call per {@code doc-type}. The flat measures path is unaffected: there is no bucket, and the
     * response side resolves the envelope by recursion.
     */
    void requireSingleDocType(List<MetricMeasuresQuery> metrics, String queryKind) {
        if (sharedDocType(metrics).isPresent()) {
            return;
        }
        var breakdown = metrics
            .stream()
            .map(metric -> metric.metric() + " (" + fieldResolver.docType(metric.metric()) + ")")
            .toList();
        throw new UnsupportedOperationException(
            "Native event metrics " + queryKind + " cannot mix documents types in a single query, got: " + breakdown
        );
    }

    Optional<String> sharedDocType(List<MetricMeasuresQuery> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return Optional.empty();
        }
        var docTypes = metrics
            .stream()
            .map(metric -> fieldResolver.docType(metric.metric()))
            .distinct()
            .toList();
        return docTypes.size() == 1 ? Optional.of(docTypes.getFirst()) : Optional.empty();
    }

    static JsonObject docTypeTerm(String docType) {
        return new JsonObject().put("term", new JsonObject().put(EventMetricsFieldResolver.DOC_TYPE_FIELD, docType));
    }

    JsonObject adaptMetrics(List<MetricMeasuresQuery> metrics) {
        return adaptMetrics(metrics, sharedDocType(metrics).isPresent());
    }

    /**
     * @param docTypeHoisted whether the root query already carries the {@code doc-type} term. It must
     *     be decided from the <b>whole</b> query, not from the metrics passed here: the facets and
     *     time-series adapters call this once per metric, and a single metric always looks
     *     unambiguous on its own.
     */
    JsonObject adaptMetrics(List<MetricMeasuresQuery> metrics, boolean docTypeHoisted) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            var measureAggs = buildMeasureAggs(metric);
            if (docTypeHoisted) {
                aggs.mergeIn(measureAggs);
            } else {
                var filterName = metric.metric().name() + AggregationAdapter.AGG_NAME_SEPARATOR + AggregationAdapter.FILTER_AGG_SUFFIX;
                var filterAgg = new JsonObject()
                    .put("filter", docTypeTerm(fieldResolver.docType(metric.metric())))
                    .put("aggs", measureAggs);
                aggs.put(filterName, filterAgg);
            }
        }
        return aggs;
    }

    JsonObject buildMeasureAggs(MetricMeasuresQuery metric) {
        if (metric.filters() != null && !metric.filters().isEmpty()) {
            // The `#__FILTER__` slot already carries the doc-type here, so honouring per-metric filters
            // would mean nesting a second filter aggregation. Until that is implemented, refuse: the
            // validator accepts these filters upstream, so dropping them would silently widen the query.
            throw new UnsupportedOperationException(
                "Native event metrics do not support per-metric filters yet, got " + metric.filters() + " on " + metric.metric()
            );
        }
        var aggs = new JsonObject();
        for (var measure : metric.measures()) {
            var aggName = AggregationAdapter.adaptName(metric.metric(), measure);
            aggregate(aggName, metric.metric(), measure).ifPresent(agg ->
                aggs.put(agg.keySet().iterator().next(), agg.values().iterator().next())
            );
        }
        return aggs;
    }

    private Optional<Map<String, JsonObject>> aggregate(String aggName, Metric metric, Measure measure) {
        if (fieldResolver.isAccumulatedDuration(metric)) {
            if (measure != Measure.AVG) {
                throw new UnsupportedOperationException(
                    "Native operation durations are accumulated sums, so only AVG is supported, got: " + measure
                );
            }
            return Optional.of(
                durationBuilder.build(aggName, fieldResolver.fromMetric(metric), fieldResolver.durationSampleCountField(metric))
            );
        }

        var field = fieldResolver.fromMetric(metric);
        return switch (measure) {
            case SUM -> Optional.of(sumBuilder.build(aggName, field));
            case MAX -> Optional.of(maxBuilder.build(aggName, field));
            default -> throw new UnsupportedOperationException(
                "Native event metric " + metric + " supports SUM and MAX only, got: " + measure
            );
        };
    }
}
