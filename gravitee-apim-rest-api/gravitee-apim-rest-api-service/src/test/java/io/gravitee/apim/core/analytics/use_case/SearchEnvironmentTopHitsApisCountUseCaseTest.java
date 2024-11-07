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

import static assertions.CoreAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchEnvironmentTopHitsApisCountUseCaseTest {

    public static final String ENV_ID = "environment-id";
    public static final String ORG_ID = "org-id";
    private static final long FROM = 1728981738L;
    private static final long TO = 1729068138L;
    private final ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);

    private FakeAnalyticsQueryService analyticsQueryService;
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private SearchEnvironmentTopHitsApisCountUseCase cut;

    @BeforeEach
    void setUp() {
        analyticsQueryService = mock(FakeAnalyticsQueryService.class);
        cut = new SearchEnvironmentTopHitsApisCountUseCase(analyticsQueryService, apiQueryService);
    }

    @AfterEach
    void tearDown() {
        analyticsQueryService.reset();
        apiQueryService.reset();
    }

    @Test
    void should_get_environment_top_hits_count_only_for_v4_apis() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").name("Message Api v4").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").name("Proxy Api v4").build(),
                ApiFixtures.aProxyApiV2().toBuilder().id("proxy-api-v2-id").name("Proxy Api v2").build()
            )
        );
        var input = SearchEnvironmentTopHitsApisCountUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchTopHitsApis(any(), any()))
            .thenReturn(
                Optional.of(
                    TopHitsApis
                        .builder()
                        .data(
                            List.of(
                                TopHitsApis.TopHitApi.builder().id("message-api-v4-id").count(17L).build(),
                                TopHitsApis.TopHitApi.builder().id("proxy-api-v4-id").count(3L).build()
                            )
                        )
                        .build()
                )
            );

        var result = cut.execute(input).topHitsApis();

        assertThat(result)
            .isNotNull()
            .isPresent()
            .hasValueSatisfying(topHitsApis ->
                assertThat(topHitsApis.getData())
                    .containsExactly(
                        TopHitsApis.TopHitApi.builder().id("message-api-v4-id").name("Message Api v4").count(17L).build(),
                        TopHitsApis.TopHitApi.builder().id("proxy-api-v4-id").name("Proxy Api v4").count(3L).build()
                    )
            );
    }

    @Test
    void should_get_environment_top_hits_count_only_for_specific_environment() {
        var otherEnvId = "other_env";
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").name("Message Api v4").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").environmentId(otherEnvId).name("Proxy Api v4").build()
            )
        );
        var input = SearchEnvironmentTopHitsApisCountUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchTopHitsApis(any(), any()))
            .thenReturn(
                Optional.of(
                    TopHitsApis.builder().data(List.of(TopHitsApis.TopHitApi.builder().id("message-api-v4-id").count(17L).build())).build()
                )
            );

        var result = cut.execute(input).topHitsApis();

        assertThat(result)
            .isNotNull()
            .isPresent()
            .hasValueSatisfying(topHitsApis ->
                assertThat(topHitsApis.getData())
                    .containsExactly(TopHitsApis.TopHitApi.builder().id("message-api-v4-id").name("Message Api v4").count(17L).build())
            );
    }

    @Test
    void should_get_top_hits_sorted_by_count_number_in_descending_order() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").name("Message Api v4").build(),
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id-2").name("Message Api v4 2").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").name("Proxy Api v4").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id-2").name("Proxy Api v4 2").build()
            )
        );
        var input = SearchEnvironmentTopHitsApisCountUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchTopHitsApis(any(), any()))
            .thenReturn(
                Optional.of(
                    TopHitsApis
                        .builder()
                        .data(
                            List.of(
                                TopHitsApis.TopHitApi.builder().id("message-api-v4-id").count(5L).build(),
                                TopHitsApis.TopHitApi.builder().id("message-api-v4-id-2").count(3L).build(),
                                TopHitsApis.TopHitApi.builder().id("proxy-api-v4-id").count(17L).build(),
                                TopHitsApis.TopHitApi.builder().id("proxy-api-v4-id-2").count(2L).build()
                            )
                        )
                        .build()
                )
            );

        var result = cut.execute(input).topHitsApis();

        assertThat(result)
            .isNotNull()
            .isPresent()
            .hasValueSatisfying(topHitsApis ->
                assertThat(topHitsApis.getData()).extracting(TopHitsApis.TopHitApi::count).containsExactly(17L, 5L, 3L, 2L)
            );
    }
}
