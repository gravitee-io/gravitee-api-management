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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.infra.query_service.analytics_engine.AuthzAnalyticsQueryService;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline.MetricConditions;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.RecordType;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

// Real filter catalog: RECORD_TYPE and the metric-level conditions go through the real validation.
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsRequestPipelineConditionsNotAppliedTest {

    private static final FilterCondition DECISIONS_ONLY = new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("AUTHZ_DECISION"));
    private static final FilterCondition HTTP_STATUS = new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("500"));

    private final AnalyticsRequestPipeline pipeline = new AnalyticsRequestPipeline(
        new ObservabilityFilterValidator(new SpiFilterRegistry()),
        new AccessibleApiScopeDomainService()
    );

    @Test
    void should_report_a_record_type_that_does_not_select_the_metric() {
        var notApplied = pipeline.conditionsNotApplied(
            List.of(DECISIONS_ONLY),
            List.of(metric("HTTP_REQUESTS"), metric("AUTHZ_DECISIONS"))
        );

        assertThat(notApplied).isEqualTo(Map.of("HTTP_REQUESTS", List.of("RECORD_TYPE")));
    }

    @Test
    void should_read_every_value_of_an_in_condition() {
        var both = new FilterCondition("RECORD_TYPE", FilterOperator.IN, List.of("REQUEST", "AUTHZ_DECISION"));

        assertThat(pipeline.conditionsNotApplied(List.of(both), List.of(metric("HTTP_REQUESTS"), metric("AUTHZ_PERMITS")))).isEmpty();
    }

    @Test
    void should_count_message_and_kafka_metrics_as_requests() {
        var requestsOnly = new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("REQUEST"));

        assertThat(
            pipeline.conditionsNotApplied(
                List.of(requestsOnly),
                List.of(metric("MESSAGES"), metric("NATIVE_MESSAGES_PRODUCED_DOWNSTREAM"), metric("AUTHZ_EVAL_DURATION"))
            )
        ).isEqualTo(Map.of("AUTHZ_EVAL_DURATION", List.of("RECORD_TYPE")));
    }

    @Test
    void should_report_every_metric_level_condition_after_the_record_type() {
        var notApplied = pipeline.conditionsNotApplied(List.of(DECISIONS_ONLY), List.of(metric("HTTP_REQUESTS", HTTP_STATUS)));

        assertThat(notApplied).isEqualTo(Map.of("HTTP_REQUESTS", List.of("RECORD_TYPE", "HTTP_STATUS")));
    }

    @Test
    void should_merge_the_lists_of_a_metric_queried_twice() {
        var plan = new FilterCondition("PLAN", FilterOperator.EQ, List.of("plan-1"));

        var notApplied = pipeline.conditionsNotApplied(
            List.of(),
            List.of(metric("HTTP_REQUESTS", HTTP_STATUS), metric("HTTP_REQUESTS", plan, HTTP_STATUS))
        );

        assertThat(notApplied).isEqualTo(Map.of("HTTP_REQUESTS", List.of("HTTP_STATUS", "PLAN")));
    }

    @Test
    void should_refuse_a_metric_level_condition_the_catalog_refuses() {
        var unknown = new FilterCondition("NOT_A_FILTER", FilterOperator.EQ, List.of("x"));

        assertThatThrownBy(() -> pipeline.conditionsNotApplied(List.of(), List.of(metric("HTTP_REQUESTS", unknown)))).isInstanceOf(
            UnsupportedObservabilityFilterException.class
        );
    }

    @Test
    void should_report_nothing_without_a_record_type_or_metric_level_condition() {
        assertThat(pipeline.conditionsNotApplied(List.of(HTTP_STATUS), List.of(metric("HTTP_REQUESTS")))).isEmpty();
    }

    /** The decision metrics are the ones the engine reads from decision records: its authz query service's. */
    @Test
    void should_class_as_decisions_exactly_the_metrics_the_engine_reads_from_decision_records() {
        var decisionMetrics = new AuthzAnalyticsQueryService(null).metrics().stream().map(Enum::name).toList();

        assertThat(AnalyticsRequestPipeline.AUTHZ_DECISION_METRICS).containsExactlyInAnyOrderElementsOf(decisionMetrics);
        assertThat(decisionMetrics).allSatisfy(name ->
            assertThat(AnalyticsRequestPipeline.recordTypeOf(name)).isEqualTo(RecordType.AUTHZ_DECISION)
        );
    }

    private static MetricConditions metric(String name, FilterCondition... conditions) {
        return new MetricConditions(name, List.of(conditions));
    }
}
