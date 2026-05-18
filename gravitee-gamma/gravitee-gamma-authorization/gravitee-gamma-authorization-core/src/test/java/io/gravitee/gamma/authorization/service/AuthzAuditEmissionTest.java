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
package io.gravitee.gamma.authorization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.authorization.api.AuthzAuditEntry;
import io.gravitee.gamma.authorization.api.AuthzAuditReferenceKind;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEntityAuditEvent;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.api.AuthzPolicyAuditEvent;
import io.gravitee.gamma.authorization.api.EntityAuditSnapshot;
import io.gravitee.gamma.authorization.api.PolicyAuditSnapshot;
import io.gravitee.gamma.authorization.audit.RecordingAuthzAuditPort;
import io.gravitee.gamma.authorization.repository.InMemoryEntityRepository;
import io.gravitee.gamma.authorization.repository.InMemoryPolicyRepository;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntityKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AuthzAuditEmissionTest {

    private static final Instant FIXED = Instant.parse("2025-01-01T00:00:00Z");
    private static final String ENV = "env-1";
    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", ENV, "alice");

    private InMemoryPolicyRepository policyRepository;
    private InMemoryEntityRepository entityRepository;
    private PolicyServiceImpl policyService;
    private EntityServiceImpl entityService;
    private RecordingAuthzAuditPort audit;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(FIXED, ZoneOffset.UTC));
        policyRepository = new InMemoryPolicyRepository();
        entityRepository = new InMemoryEntityRepository();
        EntityIdValidator validator = new EntityIdValidator();
        audit = new RecordingAuthzAuditPort();
        SchemaServiceImpl schemaService = new SchemaServiceImpl(entityRepository, policyRepository);
        policyService = new PolicyServiceImpl(policyRepository, validator, schemaService, mock(AuthzEventPublisher.class), audit);
        entityService = new EntityServiceImpl(
            entityRepository,
            policyRepository,
            validator,
            schemaService,
            mock(AuthzEventPublisher.class),
            audit
        );
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Nested
    class PolicyEmissions {

        @Test
        void create_emits_POLICY_CREATED_with_null_old_and_populated_new_snapshot() {
            AuthorizationPolicy created = policyService.create(
                CALLER,
                new CreatePolicyCommand(ENV, "global", AuthorizationPolicyKind.GLOBAL, null, "permit")
            );

            assertThat(audit.entries()).hasSize(1);
            AuthzAuditEntry entry = audit.entries().get(0);
            assertThat(entry.event()).isEqualTo(AuthzPolicyAuditEvent.POLICY_CREATED);
            assertThat(entry.referenceKind()).isEqualTo(AuthzAuditReferenceKind.POLICY);
            assertThat(entry.referenceId()).isEqualTo(created.id());
            assertThat(entry.oldSnapshot()).isNull();
            assertThat(entry.newSnapshot()).isInstanceOf(PolicyAuditSnapshot.class);
            assertThat(((PolicyAuditSnapshot) entry.newSnapshot()).id()).isEqualTo(created.id());
            assertThat(((PolicyAuditSnapshot) entry.newSnapshot()).name()).isEqualTo("global");
            assertThat(entry.caller()).isEqualTo(CALLER);
        }

        @Test
        void update_emits_POLICY_UPDATED_with_both_snapshots() {
            AuthorizationPolicy created = policyService.create(
                CALLER,
                new CreatePolicyCommand(ENV, "global", AuthorizationPolicyKind.GLOBAL, null, "permit")
            );
            audit.clear();

            policyService.update(CALLER, created.id(), new UpdatePolicyCommand("global", "deny"));

            assertThat(audit.entries()).hasSize(1);
            AuthzAuditEntry entry = audit.entries().get(0);
            assertThat(entry.event()).isEqualTo(AuthzPolicyAuditEvent.POLICY_UPDATED);
            assertThat(entry.oldSnapshot()).isNotNull();
            assertThat(entry.newSnapshot()).isNotNull();
        }

        @Test
        void deploy_emits_POLICY_DEPLOYED() {
            AuthorizationPolicy created = policyService.create(
                CALLER,
                new CreatePolicyCommand(ENV, "global", AuthorizationPolicyKind.GLOBAL, null, "permit")
            );
            audit.clear();

            policyService.deploy(CALLER, created.id());

            assertThat(audit.entries()).hasSize(1);
            assertThat(audit.entries().get(0).event()).isEqualTo(AuthzPolicyAuditEvent.POLICY_DEPLOYED);
        }

        @Test
        void disable_emits_POLICY_DISABLED() {
            AuthorizationPolicy created = policyService.create(
                CALLER,
                new CreatePolicyCommand(ENV, "global", AuthorizationPolicyKind.GLOBAL, null, "permit")
            );
            policyService.deploy(CALLER, created.id());
            audit.clear();

            policyService.disable(CALLER, created.id());

            assertThat(audit.entries()).hasSize(1);
            assertThat(audit.entries().get(0).event()).isEqualTo(AuthzPolicyAuditEvent.POLICY_DISABLED);
        }

        @Test
        void delete_emits_POLICY_DELETED_with_pre_delete_snapshot_and_null_new() {
            AuthorizationPolicy created = policyService.create(
                CALLER,
                new CreatePolicyCommand(ENV, "global", AuthorizationPolicyKind.GLOBAL, null, "permit")
            );
            audit.clear();

            policyService.delete(CALLER, created.id());

            assertThat(audit.entries()).hasSize(1);
            AuthzAuditEntry entry = audit.entries().get(0);
            assertThat(entry.event()).isEqualTo(AuthzPolicyAuditEvent.POLICY_DELETED);
            assertThat(entry.oldSnapshot()).isInstanceOf(PolicyAuditSnapshot.class);
            assertThat(entry.newSnapshot()).isNull();
            assertThat(entry.referenceId()).isEqualTo(created.id());
        }

        @Test
        void noop_delete_of_missing_policy_does_not_emit_audit() {
            policyService.delete(CALLER, "missing-id");

            assertThat(audit.eventsFor(AuthzAuditReferenceKind.POLICY)).isEmpty();
        }
    }

    @Nested
    class EntityEmissions {

        @Test
        void upsert_create_emits_ENTITY_CREATED_with_null_old() {
            UpsertResult result = entityService.upsert(
                CALLER,
                new CreateOrReplaceEntityCommand(ENV, "api.123", AuthorizationEntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim")
            );

            assertThat(audit.entries()).hasSize(1);
            AuthzAuditEntry entry = audit.entries().get(0);
            assertThat(entry.event()).isEqualTo(AuthzEntityAuditEvent.ENTITY_CREATED);
            assertThat(entry.referenceKind()).isEqualTo(AuthzAuditReferenceKind.ENTITY);
            assertThat(entry.referenceId()).isEqualTo("api.123");
            assertThat(entry.oldSnapshot()).isNull();
            assertThat(entry.newSnapshot()).isInstanceOf(EntityAuditSnapshot.class);
            assertThat(result.created()).isTrue();
        }

        @Test
        void upsert_replace_emits_ENTITY_UPDATED_with_both_snapshots() {
            entityService.upsert(
                CALLER,
                new CreateOrReplaceEntityCommand(ENV, "api.123", AuthorizationEntityKind.RESOURCE, Map.of(), List.of(), "apim")
            );
            audit.clear();

            entityService.upsert(
                CALLER,
                new CreateOrReplaceEntityCommand(ENV, "api.123", AuthorizationEntityKind.RESOURCE, Map.of("k", "v2"), List.of(), "apim")
            );

            assertThat(audit.entries()).hasSize(1);
            AuthzAuditEntry entry = audit.entries().get(0);
            assertThat(entry.event()).isEqualTo(AuthzEntityAuditEvent.ENTITY_UPDATED);
            assertThat(entry.oldSnapshot()).isNotNull();
            assertThat(entry.newSnapshot()).isNotNull();
        }

        @Test
        void update_emits_ENTITY_UPDATED_with_both_snapshots() {
            AuthorizationEntity created = entityService
                .upsert(
                    CALLER,
                    new CreateOrReplaceEntityCommand(ENV, "api.123", AuthorizationEntityKind.RESOURCE, Map.of(), List.of(), "apim")
                )
                .entity();
            audit.clear();

            entityService.update(CALLER, created.entityId(), new UpdateEntityCommand(Map.of("k", "v"), List.of()));

            assertThat(audit.entries()).hasSize(1);
            assertThat(audit.entries().get(0).event()).isEqualTo(AuthzEntityAuditEvent.ENTITY_UPDATED);
        }

        @Test
        void delete_emits_ENTITY_DELETED_with_pre_delete_snapshot() {
            entityService.upsert(
                CALLER,
                new CreateOrReplaceEntityCommand(ENV, "api.123", AuthorizationEntityKind.RESOURCE, Map.of(), List.of(), "apim")
            );
            audit.clear();

            entityService.delete(CALLER, "api.123");

            List<AuthzAuditEntry> entityEntries = audit.eventsFor(AuthzAuditReferenceKind.ENTITY);
            assertThat(entityEntries).hasSize(1);
            AuthzAuditEntry entry = entityEntries.get(0);
            assertThat(entry.event()).isEqualTo(AuthzEntityAuditEvent.ENTITY_DELETED);
            assertThat(entry.oldSnapshot()).isInstanceOf(EntityAuditSnapshot.class);
            assertThat(entry.newSnapshot()).isNull();
        }
    }

    @Nested
    class CascadeEmissions {

        @Test
        void cascade_delete_emits_one_entity_entry_per_affected_row() {
            entityService.upsert(
                CALLER,
                new CreateOrReplaceEntityCommand(ENV, "api.svc", AuthorizationEntityKind.RESOURCE, Map.of(), List.of(), "apim")
            );
            entityService.upsert(
                CALLER,
                new CreateOrReplaceEntityCommand(ENV, "api.svc.endpoint1", AuthorizationEntityKind.RESOURCE, Map.of(), List.of(), "apim")
            );
            entityService.upsert(
                CALLER,
                new CreateOrReplaceEntityCommand(ENV, "api.svc.endpoint2", AuthorizationEntityKind.RESOURCE, Map.of(), List.of(), "apim")
            );
            audit.clear();

            entityService.delete(CALLER, "api.svc");

            List<AuthzAuditEntry> entityEntries = audit.eventsFor(AuthzAuditReferenceKind.ENTITY);
            assertThat(entityEntries).hasSize(3);
            assertThat(entityEntries).allMatch(e -> e.event() == AuthzEntityAuditEvent.ENTITY_DELETED);
            assertThat(entityEntries.stream().map(AuthzAuditEntry::referenceId).toList()).containsExactlyInAnyOrder(
                "api.svc",
                "api.svc.endpoint1",
                "api.svc.endpoint2"
            );
        }

        @Test
        void cascade_delete_emits_one_policy_entry_per_attached_policy() {
            entityService.upsert(
                CALLER,
                new CreateOrReplaceEntityCommand(ENV, "api.svc", AuthorizationEntityKind.RESOURCE, Map.of(), List.of(), "apim")
            );
            AuthorizationPolicy attached = policyService.create(
                CALLER,
                new CreatePolicyCommand(ENV, "svc-policy", AuthorizationPolicyKind.RESOURCE, "api.svc", "permit")
            );
            audit.clear();

            entityService.delete(CALLER, "api.svc");

            List<AuthzAuditEntry> policyEntries = audit.eventsFor(AuthzAuditReferenceKind.POLICY);
            assertThat(policyEntries).hasSize(1);
            AuthzAuditEntry entry = policyEntries.get(0);
            assertThat(entry.event()).isEqualTo(AuthzPolicyAuditEvent.POLICY_DELETED);
            assertThat(entry.referenceId()).isEqualTo(attached.id());
        }
    }

    @Nested
    class CallerIdentity {

        @Test
        void system_caller_does_not_emit_audit() {
            AuthzCallerContext system = AuthzCallerContext.system(ENV);

            entityService.upsert(
                system,
                new CreateOrReplaceEntityCommand(ENV, "api.from-system", AuthorizationEntityKind.RESOURCE, Map.of(), List.of(), "apim")
            );

            assertThat(audit.entries()).isEmpty();
        }

        @Test
        void user_caller_metadata_round_trips_into_audit_entry() {
            AuthzCallerContext custom = AuthzCallerContext.ofUser("org-42", "env-99", "bob");

            policyService.create(custom, new CreatePolicyCommand("env-99", "p", AuthorizationPolicyKind.GLOBAL, null, "permit"));

            assertThat(audit.entries()).hasSize(1);
            AuthzCallerContext seen = audit.entries().get(0).caller();
            assertThat(seen.organizationId()).isEqualTo("org-42");
            assertThat(seen.environmentId()).isEqualTo("env-99");
            assertThat(seen.userId()).isEqualTo("bob");
        }
    }
}
