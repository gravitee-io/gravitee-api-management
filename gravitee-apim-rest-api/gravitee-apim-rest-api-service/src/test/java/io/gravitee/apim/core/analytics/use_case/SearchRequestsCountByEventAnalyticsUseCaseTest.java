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
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SearchRequestsCountByEventAnalyticsUseCaseTest {

    private static final String ENV_ID = "environment-id";

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT_NOW;

    private static final long FROM_EPOCH = FROM.toEpochMilli();
    private static final long TO_EPOCH = TO.toEpochMilli();

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchRequestsCountByEventAnalyticsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchRequestsCountByEventAnalyticsUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    @Test
    void should_throw_if_no_api_found() {
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new SearchRequestsCountByEventAnalyticsUseCase.Input(MY_API, FROM_EPOCH, TO_EPOCH, Optional.empty())
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
                    new SearchRequestsCountByEventAnalyticsUseCase.Input(MY_API, FROM_EPOCH, TO_EPOCH, Optional.empty())
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
                    new SearchRequestsCountByEventAnalyticsUseCase.Input(MY_API, FROM_EPOCH, TO_EPOCH, Optional.empty())
                )
            )
            .isInstanceOf(TcpProxyNotSupportedException.class)
            .hasMessage("TCP Proxy not supported");
    }

    @Test
    void should_not_find_requests_count() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.requestsCount = null;
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        final SearchRequestsCountByEventAnalyticsUseCase.Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchRequestsCountByEventAnalyticsUseCase.Input(MY_API, FROM_EPOCH, TO_EPOCH, Optional.empty())
        );
        assertThat(result.result()).extracting(RequestsCount::getCountsByEntrypoint, RequestsCount::getTotal).containsExactly(Map.of(), 0L);
    }

    @Test
    void should_get_requests_count_for_a_v4_api() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        analyticsQueryService.requestsCount =
            RequestsCount.builder().total(56L).countsByEntrypoint(Map.of("http-get", 26L, "http-post", 30L)).build();
        final SearchRequestsCountByEventAnalyticsUseCase.Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchRequestsCountByEventAnalyticsUseCase.Input(MY_API, FROM_EPOCH, TO_EPOCH, Optional.empty())
        );
        assertThat(result.result().getTotal()).isEqualTo(56);
        assertThat(result.result().getCountsByEntrypoint()).isEqualTo(Map.of("http-get", 26L, "http-post", 30L));
    }

    @Test
    void should_pass_query_argument_to_count_query() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV4()));
        GraviteeContext.setCurrentEnvironment("environment-id");
        var queryString = "status:200 AND method:GET";
        analyticsQueryService.requestsCount =
            RequestsCount.builder().total(56L).countsByEntrypoint(Map.of("http-get", 26L, "http-post", 30L)).build();
        final SearchRequestsCountByEventAnalyticsUseCase.Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchRequestsCountByEventAnalyticsUseCase.Input(MY_API, FROM_EPOCH, TO_EPOCH, Optional.of(queryString))
        );
        assertThat(result.result().getTotal()).isNotNull();
    }
}
