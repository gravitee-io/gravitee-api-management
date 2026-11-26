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
import io.gravitee.apim.core.plugin.model.FlowPhase;
import io.gravitee.apim.core.policy.exception.UnexpectedPoliciesException;
import io.gravitee.apim.core.secret.domain_service.SecretReferencePermissionDomainService;
import io.gravitee.apim.core.secret.exception.InvalidSecretReferenceException;
import io.gravitee.apim.core.secret.exception.SecretReferenceNotAllowedException;
import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateCreateSharedPolicyGroupDomainService;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupDuplicateCrossIdException;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupInvalidPhaseException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.credential.SpelSecretExpressionParser;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
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
public class CreateSharedPolicyGroupUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private final String ORG_ID = "org-id";
    private final String ENV_ID = "env-id";
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
    private final ValidateCreateSharedPolicyGroupDomainService validateCreateSharedPolicyGroupDomainService =
        new ValidateCreateSharedPolicyGroupDomainService(
            sharedPolicyGroupCrudService,
            policyValidationDomainService,
            secretReferencePermissionDomainService
        );
    private CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase;

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

        createSharedPolicyGroupUseCase = new CreateSharedPolicyGroupUseCase(
            sharedPolicyGroupCrudService,
            validateCreateSharedPolicyGroupDomainService,
            auditService
        );
    }

    @Test
    void should_create() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();

        // When
        var createdSharedPolicyGroup = createSharedPolicyGroupUseCase.execute(
            new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO)
        );

        // Then
        var expected = SharedPolicyGroup.from(toCreate)
            .toBuilder()
            .id("generated-id")
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .crossId("generated-id")
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .version(0)
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .build();
        assertThat(createdSharedPolicyGroup.sharedPolicyGroup()).isNotNull();
        assertThat(createdSharedPolicyGroup.sharedPolicyGroup()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_create_with_custom_crossId() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroupWithAllFields();

        // When
        var createdSharedPolicyGroup = createSharedPolicyGroupUseCase.execute(
            new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO)
        );

        // Then
        var expected = SharedPolicyGroup.from(toCreate)
            .toBuilder()
            .id("generated-id")
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .crossId(toCreate.getCrossId())
            .description(toCreate.getDescription())
            .steps(toCreate.getSteps())
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .version(0)
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .build();
        assertThat(createdSharedPolicyGroup.sharedPolicyGroup()).isNotNull();
        assertThat(createdSharedPolicyGroup.sharedPolicyGroup()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_create_an_audit() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();

        // When
        createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO));

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
                    .properties(Map.of(AuditProperties.SHARED_POLICY_GROUP.name(), "generated-id"))
                    .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_CREATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_throw_exception_when_name_is_null() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup().toBuilder().name(null).build();

        // When
        var throwable = Assertions.catchThrowable(() ->
            createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO))
        );

        // Then
        Assertions.assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessage("Name is required.");
    }

    @Test
    void should_throw_exception_when_apiType_is_null() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup().toBuilder().apiType(null).build();

        // When
        var throwable = Assertions.catchThrowable(() ->
            createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO))
        );

        // Then
        Assertions.assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessage("ApiType is required.");
    }

    @Test
    void should_throw_exception_when_phase_is_null() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup().toBuilder().phase(null).build();

        // When
        var throwable = Assertions.catchThrowable(() ->
            createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO))
        );

        // Then
        Assertions.assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessage("Phase is required.");
    }

    @Test
    void should_throw_exception_when_sharedPolicyGroup_with_crossId_already_exists() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        existingSharedPolicyGroup.setEnvironmentId(ENV_ID);

        this.sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();
        toCreate.setCrossId(existingSharedPolicyGroup.getCrossId());

        // When
        var throwable = Assertions.catchThrowable(() ->
            createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO))
        );

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(SharedPolicyGroupDuplicateCrossIdException.class)
            .hasMessage("SharedPolicyGroup with crossId [crossId] already exists for environment [env-id].");
    }

    @Test
    void should_validate_and_sanitize_configuration() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();
        toCreate.setSteps(List.of(Step.builder().policy("policy").configuration("{ \"key\": \"value\" }").build()));

        // When
        createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO));
    }

    @Test
    void should_throw_exception_when_policy_validation_fails() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();
        toCreate.setSteps(
            List.of(Step.builder().policy("policy_throw_invalid_data_exception").configuration("{ \"key\": \"value\" }").build())
        );

        // When
        var throwable = Assertions.catchThrowable(() ->
            createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO))
        );

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("Invalid configuration for policy policy_throw_invalid_data_exception");
    }

    @Test
    void should_throw_exception_when_policy_execution_phase_validation_fails() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();
        toCreate.setSteps(
            List.of(Step.builder().policy("policy_throw_unexpected_policy_exception").configuration("{ \"key\": \"value\" }").build())
        );

        // When
        var throwable = Assertions.catchThrowable(() ->
            createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO))
        );

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(UnexpectedPoliciesException.class)
            .hasMessage("Unexpected policies [policy_throw_unexpected_policy_exception] for API type MESSAGE and phase REQUEST");
    }

    @Test
    void should_throw_exception_when_phase_is_invalid() {
        // Given
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();
        toCreate.setApiType(ApiType.PROXY);
        toCreate.setPhase(FlowPhase.SUBSCRIBE);

        // When
        var throwable = Assertions.catchThrowable(() ->
            createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO))
        );

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(SharedPolicyGroupInvalidPhaseException.class)
            .hasMessage("Invalid phase SUBSCRIBE for API type PROXY");
    }

    @Test
    void should_refuse_a_step_that_references_a_secret_of_another_environment_and_store_nothing() {
        when(permissionDomainService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();
        toCreate.setSteps(List.of(stepReferencing("/gravitee/db-password:value?envId=other-env")));

        assertThatThrownBy(() -> createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO)))
            .isInstanceOf(InvalidSecretReferenceException.class)
            .hasMessageContaining("?envId=env-id");
        assertThat(sharedPolicyGroupCrudService.storage()).isEmpty();
    }

    @Test
    void should_refuse_a_step_that_references_a_stored_secret_without_the_secret_read_permission() {
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();
        toCreate.setSteps(List.of(stepReferencing("/gravitee/db-password:value?envId=env-id")));

        assertThatThrownBy(() ->
            createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO))
        ).isInstanceOf(SecretReferenceNotAllowedException.class);
        assertThat(sharedPolicyGroupCrudService.storage()).isEmpty();
    }

    @Test
    void should_create_a_step_that_references_a_stored_secret_with_the_secret_read_permission() {
        when(permissionDomainService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();
        toCreate.setSteps(List.of(stepReferencing("/gravitee/db-password:value?envId=env-id")));

        var created = createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO));

        assertThat(created.sharedPolicyGroup().getSteps()).hasSize(1);
    }

    @Test
    void should_refuse_a_step_whose_policy_sanitizes_away_a_reference_to_a_secret_of_another_environment() {
        when(permissionDomainService.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        var step = stepReferencing("/gravitee/db-password:value?envId=other-env");
        step.setPolicy(FakePolicyValidationDomainService.SANITIZING_POLICY);
        var toCreate = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();
        toCreate.setSteps(List.of(step));

        assertThatThrownBy(() ->
            createSharedPolicyGroupUseCase.execute(new CreateSharedPolicyGroupUseCase.Input(toCreate, AUDIT_INFO))
        ).isInstanceOf(InvalidSecretReferenceException.class);
        assertThat(sharedPolicyGroupCrudService.storage()).isEmpty();
    }

    private static Step stepReferencing(String path) {
        return Step.builder().policy("policy").enabled(true).configuration("{ \"value\": \"{#secrets.get('" + path + "')}\" }").build();
    }
}
