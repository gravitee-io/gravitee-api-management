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
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchEnvironmentRequestResponseTimeUseCaseTest {

    public static final String ENV_ID = "environment-id";
    public static final String ORG_ID = "org-id";
    private static final long FROM = 1728981738L;
    private static final long TO = 1729068138L;
    private final ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);

    private FakeAnalyticsQueryService analyticsQueryService;
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private SearchEnvironmentRequestResponseTimeUseCase cut;

    private ArgumentCaptor<AnalyticsQueryParameters> argumentCaptor;

    @BeforeEach
    void setUp() {
        analyticsQueryService = mock(FakeAnalyticsQueryService.class);
        cut = new SearchEnvironmentRequestResponseTimeUseCase(apiQueryService, analyticsQueryService);

        argumentCaptor = ArgumentCaptor.forClass(AnalyticsQueryParameters.class);
    }

    @AfterEach
    void tearDown() {
        analyticsQueryService.reset();
        apiQueryService.reset();
    }

    @Test
    public void should_get_request_response_time_only_for_v4_api() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").build(),
                ApiFixtures.aProxyApiV2().toBuilder().id("proxy-api-v2-id").build()
            )
        );
        var input = SearchEnvironmentRequestResponseTimeUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchRequestResponseTime(any(), any()))
            .thenReturn(
                RequestResponseTime
                    .builder()
                    .requestsPerSecond(3.7d)
                    .requestsTotal(25600L)
                    .responseMinTime(32.5d)
                    .responseMaxTime(1220.87d)
                    .responseAvgTime(159.2d)
                    .build()
            );

        var result = cut.execute(input).requestResponseTime();

        verify(analyticsQueryService).searchRequestResponseTime(any(), argumentCaptor.capture());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions
                .assertThat(argumentCaptor.getValue())
                .isEqualTo(
                    AnalyticsQueryParameters.builder().from(FROM).to(TO).apiIds(List.of("message-api-v4-id", "proxy-api-v4-id")).build()
                );
            softAssertions
                .assertThat(result)
                .isNotNull()
                .get()
                .isEqualTo(
                    RequestResponseTime
                        .builder()
                        .requestsPerSecond(3.7d)
                        .requestsTotal(25600L)
                        .responseMinTime(32.5d)
                        .responseMaxTime(1220.87d)
                        .responseAvgTime(159.2d)
                        .build()
                );
        });
    }

    @Test
    public void should_get_request_response_time_only_for_specified_env() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aProxyApiV4().toBuilder().id("proper-env-proxy-api-v4-id").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("other-env-proxy-api-v4-id").environmentId("other-env").build()
            )
        );
        var input = SearchEnvironmentRequestResponseTimeUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchRequestResponseTime(any(), any()))
            .thenReturn(
                RequestResponseTime
                    .builder()
                    .requestsPerSecond(3.7d)
                    .requestsTotal(25600L)
                    .responseMinTime(32.5d)
                    .responseMaxTime(1220.87d)
                    .responseAvgTime(159.2d)
                    .build()
            );

        var result = cut.execute(input).requestResponseTime();

        verify(analyticsQueryService).searchRequestResponseTime(any(), argumentCaptor.capture());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions
                .assertThat(argumentCaptor.getValue())
                .isEqualTo(AnalyticsQueryParameters.builder().from(FROM).to(TO).apiIds(List.of("proper-env-proxy-api-v4-id")).build());
            softAssertions
                .assertThat(result)
                .isPresent()
                .isNotNull()
                .get()
                .isEqualTo(
                    RequestResponseTime
                        .builder()
                        .requestsPerSecond(3.7d)
                        .requestsTotal(25600L)
                        .responseMinTime(32.5d)
                        .responseMaxTime(1220.87d)
                        .responseAvgTime(159.2d)
                        .build()
                );
        });
    }
}
