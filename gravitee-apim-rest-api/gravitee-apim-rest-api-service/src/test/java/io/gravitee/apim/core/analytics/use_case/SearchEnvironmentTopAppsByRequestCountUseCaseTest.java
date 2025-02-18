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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ApplicationQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.analytics.TopHitsApps;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchEnvironmentTopAppsByRequestCountUseCaseTest {

    public static final String ENV_ID = "environment-id";
    public static final String ORG_ID = "org-id";
    private static final long FROM = 1728981738L;
    private static final long TO = 1729068138L;
    private final ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);

    private FakeAnalyticsQueryService analyticsQueryService;
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ApplicationQueryServiceInMemory applicationQueryService = new ApplicationQueryServiceInMemory();
    private SearchEnvironmentTopAppsByRequestCountUseCase cut;

    @BeforeEach
    void setUp() {
        analyticsQueryService = mock(FakeAnalyticsQueryService.class);
        cut = new SearchEnvironmentTopAppsByRequestCountUseCase(analyticsQueryService, apiQueryService, applicationQueryService);
    }

    @AfterEach
    void tearDown() {
        analyticsQueryService.reset();
        Stream.of(apiQueryService, applicationQueryService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_get_environment_top_apps_by_request_count_for_v4_apis() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").name("Message Api v4").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").name("Proxy Api v4").build(),
                ApiFixtures.aProxyApiV2().toBuilder().id("proxy-api-v2-id").name("Proxy Api v2").build()
            )
        );
        applicationQueryService.initWith(
            List.of(
                BaseApplicationEntity.builder().id("app-id-1").name("Application 1").environmentId(ENV_ID).build(),
                BaseApplicationEntity.builder().id("app-id-2").name("Application 2").environmentId(ENV_ID).build()
            )
        );
        var input = SearchEnvironmentTopAppsByRequestCountUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchTopHitsApps(any(), any()))
            .thenReturn(
                Optional.of(
                    TopHitsApps
                        .builder()
                        .data(
                            List.of(
                                TopHitsApps.TopHitApp.builder().id("app-id-1").count(5L).build(),
                                TopHitsApps.TopHitApp.builder().id("app-id-2").count(4L).build()
                            )
                        )
                        .build()
                )
            );

        var result = cut.execute(input).topHitsApps();

        assertThat(result)
            .extracting(TopHitsApps::getData)
            .isEqualTo(
                List.of(
                    TopHitsApps.TopHitApp.builder().id("app-id-1").name("Application 1").count(5L).build(),
                    TopHitsApps.TopHitApp.builder().id("app-id-2").name("Application 2").count(4L).build()
                )
            );
    }

    @Test
    void should_get_environment_top_apps_by_request_count_for_specific_environment() {
        var otherEnvId = "other_env";
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").name("Message Api v4").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").environmentId(otherEnvId).name("Proxy Api v4").build()
            )
        );
        applicationQueryService.initWith(
            List.of(
                BaseApplicationEntity.builder().id("app-id-1").name("Application 1").environmentId(ENV_ID).build(),
                BaseApplicationEntity.builder().id("app-id-2").name("Application 2").environmentId(otherEnvId).build()
            )
        );
        var input = SearchEnvironmentTopAppsByRequestCountUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchTopHitsApps(any(), any()))
            .thenReturn(
                Optional.of(TopHitsApps.builder().data(List.of(TopHitsApps.TopHitApp.builder().id("app-id-1").count(17L).build())).build())
            );

        var result = cut.execute(input).topHitsApps();

        assertThat(result)
            .extracting(TopHitsApps::getData)
            .isEqualTo(List.of(TopHitsApps.TopHitApp.builder().id("app-id-1").name("Application 1").count(17L).build()));
    }

    @Test
    void should_get_top_apps_sorted_by_count_in_descending_order() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").name("Message Api v4").build(),
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id-2").name("Message Api v4 2").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").name("Proxy Api v4").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id-2").name("Proxy Api v4 2").build()
            )
        );
        applicationQueryService.initWith(
            List.of(
                BaseApplicationEntity.builder().id("app-id-1").name("Application 1").environmentId(ENV_ID).build(),
                BaseApplicationEntity.builder().id("app-id-2").name("Application 2").environmentId(ENV_ID).build(),
                BaseApplicationEntity.builder().id("app-id-3").name("Application 3").environmentId(ENV_ID).build()
            )
        );

        var input = SearchEnvironmentTopAppsByRequestCountUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchTopHitsApps(any(), any()))
            .thenReturn(
                Optional.of(
                    TopHitsApps
                        .builder()
                        .data(
                            List.of(
                                TopHitsApps.TopHitApp.builder().id("app-id-1").count(5L).build(),
                                TopHitsApps.TopHitApp.builder().id("app-id-2").count(3L).build(),
                                TopHitsApps.TopHitApp.builder().id("app-id-3").count(17L).build()
                            )
                        )
                        .build()
                )
            );

        var result = cut.execute(input).topHitsApps();

        assertThat(result.getData()).extracting(TopHitsApps.TopHitApp::count).containsExactly(17L, 5L, 3L);
    }

    @Test
    void should_get_empty_top_apps_list_if_no_records_found() {
        var input = SearchEnvironmentTopAppsByRequestCountUseCase.Input
            .builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .build();

        when(analyticsQueryService.searchTopHitsApps(any(), any())).thenReturn(Optional.empty());

        var result = cut.execute(input).topHitsApps();

        assertThat(result).extracting(TopHitsApps::getData).isEqualTo(List.of());
    }
}
