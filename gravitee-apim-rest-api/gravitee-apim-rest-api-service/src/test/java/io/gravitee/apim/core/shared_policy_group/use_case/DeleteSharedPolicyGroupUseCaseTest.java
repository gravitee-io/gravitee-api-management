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

import fixtures.core.model.SharedPolicyGroupFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.SharedPolicyGroupCrudServiceInMemory;
import inmemory.SharedPolicyGroupHistoryCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupNotFoundException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
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
public class DeleteSharedPolicyGroupUseCaseTest {

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
    private final SharedPolicyGroupHistoryCrudServiceInMemory sharedPolicyGroupHistoryCrudService =
        new SharedPolicyGroupHistoryCrudServiceInMemory();
    private DeleteSharedPolicyGroupUseCase deleteSharedPolicyGroupUseCase;

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

        deleteSharedPolicyGroupUseCase =
            new DeleteSharedPolicyGroupUseCase(sharedPolicyGroupCrudService, sharedPolicyGroupHistoryCrudService, auditService);
    }

    @Test
    void should_delete() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));
        sharedPolicyGroupHistoryCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        deleteSharedPolicyGroupUseCase.execute(new DeleteSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), AUDIT_INFO));

        // Then
        assertThat(sharedPolicyGroupCrudService.storage()).isEmpty();
        assertThat(sharedPolicyGroupHistoryCrudService.storage()).isEmpty();
    }

    @Test
    void should_create_an_audit() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));
        sharedPolicyGroupHistoryCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        deleteSharedPolicyGroupUseCase.execute(new DeleteSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), AUDIT_INFO));

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
                    .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_DELETED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_throw_exception_when_shared_policy_group_not_found() {
        // Given
        var existingSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        existingSharedPolicyGroup.setEnvironmentId("ANOTHER_ENV_ID");
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));

        // When
        var throwable = Assertions.catchThrowable(() ->
            deleteSharedPolicyGroupUseCase.execute(new DeleteSharedPolicyGroupUseCase.Input(existingSharedPolicyGroup.getId(), AUDIT_INFO))
        );

        // Then
        assertThat(throwable)
            .isInstanceOf(SharedPolicyGroupNotFoundException.class)
            .hasMessage("SharedPolicyGroup [sharedPolicyGroupId] cannot be found.");
    }
}
