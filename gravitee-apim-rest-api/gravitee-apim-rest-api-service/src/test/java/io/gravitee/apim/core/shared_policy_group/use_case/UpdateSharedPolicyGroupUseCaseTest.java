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
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.apim.core.shared_policy_group.model.UpdateSharedPolicyGroup;
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

        updateSharedPolicyGroupUseCase =
            new UpdateSharedPolicyGroupUseCase(sharedPolicyGroupCrudService, policyValidationDomainService, auditService);
    }

    @Test
    void should_update() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup
            .builder()
            .crossId("new-cross-id")
            .name("new-name")
            .description("new-description")
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
            .steps(List.of(Step.builder().policy("policy").configuration("{ \"key\": \"newValue\" }").build()))
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
        var toUpdate = UpdateSharedPolicyGroup
            .builder()
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
                AuditEntity
                    .builder()
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
        Assertions
            .assertThat(throwable)
            .isInstanceOf(SharedPolicyGroupDuplicateCrossIdException.class)
            .hasMessage("SharedPolicyGroup with crossId [cross-id-1] already exists for environment [environmentId].");
    }

    @Test
    void should_throw_exception_when_policy_validation_fails() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup
            .builder()
            .steps(List.of(Step.builder().policy("policy_throw_invalid_data_exception").configuration("{ \"key\": \"value\" }").build()))
            .build();
        var throwable = Assertions.catchThrowable(() ->
            updateSharedPolicyGroupUseCase.execute(
                new UpdateSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), toUpdate, AUDIT_INFO)
            )
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
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var toUpdate = UpdateSharedPolicyGroup
            .builder()
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
        Assertions
            .assertThat(throwable)
            .isInstanceOf(UnexpectedPoliciesException.class)
            .hasMessage("Unexpected policies [policy_throw_unexpected_policy_exception] for API type MESSAGE and phase REQUEST");
    }
}
