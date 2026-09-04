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
package io.gravitee.apim.infra.performance_target;

import static io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status.BREACH;
import static io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status.NOT_EVALUABLE;
import static io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status.PASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PerformanceTargetFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerformanceTargetEvaluatorImplTest {

    private static final String ENVIRONMENT_ID = PerformanceTargetFixtures.ENVIRONMENT_ID;
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String A2A_API_ID = "a2a-api";
    private static final String LLM_API_ID = "llm-api";
    private static final Instant NOW = Instant.parse("2021-06-01T10:00:00Z");
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private static final PerformanceTarget.Rule A2A_LATENCY = PerformanceTargetFixtures.aLatencyRule()
        .toBuilder()
        .apiTypes(Set.of(ApiType.A2A_PROXY))
        .build();
    private static final PerformanceTarget.Rule ERROR_RATE = PerformanceTarget.Rule.builder()
        .metric(MetricSpec.Name.HTTP_ERROR_RATE)
        .measure(MetricSpec.Measure.PERCENTAGE)
        .operator(PerformanceTarget.Operator.LTE)
        .threshold(5)
        .build();
    private static final PerformanceTarget.Rule LLM_COST = PerformanceTarget.Rule.builder()
        .metric(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST)
        .measure(MetricSpec.Measure.AVG)
        .operator(PerformanceTarget.Operator.LTE)
        .threshold(0.01)
        .apiTypes(Set.of(ApiType.LLM_PROXY))
        .build();

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    RecordingAnalyticsEngineQueryService analytics = new RecordingAnalyticsEngineQueryService();

    PerformanceTargetEvaluatorImpl evaluator = new PerformanceTargetEvaluatorImpl(
        apiCrudService,
        environmentCrudService,
        new AnalyticsQueryContextProvider(List.of(analytics))
    );

    @BeforeEach
    void setUp() {
        environmentCrudService.initWith(List.of(Environment.builder().id(ENVIRONMENT_ID).organizationId(ORGANIZATION_ID).build()));
        apiCrudService.initWith(
            List.of(
                ApiFixtures.anA2AProxyApiV4().toBuilder().id(A2A_API_ID).build(),
                ApiFixtures.aLLMProxyApiV4().toBuilder().id(LLM_API_ID).build()
            )
        );
    }

    @Test
    void should_query_each_filter_set_once_with_the_request_count_and_the_rules_metrics() {
        evaluator.evaluate(anAgentTarget(A2A_LATENCY, ERROR_RATE, LLM_COST), NOW);

        assertThat(analytics.contexts).containsOnly(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID));
        assertThat(analytics.requests)
            .hasSize(3)
            .allSatisfy(request -> assertThat(request.timeRange()).isEqualTo(new TimeRange(NOW.minus(WINDOW), NOW)))
            .satisfiesExactlyInAnyOrder(
                request -> {
                    assertThat(request.filters()).containsExactly(apiIn(A2A_API_ID));
                    assertThat(request.metrics()).containsExactlyInAnyOrder(
                        new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)),
                        new MetricMeasuresRequest(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, List.of(MetricSpec.Measure.P95))
                    );
                },
                request -> {
                    assertThat(request.filters()).containsExactly(apiIn(A2A_API_ID, LLM_API_ID));
                    assertThat(request.metrics()).containsExactlyInAnyOrder(
                        new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)),
                        new MetricMeasuresRequest(MetricSpec.Name.HTTP_ERROR_RATE, List.of(MetricSpec.Measure.PERCENTAGE))
                    );
                },
                request -> {
                    assertThat(request.filters()).containsExactly(apiIn(LLM_API_ID));
                    assertThat(request.metrics()).containsExactlyInAnyOrder(
                        new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)),
                        new MetricMeasuresRequest(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST, List.of(MetricSpec.Measure.AVG))
                    );
                }
            );
    }

    @Test
    void should_evaluate_each_rule_against_the_documents_of_its_own_scope() {
        analytics.given(
            Set.of(A2A_API_ID),
            requests(63),
            measure(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 4810)
        );
        analytics.given(
            Set.of(A2A_API_ID, LLM_API_ID),
            requests(91),
            measure(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 1.5)
        );
        analytics.given(Set.of(LLM_API_ID), requests(0), measure(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST, MetricSpec.Measure.AVG, 0));

        var evaluation = evaluator.evaluate(anAgentTarget(A2A_LATENCY, ERROR_RATE, LLM_COST), NOW);

        assertThat(evaluation).isEqualTo(
            PerformanceTargetEvaluation.builder()
                .targetId("target-id")
                .environmentId(ENVIRONMENT_ID)
                .reference("agent-42")
                .status(BREACH)
                .rules(
                    List.of(
                        new PerformanceTargetEvaluation.RuleResult(
                            MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME,
                            MetricSpec.Measure.P95,
                            PerformanceTarget.Operator.LTE,
                            2000,
                            4810.0,
                            new PerformanceTargetEvaluation.Deviation(2810, 1.405),
                            63,
                            BREACH
                        ),
                        new PerformanceTargetEvaluation.RuleResult(
                            MetricSpec.Name.HTTP_ERROR_RATE,
                            MetricSpec.Measure.PERCENTAGE,
                            PerformanceTarget.Operator.LTE,
                            5,
                            1.5,
                            new PerformanceTargetEvaluation.Deviation(-3.5, -0.7),
                            91,
                            PASS
                        ),
                        new PerformanceTargetEvaluation.RuleResult(
                            MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST,
                            MetricSpec.Measure.AVG,
                            PerformanceTarget.Operator.LTE,
                            0.01,
                            null,
                            null,
                            0,
                            NOT_EVALUABLE
                        )
                    )
                )
                .windowFrom(NOW.minus(WINDOW))
                .windowTo(NOW)
                .coveredApiIds(List.of(A2A_API_ID, LLM_API_ID))
                .evaluatedAt(NOW)
                .latest(false)
                .build()
        );
    }

    @Test
    void should_not_be_evaluable_without_traffic() {
        analytics.given(
            Set.of(A2A_API_ID, LLM_API_ID),
            requests(0),
            measure(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 0)
        );

        var evaluation = evaluator.evaluate(anAgentTarget(ERROR_RATE), NOW);

        assertThat(evaluation.status()).isEqualTo(NOT_EVALUABLE);
        assertThat(evaluation.rules())
            .singleElement()
            .satisfies(rule -> {
                assertThat(rule.status()).isEqualTo(NOT_EVALUABLE);
                assertThat(rule.observed()).isNull();
                assertThat(rule.deviation()).isNull();
                assertThat(rule.sampleCount()).isZero();
            });
    }

    @Test
    void should_not_be_evaluable_below_the_minimum_sample_size() {
        analytics.given(
            Set.of(A2A_API_ID, LLM_API_ID),
            requests(19),
            measure(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 50)
        );

        var evaluation = evaluator.evaluate(anAgentTarget(ERROR_RATE), NOW);

        assertThat(evaluation.status()).isEqualTo(NOT_EVALUABLE);
        assertThat(evaluation.rules()).singleElement().extracting(PerformanceTargetEvaluation.RuleResult::sampleCount).isEqualTo(19L);
    }

    @Test
    void should_keep_evaluating_the_other_rules_when_a_metric_is_missing_from_the_response() {
        analytics.given(Set.of(A2A_API_ID), requests(63));
        analytics.given(
            Set.of(A2A_API_ID, LLM_API_ID),
            requests(91),
            measure(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 1.5)
        );

        var evaluation = evaluator.evaluate(anAgentTarget(A2A_LATENCY, ERROR_RATE), NOW);

        assertThat(evaluation.status()).isEqualTo(NOT_EVALUABLE);
        assertThat(evaluation.rules())
            .extracting(PerformanceTargetEvaluation.RuleResult::status, PerformanceTargetEvaluation.RuleResult::sampleCount)
            .containsExactly(tuple(NOT_EVALUABLE, 63L), tuple(PASS, 91L));
    }

    @Test
    void should_not_query_for_a_rule_whose_api_types_are_absent_from_the_subject() {
        analytics.given(
            Set.of(A2A_API_ID),
            requests(63),
            measure(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 1500)
        );
        var target = anAgentTarget(A2A_LATENCY, LLM_COST)
            .toBuilder()
            .subject(new PerformanceTarget.Subject(List.of(A2A_API_ID), "agent-42"))
            .build();

        var evaluation = evaluator.evaluate(target, NOW);

        assertThat(analytics.requests).singleElement().extracting(MeasuresRequest::filters).isEqualTo(List.of(apiIn(A2A_API_ID)));
        assertThat(evaluation.status()).isEqualTo(NOT_EVALUABLE);
        assertThat(evaluation.rules())
            .extracting(PerformanceTargetEvaluation.RuleResult::status, PerformanceTargetEvaluation.RuleResult::sampleCount)
            .containsExactly(tuple(PASS, 63L), tuple(NOT_EVALUABLE, 0L));
    }

    @Test
    void should_cover_only_the_subject_apis_that_still_exist() {
        var target = anAgentTarget(ERROR_RATE)
            .toBuilder()
            .subject(new PerformanceTarget.Subject(List.of(A2A_API_ID, "deleted-api", LLM_API_ID), "agent-42"))
            .build();

        var evaluation = evaluator.evaluate(target, NOW);

        assertThat(evaluation.coveredApiIds()).containsExactly(A2A_API_ID, LLM_API_ID);
        assertThat(analytics.requests)
            .singleElement()
            .extracting(MeasuresRequest::filters)
            .isEqualTo(List.of(apiIn(A2A_API_ID, LLM_API_ID)));
    }

    @Test
    void should_forward_the_rule_filters_and_query_them_separately() {
        var searchTool = new Filter(FilterSpec.Name.MCP_PROXY_TOOL, FilterOperator.EQ, "search");
        var searchToolErrorRate = ERROR_RATE.toBuilder().filters(List.of(searchTool)).build();

        evaluator.evaluate(anAgentTarget(ERROR_RATE, searchToolErrorRate), NOW);

        assertThat(analytics.requests)
            .extracting(MeasuresRequest::filters)
            .containsExactlyInAnyOrder(List.of(apiIn(A2A_API_ID, LLM_API_ID)), List.of(apiIn(A2A_API_ID, LLM_API_ID), searchTool));
    }

    @Test
    void should_request_every_measure_of_a_metric_shared_by_several_rules_at_once() {
        var averageLatency = PerformanceTargetFixtures.aLatencyRule().toBuilder().measure(MetricSpec.Measure.AVG).threshold(800).build();
        analytics.given(
            Set.of(A2A_API_ID, LLM_API_ID),
            requests(63),
            measure(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 1500),
            measure(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.AVG, 900)
        );

        var evaluation = evaluator.evaluate(anAgentTarget(PerformanceTargetFixtures.aLatencyRule(), averageLatency), NOW);

        assertThat(analytics.requests)
            .singleElement()
            .extracting(MeasuresRequest::metrics)
            .asInstanceOf(list(MetricMeasuresRequest.class))
            .containsExactlyInAnyOrder(
                new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)),
                new MetricMeasuresRequest(
                    MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME,
                    List.of(MetricSpec.Measure.P95, MetricSpec.Measure.AVG)
                )
            );
        assertThat(evaluation.rules())
            .extracting(PerformanceTargetEvaluation.RuleResult::observed, PerformanceTargetEvaluation.RuleResult::status)
            .containsExactly(tuple(1500.0, PASS), tuple(900.0, BREACH));
    }

    @Test
    void should_count_samples_with_the_metric_of_the_subject_api_family() {
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi().toBuilder().id("kafka-api").build()));
        var throughput = PerformanceTarget.Rule.builder()
            .metric(MetricSpec.Name.NATIVE_OPERATION_BROKER_DURATION)
            .measure(MetricSpec.Measure.AVG)
            .operator(PerformanceTarget.Operator.LTE)
            .threshold(50)
            .build();
        var target = anAgentTarget(throughput)
            .toBuilder()
            .subject(new PerformanceTarget.Subject(List.of("kafka-api"), "kafka-api"))
            .build();

        evaluator.evaluate(target, NOW);

        assertThat(analytics.requests)
            .singleElement()
            .extracting(MeasuresRequest::metrics)
            .asInstanceOf(list(MetricMeasuresRequest.class))
            .containsExactlyInAnyOrder(
                new MetricMeasuresRequest(MetricSpec.Name.NATIVE_OPERATIONS_RECEIVED, List.of(MetricSpec.Measure.SUM)),
                new MetricMeasuresRequest(MetricSpec.Name.NATIVE_OPERATION_BROKER_DURATION, List.of(MetricSpec.Measure.AVG))
            );
    }

    private static PerformanceTarget anAgentTarget(PerformanceTarget.Rule... rules) {
        return PerformanceTargetFixtures.aTarget()
            .toBuilder()
            .subject(new PerformanceTarget.Subject(List.of(A2A_API_ID, LLM_API_ID), "agent-42"))
            .window(WINDOW)
            .rules(List.of(rules))
            .build();
    }

    private static Filter apiIn(String... apiIds) {
        return new Filter(FilterSpec.Name.API, FilterOperator.IN, List.of(apiIds));
    }

    private static MetricMeasuresResponse requests(long count) {
        return measure(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Measure.COUNT, count);
    }

    private static MetricMeasuresResponse measure(MetricSpec.Name metric, MetricSpec.Measure measure, Number value) {
        return new MetricMeasuresResponse(metric, null, List.of(new Measure(measure, value)));
    }

    /**
     * Answers a measures request with the metrics configured for the API ids it filters on, merging several
     * configured responses for the same metric, and records what it was asked so tests can assert the query shape.
     */
    static class RecordingAnalyticsEngineQueryService implements AnalyticsEngineQueryService {

        final List<ExecutionContext> contexts = new ArrayList<>();
        final List<MeasuresRequest> requests = new ArrayList<>();
        private final Map<Set<String>, List<MetricMeasuresResponse>> responsesByApiIds = new HashMap<>();

        void given(Set<String> apiIds, MetricMeasuresResponse... metrics) {
            for (var metric : metrics) {
                responsesByApiIds.computeIfAbsent(apiIds, ids -> new ArrayList<>()).add(metric);
            }
        }

        @Override
        public Set<MetricSpec.Name> metrics() {
            return Set.of(MetricSpec.Name.values());
        }

        @Override
        @SuppressWarnings("unchecked")
        public MeasuresResponse searchMeasures(ExecutionContext context, MeasuresRequest request) {
            contexts.add(context);
            requests.add(request);
            var apiIds = request
                .filters()
                .stream()
                .filter(filter -> filter.name() == FilterSpec.Name.API)
                .findFirst()
                .map(filter -> Set.copyOf((Collection<String>) filter.value()))
                .orElse(Set.of());
            var byMetric = new HashMap<MetricSpec.Name, List<Measure>>();
            for (var response : responsesByApiIds.getOrDefault(apiIds, List.of())) {
                byMetric.computeIfAbsent(response.name(), name -> new ArrayList<>()).addAll(response.measures());
            }
            return new MeasuresResponse(
                new TreeSet<>(byMetric.keySet())
                    .stream()
                    .map(name -> new MetricMeasuresResponse(name, null, byMetric.get(name)))
                    .toList()
            );
        }

        @Override
        public FacetsResponse searchFacets(ExecutionContext context, FacetsRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TimeSeriesResponse searchTimeSeries(ExecutionContext context, TimeSeriesRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
