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
package io.gravitee.apim.core.log.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.repository.ConnectionLogFixtures;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ConnectionLogsCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.log.use_case.SearchEnvironmentConnectionLogErrorKeysUseCase.Input;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchEnvironmentConnectionLogErrorKeysUseCaseTest {

    private static final Long FROM = Instant.parse("2020-02-01T00:01:00.00Z").toEpochMilli();
    private static final Long TO = Instant.parse("2020-02-02T23:59:59.00Z").toEpochMilli();
    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORG_ID, ENV_ID);
    private static final Api API = ApiFixtures.aProxyApiV4().toBuilder().id("api-id").build();
    private static final Api API_2 = ApiFixtures.aProxyApiV4().toBuilder().id("api2").build();

    ConnectionLogFixtures connectionLogFixtures = new ConnectionLogFixtures("api-id", "app-id", "plan-id");
    ConnectionLogsCrudServiceInMemory logStorageService = new ConnectionLogsCrudServiceInMemory();
    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService = new ApiAuthorizationDomainServiceInMemory();
    PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);

    SearchEnvironmentConnectionLogErrorKeysUseCase usecase;

    @BeforeEach
    void setUp() {
        usecase = new SearchEnvironmentConnectionLogErrorKeysUseCase(
            logStorageService,
            new EnvironmentAnalyticsApisDomainService(apiQueryService, apiAuthorizationDomainService, permissionDomainService)
        );
        apiQueryService.initWith(List.of(API, API_2));
        givenUserReadsThePlatform(true);
    }

    @AfterEach
    void tearDown() {
        Stream.of(logStorageService, apiQueryService, apiAuthorizationDomainService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_error_keys_of_an_environment() {
        givenConnectionLogsOfBothApis();

        var result = usecase.execute(EXECUTION_CONTEXT, new Input(FROM, TO, USER_ID));

        assertThat(result.errorKeys()).containsExactly("ERROR_1", "ERROR_2");
    }

    @Test
    void should_return_error_keys_only_of_the_apis_whose_analytics_the_user_can_read() {
        givenUserReadsThePlatform(false);
        givenConnectionLogsOfBothApis();
        givenUserCanReadAnalyticsOf(API_2);

        var result = usecase.execute(EXECUTION_CONTEXT, new Input(FROM, TO, USER_ID));

        assertThat(result.errorKeys()).containsExactly("ERROR_2");
    }

    @Test
    void should_return_no_error_key_when_the_user_can_read_no_api() {
        givenUserReadsThePlatform(false);
        givenConnectionLogsOfBothApis();

        var result = usecase.execute(EXECUTION_CONTEXT, new Input(FROM, TO, USER_ID));

        assertThat(result.errorKeys()).isEmpty();
    }

    @Test
    void should_return_empty_list_if_no_error_keys() {
        logStorageService.initWithConnectionLogs(
            List.of(connectionLogFixtures.aConnectionLog("req1").toBuilder().errorKey(null).timestamp("2020-02-01T20:00:00.00Z").build())
        );

        var result = usecase.execute(EXECUTION_CONTEXT, new Input(FROM, TO, USER_ID));

        assertThat(result.errorKeys()).isEmpty();
    }

    private void givenConnectionLogsOfBothApis() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().errorKey("ERROR_1").timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures
                    .aConnectionLog("req2")
                    .toBuilder()
                    .apiId("api2")
                    .errorKey("ERROR_2")
                    .timestamp("2020-02-01T21:00:00.00Z")
                    .build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().errorKey("ERROR_1").timestamp("2020-02-01T22:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req4").toBuilder().errorKey("ERROR_3").timestamp("2020-02-03T20:00:00.00Z").build() // Out of range
            )
        );
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
