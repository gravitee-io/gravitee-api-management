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
package io.gravitee.gamma.rest.core.observability.analytics.port.service_provider;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsNumberRange;
import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.model.SeriesRanking;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Core-side port onto the analytics computation engine. Uses only Gamma-native types so the core
 * layer stays independent of the APIM analytics-engine model. The infra adapter handles all
 * translation to the platform compute use cases.
 *
 * <p>Responses are returned as {@link JsonNode} so the rich, nested APIM response hierarchy flows
 * through without requiring duplicate records in the Gamma core. Jackson serializes the node tree
 * directly to the HTTP response body.
 *
 * <p>Each query carries the conditions Gamma did not hand to the engine. A response lists them in each
 * metric's {@code ignoredFilters}, after the ones the engine skipped.
 *
 * @author GraviteeSource Team
 */
public interface ObservabilityAnalyticsDataPort {
    /** The APIs whose data of this kind the caller reads: none, never every API, when they read none. */
    List<AccessibleApi> loadAccessibleApis(String organizationId, String environmentId, DataKind kind);

    /**
     * The APIs of an AI workspace the caller reads: their calls through that workspace only may be counted. None when
     * the caller does not read it, or when it is no workspace of the environment.
     */
    List<AccessibleApi> loadWorkspaceApis(String organizationId, String environmentId, String workspaceId);

    /**
     * Whether the engine counts, for every one of these metrics, only the calls an {@code API_PRODUCT} condition
     * matches. Some search paths ignore it: a workspace may then open none of its APIs. False for an empty list or an
     * unknown metric.
     */
    boolean narrowsByApiProduct(Collection<String> metricNames);

    JsonNode computeMeasures(MeasuresQuery query);

    /**
     * A measures request split into groups, one engine query each: the metrics of a group share their
     * own conditions, already part of the group's scope. A group whose scope is empty answers zero-valued
     * entries without querying anything.
     *
     * <p>When the metrics carry ids, the response has one entry per metric, in request order, echoing its
     * id and holding only its own measures. Otherwise it has one entry per metric name, measures merged:
     * without ids, metrics sharing a name always share a group. A zero-valued entry lists only the
     * conditions Gamma did not apply, the engine having been asked nothing.
     *
     * @param groups  The scope of each group's query.
     * @param metrics The request's metrics, in request order.
     */
    record MeasuresQuery(
        String organizationId,
        String environmentId,
        List<AnalyticsRequestPipeline.PreparedScope> groups,
        List<Metric> metrics
    ) {
        /**
         * @param group                Index in {@link #groups()} of the query computing this metric.
         * @param conditionsNotApplied Conditions of the request or of the metric that its group's query
         *                             does not apply, to report on its entry.
         */
        public record Metric(AnalyticsMetricQuery query, int group, List<String> conditionsNotApplied) {}

        public boolean identified() {
            return metrics.stream().anyMatch(metric -> metric.query().id() != null);
        }

        public List<AnalyticsMetricQuery> metricsOf(int group) {
            return metrics
                .stream()
                .filter(metric -> metric.group() == group)
                .map(Metric::query)
                .toList();
        }
    }

    JsonNode computeFacets(FacetsQuery query);

    record FacetsQuery(
        String organizationId,
        String environmentId,
        AnalyticsRequestPipeline.PreparedScope scope,
        List<String> facets,
        Integer limit,
        List<AnalyticsFacetMetricQuery> metrics,
        List<AnalyticsNumberRange> ranges,
        Map<String, List<String>> conditionsNotApplied
    ) {}

    JsonNode computeTimeSeries(TimeSeriesQuery query);

    /**
     * @param ranking the only series to plot, or {@code null} to plot every series the engine returns. The
     *     conditions it takes to fetch them are applied on top of the scope and never reported as ignored.
     */
    record TimeSeriesQuery(
        String organizationId,
        String environmentId,
        AnalyticsRequestPipeline.PreparedScope scope,
        Long interval,
        List<String> facets,
        List<AnalyticsFacetMetricQuery> metrics,
        List<AnalyticsNumberRange> ranges,
        Map<String, List<String>> conditionsNotApplied,
        SeriesRanking ranking
    ) {
        public TimeSeriesQuery withRanking(SeriesRanking ranking) {
            return new TimeSeriesQuery(
                organizationId,
                environmentId,
                scope,
                interval,
                facets,
                metrics,
                ranges,
                conditionsNotApplied,
                ranking
            );
        }
    }

    /**
     * Refuses a query the analytics engine would refuse, without reading any data: an unknown metric,
     * measure, facet or sort, and the engine's own rules. The use cases call it whatever the caller can
     * read, so a malformed query gets the same 400 from a caller with no readable API as from an admin.
     */
    void validate(MeasuresQuery query);

    void validate(FacetsQuery query);

    void validate(TimeSeriesQuery query);

    JsonNode emptyFacetsResponse();
    JsonNode emptyTimeSeriesResponse();
}
