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
package io.gravitee.apim.core.integration.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.IntegrationFixture;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationCrudServiceInMemory;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.use_case.CheckIntegrationUseCase.Input;
import io.gravitee.apim.core.integration.use_case.CheckIntegrationUseCase.Output;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckIntegrationUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String USER_ID = "user-id";
    private static final String ORGANIZATION_1 = "organization-1";
    private static final String ENVIRONMENT_1 = "environment-1";
    private static final String ORGANIZATION_2 = "organization-2";
    private static final String ENVIRONMENT_2 = "environment-2";
    private static final String INTEGRATION_ID = "my-integration-id";
    private static final String PROVIDER = "aws-api-gateway";
    private static final Integration INTEGRATION = IntegrationFixture
        .anIntegration()
        .toBuilder()
        .id(INTEGRATION_ID)
        .provider(PROVIDER)
        .environmentId(ENVIRONMENT_1)
        .build();

    IntegrationCrudServiceInMemory integrationCrudService = new IntegrationCrudServiceInMemory();
    EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);

    CheckIntegrationUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase = new CheckIntegrationUseCase(integrationCrudService, environmentCrudService, permissionDomainService);

        environmentCrudService.initWith(
            List.of(
                Environment.builder().id(ENVIRONMENT_1).organizationId(ORGANIZATION_1).build(),
                Environment.builder().id(ENVIRONMENT_2).organizationId(ORGANIZATION_2).build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(integrationCrudService, environmentCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_fail_when_integration_does_not_exist() {
        // When
        var result = useCase.execute(new Input(ORGANIZATION_1, USER_ID, "unknown", PROVIDER));

        // Then
        assertThat(result).extracting(Output::success, Output::message).containsExactly(false, "Integration [id=unknown] not found");
    }

    @Test
    void should_fail_when_integration_does_not_match_with_agent_connected() {
        // Given
        givenExistingIntegration(INTEGRATION);
        givenPermission(
            RolePermission.ENVIRONMENT_INTEGRATION,
            ENVIRONMENT_1,
            RolePermissionAction.CREATE,
            RolePermissionAction.READ,
            RolePermissionAction.UPDATE,
            RolePermissionAction.DELETE
        );

        // When
        var result = useCase.execute(new Input(ORGANIZATION_1, USER_ID, INTEGRATION_ID, "other"));

        // Then
        assertThat(result)
            .extracting(Output::success, Output::message)
            .containsExactly(false, "Integration [id=my-integration-id] does not match. Expected provider [provider=aws-api-gateway]");
    }

    @Test
    void should_fail_when_using_integration_from_other_organization() {
        // Given
        givenExistingIntegration(INTEGRATION);

        // When
        var result = useCase.execute(new Input(ORGANIZATION_2, USER_ID, INTEGRATION_ID, PROVIDER));

        // Then
        assertThat(result)
            .extracting(Output::success, Output::message)
            .containsExactly(false, "Integration [id=my-integration-id] not found");
    }

    @Test
    void should_fail_when_user_has_not_enough_permission_for_integration() {
        // Given
        givenExistingIntegration(INTEGRATION);

        // When
        var result = useCase.execute(new Input(ORGANIZATION_1, USER_ID, INTEGRATION_ID, PROVIDER));

        // Then
        assertThat(result)
            .extracting(Output::success, Output::message)
            .containsExactly(false, "Integration [id=my-integration-id] not found");
    }

    @Test
    void should_return_success_response_when_user_has_correct_environment_integration_permissions() {
        // Given
        givenExistingIntegration(INTEGRATION);
        givenPermission(
            RolePermission.ENVIRONMENT_INTEGRATION,
            ENVIRONMENT_1,
            RolePermissionAction.CREATE,
            RolePermissionAction.READ,
            RolePermissionAction.UPDATE,
            RolePermissionAction.DELETE
        );

        var result = useCase.execute(new Input(ORGANIZATION_1, USER_ID, INTEGRATION_ID, PROVIDER));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result).extracting(Output::success).isEqualTo(true);
        });
    }

    @Test
    void should_return_success_response_when_user_has_correct_integration_permissions() {
        // Given
        givenExistingIntegration(INTEGRATION);
        givenPermission(RolePermission.INTEGRATION_DEFINITION, INTEGRATION_ID, RolePermissionAction.CREATE);

        var result = useCase.execute(new Input(ORGANIZATION_1, USER_ID, INTEGRATION_ID, PROVIDER));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result).extracting(Output::success).isEqualTo(true);
        });
    }

    private void givenExistingIntegration(Integration... integration) {
        integrationCrudService.initWith(Arrays.asList(integration));
    }

    private void givenPermission(RolePermission rolePermission, String referenceId, RolePermissionAction... acls) {
        when(permissionDomainService.hasExactPermissions(ORGANIZATION_1, USER_ID, rolePermission, referenceId, acls)).thenReturn(true);
        when(permissionDomainService.hasPermission(ORGANIZATION_1, USER_ID, rolePermission, referenceId, acls)).thenReturn(true);
    }
}
