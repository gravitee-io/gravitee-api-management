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
package io.gravitee.apim.core.shared_policy_group.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fakes.FakePolicyValidationDomainService;
import fixtures.core.model.SharedPolicyGroupFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.CredentialCrudServiceInMemory;
import inmemory.SharedPolicyGroupCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.policy.exception.UnexpectedPoliciesException;
import io.gravitee.apim.core.secret.domain_service.SecretReferencePermissionDomainService;
import io.gravitee.apim.core.secret.exception.InvalidSecretReferenceException;
import io.gravitee.apim.core.secret.exception.SecretReferenceNotAllowedException;
import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateUpdateSharedPolicyGroupDomainService;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupDuplicateCrossIdException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.apim.core.shared_policy_group.model.UpdateSharedPolicyGroup;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.credential.SpelSecretExpressionParser;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class UpdateSharedPolicyGroupUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private final String ORG_ID = SharedPolicyGroupFixtures.aSharedPolicyGroup().getOrganizationId();
    private final String ENV_ID = SharedPolicyGroupFixtures.aSharedPolicyGroup().getEnvironmentId();
    private final String USER_ID = "user-id";
    private final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORG_ID)
        .environmentId(ENV_ID)
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    private final SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService = new SharedPolicyGroupCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final FakePolicyValidationDomainService policyValidationDomainService = new FakePolicyValidationDomainService();

    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);
    private final SecretReferencePermissionDomainService secretReferencePermissionDomainService =
        new SecretReferencePermissionDomainService(
            new SpelSecretExpressionParser(),
            permissionDomainService,
            new CredentialCrudServiceInMemory(),
            GraviteeJacksonMapper.getInstance()
        );

    private final ValidateUpdateSharedPolicyGroupDomainService validateUpdateSharedPolicyGroupDomainService =
        new ValidateUpdateSharedPolicyGroupDomainService(
            sharedPolicyGroupCrudService,
            policyValidationDomainService,
            secretReferencePermissionDomainService
        );
    private UpdateSharedPolicyGroupUseCase updateSharedPolicyGroupUseCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());

        updateSharedPolicyGroupUseCase = new UpdateSharedPolicyGroupUseCase(
            sharedPolicyGroupCrudService,
            validateUpdateSharedPolicyGroupDomainService,
            auditService
        );
    }

    @Test
    void should_update_with_DEPLOYED_state() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup.builder()
            .crossId("new-cross-id")
            .name("new-name")
            .description("new-description")
            .prerequisiteMessage("new-prerequisite-message")
            .steps(List.of(Step.builder().policy("policy").configuration("{ \"key\": \"newValue\" }").build()))
            .build();
        var updatedSharedPolicyGroup = updateSharedPolicyGroupUseCase.execute(
            new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
        );

        // Then
        var expected = existingSharedPolicyGroup
            .toBuilder()
            .crossId("new-cross-id")
            .name("new-name")
            .description("new-description")
            .prerequisiteMessage("new-prerequisite-message")
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.PENDING)
            .steps(List.of(Step.builder().policy("policy").configuration("{ \"key\": \"newValue\" }").build()))
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .build();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).isNotNull();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_update_with_PENDING_state() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        existingSharedPolicyGroup.setLifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.PENDING);
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup.builder().name("new-name").build();
        var updatedSharedPolicyGroup = updateSharedPolicyGroupUseCase.execute(
            new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
        );

        // Then
        var expected = existingSharedPolicyGroup
            .toBuilder()
            .name("new-name")
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.PENDING)
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .build();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).isNotNull();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_update_with_UNDEPLOYED_state() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        existingSharedPolicyGroup.setLifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED);
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup.builder().name("new-name").build();
        var updatedSharedPolicyGroup = updateSharedPolicyGroupUseCase.execute(
            new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
        );

        // Then
        var expected = existingSharedPolicyGroup
            .toBuilder()
            .name("new-name")
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .build();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).isNotNull();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_create_an_audit() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup.builder()
            .crossId("new-cross-id")
            .name("new-name")
            .description("new-description")
            .steps(List.of(Step.builder().policy("policy").configuration("{ \"key\": \"newValue\" }").build()))
            .build();
        updateSharedPolicyGroupUseCase.execute(
            new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
        );

        // Then
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .containsExactly(
                AuditEntity.builder()
                    .id("generated-id")
                    .organizationId(ORG_ID)
                    .environmentId(ENV_ID)
                    .referenceType(AuditEntity.AuditReferenceType.ENVIRONMENT)
                    .referenceId(ENV_ID)
                    .user(USER_ID)
                    .properties(Map.of(AuditProperties.SHARED_POLICY_GROUP.name(), existingSharedPolicyGroup.getId()))
                    .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_UPDATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_update_only_description() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup.builder().description("new-description").build();
        var updatedSharedPolicyGroup = updateSharedPolicyGroupUseCase.execute(
            new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
        );

        // Then
        var expected = existingSharedPolicyGroup
            .toBuilder()
            .description("new-description")
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.PENDING)
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .build();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).isNotNull();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_update_and_keep_hrid() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup().toBuilder().hrid("hrid").build();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup.builder().description("new-description").build();
        var updatedSharedPolicyGroup = updateSharedPolicyGroupUseCase.execute(
            new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
        );

        // Then
        var expected = existingSharedPolicyGroup
            .toBuilder()
            .description("new-description")
            .hrid("hrid")
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.PENDING)
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .build();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).isNotNull();
        assertThat(updatedSharedPolicyGroup.sharedPolicyGroup()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_throw_exception_when_sharedPolicyGroup_with_crossId_already_exists() {
        // Given
        var existingSharedPolicyGroup_1 = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        existingSharedPolicyGroup_1.setId("shared-policy-group-id-1");
        existingSharedPolicyGroup_1.setCrossId("cross-id-1");
        var existingSharedPolicyGroup_2 = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        existingSharedPolicyGroup_2.setId("shared-policy-group-id-2");
        existingSharedPolicyGroup_2.setCrossId("cross-id-2");

        this.sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup_1, existingSharedPolicyGroup_2));

        // When
        var toUpdate = UpdateSharedPolicyGroup.builder().crossId(existingSharedPolicyGroup_1.getCrossId()).build();
        var throwable = Assertions.catchThrowable(() ->
            updateSharedPolicyGroupUseCase.execute(
                new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup_2.getId(), toUpdate, AUDIT_INFO)
            )
        );

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(SharedPolicyGroupDuplicateCrossIdException.class)
            .hasMessage("SharedPolicyGroup with crossId [cross-id-1] already exists for environment [environmentId].");
    }

    @Test
    void should_throw_exception_when_policy_validation_fails() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup.builder()
            .steps(List.of(Step.builder().policy("policy_throw_invalid_data_exception").configuration("{ \"key\": \"value\" }").build()))
            .build();
        var throwable = Assertions.catchThrowable(() ->
            updateSharedPolicyGroupUseCase.execute(
                new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
            )
        );

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("Invalid configuration for policy policy_throw_invalid_data_exception");
    }

    @Test
    void should_throw_exception_when_policy_execution_phase_validation_fails() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup.builder()
            .steps(
                List.of(Step.builder().policy("policy_throw_unexpected_policy_exception").configuration("{ \"key\": \"value\" }").build())
            )
            .build();
        var throwable = Assertions.catchThrowable(() ->
            updateSharedPolicyGroupUseCase.execute(
                new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
            )
        );

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(UnexpectedPoliciesException.class)
            .hasMessage("Unexpected policies [policy_throw_unexpected_policy_exception] for API type MESSAGE and phase REQUEST");
    }

    @Test
    void should_refuse_a_step_that_adds_a_reference_to_a_secret_of_another_environment_and_keep_the_stored_version() {
        when(permissionDomainService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));
        var toUpdate = UpdateSharedPolicyGroup.builder()
            .steps(List.of(stepReferencing("/gravitee/db-password:value?envId=other-env")))
            .build();

        assertThatThrownBy(() ->
            updateSharedPolicyGroupUseCase.execute(
                new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
            )
        )
            .isInstanceOf(InvalidSecretReferenceException.class)
            .hasMessageContaining("?envId=" + ENV_ID);
        assertThat(sharedPolicyGroupCrudService.storage()).containsExactly(existingSharedPolicyGroup);
    }

    @Test
    void should_refuse_a_step_that_adds_a_reference_to_a_stored_secret_without_the_secret_read_permission() {
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));
        var toUpdate = UpdateSharedPolicyGroup.builder()
            .steps(List.of(stepReferencing("/gravitee/db-password:value?envId=" + ENV_ID)))
            .build();

        assertThatThrownBy(() ->
            updateSharedPolicyGroupUseCase.execute(
                new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
            )
        ).isInstanceOf(SecretReferenceNotAllowedException.class);
        assertThat(sharedPolicyGroupCrudService.storage()).containsExactly(existingSharedPolicyGroup);
    }

    @Test
    void should_save_a_step_whose_reference_the_stored_version_already_has() {
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup()
            .toBuilder()
            .steps(List.of(stepReferencing("/gravitee/db-password:value?envId=other-env")))
            .build();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));
        var toUpdate = UpdateSharedPolicyGroup.builder()
            .name("renamed")
            .steps(List.of(stepReferencing("/gravitee/db-password:value?envId=other-env")))
            .build();

        var updated = updateSharedPolicyGroupUseCase.execute(
            new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
        );

        assertThat(updated.sharedPolicyGroup().getName()).isEqualTo("renamed");
    }

    @Test
    void should_refuse_a_step_whose_policy_sanitizes_away_a_new_reference_to_a_secret_of_another_environment() {
        when(permissionDomainService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));
        var step = stepReferencing("/gravitee/db-password:value?envId=other-env");
        step.setPolicy(FakePolicyValidationDomainService.SANITIZING_POLICY);
        var toUpdate = UpdateSharedPolicyGroup.builder().steps(List.of(step)).build();

        assertThatThrownBy(() ->
            updateSharedPolicyGroupUseCase.execute(
                new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
            )
        ).isInstanceOf(InvalidSecretReferenceException.class);
        assertThat(sharedPolicyGroupCrudService.storage()).containsExactly(existingSharedPolicyGroup);
    }

    private static Step stepReferencing(String path) {
        return Step.builder().policy("policy").enabled(true).configuration("{ \"value\": \"{#secrets.get('" + path + "')}\" }").build();
    }
}
