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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchEnvironmentResponseStatusRangesUseCaseTest {

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
    private SearchEnvironmentResponseStatusRangesUseCase cut;

    private ArgumentCaptor<AnalyticsQueryParameters> argumentCaptor;

    @BeforeEach
    void setUp() {
        analyticsQueryService = mock(FakeAnalyticsQueryService.class);
        cut = new SearchEnvironmentResponseStatusRangesUseCase(
            analyticsQueryService,
            new EnvironmentAnalyticsApisDomainService(apiQueryService, apiAuthorizationDomainService, permissionDomainService)
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
    public void should_get_proper_ranges_only_for_all_apis() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiV4().toBuilder().id("message-api-v4-id").build(),
                ApiFixtures.aProxyApiV4().toBuilder().id("proxy-api-v4-id").build(),
                ApiFixtures.aProxyApiV2().toBuilder().id("proxy-api-v2-id").build()
            )
        );
        var input = SearchEnvironmentResponseStatusRangesUseCase.Input.builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .userId(USER_ID)
            .build();

        when(analyticsQueryService.searchResponseStatusRanges(any(), any())).thenReturn(
            Optional.of(
                ResponseStatusRanges.builder()
                    .ranges(Map.of("100.0-200.0", 1L, "200.0-300.0", 17L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L))
                    .build()
            )
        );

        var result = cut.execute(input).responseStatusRanges();

        verify(analyticsQueryService).searchResponseStatusRanges(any(), argumentCaptor.capture());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(argumentCaptor.getValue().getFrom()).isEqualTo(FROM);
            softly.assertThat(argumentCaptor.getValue().getTo()).isEqualTo(TO);
            softly
                .assertThat(argumentCaptor.getValue().getApiIds())
                .containsExactlyInAnyOrder("proxy-api-v2-id", "message-api-v4-id", "proxy-api-v4-id");
            softly
                .assertThat(argumentCaptor.getValue().getDefinitionVersions())
                .containsExactlyInAnyOrder(DefinitionVersion.V2, DefinitionVersion.V4);
            softly
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
        var input = SearchEnvironmentResponseStatusRangesUseCase.Input.builder()
            .executionContext(executionContext)
            .parameters(AnalyticsQueryParameters.builder().from(FROM).to(TO).build())
            .userId(USER_ID)
            .build();

        when(analyticsQueryService.searchResponseStatusRanges(any(), any())).thenReturn(
            Optional.of(
                ResponseStatusRanges.builder()
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
                    AnalyticsQueryParameters.builder()
                        .from(FROM)
                        .to(TO)
                        .apiIds(List.of("proper-env-proxy-api-v4-id"))
                        .definitionVersions(Set.of(DefinitionVersion.V4))
                        .build()
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
    void should_get_ranges_only_for_the_apis_whose_analytics_the_user_can_read() {
        givenUserReadsThePlatform(false);
        var readableV4 = ApiFixtures.aProxyApiV4().toBuilder().id("readable-v4").build();
        var readableV2 = ApiFixtures.aProxyApiV2().toBuilder().id("readable-v2").build();
        apiQueryService.initWith(List.of(readableV4, readableV2, ApiFixtures.aMessageApiV4().toBuilder().id("unreadable").build()));
        givenUserCanReadAnalyticsOf(readableV4, readableV2);
        when(analyticsQueryService.searchResponseStatusRanges(any(), any())).thenReturn(Optional.empty());

        cut.execute(anInput());

        verify(analyticsQueryService).searchResponseStatusRanges(any(), argumentCaptor.capture());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(argumentCaptor.getValue().getApiIds()).containsExactlyInAnyOrder("readable-v4", "readable-v2");
            softAssertions
                .assertThat(argumentCaptor.getValue().getDefinitionVersions())
                .containsExactlyInAnyOrder(DefinitionVersion.V4, DefinitionVersion.V2);
        });
    }

    @Test
    void should_return_no_range_without_searching_analytics_when_the_user_can_read_no_api() {
        givenUserReadsThePlatform(false);
        apiQueryService.initWith(List.of(ApiFixtures.aProxyApiV4()));

        var result = cut.execute(anInput()).responseStatusRanges();

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(result).contains(new ResponseStatusRanges());
        });
        verifyNoInteractions(analyticsQueryService);
    }

    private SearchEnvironmentResponseStatusRangesUseCase.Input anInput() {
        return SearchEnvironmentResponseStatusRangesUseCase.Input.builder()
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
