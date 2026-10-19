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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.repository.analytics.engine.api.query.AnalyticsSearchPath;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.FilterOutcome;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Each query service says whether a condition narrows the search of a metric on the path it takes, as the repository
 * answers for that path: a workspace grant relies on it to count only the calls made through the workspace.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FilterNarrowingQueryServiceTest {

    private final AnalyticsRepository repository = mock(AnalyticsRepository.class);

    /** The API product condition, as the Elasticsearch repository treats it per path. */
    private static final Map<AnalyticsSearchPath, FilterOutcome> API_PRODUCT_OUTCOME = Map.of(
        AnalyticsSearchPath.HTTP,
        FilterOutcome.APPLIED,
        AnalyticsSearchPath.MESSAGE,
        FilterOutcome.APPLIED,
        AnalyticsSearchPath.EDGE,
        FilterOutcome.IGNORED,
        AnalyticsSearchPath.NATIVE,
        FilterOutcome.IGNORED,
        AnalyticsSearchPath.EVENT_METRICS,
        FilterOutcome.IGNORED,
        AnalyticsSearchPath.AUTHZ,
        FilterOutcome.IGNORED,
        AnalyticsSearchPath.AUTHZ_TRAFFIC,
        FilterOutcome.EMPTIES
    );

    @BeforeEach
    void stub_the_repository_answer() {
        when(repository.filterOutcome(any(), any())).thenAnswer(invocation ->
            invocation.getArgument(1) == Filter.Name.API_PRODUCT
                ? API_PRODUCT_OUTCOME.get(invocation.<AnalyticsSearchPath>getArgument(0))
                : FilterOutcome.APPLIED
        );
    }

    @Test
    void should_narrow_the_request_metrics_by_the_api_product_on_the_http_path() {
        var service = new HTTPDataPlaneAnalyticsQueryService(repository);

        assertThat(service.narrowsBy(MetricSpec.Name.HTTP_REQUESTS, FilterSpec.Name.API_PRODUCT)).isTrue();
        assertThat(service.narrowsBy(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST, FilterSpec.Name.API_PRODUCT)).isTrue();
    }

    /** Edge facets search the Edge path, which ignores the API product. */
    @Test
    void should_not_narrow_the_edge_metrics_by_a_condition_the_edge_path_ignores() {
        var service = new HTTPDataPlaneAnalyticsQueryService(repository);

        assertThat(service.narrowsBy(MetricSpec.Name.EDGE_TOKENS_IN, FilterSpec.Name.API_PRODUCT)).isFalse();
        assertThat(service.narrowsBy(MetricSpec.Name.EDGE_TOKENS_IN, FilterSpec.Name.API)).isTrue();
    }

    @Test
    void should_narrow_the_message_metrics_by_the_api_product() {
        assertThat(new MessageDataPlaneQueryService(repository).narrowsBy(MetricSpec.Name.MESSAGES, FilterSpec.Name.API_PRODUCT)).isTrue();
    }

    @Test
    void should_not_narrow_the_metrics_of_a_path_ignoring_the_api_product() {
        assertThat(
            new NativeApiAnalyticsQueryService(repository).narrowsBy(
                MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY,
                FilterSpec.Name.API_PRODUCT
            )
        ).isFalse();
        assertThat(
            new EventMetricsAnalyticsQueryService(repository).narrowsBy(
                MetricSpec.Name.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM,
                FilterSpec.Name.API_PRODUCT
            )
        ).isFalse();
        assertThat(
            new AuthzAnalyticsQueryService(repository).narrowsBy(MetricSpec.Name.AUTHZ_DECISIONS, FilterSpec.Name.API_PRODUCT)
        ).isFalse();
    }

    /** A condition that empties the result counts nothing, which is no narrowing either. */
    @Test
    void should_not_take_a_condition_emptying_the_result_for_a_narrowing() {
        assertThat(
            new AuthzTrafficAnalyticsQueryService(repository).narrowsBy(MetricSpec.Name.AUTHZ_OPERATIONS, FilterSpec.Name.API_PRODUCT)
        ).isFalse();
    }

    @Test
    void should_ask_the_service_of_the_metric() {
        var provider = new AnalyticsQueryContextProvider(
            List.of(new HTTPDataPlaneAnalyticsQueryService(repository), new AuthzAnalyticsQueryService(repository))
        );

        assertThat(provider.narrowsBy(MetricSpec.Name.HTTP_REQUESTS, FilterSpec.Name.API_PRODUCT)).isTrue();
        assertThat(provider.narrowsBy(MetricSpec.Name.AUTHZ_DECISIONS, FilterSpec.Name.API_PRODUCT)).isFalse();
    }
}
