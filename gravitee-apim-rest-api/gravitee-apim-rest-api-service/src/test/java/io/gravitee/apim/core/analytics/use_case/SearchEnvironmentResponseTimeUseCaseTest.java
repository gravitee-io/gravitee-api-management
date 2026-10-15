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
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchEnvironmentResponseTimeUseCaseTest {

    public static final String ENV_ID = "environment-id";
    private static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORG_ID, ENV_ID);
    private final FakeAnalyticsQueryService analyticsQueryService = spy(new FakeAnalyticsQueryService());
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService = new ApiAuthorizationDomainServiceInMemory();
    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);

    SearchEnvironmentResponseTimeOverTimeUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchEnvironmentResponseTimeOverTimeUseCase(
            analyticsQueryService,
            new EnvironmentAnalyticsApisDomainService(apiQueryService, apiAuthorizationDomainService, permissionDomainService)
        );
        givenUserReadsThePlatform(true);
    }

    @AfterEach
    void tearDown() {
        apiQueryService.reset();
        apiAuthorizationDomainService.reset();
    }

    @Test
    void nominal_case() {
        // Given
        var input = new SearchEnvironmentResponseTimeOverTimeUseCase.Input(Instant.now().minus(Duration.ofDays(1)), Instant.now(), USER_ID);
        var definition = io.gravitee.definition.model.v4.Api.builder()
            .type(ApiType.MESSAGE)
            .definitionVersion(DefinitionVersion.V4)
            .build();
        apiQueryService.initWith(List.of(Api.builder().id("MyApiID").apiDefinitionHttpV4(definition).environmentId(ENV_ID).build()));
        // the order of keys is important
        analyticsQueryService.averageAggregate = new LinkedHashMap<>();
        analyticsQueryService.averageAggregate.put("1970-01-01T00:00:00", 1.2D);
        analyticsQueryService.averageAggregate.put("1970-01-01T00:30:00", 1.6D);

        // When
        var output = cut.execute(EXECUTION_CONTEXT, input).blockingGet();

        // Then
        assertThat(output.data()).containsExactly(1L, 2L);
        assertThat(Duration.between(output.from(), output.to())).isCloseTo(Duration.ofDays(1), Duration.ofSeconds(1));
        assertThat(output.interval()).isEqualTo(Duration.ofMinutes(10));
        assertThat(output.to()).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    void should_search_response_time_only_for_the_apis_whose_analytics_the_user_can_read() {
        givenUserReadsThePlatform(false);
        var readable = ApiFixtures.aProxyApiV4().toBuilder().id("readable").build();
        apiQueryService.initWith(List.of(readable, ApiFixtures.aProxyApiV4().toBuilder().id("unreadable").build()));
        givenUserCanReadAnalyticsOf(readable);

        cut.execute(EXECUTION_CONTEXT, anInput()).blockingGet();

        verify(analyticsQueryService).searchAvgResponseTimeOverTime(any(), eq(List.of("readable")), any(), any(), any(), any());
    }

    @Test
    void should_return_no_data_without_searching_analytics_when_the_user_can_read_no_api() {
        givenUserReadsThePlatform(false);
        apiQueryService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        var input = anInput();

        var output = cut.execute(EXECUTION_CONTEXT, input).blockingGet();

        assertThat(output.data()).isEmpty();
        assertThat(output.from()).isEqualTo(input.from());
        assertThat(output.to()).isEqualTo(input.to());
        verifyNoInteractions(analyticsQueryService);
    }

    private static SearchEnvironmentResponseTimeOverTimeUseCase.Input anInput() {
        return new SearchEnvironmentResponseTimeOverTimeUseCase.Input(Instant.now().minus(Duration.ofDays(1)), Instant.now(), USER_ID);
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
