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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.analytics.TopFailedApis;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchEnvironmentTopFailedApisUseCaseTest {

    public static final String ENV_ID = "environment-id";
    public static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-id";
    private static final long FROM = 1728981738L;
    private static final long TO = 1729068138L;
    private final ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);

    private FakeAnalyticsQueryService analyticsQueryService;
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService = new ApiAuthorizationDomainServiceInMemory();
    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);
    private SearchEnvironmentTopFailedApisUseCase cut;

    @BeforeEach
    void setUp() {
        analyticsQueryService = mock(FakeAnalyticsQueryService.class);
        cut = new SearchEnvironmentTopFailedApisUseCase(
            analyticsQueryService,
            new EnvironmentAnalyticsApisDomainService(apiQueryService, apiAuthorizationDomainService, permissionDomainService)
        );
        givenUserReadsThePlatform(true);
    }

    @AfterEach
    void tearDown() {
        analyticsQueryService.reset();
        apiQueryService.reset();
        apiAuthorizationDomainService.reset();
    }

    @Test
    void should_get_environment_top_failed_apis() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").name("Message Api v4").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").name("Proxy Api v4").build(),
                ApiFixtures.aProxyApiV2().toBuilder().id("proxy-api-v2-id").name("Proxy Api v2").build()
            )
        );
        var input = SearchEnvironmentTopFailedApisUseCase.Input.builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .userId(USER_ID)
            .build();

        when(analyticsQueryService.searchTopFailedApis(any(), any())).thenReturn(
            Optional.of(
                TopFailedApis.builder()
                    .data(
                        List.of(
                            TopFailedApis.TopFailedApi.builder().id("message-api-v4-id").failedCalls(7).failedCallsRatio(0.3).build(),
                            TopFailedApis.TopFailedApi.builder().id("proxy-api-v4-id").failedCalls(12).failedCallsRatio(0.2).build(),
                            TopFailedApis.TopFailedApi.builder().id("proxy-api-v2-id").failedCalls(3).failedCallsRatio(0.1).build()
                        )
                    )
                    .build()
            )
        );

        var result = cut.execute(input).topFailedApis();

        assertThat(result)
            .extracting(TopFailedApis::getData)
            .isEqualTo(
                List.of(
                    TopFailedApis.TopFailedApi.builder()
                        .id("proxy-api-v4-id")
                        .name("Proxy Api v4")
                        .definitionVersion(DefinitionVersion.V4)
                        .failedCalls(12L)
                        .failedCallsRatio(0.2)
                        .build(),
                    TopFailedApis.TopFailedApi.builder()
                        .id("message-api-v4-id")
                        .name("Message Api v4")
                        .definitionVersion(DefinitionVersion.V4)
                        .failedCalls(7L)
                        .failedCallsRatio(0.3)
                        .build(),
                    TopFailedApis.TopFailedApi.builder()
                        .id("proxy-api-v2-id")
                        .name("Proxy Api v2")
                        .definitionVersion(DefinitionVersion.V2)
                        .failedCalls(3L)
                        .failedCallsRatio(0.1)
                        .build()
                )
            );
    }

    @Test
    void should_get_environment_top_failed_for_specific_environment() {
        var otherEnvId = "other_env";
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").name("Message Api v4").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").environmentId(otherEnvId).name("Proxy Api v4").build()
            )
        );
        var input = SearchEnvironmentTopFailedApisUseCase.Input.builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .userId(USER_ID)
            .build();

        when(analyticsQueryService.searchTopFailedApis(any(), any())).thenReturn(
            Optional.of(
                TopFailedApis.builder()
                    .data(
                        List.of(TopFailedApis.TopFailedApi.builder().id("message-api-v4-id").failedCalls(7).failedCallsRatio(0.3).build())
                    )
                    .build()
            )
        );

        var result = cut.execute(input).topFailedApis();

        assertThat(result)
            .extracting(TopFailedApis::getData)
            .isEqualTo(
                List.of(
                    TopFailedApis.TopFailedApi.builder()
                        .id("message-api-v4-id")
                        .name("Message Api v4")
                        .definitionVersion(DefinitionVersion.V4)
                        .failedCalls(7L)
                        .failedCallsRatio(0.3)
                        .build()
                )
            );
    }

    @Test
    void should_get_top_failed_sorted_by_count_number_in_descending_order() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").name("Message Api v4").build(),
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id-2").name("Message Api v4 2").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").name("Proxy Api v4").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id-2").name("Proxy Api v4 2").build()
            )
        );
        var input = SearchEnvironmentTopFailedApisUseCase.Input.builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .userId(USER_ID)
            .build();

        when(analyticsQueryService.searchTopFailedApis(any(), any())).thenReturn(
            Optional.of(
                TopFailedApis.builder()
                    .data(
                        List.of(
                            TopFailedApis.TopFailedApi.builder().id("message-api-v4-id").failedCalls(5L).build(),
                            TopFailedApis.TopFailedApi.builder().id("message-api-v4-id-2").failedCalls(3L).build(),
                            TopFailedApis.TopFailedApi.builder().id("proxy-api-v4-id").failedCalls(17L).build(),
                            TopFailedApis.TopFailedApi.builder().id("proxy-api-v4-id-2").failedCalls(2L).build()
                        )
                    )
                    .build()
            )
        );

        var result = cut.execute(input).topFailedApis();

        assertThat(result.getData()).extracting(TopFailedApis.TopFailedApi::failedCalls).containsExactly(17L, 5L, 3L, 2L);
    }

    @Test
    void should_get_empty_top_failed_list_in_case_of_no_records_found() {
        var input = SearchEnvironmentTopFailedApisUseCase.Input.builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .userId(USER_ID)
            .build();

        when(analyticsQueryService.searchTopFailedApis(any(), any())).thenReturn(Optional.empty());

        var result = cut.execute(input).topFailedApis();

        assertThat(result).extracting(TopFailedApis::getData).isEqualTo(List.of());
    }

    @Test
    void should_get_top_failed_apis_only_for_the_apis_whose_analytics_the_user_can_read() {
        givenUserReadsThePlatform(false);
        var readable = ApiFixtures.aProxyApiV4().toBuilder().id("readable").name("Readable").build();
        apiQueryService.initWith(List.of(readable, ApiFixtures.aProxyApiV4().toBuilder().id("unreadable").build()));
        givenUserCanReadAnalyticsOf(readable);
        when(analyticsQueryService.searchTopFailedApis(any(), any())).thenReturn(
            Optional.of(
                TopFailedApis.builder()
                    .data(List.of(TopFailedApis.TopFailedApi.builder().id("readable").failedCalls(2L).failedCallsRatio(0.5).build()))
                    .build()
            )
        );

        var result = cut.execute(anInput()).topFailedApis();

        verify(analyticsQueryService).searchTopFailedApis(any(), argThat(parameters -> parameters.getApiIds().equals(List.of("readable"))));
        assertThat(result.getData()).extracting(TopFailedApis.TopFailedApi::name).containsExactly("Readable");
    }

    @Test
    void should_return_no_failed_api_without_searching_analytics_when_the_user_can_read_no_api() {
        givenUserReadsThePlatform(false);
        apiQueryService.initWith(List.of(ApiFixtures.aProxyApiV4()));

        var result = cut.execute(anInput()).topFailedApis();

        assertThat(result.getData()).isEmpty();
        verifyNoInteractions(analyticsQueryService);
    }

    private SearchEnvironmentTopFailedApisUseCase.Input anInput() {
        return SearchEnvironmentTopFailedApisUseCase.Input.builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .userId(USER_ID)
            .build();
    }

    private void givenUserReadsThePlatform(boolean readsPlatform) {
        when(
            permissionDomainService.hasPermission(ORG_ID, USER_ID, RolePermission.ENVIRONMENT_PLATFORM, ENV_ID, RolePermissionAction.READ)
        ).thenReturn(readsPlatform);
    }

    private void givenUserCanReadAnalyticsOf(Api... apis) {
        apiAuthorizationDomainService.initWith(List.of(apis));
        for (Api api : apis) {
            when(
                permissionDomainService.hasPermission(ORG_ID, USER_ID, RolePermission.API_ANALYTICS, api.getId(), RolePermissionAction.READ)
            ).thenReturn(true);
        }
    }
}
