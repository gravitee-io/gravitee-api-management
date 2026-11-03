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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.ValueType;
import io.gravitee.repository.analytics.engine.api.query.AnalyticsSearchPath;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.FilterName;
import io.gravitee.repository.analytics.engine.api.query.FilterOutcome;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.analytics.engine.api.query.TrafficScope;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pins {@link FilterAdapter#outcome} to the queries the adapters actually build: for every search path and
 * every filter name, the query built with a condition on that name either carries it (APPLIED), is the
 * same as without it (IGNORED), or matches nothing (EMPTIES). The answer is what analytics responses
 * report as ignored filters, so a list edited without the other is caught here. Additional metric fields are
 * checked alongside the built-in names, one per value type a condition can take.
 */
class FilterAdapterOutcomeTest {

    private static final JsonObject MATCH_NONE = JsonObject.of("match_none", JsonObject.of());
    private static final TimeRange TIME_RANGE = new TimeRange(Instant.ofEpochMilli(1756104349879L), Instant.ofEpochMilli(1756190749879L));

    // TODO(GMA-932): allow-listed for messages but MessageFieldResolver cannot resolve them, so the query
    // fails instead of being filtered. Must only shrink: the test fails once one of them resolves.
    private static final Set<Filter.Name> KNOWN_UNRESOLVABLE_ON_MESSAGE = Set.of(
        Filter.Name.MESSAGE_SIZE,
        Filter.Name.MESSAGE_COUNT,
        Filter.Name.MESSAGE_ERROR_COUNT
    );

    /** The adapt calls each path makes, with the field resolver its query adapter uses. */
    private static final Map<AnalyticsSearchPath, List<Function<Query, JsonArray>>> PHASES = Map.of(
        AnalyticsSearchPath.HTTP,
        List.of(new FilterAdapter(new HTTPFieldResolver())::adaptForHTTP),
        AnalyticsSearchPath.EDGE,
        List.of(new FilterAdapter(new HTTPFieldResolver())::adaptForEdge),
        // The join: connections through the HTTP facets adapter, then the messages themselves.
        AnalyticsSearchPath.MESSAGE,
        List.of(
            new FilterAdapter(new HTTPFieldResolver())::adaptForMessageConnexion,
            new FilterAdapter(new MessageFieldResolver())::adaptForMessage
        ),
        AnalyticsSearchPath.NATIVE,
        List.of(new FilterAdapter(new NativeApiFieldResolver())::adaptForNative),
        AnalyticsSearchPath.EVENT_METRICS,
        List.of(new FilterAdapter(new EventMetricsFieldResolver())::adaptForEventMetrics),
        AnalyticsSearchPath.AUTHZ,
        List.of(new FilterAdapter(new AuthzFieldResolver())::adaptForAuthz),
        AnalyticsSearchPath.AUTHZ_TRAFFIC,
        List.of(new FilterAdapter(new AuthzTrafficFieldResolver())::adaptForAuthzTraffic)
    );

    private static final AdditionalMetricField KEYWORD_FIELD = new AdditionalMetricField(ValueType.KEYWORD, "keyword_fraud-check_tier");
    private static final AdditionalMetricField LONG_FIELD = new AdditionalMetricField(ValueType.LONG, "long_failover_count");
    private static final AdditionalMetricField INT_FIELD = new AdditionalMetricField(ValueType.INT, "int_retry_attempts");
    private static final AdditionalMetricField DOUBLE_FIELD = new AdditionalMetricField(ValueType.DOUBLE, "double_llm-proxy_sent-cost");
    private static final AdditionalMetricField BOOL_FIELD = new AdditionalMetricField(ValueType.BOOL, "bool_cache_hit");
    private static final List<AdditionalMetricField> ADDITIONAL_METRIC_FIELDS = List.of(
        KEYWORD_FIELD,
        LONG_FIELD,
        INT_FIELD,
        DOUBLE_FIELD,
        BOOL_FIELD
    );

    static Stream<Arguments> everyPathAndName() {
        var names = Stream.concat(Arrays.stream(Filter.Name.values()), ADDITIONAL_METRIC_FIELDS.stream()).toList();
        return Arrays.stream(AnalyticsSearchPath.values()).flatMap(path -> names.stream().map(name -> Arguments.of(path, name)));
    }

    /** The rule OBS-70 §8 states for an additional metric condition, path by path. */
    static Stream<Arguments> additionalMetricOutcomes() {
        return Stream.of(
            Arguments.of(AnalyticsSearchPath.HTTP, FilterOutcome.APPLIED),
            Arguments.of(AnalyticsSearchPath.EDGE, FilterOutcome.APPLIED),
            Arguments.of(AnalyticsSearchPath.MESSAGE, FilterOutcome.IGNORED),
            Arguments.of(AnalyticsSearchPath.NATIVE, FilterOutcome.IGNORED),
            Arguments.of(AnalyticsSearchPath.EVENT_METRICS, FilterOutcome.IGNORED),
            Arguments.of(AnalyticsSearchPath.AUTHZ, FilterOutcome.IGNORED),
            Arguments.of(AnalyticsSearchPath.AUTHZ_TRAFFIC, FilterOutcome.EMPTIES)
        );
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("additionalMetricOutcomes")
    void should_apply_an_additional_metric_on_the_request_documents_only_and_empty_the_authz_traffic_path(
        AnalyticsSearchPath path,
        FilterOutcome outcome
    ) {
        assertThat(ADDITIONAL_METRIC_FIELDS).allSatisfy(field -> assertThat(FilterAdapter.outcome(path, field)).isEqualTo(outcome));
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("everyPathAndName")
    void should_answer_what_the_query_does_with_the_condition(AnalyticsSearchPath path, FilterName name) {
        if (path == AnalyticsSearchPath.MESSAGE && KNOWN_UNRESOLVABLE_ON_MESSAGE.contains(name)) {
            assertThatThrownBy(() -> observed(path, name)).isInstanceOf(UnsupportedOperationException.class);
            return;
        }

        assertThat(FilterAdapter.outcome(path, name)).isEqualTo(observed(path, name));
    }

    @Test
    void should_cover_every_path() {
        assertThat(PHASES).containsOnlyKeys(AnalyticsSearchPath.values());
    }

    /**
     * The direct message read skips the join and only runs when every condition is one the message
     * documents carry, so each of those must count as applied on the message path.
     */
    @Test
    void should_apply_on_the_message_path_every_condition_the_direct_read_carries() {
        assertThat(FilterAdapter.ENRICHED_MESSAGE_FILTER_NAMES).allSatisfy(name ->
            assertThat(FilterAdapter.outcome(AnalyticsSearchPath.MESSAGE, name)).isEqualTo(FilterOutcome.APPLIED)
        );
    }

    private static FilterOutcome observed(AnalyticsSearchPath path, FilterName name) {
        var applied = false;
        for (var phase : PHASES.get(path)) {
            var without = phase.apply(query(List.of()));
            var with = phase.apply(query(List.of(conditionOn(name))));
            if (with.contains(MATCH_NONE)) {
                return FilterOutcome.EMPTIES;
            }
            applied |= !with.equals(without);
        }
        return applied ? FilterOutcome.APPLIED : FilterOutcome.IGNORED;
    }

    private static Query query(List<Filter> filters) {
        return new MeasuresQuery(TIME_RANGE, filters, List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT))));
    }

    /** A value each name's own translation accepts; only whether the condition lands matters here. */
    private static Filter conditionOn(FilterName name) {
        return switch (name) {
            case Filter.Name builtIn -> conditionOn(builtIn);
            case AdditionalMetricField field -> switch (field.valueType()) {
                case LONG, INT, DOUBLE -> new Filter(field, Filter.Operator.GTE, 3);
                case BOOL -> new Filter(field, Filter.Operator.EQ, "true");
                case KEYWORD, STRING, JSON -> new Filter(field, Filter.Operator.EQ, "high");
            };
        };
    }

    private static Filter conditionOn(Filter.Name name) {
        return switch (name) {
            case HTTP_STATUS_CODE_GROUP -> new Filter(name, Filter.Operator.EQ, "2XX");
            case HTTP_METHOD -> new Filter(name, Filter.Operator.EQ, "GET");
            case TRAFFIC_SCOPE -> new Filter(name, Filter.Operator.IN, new TrafficScope(Set.of("api"), Set.of("mcp")));
            default -> new Filter(name, Filter.Operator.EQ, "value");
        };
    }
}
