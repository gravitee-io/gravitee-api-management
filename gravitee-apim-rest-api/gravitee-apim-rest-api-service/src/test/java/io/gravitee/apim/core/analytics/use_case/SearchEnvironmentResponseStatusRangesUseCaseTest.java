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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchEnvironmentResponseStatusRangesUseCaseTest {

    public static final String ENV_ID = "environment-id";
    public static final String ORG_ID = "org-id";
    private static final long FROM = 1728981738L;
    private static final long TO = 1729068138L;
    private final ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);

    private FakeAnalyticsQueryService analyticsQueryService;
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private SearchEnvironmentResponseStatusRangesUseCase cut;

    private ArgumentCaptor<AnalyticsQueryParameters> argumentCaptor;

    @BeforeEach
    void setUp() {
        analyticsQueryService = mock(FakeAnalyticsQueryService.class);
        cut = new SearchEnvironmentResponseStatusRangesUseCase(analyticsQueryService, apiQueryService);

        argumentCaptor = ArgumentCaptor.forClass(AnalyticsQueryParameters.class);
    }

    @AfterEach
    void tearDown() {
        analyticsQueryService.reset();
        apiQueryService.reset();
    }

    @Test
    public void should_get_proper_ranges_only_for_v4_api() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").build(),
                ApiFixtures.aProxyApiV2().toBuilder().id("proxy-api-v2-id").build()
            )
        );
        var input = SearchEnvironmentResponseStatusRangesUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchResponseStatusRanges(any(), any()))
            .thenReturn(
                Optional.of(
                    ResponseStatusRanges
                        .builder()
                        .ranges(Map.of("100.0-200.0", 1L, "200.0-300.0", 17L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L))
                        .build()
                )
            );

        var result = cut.execute(input).responseStatusRanges();

        verify(analyticsQueryService).searchResponseStatusRanges(any(), argumentCaptor.capture());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions
                .assertThat(argumentCaptor.getValue())
                .isEqualTo(
                    AnalyticsQueryParameters.builder().from(FROM).to(TO).apiIds(List.of("message-api-v4-id", "proxy-api-v4-id")).build()
                );
            softAssertions
                .assertThat(result)
                .isPresent()
                .isNotNull()
                .get()
                .extracting(ResponseStatusRanges::getRanges)
                .isEqualTo(Map.of("100.0-200.0", 1L, "200.0-300.0", 17L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L));
        });
    }

    @Test
    public void should_get_ranges_only_for_specified_env() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aProxyApiV4().toBuilder().id("proper-env-proxy-api-v4-id").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("other-env-proxy-api-v4-id").environmentId("other-env").build()
            )
        );
        var input = SearchEnvironmentResponseStatusRangesUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchResponseStatusRanges(any(), any()))
            .thenReturn(
                Optional.of(
                    ResponseStatusRanges
                        .builder()
                        .ranges(Map.of("100.0-200.0", 1L, "200.0-300.0", 17L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L))
                        .build()
                )
            );

        var result = cut.execute(input).responseStatusRanges();

        verify(analyticsQueryService).searchResponseStatusRanges(any(), argumentCaptor.capture());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions
                .assertThat(argumentCaptor.getValue())
                .isEqualTo(AnalyticsQueryParameters.builder().from(FROM).to(TO).apiIds(List.of("proper-env-proxy-api-v4-id")).build());
            softAssertions
                .assertThat(result)
                .isPresent()
                .isNotNull()
                .get()
                .extracting(ResponseStatusRanges::getRanges)
                .isEqualTo(Map.of("100.0-200.0", 1L, "200.0-300.0", 17L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L));
        });
    }
}
