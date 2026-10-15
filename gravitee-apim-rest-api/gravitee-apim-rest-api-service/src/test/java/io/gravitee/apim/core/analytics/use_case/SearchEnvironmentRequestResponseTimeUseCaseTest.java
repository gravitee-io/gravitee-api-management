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
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.EnumSet;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchEnvironmentRequestResponseTimeUseCaseTest {

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
    private SearchEnvironmentRequestResponseTimeUseCase cut;

    private ArgumentCaptor<AnalyticsQueryParameters> argumentCaptor;

    @BeforeEach
    void setUp() {
        analyticsQueryService = mock(FakeAnalyticsQueryService.class);
        cut = new SearchEnvironmentRequestResponseTimeUseCase(
            new EnvironmentAnalyticsApisDomainService(apiQueryService, apiAuthorizationDomainService, permissionDomainService),
            analyticsQueryService
        );

        argumentCaptor = ArgumentCaptor.forClass(AnalyticsQueryParameters.class);
        givenUserReadsThePlatform(true);
    }

    @AfterEach
    void tearDown() {
        analyticsQueryService.reset();
        apiQueryService.reset();
        apiAuthorizationDomainService.reset();
    }

    @Test
    public void should_get_request_response_time_only_for_specified_env() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aProxyApiV4().toBuilder().id("proper-env-proxy-api-v4-id").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("other-env-proxy-api-v4-id").environmentId("other-env").build()
            )
        );
        var input = SearchEnvironmentRequestResponseTimeUseCase.Input.builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .userId(USER_ID)
            .build();

        when(analyticsQueryService.searchRequestResponseTime(any(), any())).thenReturn(
            RequestResponseTime.builder()
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
                    AnalyticsQueryParameters.builder()
                        .from(FROM)
                        .to(TO)
                        .apiIds(List.of("proper-env-proxy-api-v4-id"))
                        .definitionVersions(EnumSet.of(DefinitionVersion.V4))
                        .build()
                );
            softAssertions
                .assertThat(result)
                .isPresent()
                .isNotNull()
                .get()
                .isEqualTo(
                    RequestResponseTime.builder()
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
    void should_get_request_response_time_only_for_the_apis_whose_analytics_the_user_can_read() {
        givenUserReadsThePlatform(false);
        var readableV4 = ApiFixtures.aProxyApiV4().toBuilder().id("readable-v4").build();
        var readableV2 = ApiFixtures.aProxyApiV2().toBuilder().id("readable-v2").build();
        apiQueryService.initWith(List.of(readableV4, readableV2, ApiFixtures.aProxyApiV4().toBuilder().id("unreadable").build()));
        givenUserCanReadAnalyticsOf(readableV4, readableV2);
        when(analyticsQueryService.searchRequestResponseTime(any(), any())).thenReturn(RequestResponseTime.builder().build());

        cut.execute(anInput());

        verify(analyticsQueryService).searchRequestResponseTime(any(), argumentCaptor.capture());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(argumentCaptor.getValue().getApiIds()).containsExactlyInAnyOrder("readable-v4", "readable-v2");
            softAssertions
                .assertThat(argumentCaptor.getValue().getDefinitionVersions())
                .containsExactlyInAnyOrder(DefinitionVersion.V4, DefinitionVersion.V2);
        });
    }

    @Test
    void should_return_no_request_without_searching_analytics_when_the_user_can_read_no_api() {
        givenUserReadsThePlatform(false);
        apiQueryService.initWith(List.of(ApiFixtures.aProxyApiV4()));

        var result = cut.execute(anInput()).requestResponseTime();

        assertThat(result).contains(
            RequestResponseTime.builder()
                .requestsPerSecond(0d)
                .requestsTotal(0L)
                .responseMinTime(0d)
                .responseMaxTime(0d)
                .responseAvgTime(0d)
                .build()
        );
        verifyNoInteractions(analyticsQueryService);
    }

    private SearchEnvironmentRequestResponseTimeUseCase.Input anInput() {
        return SearchEnvironmentRequestResponseTimeUseCase.Input.builder()
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
