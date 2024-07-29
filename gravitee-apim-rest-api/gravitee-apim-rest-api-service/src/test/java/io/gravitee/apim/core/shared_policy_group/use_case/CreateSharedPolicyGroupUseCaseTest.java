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

import fakes.FakePolicyValidationDomainService;
import fixtures.core.model.SharedPolicyGroupFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.SharedPolicyGroupCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.policy.exception.UnexpectedPoliciesException;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupDuplicateCrossIdException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
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
public class CreateSharedPolicyGroupUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private final String ORG_ID = "org-id";
    private final String ENV_ID = "env-id";
    private final String USER_ID = "user-id";
    private final AuditInfo AUDIT_INFO = AuditInfo
        .builder()
        .organizationId(ORG_ID)
        .environmentId(ENV_ID)
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    private final SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService = new SharedPolicyGroupCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final FakePolicyValidationDomainService policyValidationDomainService = new FakePolicyValidationDomainService();
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

        createSharedPolicyGroupUseCase =
            new CreateSharedPolicyGroupUseCase(sharedPolicyGroupCrudService, policyValidationDomainService, auditService);
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
        var expected = SharedPolicyGroup
            .from(toCreate)
            .toBuilder()
            .id("generated-id")
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .crossId("generated-id")
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
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
        var expected = SharedPolicyGroup
            .from(toCreate)
            .toBuilder()
            .id("generated-id")
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .crossId(toCreate.getCrossId())
            .description(toCreate.getDescription())
            .steps(toCreate.getSteps())
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
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
                AuditEntity
                    .builder()
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
        Assertions
            .assertThat(throwable)
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
        Assertions
            .assertThat(throwable)
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
        Assertions
            .assertThat(throwable)
            .isInstanceOf(UnexpectedPoliciesException.class)
            .hasMessage("Unexpected policies [policy_throw_unexpected_policy_exception] for API type MESSAGE and phase REQUEST");
    }
}
