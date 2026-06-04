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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEntityRepository;
import io.gravitee.gamma.authorization.audit.RecordingAuthzAuditPort;
import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.event.RecordingAuthzEventPublisher;
import io.gravitee.gamma.authorization.event.RecordingAuthzEventPublisher.EventKind;
import io.gravitee.gamma.authorization.repository.InMemoryAuthzEntityRepository;
import io.gravitee.gamma.authorization.repository.InMemoryAuthzPolicyRepository;
import io.gravitee.gamma.authorization.service.exception.AuthzCascadeTooLargeException;
import io.gravitee.gamma.authorization.service.exception.AuthzEntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthzEntityServiceImplTest {

    private static final Instant FIXED = Instant.parse("2025-01-01T00:00:00Z");
    private static final String ENV = "env-1";
    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", ENV, "alice");
    public static final int DEFAULT_CASCADE_HARD_LIMIT = 500;

    private InMemoryAuthzEntityRepository entityRepository;
    private InMemoryAuthzPolicyRepository policyRepository;
    private AuthzPolicyServiceImpl policyService;
    private AuthzEntityServiceImpl entityService;
    private RecordingAuthzEventPublisher events;
    private RecordingAuthzAuditPort audit;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(FIXED, ZoneOffset.UTC));
        entityRepository = new InMemoryAuthzEntityRepository();
        policyRepository = new InMemoryAuthzPolicyRepository();
        AuthzEntityIdValidator validator = new AuthzEntityIdValidator();
        events = new RecordingAuthzEventPublisher();
        audit = new RecordingAuthzAuditPort();
        policyService = new AuthzPolicyServiceImpl(policyRepository, validator, events, audit);
        entityService = new AuthzEntityServiceImpl(entityRepository, policyRepository, validator, events, audit);
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void upsert_creates_entity_with_generated_id_and_timestamps() {
        AuthzUpsertResult result = entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim")
        );

        assertThat(result.created()).isTrue();
        assertThat(result.entity().id()).isNotBlank();
        assertThat(result.entity().attributes()).containsEntry("k", "v");
        assertThat(result.entity().createdAt()).isEqualTo(FIXED);
        assertThat(result.entity().updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void upsert_replaces_existing_entity_preserving_id_and_createdAt() {
        AuthzUpsertResult first = entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "api.123", AuthzEntityKind.RESOURCE, Map.of(), List.of(), "apim")
        );

        Instant later = FIXED.plusSeconds(60);
        TimeProvider.overrideClock(Clock.fixed(later, ZoneOffset.UTC));
        AuthzEntityIdValidator validator = new AuthzEntityIdValidator();
        entityService = new AuthzEntityServiceImpl(
            entityRepository,
            policyRepository,
            validator,
            events,
            audit,
            DEFAULT_CASCADE_HARD_LIMIT
        );

        AuthzUpsertResult second = entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(
                ENV,
                "api.123",
                AuthzEntityKind.RESOURCE,
                Map.of("k", "v2"),
                List.of("api.parent"),
                "apim"
            )
        );

        assertThat(second.created()).isFalse();
        assertThat(second.entity().id()).isEqualTo(first.entity().id());
        assertThat(second.entity().attributes()).containsEntry("k", "v2");
        assertThat(second.entity().parents()).containsExactly("api.parent");
        assertThat(second.entity().createdAt()).isEqualTo(FIXED);
        assertThat(second.entity().updatedAt()).isEqualTo(later);
    }

    @Test
    void upsert_without_entityType_defaults_to_kind_default_at_domain_level() {
        AuthzUpsertResult resource = entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "api.123", AuthzEntityKind.RESOURCE, Map.of(), List.of(), "apim")
        );
        AuthzUpsertResult principal = entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "idp.am.alice", AuthzEntityKind.PRINCIPAL, Map.of(), List.of(), "gravitee_am_default")
        );

        assertThat(resource.entity().entityType()).isEqualTo("Resource");
        assertThat(principal.entity().entityType()).isEqualTo("Principal");
    }

    @Test
    void upsert_with_explicit_entityType_persists_typed_value() {
        AuthzUpsertResult result = entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "alice", AuthzEntityKind.PRINCIPAL, "User", Map.of(), List.of(), "apim")
        );

        assertThat(result.entity().entityType()).isEqualTo("User");
    }

    @Test
    void update_preserves_entityType_immutably_across_revisions() {
        entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "alice", AuthzEntityKind.PRINCIPAL, "User", Map.of("k", "v1"), List.of(), "apim")
        );

        AuthzEntity updated = entityService.update(CALLER, "alice", new UpdateAuthzEntityCommand(Map.of("k", "v2"), List.of("admins")));

        assertThat(updated.entityType()).isEqualTo("User");
        assertThat(updated.attributes()).containsEntry("k", "v2");
    }

    @Test
    void update_modifies_attributes_and_parents_only() {
        entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v1"), List.of(), "apim")
        );

        AuthzEntity updated = entityService.update(
            CALLER,
            "api.123",
            new UpdateAuthzEntityCommand(Map.of("k", "v2"), List.of("api.parent"))
        );

        assertThat(updated.attributes()).containsEntry("k", "v2");
        assertThat(updated.parents()).containsExactly("api.parent");
        assertThat(updated.kind()).isEqualTo(AuthzEntityKind.RESOURCE);
        assertThat(updated.source()).isEqualTo("apim");
    }

    @Test
    void update_leaves_null_fields_unchanged() {
        entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(ENV, "api.123", AuthzEntityKind.RESOURCE, Map.of("k", "v1"), List.of("p"), "apim")
        );

        AuthzEntity updated = entityService.update(CALLER, "api.123", new UpdateAuthzEntityCommand(null, null));

        assertThat(updated.attributes()).containsEntry("k", "v1");
        assertThat(updated.parents()).containsExactly("p");
    }

    @Test
    void update_rejects_unknown_entityId() {
        assertThatThrownBy(() -> entityService.update(CALLER, "missing", new UpdateAuthzEntityCommand(Map.of(), List.of()))).isInstanceOf(
            AuthzEntityNotFoundException.class
        );
    }

    @Test
    void find_with_no_filter_returns_all_entities_in_environment() {
        entityService.upsert(CALLER, create("api.1", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("idp.am.alice", AuthzEntityKind.PRINCIPAL, "gravitee_am_default"));

        assertThat(entityService.find(ENV, AuthzEntityFilter.none())).hasSize(2);
    }

    @Test
    void find_filters_by_kind() {
        entityService.upsert(CALLER, create("api.1", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("idp.am.alice", AuthzEntityKind.PRINCIPAL, "gravitee_am_default"));

        assertThat(entityService.find(ENV, new AuthzEntityFilter(AuthzEntityKind.RESOURCE, null, null)))
            .extracting(AuthzEntity::entityId)
            .containsExactly("api.1");
    }

    @Test
    void find_filters_by_source() {
        entityService.upsert(CALLER, create("api.1", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("idp.am.alice", AuthzEntityKind.PRINCIPAL, "gravitee_am_default"));

        assertThat(entityService.find(ENV, new AuthzEntityFilter(null, "apim", null)))
            .extracting(AuthzEntity::entityId)
            .containsExactly("api.1");
    }

    @Test
    void find_filters_by_entityIdPrefix() {
        entityService.upsert(CALLER, create("api.123", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.123.tool-a", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.999", AuthzEntityKind.RESOURCE, "apim"));

        assertThat(entityService.find(ENV, new AuthzEntityFilter(null, null, "api.123")))
            .extracting(AuthzEntity::entityId)
            .containsExactlyInAnyOrder("api.123", "api.123.tool-a");
    }

    @Test
    void find_excludes_entries_matching_excludeEntityIdPrefix() {
        entityService.upsert(CALLER, create("api.123", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("action.api.123.read", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("action.api.123.write", AuthzEntityKind.RESOURCE, "apim"));

        assertThat(entityService.find(ENV, new AuthzEntityFilter(null, null, null, "action.")))
            .extracting(AuthzEntity::entityId)
            .containsExactly("api.123");
    }

    @Test
    void find_combines_kind_and_excludeEntityIdPrefix() {
        entityService.upsert(CALLER, create("api.payments", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("action.api.payments.charge", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("user.alice", AuthzEntityKind.PRINCIPAL, "scim"));

        assertThat(entityService.find(ENV, new AuthzEntityFilter(AuthzEntityKind.RESOURCE, null, null, "action.")))
            .extracting(AuthzEntity::entityId)
            .containsExactly("api.payments");
    }

    @Test
    void delete_removes_target_entity_and_returns_it_in_cascade_result() {
        entityService.upsert(CALLER, create("api.123", AuthzEntityKind.RESOURCE, "apim"));

        AuthzCascadeResult result = entityService.delete(CALLER, "api.123");

        assertThat(result.deletedEntityIds()).containsExactly("api.123");
        assertThat(result.deletedPolicyIds()).isEmpty();
        assertThat(entityRepository.findByEntityId(ENV, "api.123")).isEmpty();
    }

    @Test
    void delete_cascades_to_descendants_under_the_entityId_prefix() {
        entityService.upsert(CALLER, create("api.123", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.123.tool-a", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.123.tool-b", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.999", AuthzEntityKind.RESOURCE, "apim"));

        AuthzCascadeResult result = entityService.delete(CALLER, "api.123");

        assertThat(result.deletedEntityIds()).containsExactlyInAnyOrder("api.123", "api.123.tool-a", "api.123.tool-b");
        assertThat(entityRepository.findByEntityId(ENV, "api.999")).isPresent();
    }

    @Test
    void delete_cascades_to_mcp_alias_when_target_is_api_dot_apiId() {
        entityService.upsert(CALLER, create("api.bookings", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-1", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-2", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-3", AuthzEntityKind.RESOURCE, "apim"));

        AuthzCascadeResult result = entityService.delete(CALLER, "api.bookings");

        assertThat(result.deletedEntityIds()).containsExactlyInAnyOrder(
            "api.bookings",
            "mcp.bookings.tool-1",
            "mcp.bookings.tool-2",
            "mcp.bookings.tool-3"
        );
    }

    @Test
    void delete_cascades_to_RESOURCE_policies_referencing_any_affected_entityId() {
        entityService.upsert(CALLER, create("api.bookings", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-1", AuthzEntityKind.RESOURCE, "apim"));

        var server = policyService.create(
            CALLER,
            new CreateAuthzPolicyCommand(ENV, "p-server", AuthzPolicyKind.RESOURCE, "api.bookings", "")
        );
        var tool = policyService.create(
            CALLER,
            new CreateAuthzPolicyCommand(ENV, "p-tool", AuthzPolicyKind.RESOURCE, "mcp.bookings.tool-1", "")
        );
        var global = policyService.create(CALLER, new CreateAuthzPolicyCommand(ENV, "g", AuthzPolicyKind.GLOBAL, null, ""));

        AuthzCascadeResult result = entityService.delete(CALLER, "api.bookings");

        assertThat(result.deletedPolicyIds()).containsExactlyInAnyOrder(server.id(), tool.id());
        assertThat(policyRepository.findById(ENV, global.id())).isPresent();
    }

    @Test
    void delete_cascade_total_matches_architecture_example_one_server_five_tools_fifty_policies() {
        entityService.upsert(CALLER, create("api.example", AuthzEntityKind.RESOURCE, "apim"));
        for (int i = 1; i <= 5; i++) {
            entityService.upsert(CALLER, create("mcp.example.tool-" + i, AuthzEntityKind.RESOURCE, "apim"));
        }
        policyService.create(CALLER, new CreateAuthzPolicyCommand(ENV, "p-server", AuthzPolicyKind.RESOURCE, "api.example", ""));
        for (int i = 1; i <= 5; i++) {
            policyService.create(
                CALLER,
                new CreateAuthzPolicyCommand(ENV, "p-tool-" + i, AuthzPolicyKind.RESOURCE, "mcp.example.tool-" + i, "")
            );
        }

        AuthzCascadeResult result = entityService.delete(CALLER, "api.example");

        assertThat(result.deletedEntityIds()).hasSize(6);
        assertThat(result.deletedPolicyIds()).hasSize(6);
        assertThat(result.totalAffected()).isEqualTo(12);
    }

    @Test
    void delete_does_not_remove_aliased_mcp_when_target_is_a_descendant_not_the_api() {
        entityService.upsert(CALLER, create("api.bookings", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.bookings.foo", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-1", AuthzEntityKind.RESOURCE, "apim"));

        AuthzCascadeResult result = entityService.delete(CALLER, "api.bookings.foo");

        assertThat(result.deletedEntityIds()).containsExactly("api.bookings.foo");
        assertThat(entityRepository.findByEntityId(ENV, "mcp.bookings.tool-1")).isPresent();
    }

    @Test
    void delete_of_unknown_entityId_returns_empty_cascade() {
        AuthzCascadeResult result = entityService.delete(CALLER, "api.never-existed");
        assertThat(result.totalAffected()).isZero();
    }

    @Test
    void delete_cascades_to_tools_linked_by_parents_even_outside_the_entityId_prefix() {
        entityService.upsert(CALLER, create("mcp.filesystem", AuthzEntityKind.RESOURCE, "gravitee-catalog"));
        entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(
                ENV,
                "mcptool.read_file",
                AuthzEntityKind.RESOURCE,
                Map.of(),
                List.of("mcp.filesystem"),
                "gravitee-catalog"
            )
        );
        entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(
                ENV,
                "mcptool.write_file",
                AuthzEntityKind.RESOURCE,
                Map.of(),
                List.of("mcp.filesystem"),
                "gravitee-catalog"
            )
        );
        entityService.upsert(
            CALLER,
            new CreateOrReplaceAuthzEntityCommand(
                ENV,
                "mcptool.list_repos",
                AuthzEntityKind.RESOURCE,
                Map.of(),
                List.of("mcp.github"),
                "gravitee-catalog"
            )
        );

        AuthzCascadeResult result = entityService.delete(CALLER, "mcp.filesystem");

        assertThat(result.deletedEntityIds()).containsExactlyInAnyOrder(
            "mcp.filesystem",
            "mcptool.read_file",
            "mcptool.write_file"
        );
        assertThat(entityRepository.findByEntityId(ENV, "mcptool.list_repos")).isPresent();
    }

    private static CreateOrReplaceAuthzEntityCommand create(String entityId, AuthzEntityKind kind, String source) {
        return new CreateOrReplaceAuthzEntityCommand(ENV, entityId, kind, Map.of(), List.of(), source);
    }

    @Test
    void upsert_emits_PUBLISH_AUTHZ_ENTITY_with_the_full_entity_payload() {
        events.clear();

        AuthzUpsertResult result = entityService.upsert(CALLER, create("api.123", AuthzEntityKind.RESOURCE, "apim"));

        assertThat(events.events()).hasSize(1);
        RecordingAuthzEventPublisher.Recorded event = events.events().get(0);
        assertThat(event.kind()).isEqualTo(EventKind.ENTITY_PUBLISHED);
        assertThat(event.environmentId()).isEqualTo(ENV);
        assertThat(event.id()).isEqualTo("api.123");
        assertThat(event.entity()).isEqualTo(result.entity());
    }

    @Test
    void upsert_replace_emits_a_second_PUBLISH_AUTHZ_ENTITY() {
        entityService.upsert(CALLER, create("api.123", AuthzEntityKind.RESOURCE, "apim"));
        events.clear();

        entityService.upsert(CALLER, create("api.123", AuthzEntityKind.RESOURCE, "apim"));

        assertThat(events.events()).hasSize(1);
        assertThat(events.events().get(0).kind()).isEqualTo(EventKind.ENTITY_PUBLISHED);
    }

    @Test
    void update_emits_PUBLISH_AUTHZ_ENTITY_so_the_gateway_picks_up_the_attribute_change() {
        entityService.upsert(CALLER, create("api.123", AuthzEntityKind.RESOURCE, "apim"));
        events.clear();

        entityService.update(CALLER, "api.123", new UpdateAuthzEntityCommand(Map.of("k", "v"), null));

        assertThat(events.events()).hasSize(1);
        RecordingAuthzEventPublisher.Recorded event = events.events().get(0);
        assertThat(event.kind()).isEqualTo(EventKind.ENTITY_PUBLISHED);
        assertThat(event.entity().attributes()).containsEntry("k", "v");
    }

    @Test
    void cascade_delete_emits_one_UNPUBLISH_per_affected_entity_then_one_per_affected_policy() {
        entityService.upsert(CALLER, create("api.bookings", AuthzEntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-1", AuthzEntityKind.RESOURCE, "apim"));
        var policy = policyService.create(CALLER, new CreateAuthzPolicyCommand(ENV, "p", AuthzPolicyKind.RESOURCE, "api.bookings", ""));
        policyService.deploy(CALLER, policy.id());
        events.clear();

        entityService.delete(CALLER, "api.bookings");

        assertThat(events.events()).hasSize(3);
        assertThat(events.events().get(0).kind()).isEqualTo(EventKind.ENTITY_UNPUBLISHED);
        assertThat(events.events().get(1).kind()).isEqualTo(EventKind.ENTITY_UNPUBLISHED);
        assertThat(events.events().get(2).kind()).isEqualTo(EventKind.POLICY_UNPUBLISHED);

        assertThat(events.events())
            .filteredOn(e -> e.kind() == EventKind.ENTITY_UNPUBLISHED)
            .extracting(RecordingAuthzEventPublisher.Recorded::id)
            .containsExactly("api.bookings", "mcp.bookings.tool-1");

        assertThat(events.events())
            .filteredOn(e -> e.kind() == EventKind.POLICY_UNPUBLISHED)
            .extracting(RecordingAuthzEventPublisher.Recorded::id)
            .containsExactly(policy.id());
    }

    @Test
    void cascade_delete_of_unknown_entityId_emits_no_events() {
        events.clear();

        entityService.delete(CALLER, "api.never-existed");

        assertThat(events.events()).isEmpty();
    }

    @Test
    void upsert_does_not_swallow_AuthzEventPublishException_during_retry() {
        var failingPublisher = new io.gravitee.gamma.authorization.api.AuthzEventPublisher() {
            int calls = 0;

            @Override
            public void publishPolicyDeployed(io.gravitee.gamma.authorization.domain.AuthzPolicy policy) {}

            @Override
            public void unpublishPolicy(io.gravitee.gamma.authorization.domain.AuthzPolicy policy) {}

            @Override
            public void publishEntityUpserted(io.gravitee.gamma.authorization.domain.AuthzEntity entity) {
                calls++;
                throw new io.gravitee.gamma.authorization.event.AuthzEventPublishException(
                    "simulated publish failure",
                    new RuntimeException("underlying I/O")
                );
            }

            @Override
            public void unpublishEntity(io.gravitee.gamma.authorization.domain.AuthzEntity entity) {}
        };
        AuthzEntityIdValidator validator = new AuthzEntityIdValidator();
        AuthzEntityServiceImpl failingService = new AuthzEntityServiceImpl(
            entityRepository,
            policyRepository,
            validator,
            failingPublisher,
            audit,
            DEFAULT_CASCADE_HARD_LIMIT
        );

        assertThatThrownBy(() -> failingService.upsert(CALLER, create("api.test", AuthzEntityKind.RESOURCE, "apim"))).isInstanceOf(
            io.gravitee.gamma.authorization.event.AuthzEventPublishException.class
        );

        assertThat(failingPublisher.calls).isEqualTo(1);
    }

    @Test
    void upsert_propagates_AuthzEventPublishException_thrown_after_successful_retry_save() {
        var racingRepo = new DuplicateOnFirstSaveEntityRepository(entityRepository);
        var failingPublisher = new io.gravitee.gamma.authorization.api.AuthzEventPublisher() {
            int calls = 0;

            @Override
            public void publishPolicyDeployed(io.gravitee.gamma.authorization.domain.AuthzPolicy policy) {}

            @Override
            public void unpublishPolicy(io.gravitee.gamma.authorization.domain.AuthzPolicy policy) {}

            @Override
            public void publishEntityUpserted(io.gravitee.gamma.authorization.domain.AuthzEntity entity) {
                calls++;
                throw new io.gravitee.gamma.authorization.event.AuthzEventPublishException(
                    "simulated retry-time failure",
                    new RuntimeException("underlying I/O")
                );
            }

            @Override
            public void unpublishEntity(io.gravitee.gamma.authorization.domain.AuthzEntity entity) {}
        };
        AuthzEntityIdValidator validator = new AuthzEntityIdValidator();
        AuthzEntityServiceImpl racingService = new AuthzEntityServiceImpl(
            racingRepo,
            policyRepository,
            validator,
            failingPublisher,
            audit,
            DEFAULT_CASCADE_HARD_LIMIT
        );

        assertThatThrownBy(() -> racingService.upsert(CALLER, create("api.race", AuthzEntityKind.RESOURCE, "apim"))).isInstanceOf(
            io.gravitee.gamma.authorization.event.AuthzEventPublishException.class
        );

        assertThat(failingPublisher.calls).isEqualTo(1);
        assertThat(racingRepo.saveCalls).isEqualTo(2);
    }

    private static final class DuplicateOnFirstSaveEntityRepository implements AuthzEntityRepository {

        private final AuthzEntityRepository delegate;
        int saveCalls = 0;

        DuplicateOnFirstSaveEntityRepository(AuthzEntityRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public AuthzEntity save(AuthzEntity entity) {
            saveCalls++;
            if (saveCalls == 1) {
                throw new org.springframework.dao.DuplicateKeyException("simulated unique-violation race");
            }
            return delegate.save(entity);
        }

        @Override
        public java.util.Optional<AuthzEntity> findById(String environmentId, String id) {
            return delegate.findById(environmentId, id);
        }

        @Override
        public java.util.Optional<AuthzEntity> findByEntityId(String environmentId, String entityId) {
            return delegate.findByEntityId(environmentId, entityId);
        }

        @Override
        public List<AuthzEntity> findAll(String environmentId) {
            return delegate.findAll(environmentId);
        }

        @Override
        public List<AuthzEntity> findByKind(String environmentId, AuthzEntityKind kind) {
            return delegate.findByKind(environmentId, kind);
        }

        @Override
        public List<AuthzEntity> findByEntityIdPrefix(String environmentId, String prefix) {
            return delegate.findByEntityIdPrefix(environmentId, prefix);
        }

        @Override
        public boolean deleteById(String environmentId, String id) {
            return delegate.deleteById(environmentId, id);
        }

        @Override
        public boolean deleteByEntityId(String environmentId, String entityId) {
            return delegate.deleteByEntityId(environmentId, entityId);
        }
    }

    @Test
    void bulkUpsert_creates_all_entities_in_one_call() {
        List<AuthzUpsertResult> results = entityService.bulkUpsert(
            CALLER,
            List.of(create("api.1", AuthzEntityKind.RESOURCE, "apim"), create("mcp.1.tool-a", AuthzEntityKind.RESOURCE, "apim"))
        );

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(AuthzUpsertResult::created);
        assertThat(entityRepository.findAll(ENV)).extracting(AuthzEntity::entityId).containsExactlyInAnyOrder("api.1", "mcp.1.tool-a");
    }

    @Test
    void bulkUpsert_replaces_existing_entities_preserving_id_and_createdAt() {
        AuthzUpsertResult first = entityService.upsert(CALLER, create("api.1", AuthzEntityKind.RESOURCE, "apim"));
        Instant later = FIXED.plusSeconds(60);
        TimeProvider.overrideClock(Clock.fixed(later, ZoneOffset.UTC));

        List<AuthzUpsertResult> results = entityService.bulkUpsert(
            CALLER,
            List.of(
                new CreateOrReplaceAuthzEntityCommand(ENV, "api.1", AuthzEntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim"),
                create("api.2", AuthzEntityKind.RESOURCE, "apim")
            )
        );

        assertThat(results.get(0).created()).isFalse();
        assertThat(results.get(0).entity().id()).isEqualTo(first.entity().id());
        assertThat(results.get(0).entity().createdAt()).isEqualTo(FIXED);
        assertThat(results.get(0).entity().updatedAt()).isEqualTo(later);
        assertThat(results.get(1).created()).isTrue();
    }

    @Test
    void bulkUpsert_with_empty_list_is_a_no_op() {
        List<AuthzUpsertResult> results = entityService.bulkUpsert(CALLER, List.of());

        assertThat(results).isEmpty();
        assertThat(entityRepository.findAll(ENV)).isEmpty();
        assertThat(events.events()).isEmpty();
    }

    @Test
    void bulkUpsert_emits_one_event_per_entity() {
        events.clear();

        entityService.bulkUpsert(
            CALLER,
            List.of(create("api.1", AuthzEntityKind.RESOURCE, "apim"), create("api.2", AuthzEntityKind.RESOURCE, "apim"))
        );

        assertThat(events.events()).hasSize(2);
        assertThat(events.events()).allMatch(e -> e.kind() == EventKind.ENTITY_PUBLISHED);
        assertThat(events.events()).extracting(RecordingAuthzEventPublisher.Recorded::id).containsExactly("api.1", "api.2");
    }

    @Test
    void cascade_delete_above_hard_limit_throws_CascadeTooLargeException() {
        entityService.upsert(CALLER, create("api.huge", AuthzEntityKind.RESOURCE, "apim"));
        for (int i = 0; i < DEFAULT_CASCADE_HARD_LIMIT + 1; i++) {
            entityService.upsert(CALLER, create("mcp.huge.tool-" + i, AuthzEntityKind.RESOURCE, "apim"));
        }
        events.clear();

        assertThatThrownBy(() -> entityService.delete(CALLER, "api.huge")).isInstanceOf(AuthzCascadeTooLargeException.class);

        assertThat(entityRepository.findByEntityId(ENV, "api.huge")).isPresent();
        assertThat(events.events()).isEmpty();
    }
}
