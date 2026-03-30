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
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsGroupByUseCase.Input;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsGroupByUseCase.Output;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsField;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByOrder;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApiAnalyticsGroupByUseCaseTest {

    private static final class TrackingFakeAnalyticsQueryService extends FakeAnalyticsQueryService {

        int searchApiAnalyticsGroupByCalls;

        @Override
        public Optional<ApiAnalyticsGroupByAggregate> searchApiAnalyticsGroupBy(
            ExecutionContext executionContext,
            ApiAnalyticsGroupByQuery query
        ) {
            searchApiAnalyticsGroupByCalls++;
            return super.searchApiAnalyticsGroupBy(executionContext, query);
        }
    }

    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT_NOW;

    private TrackingFakeAnalyticsQueryService analyticsQueryService;
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchApiAnalyticsGroupByUseCase cut;

    @BeforeEach
    void setUp() {
        analyticsQueryService = new TrackingFakeAnalyticsQueryService();
        cut = new SearchApiAnalyticsGroupByUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    @Test
    void should_throw_if_no_api_does_not_belong_to_current_environment() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4WithAnalyticsEnabled()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(
                        MY_API,
                        "another-environment",
                        Optional.of(FROM),
                        Optional.of(TO),
                        Optional.of(ApiAnalyticsField.STATUS),
                        Optional.of(10),
                        Optional.of(ApiAnalyticsGroupByOrder.COUNT_DESC)
                    )
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_no_api_found() {
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(
                        MY_API,
                        ENV_ID,
                        Optional.of(FROM),
                        Optional.of(TO),
                        Optional.of(ApiAnalyticsField.STATUS),
                        Optional.of(10),
                        Optional.of(ApiAnalyticsGroupByOrder.COUNT_DESC)
                    )
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_definition_not_v4() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(
                        MY_API,
                        ENV_ID,
                        Optional.of(FROM),
                        Optional.of(TO),
                        Optional.of(ApiAnalyticsField.STATUS),
                        Optional.of(10),
                        Optional.of(ApiAnalyticsGroupByOrder.COUNT_DESC)
                    )
                )
            )
            .isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_throw_if_api_is_tcp() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(
                        MY_API,
                        ENV_ID,
                        Optional.of(FROM),
                        Optional.of(TO),
                        Optional.of(ApiAnalyticsField.STATUS),
                        Optional.of(10),
                        Optional.of(ApiAnalyticsGroupByOrder.COUNT_DESC)
                    )
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Analytics are not supported for TCP Proxy APIs");
    }

    @Test
    void should_return_empty_when_analytics_disabled_without_querying_repository() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        final Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(
                MY_API,
                ENV_ID,
                Optional.of(FROM),
                Optional.of(TO),
                Optional.of(ApiAnalyticsField.STATUS),
                Optional.of(10),
                Optional.of(ApiAnalyticsGroupByOrder.COUNT_DESC)
            )
        );
        assertThat(result.aggregate()).isEmpty();
        assertThat(analyticsQueryService.searchApiAnalyticsGroupByCalls).isZero();
    }

    @Test
    void should_not_find_group_by_when_repository_empty() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4WithAnalyticsEnabled()));
        analyticsQueryService.apiAnalyticsGroupBy = null;
        final Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(
                MY_API,
                ENV_ID,
                Optional.of(FROM),
                Optional.of(TO),
                Optional.of(ApiAnalyticsField.STATUS),
                Optional.empty(),
                Optional.empty()
            )
        );
        assertThat(result.aggregate()).isEmpty();
        assertThat(analyticsQueryService.searchApiAnalyticsGroupByCalls).isEqualTo(1);
    }

    @Test
    void should_get_group_by_for_a_v4_api_with_analytics_enabled() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4WithAnalyticsEnabled()));
        analyticsQueryService.apiAnalyticsGroupBy = ApiAnalyticsGroupByAggregate.builder().values(Map.of("200", 5L, "500", 1L)).build();
        final Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(
                MY_API,
                ENV_ID,
                Optional.of(FROM),
                Optional.of(TO),
                Optional.of(ApiAnalyticsField.STATUS),
                Optional.of(10),
                Optional.of(ApiAnalyticsGroupByOrder.COUNT_DESC)
            )
        );
        assertThat(result.aggregate()).hasValueSatisfying(agg -> assertThat(agg.getValues()).containsEntry("200", 5L));
    }
}
