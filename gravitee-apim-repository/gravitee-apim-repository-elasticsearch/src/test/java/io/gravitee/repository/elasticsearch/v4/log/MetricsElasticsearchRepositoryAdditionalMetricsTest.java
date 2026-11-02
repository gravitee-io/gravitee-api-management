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
package io.gravitee.repository.elasticsearch.v4.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.ValueType;
import io.gravitee.repository.analytics.engine.api.query.Filter.Operator;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery.Filter;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery.Filter.AdditionalMetricCondition;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The logs search on additional metric fields, over the "additional-metrics-logs-api-001" block of
 * {@code freemarker/v4-metrics.ftl}: four HTTP requests, three of them carrying the fields, and one native connection
 * carrying two of them next to its own native keys. The HTTP requests are PUTs answered 201 or 502, values the unscoped
 * searches of {@link MetricsElasticsearchRepositoryTest} never count.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricsElasticsearchRepositoryAdditionalMetricsTest extends AbstractElasticsearchRepositoryTest {

    private static final String API = "additional-metrics-logs-api-001";
    private static final AdditionalMetricField TIER = new AdditionalMetricField(ValueType.KEYWORD, "keyword_fraud-check_tier");
    private static final AdditionalMetricField FAILOVERS = new AdditionalMetricField(ValueType.LONG, "long_failover_count");
    private static final AdditionalMetricField RETRIES = new AdditionalMetricField(ValueType.INT, "int_retry_count");
    private static final AdditionalMetricField SCORE = new AdditionalMetricField(ValueType.DOUBLE, "double_fraud-check_score");
    private static final AdditionalMetricField FLAGGED = new AdditionalMetricField(ValueType.BOOL, "bool_fraud-check_flagged");

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private MetricsElasticsearchRepository metricsV4Repository;

    @Test
    void should_return_only_the_rows_holding_a_keyword_value() {
        assertThat(requestIds(Filter.builder(), condition(TIER, Operator.EQ, "high"))).containsExactlyInAnyOrder(
            "am-log-001",
            "am-log-002",
            "am-log-native-001"
        );
    }

    @Test
    void should_never_return_a_row_without_the_field() {
        assertThat(requestIds(Filter.builder(), condition(TIER, Operator.IN, "high", "low"))).containsExactlyInAnyOrder(
            "am-log-001",
            "am-log-002",
            "am-log-003",
            "am-log-native-001"
        );
    }

    @Test
    void should_return_the_rows_from_a_lower_bound() {
        assertThat(requestIds(Filter.builder(), condition(FAILOVERS, Operator.GTE, "1"))).containsExactlyInAnyOrder(
            "am-log-002",
            "am-log-003",
            "am-log-native-001"
        );
    }

    @Test
    void should_return_the_rows_between_both_bounds() {
        assertThat(
            requestIds(Filter.builder(), condition(FAILOVERS, Operator.GTE, "1"), condition(FAILOVERS, Operator.LTE, "2"))
        ).containsExactlyInAnyOrder("am-log-002", "am-log-native-001");
    }

    @Test
    void should_return_the_rows_equal_to_a_whole_number() {
        assertThat(requestIds(Filter.builder(), condition(FAILOVERS, Operator.EQ, "5"))).containsExactly("am-log-003");
        assertThat(requestIds(Filter.builder(), condition(RETRIES, Operator.EQ, "3"))).containsExactly("am-log-002");
    }

    @Test
    void should_read_a_number_written_with_spaces() {
        assertThat(requestIds(Filter.builder(), condition(FAILOVERS, Operator.GTE, " 1"))).containsExactlyInAnyOrder(
            "am-log-002",
            "am-log-003",
            "am-log-native-001"
        );
        assertThat(requestIds(Filter.builder(), condition(RETRIES, Operator.EQ, "3 "))).containsExactly("am-log-002");
    }

    @Test
    void should_compare_an_int_field_as_a_number() {
        assertThat(requestIds(Filter.builder(), condition(RETRIES, Operator.GTE, "2"))).containsExactly("am-log-002");
    }

    @Test
    void should_compare_a_double_field_with_its_decimals() {
        assertThat(requestIds(Filter.builder(), condition(SCORE, Operator.LTE, "0.4"))).containsExactlyInAnyOrder(
            "am-log-002",
            "am-log-003"
        );
    }

    @Test
    void should_return_the_rows_holding_a_boolean_value() {
        assertThat(requestIds(Filter.builder(), condition(FLAGGED, Operator.EQ, "false"))).containsExactly("am-log-002");
    }

    @Test
    void should_narrow_the_built_in_filters() {
        assertThat(requestIds(Filter.builder().statuses(Set.of(201)), condition(TIER, Operator.EQ, "high"))).containsExactly("am-log-001");
    }

    @Test
    void should_narrow_native_rows_like_any_other() {
        assertThat(
            requestIds(Filter.builder().nativeConnectionStatuses(Set.of("CONNECTED")), condition(FAILOVERS, Operator.GTE, "1"))
        ).containsExactly("am-log-native-001");
        assertThat(
            requestIds(Filter.builder().nativeConnectionStatuses(Set.of("CONNECTED")), condition(FAILOVERS, Operator.GTE, "2"))
        ).isEmpty();
    }

    @Test
    void should_answer_an_empty_page_for_a_field_no_document_carries() {
        var neverWritten = new AdditionalMetricField(ValueType.KEYWORD, "keyword_never_written");

        var response = search(Filter.builder(), condition(neverWritten, Operator.EQ, "high"));

        assertThat(response.total()).isZero();
        assertThat(response.data()).isEmpty();
    }

    private static AdditionalMetricCondition condition(AdditionalMetricField field, Operator operator, String... values) {
        return new AdditionalMetricCondition(field, operator, List.of(values));
    }

    private List<String> requestIds(Filter.FilterBuilder filter, AdditionalMetricCondition... conditions) {
        return search(filter, conditions).data().stream().map(Metrics::getRequestId).toList();
    }

    private LogResponse<Metrics> search(Filter.FilterBuilder filter, AdditionalMetricCondition... conditions) {
        return metricsV4Repository.searchMetrics(
            queryContext,
            MetricsQuery.builder()
                .page(1)
                .size(20)
                .filter(filter.apiIds(Set.of(API)).additionalMetrics(List.of(conditions)).build())
                .build(),
            List.of(DefinitionVersion.V4)
        );
    }
}
