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

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AuditCrudServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.SharedPolicyGroupCrudServiceInMemory;
import inmemory.SharedPolicyGroupHistoryCrudServiceInMemory;
import inmemory.SharedPolicyGroupHistoryQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupNotFoundException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.apim.core.shared_policy_group.use_case.UndeploySharedPolicyGroupUseCase.Input;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UndeploySharedPolicyGroupUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String SHARED_POLICY_GROUP_ID = "shared-policy-group-id";
    private static final String SHARED_POLICY_GROUP_CROSS_ID = "shared-policy-group-cross-id";
    private final String ORG_ID = "org-id";
    private final String ENV_ID = "env-id";
    private final String USER_ID = "user-id";
    private final AuditInfo AUDIT_INFO = AuditInfo
        .builder()
        .organizationId(ORG_ID)
        .environmentId(ENV_ID)
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    private final EventCrudInMemory eventCrudInMemory = new EventCrudInMemory();
    private final EventLatestCrudInMemory eventLatestCrudInMemory = new EventLatestCrudInMemory();
    private final SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService = new SharedPolicyGroupCrudServiceInMemory();
    private final SharedPolicyGroupHistoryCrudServiceInMemory sharedPolicyGroupHistoryCrudService =
        new SharedPolicyGroupHistoryCrudServiceInMemory();
    private final SharedPolicyGroupHistoryQueryServiceInMemory sharedPolicyGroupHistoryQueryService =
        new SharedPolicyGroupHistoryQueryServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    private UndeploySharedPolicyGroupUseCase cut;

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
        cut =
            new UndeploySharedPolicyGroupUseCase(
                eventCrudInMemory,
                eventLatestCrudInMemory,
                sharedPolicyGroupCrudService,
                sharedPolicyGroupHistoryCrudService,
                sharedPolicyGroupHistoryQueryService,
                auditService
            );
    }

    @Test
    void should_not_undeploy_shared_policy_group() {
        assertThatThrownBy(() -> cut.execute(new Input(SHARED_POLICY_GROUP_ID, ENV_ID, AUDIT_INFO)))
            .isInstanceOf(SharedPolicyGroupNotFoundException.class);
    }

    @Test
    void should_undeploy_shared_policy_group() {
        // Given
        final SharedPolicyGroup existingSharedPolicyGroup = SharedPolicyGroup
            .builder()
            .environmentId(ENV_ID)
            .phase(PolicyPlugin.ExecutionPhase.REQUEST)
            .id(SHARED_POLICY_GROUP_ID)
            .crossId(SHARED_POLICY_GROUP_CROSS_ID)
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED)
            .version(1)
            .build();
        sharedPolicyGroupCrudService.initWith(List.of(existingSharedPolicyGroup));
        sharedPolicyGroupHistoryQueryService.initWith(List.of(existingSharedPolicyGroup));

        // When
        cut.execute(new Input(SHARED_POLICY_GROUP_ID, ENV_ID, AUDIT_INFO));

        // Then
        // - Check events
        assertThat(eventCrudInMemory.storage())
            .hasSize(1)
            .first()
            .satisfies(event -> {
                // The Event is generated with a random UUID
                assertThat(event.getId()).isNotEqualTo(SHARED_POLICY_GROUP_ID);
                assertThat(event.getEnvironments()).containsExactly(ENV_ID);
                assertThat(event.getProperties())
                    .containsAllEntriesOf(
                        Map.ofEntries(
                            entry(Event.EventProperties.USER, USER_ID),
                            entry(Event.EventProperties.SHARED_POLICY_GROUP_ID, SHARED_POLICY_GROUP_CROSS_ID)
                        )
                    );
                assertThat(event.getType()).isEqualTo(EventType.UNDEPLOY_SHARED_POLICY_GROUP);
                final io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup sharedPolicyGroupDefinition =
                    GraviteeJacksonMapper
                        .getInstance()
                        .readValue(event.getPayload(), io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.class);
                assertThat(sharedPolicyGroupDefinition.getId()).isEqualTo(SHARED_POLICY_GROUP_CROSS_ID);
                assertThat(sharedPolicyGroupDefinition.getEnvironmentId()).isEqualTo(ENV_ID);
                assertThat(sharedPolicyGroupDefinition.getPhase())
                    .isEqualTo(io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase.REQUEST);
            });
        assertThat(eventLatestCrudInMemory.storage())
            .hasSize(1)
            .first()
            .satisfies(event -> {
                // The Event latest is generated with ID equals to shared policy group cross id
                assertThat(event.getId()).isEqualTo(SHARED_POLICY_GROUP_CROSS_ID);
                assertThat(event.getEnvironments()).containsExactly(ENV_ID);
                assertThat(event.getProperties())
                    .containsAllEntriesOf(
                        Map.ofEntries(
                            entry(Event.EventProperties.USER, USER_ID),
                            entry(Event.EventProperties.SHARED_POLICY_GROUP_ID, SHARED_POLICY_GROUP_CROSS_ID)
                        )
                    );
                assertThat(event.getType()).isEqualTo(EventType.UNDEPLOY_SHARED_POLICY_GROUP);
                final io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup sharedPolicyGroupDefinition =
                    GraviteeJacksonMapper
                        .getInstance()
                        .readValue(event.getPayload(), io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.class);
                assertThat(sharedPolicyGroupDefinition.getId()).isEqualTo(SHARED_POLICY_GROUP_CROSS_ID);
                assertThat(sharedPolicyGroupDefinition.getEnvironmentId()).isEqualTo(ENV_ID);
                assertThat(sharedPolicyGroupDefinition.getPhase())
                    .isEqualTo(io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase.REQUEST);
            });

        // - Check shared policy group CRUD service
        assertThat(sharedPolicyGroupCrudService.storage())
            .hasSize(1)
            .first()
            .satisfies(sharedPolicyGroup -> {
                assertThat(sharedPolicyGroup.getId()).isEqualTo(SHARED_POLICY_GROUP_ID);
                assertThat(sharedPolicyGroup.getLifecycleState()).isEqualTo(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED);
                assertThat(sharedPolicyGroup.getVersion()).isEqualTo(1);
            });
        assertThat(sharedPolicyGroupHistoryCrudService.storage())
            .hasSize(1)
            .last()
            .satisfies(sharedPolicyGroup -> {
                assertThat(sharedPolicyGroup.getId()).isEqualTo(SHARED_POLICY_GROUP_ID);
                assertThat(sharedPolicyGroup.getLifecycleState()).isEqualTo(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED);
                assertThat(sharedPolicyGroup.getVersion()).isEqualTo(1);
            });

        // - Check audit
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
                    .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_UNDEPLOYED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }
}
