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
package io.gravitee.apim.infra.query_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricName;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricName;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Every metric/measure pair the definition serves is accepted by the validator and searched by a query service: the
 * built-ins of the catalog for every API kind, and a number field with each of {@link AdditionalMetricName#MEASURES},
 * routed to the one service that measures such a field whatever the kind (a kind narrows a query through the API
 * filters the use case derives from it, before the services). The kinds the definition serves a number field for are
 * those of the built-ins that service searches, pinned apart. None is refused before the repository. Found
 * by the OBS-82 end-to-end run, where {@code NATIVE_CONNECTIONS_SUMMARY} was served with {@code COUNT} and its
 * service threw on any measures search. The Elasticsearch adapters' own measure limits are pinned in the repository
 * module, and the measures the Native service's metrics declare in {@link NativeApiAnalyticsQueryServiceTest}.
 */
class ServedMetricMeasuresTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");
    private static final TimeRange A_DAY = new TimeRange(Instant.parse("2026-11-03T00:00:00Z"), Instant.parse("2026-11-04T00:00:00Z"));
    private static final MetricName A_NUMBER_FIELD = MetricName.parse("additional-metrics.long_failover_count").orElseThrow();

    /** Every measures search answers nothing: what matters is that each service accepts the search. */
    private final AnalyticsRepository repository = mock(
        AnalyticsRepository.class,
        withSettings().defaultAnswer(invocation ->
            MeasuresResult.class.equals(invocation.getMethod().getReturnType())
                ? new MeasuresResult(List.of())
                : RETURNS_DEFAULTS.answer(invocation)
        )
    );
    private final AnalyticsDefinitionYAMLQueryService definition = new AnalyticsDefinitionYAMLQueryService();
    private final AnalyticsQueryValidator validator = new AnalyticsQueryValidator(definition);
    private final AnalyticsQueryContextProvider provider = new AnalyticsQueryContextProvider(
        List.of(
            new HTTPDataPlaneAnalyticsQueryService(repository),
            new MessageDataPlaneQueryService(repository),
            new NativeApiAnalyticsQueryService(repository),
            new EventMetricsAnalyticsQueryService(repository),
            new AuthzAnalyticsQueryService(repository),
            new AuthzTrafficAnalyticsQueryService(repository)
        )
    );

    /** The kinds the definition serves a number field for are read from the routing, never listed by hand. */
    @Test
    void should_measure_a_number_field_for_the_kinds_whose_requests_the_http_service_reads_edge_included() {
        assertThat(servedKindsOfAdditionalMetrics()).containsExactly(
            ApiSpec.Name.HTTP_PROXY,
            ApiSpec.Name.MESSAGE,
            ApiSpec.Name.LLM,
            ApiSpec.Name.MCP,
            ApiSpec.Name.A2A,
            ApiSpec.Name.EDGE
        );
    }

    @TestFactory
    Stream<DynamicTest> should_search_every_measure_the_definition_serves_for_a_number_field() {
        return AdditionalMetricName.MEASURES.stream().map(measure ->
            DynamicTest.dynamicTest(A_NUMBER_FIELD.name() + "#" + measure, () ->
                assertThatCode(() -> measure(A_NUMBER_FIELD, measure)).doesNotThrowAnyException()
            )
        );
    }

    @TestFactory
    Stream<DynamicTest> should_answer_every_measure_the_definition_serves_for_a_metric() {
        return Arrays.stream(ApiSpec.Name.values()).flatMap(kind ->
            definition
                .getMetrics(kind)
                .stream()
                .flatMap(metric ->
                    metric
                        .measures()
                        .stream()
                        .map(measure ->
                            DynamicTest.dynamicTest(kind + " " + metric.name() + "#" + measure, () ->
                                assertThatCode(() -> measure(metric.name(), measure)).doesNotThrowAnyException()
                            )
                        )
                )
        );
    }

    private void measure(MetricName metric, MetricSpec.Measure measure) {
        var request = new MeasuresRequest(
            A_DAY,
            new ArrayList<>(),
            new ArrayList<>(List.of(new MetricMeasuresRequest(metric, List.of(measure))))
        );
        validator.validateMeasuresRequest(request);
        provider.resolve(request).forEach((service, part) -> service.searchMeasures(EXECUTION_CONTEXT, part));
    }

    private Stream<ApiSpec.Name> servedKindsOfAdditionalMetrics() {
        return Arrays.stream(ApiSpec.Name.values()).filter(kind ->
            definition
                .getMetrics(kind)
                .stream()
                .anyMatch(spec -> provider.measuresAdditionalMetricsLike(spec.name()))
        );
    }
}
