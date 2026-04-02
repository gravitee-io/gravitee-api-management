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
package io.gravitee.apim.core.analytics.use_case;

import static fixtures.core.model.ApiFixtures.MY_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.model.AnalyticsDateHisto;
import io.gravitee.apim.core.analytics.model.AnalyticsGroupBy;
import io.gravitee.apim.core.analytics.model.AnalyticsStats;
import io.gravitee.apim.core.analytics.model.AnalyticsType;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.Input;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase.Output;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SearchApiAnalyticsUseCase}.
 *
 * <p>Story 1 tests cover the common validation pipeline (API existence, definition version,
 * TCP proxy guard, and multi-tenancy). Each nested class represents one AnalyticsType;
 * data-fetching assertions are added in Stories 2–5.</p>
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApiAnalyticsUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT_NOW;

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchApiAnalyticsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchApiAnalyticsUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    // =========================================================================
    // Common validation — applied to every AnalyticsType
    // We use COUNT as the representative type; validation runs before the switch.
    // =========================================================================

    @Nested
    class CommonValidation {

        @Test
        void should_throw_when_api_does_not_exist() {
            assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), countInput(MY_API, ENV_ID)))
                .isInstanceOf(ApiNotFoundException.class);
        }

        @Test
        void should_throw_when_api_belongs_to_a_different_environment() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4()));
            // aProxyApiV4() has environmentId "environment-id"; pass a different one
            assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), countInput(MY_API, "another-environment")))
                .isInstanceOf(ApiNotFoundException.class);
        }

        @Test
        void should_throw_when_api_is_not_v4() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
            assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), countInput(MY_API, ENV_ID)))
                .isInstanceOf(ApiInvalidDefinitionVersionException.class);
        }

        @Test
        void should_throw_when_api_is_a_tcp_proxy() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4()));
            assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), countInput(MY_API, ENV_ID)))
                .isInstanceOf(TcpProxyNotSupportedException.class);
        }
    }

    // =========================================================================
    // COUNT — Story 2: real data-fetching via searchRequestsCount
    // =========================================================================

    @Nested
    class WhenTypeIsCount {

        @BeforeEach
        void setUpApi() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4()));
        }

        @Test
        void should_return_total_request_count() {
            analyticsQueryService.requestsCount = RequestsCount.builder().total(42L).build();

            var result = cut.execute(GraviteeContext.getExecutionContext(), countInput(MY_API, ENV_ID));

            assertThat(result).isInstanceOf(Output.Count.class);
            assertThat(((Output.Count) result).count()).isEqualTo(42L);
        }

        @Test
        void should_return_zero_when_total_is_null() {
            // RequestsCount.total can be null when Elasticsearch has no matching docs
            analyticsQueryService.requestsCount = RequestsCount.builder().total(null).build();

            var result = cut.execute(GraviteeContext.getExecutionContext(), countInput(MY_API, ENV_ID));

            assertThat(((Output.Count) result).count()).isZero();
        }

        @Test
        void should_return_zero_when_analytics_service_returns_empty() {
            analyticsQueryService.requestsCount = null; // FakeAnalyticsQueryService returns Optional.empty()

            var result = cut.execute(GraviteeContext.getExecutionContext(), countInput(MY_API, ENV_ID));

            assertThat(((Output.Count) result).count()).isZero();
        }

        @Test
        void should_return_result_with_time_range() {
            // Note: FakeAnalyticsQueryService does not capture call arguments, so we verify
            // that the result is returned correctly when a time range is supplied in the Input.
            analyticsQueryService.requestsCount = RequestsCount.builder().total(7L).build();

            var result = cut.execute(GraviteeContext.getExecutionContext(), countInput(MY_API, ENV_ID));

            assertThat(((Output.Count) result).count()).isEqualTo(7L);
        }
    }

    // =========================================================================
    // STATS — Story 3: real data-fetching via searchStats
    // =========================================================================

    @Nested
    class WhenTypeIsStats {

        @BeforeEach
        void setUpApi() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4()));
        }

        @Test
        void should_return_stats_output_for_a_valid_v4_proxy_api() {
            analyticsQueryService.analyticsStats = new AnalyticsStats(100L, 5.0, 200.0, 42.3, 4230.0);

            var result = cut.execute(GraviteeContext.getExecutionContext(), statsInput(MY_API, ENV_ID, "gateway-response-time-ms"));

            assertThat(result).isInstanceOf(Output.Stats.class);
        }

        @Test
        void should_return_all_stats_fields() {
            analyticsQueryService.analyticsStats = new AnalyticsStats(100L, 5.0, 200.0, 42.3, 4230.0);

            var result = (Output.Stats) cut.execute(
                GraviteeContext.getExecutionContext(),
                statsInput(MY_API, ENV_ID, "gateway-response-time-ms")
            );

            assertThat(result.count()).isEqualTo(100L);
            assertThat(result.min()).isEqualTo(5.0);
            assertThat(result.max()).isEqualTo(200.0);
            assertThat(result.avg()).isEqualTo(42.3);
            assertThat(result.sum()).isEqualTo(4230.0);
        }

        @Test
        void should_return_zero_count_and_null_values_when_service_returns_empty() {
            analyticsQueryService.analyticsStats = null;

            var result = (Output.Stats) cut.execute(
                GraviteeContext.getExecutionContext(),
                statsInput(MY_API, ENV_ID, "gateway-response-time-ms")
            );

            assertThat(result.count()).isZero();
            assertThat(result.min()).isNull();
            assertThat(result.max()).isNull();
            assertThat(result.avg()).isNull();
            assertThat(result.sum()).isNull();
        }
    }

    // =========================================================================
    // GROUP_BY — Story 4: real data-fetching via searchGroupBy
    // =========================================================================

    @Nested
    class WhenTypeIsGroupBy {

        @BeforeEach
        void setUpApi() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4()));
        }

        @Test
        void should_return_group_by_output_for_a_valid_v4_proxy_api() {
            analyticsQueryService.analyticsGroupBy =
                new AnalyticsGroupBy(Map.of("200", 1000L, "404", 50L), Map.of("200", Map.of("name", "200")));

            var result = cut.execute(GraviteeContext.getExecutionContext(), groupByInput(MY_API, ENV_ID, "status"));

            assertThat(result).isInstanceOf(Output.GroupBy.class);
        }

        @Test
        void should_return_values_and_metadata() {
            analyticsQueryService.analyticsGroupBy =
                new AnalyticsGroupBy(Map.of("200", 1000L, "404", 50L), Map.of("200", Map.of("name", "200"), "404", Map.of("name", "404")));

            var result = (Output.GroupBy) cut.execute(GraviteeContext.getExecutionContext(), groupByInput(MY_API, ENV_ID, "status"));

            assertThat(result.values()).containsEntry("200", 1000L).containsEntry("404", 50L);
            assertThat(result.metadata()).containsKey("200").containsKey("404");
        }

        @Test
        void should_return_empty_maps_when_service_returns_empty() {
            analyticsQueryService.analyticsGroupBy = null;

            var result = (Output.GroupBy) cut.execute(GraviteeContext.getExecutionContext(), groupByInput(MY_API, ENV_ID, "status"));

            assertThat(result.values()).isEmpty();
            assertThat(result.metadata()).isEmpty();
        }
    }

    // =========================================================================
    // DATE_HISTO — Story 5: real data-fetching via searchDateHisto
    // =========================================================================

    @Nested
    class WhenTypeIsDateHisto {

        @BeforeEach
        void setUpApi() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4()));
        }

        @Test
        void should_return_date_histo_output_for_a_valid_v4_proxy_api() {
            analyticsQueryService.analyticsDateHisto =
                new AnalyticsDateHisto(List.of(1697000000000L), List.of(new AnalyticsDateHisto.Bucket("200", List.of(5L), Map.of("name", "200"))));

            var result = cut.execute(GraviteeContext.getExecutionContext(), dateHistoInput(MY_API, ENV_ID, "status", 3_600_000L));

            assertThat(result).isInstanceOf(Output.DateHisto.class);
            assertThat(((Output.DateHisto) result).timestamps()).containsExactly(1697000000000L);
            assertThat(((Output.DateHisto) result).buckets()).hasSize(1);
        }

        @Test
        void should_return_timestamps_and_buckets() {
            analyticsQueryService.analyticsDateHisto =
                new AnalyticsDateHisto(
                    List.of(1697000000000L, 1697003600000L),
                    List.of(
                        new AnalyticsDateHisto.Bucket("200", List.of(120L, 130L), Map.of("name", "200")),
                        new AnalyticsDateHisto.Bucket("500", List.of(2L, 5L), Map.of("name", "500"))
                    )
                );

            var result = (Output.DateHisto) cut.execute(
                GraviteeContext.getExecutionContext(),
                dateHistoInput(MY_API, ENV_ID, "status", 3_600_000L)
            );

            assertThat(result.timestamps()).containsExactly(1697000000000L, 1697003600000L);
            assertThat(result.buckets()).hasSize(2);
            assertThat(result.buckets().get(0).field()).isEqualTo("200");
            assertThat(result.buckets().get(0).buckets()).containsExactly(120L, 130L);
            assertThat(result.buckets().get(1).field()).isEqualTo("500");
        }

        @Test
        void should_return_empty_lists_when_service_returns_empty() {
            analyticsQueryService.analyticsDateHisto = null;

            var result = (Output.DateHisto) cut.execute(
                GraviteeContext.getExecutionContext(),
                dateHistoInput(MY_API, ENV_ID, "status", 3_600_000L)
            );

            assertThat(result.timestamps()).isEmpty();
            assertThat(result.buckets()).isEmpty();
        }
    }

    // =========================================================================
    // Input factory helpers
    // =========================================================================

    private static Input countInput(String apiId, String environmentId) {
        return new Input(
            apiId,
            environmentId,
            AnalyticsType.COUNT,
            Optional.of(FROM),
            Optional.of(TO),
            Optional.empty(),
            Optional.empty(),
            10
        );
    }

    private static Input statsInput(String apiId, String environmentId, String field) {
        return new Input(
            apiId,
            environmentId,
            AnalyticsType.STATS,
            Optional.of(FROM),
            Optional.of(TO),
            Optional.of(field),
            Optional.empty(),
            10
        );
    }

    private static Input groupByInput(String apiId, String environmentId, String field) {
        return new Input(
            apiId,
            environmentId,
            AnalyticsType.GROUP_BY,
            Optional.of(FROM),
            Optional.of(TO),
            Optional.of(field),
            Optional.empty(),
            10
        );
    }

    private static Input dateHistoInput(String apiId, String environmentId, String field, long intervalMs) {
        return new Input(
            apiId,
            environmentId,
            AnalyticsType.DATE_HISTO,
            Optional.of(FROM),
            Optional.of(TO),
            Optional.of(field),
            Optional.of(intervalMs),
            10
        );
    }
}
