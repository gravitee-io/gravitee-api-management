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
package io.gravitee.apim.authorization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.authorization.api.AuthzCallerContext;
import io.gravitee.apim.authorization.audit.RecordingAuthzAuditPort;
import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.domain.EntityKind;
import io.gravitee.apim.authorization.domain.PolicyKind;
import io.gravitee.apim.authorization.event.RecordingAuthzEventPublisher;
import io.gravitee.apim.authorization.event.RecordingAuthzEventPublisher.EventKind;
import io.gravitee.apim.authorization.repository.InMemoryEntityRepository;
import io.gravitee.apim.authorization.repository.InMemoryPolicyRepository;
import io.gravitee.apim.authorization.service.exception.EntityNotFoundException;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntityServiceImplTest {

    private static final Instant FIXED = Instant.parse("2025-01-01T00:00:00Z");
    private static final String ENV = "env-1";
    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", ENV, "alice");
    public static final int DEFAULT_CASCADE_HARD_LIMIT = 500;

    private InMemoryEntityRepository entityRepository;
    private InMemoryPolicyRepository policyRepository;
    private PolicyServiceImpl policyService;
    private EntityServiceImpl entityService;
    private RecordingAuthzEventPublisher events;
    private RecordingAuthzAuditPort audit;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(FIXED, ZoneOffset.UTC));
        entityRepository = new InMemoryEntityRepository();
        policyRepository = new InMemoryPolicyRepository();
        EntityIdValidator validator = new EntityIdValidator();
        events = new RecordingAuthzEventPublisher();
        audit = new RecordingAuthzAuditPort();
        SchemaServiceImpl schemaService = new SchemaServiceImpl(entityRepository, policyRepository);
        policyService = new PolicyServiceImpl(policyRepository, validator, schemaService, events, audit);
        entityService = new EntityServiceImpl(entityRepository, policyRepository, validator, schemaService, events, audit);
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void upsert_creates_entity_with_generated_id_and_timestamps() {
        UpsertResult result = entityService.upsert(
            CALLER,
            new CreateOrReplaceEntityCommand(ENV, "api.123", EntityKind.RESOURCE, Map.of("k", "v"), List.of(), "apim")
        );

        assertThat(result.created()).isTrue();
        assertThat(result.entity().id()).isNotBlank();
        assertThat(result.entity().attributes()).containsEntry("k", "v");
        assertThat(result.entity().createdAt()).isEqualTo(FIXED);
        assertThat(result.entity().updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void upsert_replaces_existing_entity_preserving_id_and_createdAt() {
        UpsertResult first = entityService.upsert(
            CALLER,
            new CreateOrReplaceEntityCommand(ENV, "api.123", EntityKind.RESOURCE, Map.of(), List.of(), "apim")
        );

        Instant later = FIXED.plusSeconds(60);
        TimeProvider.overrideClock(Clock.fixed(later, ZoneOffset.UTC));
        EntityIdValidator validator = new EntityIdValidator();
        SchemaServiceImpl schemaService = new SchemaServiceImpl(entityRepository, policyRepository);
        entityService = new EntityServiceImpl(
            entityRepository,
            policyRepository,
            validator,
            schemaService,
            events,
            audit,
            DEFAULT_CASCADE_HARD_LIMIT
        );

        UpsertResult second = entityService.upsert(
            CALLER,
            new CreateOrReplaceEntityCommand(ENV, "api.123", EntityKind.RESOURCE, Map.of("k", "v2"), List.of("api.parent"), "apim")
        );

        assertThat(second.created()).isFalse();
        assertThat(second.entity().id()).isEqualTo(first.entity().id());
        assertThat(second.entity().attributes()).containsEntry("k", "v2");
        assertThat(second.entity().parents()).containsExactly("api.parent");
        assertThat(second.entity().createdAt()).isEqualTo(FIXED);
        assertThat(second.entity().updatedAt()).isEqualTo(later);
    }

    @Test
    void update_modifies_attributes_and_parents_only() {
        entityService.upsert(
            CALLER,
            new CreateOrReplaceEntityCommand(ENV, "api.123", EntityKind.RESOURCE, Map.of("k", "v1"), List.of(), "apim")
        );

        Entity updated = entityService.update(CALLER, "api.123", new UpdateEntityCommand(Map.of("k", "v2"), List.of("api.parent")));

        assertThat(updated.attributes()).containsEntry("k", "v2");
        assertThat(updated.parents()).containsExactly("api.parent");
        assertThat(updated.kind()).isEqualTo(EntityKind.RESOURCE);
        assertThat(updated.source()).isEqualTo("apim");
    }

    @Test
    void update_leaves_null_fields_unchanged() {
        entityService.upsert(
            CALLER,
            new CreateOrReplaceEntityCommand(ENV, "api.123", EntityKind.RESOURCE, Map.of("k", "v1"), List.of("p"), "apim")
        );

        Entity updated = entityService.update(CALLER, "api.123", new UpdateEntityCommand(null, null));

        assertThat(updated.attributes()).containsEntry("k", "v1");
        assertThat(updated.parents()).containsExactly("p");
    }

    @Test
    void update_rejects_unknown_entityId() {
        assertThatThrownBy(() -> entityService.update(CALLER, "missing", new UpdateEntityCommand(Map.of(), List.of()))).isInstanceOf(
            EntityNotFoundException.class
        );
    }

    @Test
    void find_with_no_filter_returns_all_entities_in_environment() {
        entityService.upsert(CALLER, create("api.1", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("idp.am.alice", EntityKind.PRINCIPAL, "gravitee_am_default"));

        assertThat(entityService.find(ENV, EntityFilter.none())).hasSize(2);
    }

    @Test
    void find_filters_by_kind() {
        entityService.upsert(CALLER, create("api.1", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("idp.am.alice", EntityKind.PRINCIPAL, "gravitee_am_default"));

        assertThat(entityService.find(ENV, new EntityFilter(EntityKind.RESOURCE, null, null)))
            .extracting(Entity::entityId)
            .containsExactly("api.1");
    }

    @Test
    void find_filters_by_source() {
        entityService.upsert(CALLER, create("api.1", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("idp.am.alice", EntityKind.PRINCIPAL, "gravitee_am_default"));

        assertThat(entityService.find(ENV, new EntityFilter(null, "apim", null))).extracting(Entity::entityId).containsExactly("api.1");
    }

    @Test
    void find_filters_by_entityIdPrefix() {
        entityService.upsert(CALLER, create("api.123", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.123.tool-a", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.999", EntityKind.RESOURCE, "apim"));

        assertThat(entityService.find(ENV, new EntityFilter(null, null, "api.123")))
            .extracting(Entity::entityId)
            .containsExactlyInAnyOrder("api.123", "api.123.tool-a");
    }

    @Test
    void delete_removes_target_entity_and_returns_it_in_cascade_result() {
        entityService.upsert(CALLER, create("api.123", EntityKind.RESOURCE, "apim"));

        CascadeResult result = entityService.delete(CALLER, "api.123");

        assertThat(result.deletedEntityIds()).containsExactly("api.123");
        assertThat(result.deletedPolicyIds()).isEmpty();
        assertThat(entityRepository.findByEntityId(ENV, "api.123")).isEmpty();
    }

    @Test
    void delete_cascades_to_descendants_under_the_entityId_prefix() {
        entityService.upsert(CALLER, create("api.123", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.123.tool-a", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.123.tool-b", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.999", EntityKind.RESOURCE, "apim"));

        CascadeResult result = entityService.delete(CALLER, "api.123");

        assertThat(result.deletedEntityIds()).containsExactlyInAnyOrder("api.123", "api.123.tool-a", "api.123.tool-b");
        assertThat(entityRepository.findByEntityId(ENV, "api.999")).isPresent();
    }

    @Test
    void delete_cascades_to_mcp_alias_when_target_is_api_dot_apiId() {
        entityService.upsert(CALLER, create("api.bookings", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-1", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-2", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-3", EntityKind.RESOURCE, "apim"));

        CascadeResult result = entityService.delete(CALLER, "api.bookings");

        assertThat(result.deletedEntityIds()).containsExactlyInAnyOrder(
            "api.bookings",
            "mcp.bookings.tool-1",
            "mcp.bookings.tool-2",
            "mcp.bookings.tool-3"
        );
    }

    @Test
    void delete_cascades_to_RESOURCE_policies_referencing_any_affected_entityId() {
        entityService.upsert(CALLER, create("api.bookings", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-1", EntityKind.RESOURCE, "apim"));

        var server = policyService.create(CALLER, new CreatePolicyCommand(ENV, "p-server", PolicyKind.RESOURCE, "api.bookings", ""));
        var tool = policyService.create(CALLER, new CreatePolicyCommand(ENV, "p-tool", PolicyKind.RESOURCE, "mcp.bookings.tool-1", ""));
        var global = policyService.create(CALLER, new CreatePolicyCommand(ENV, "g", PolicyKind.GLOBAL, null, ""));

        CascadeResult result = entityService.delete(CALLER, "api.bookings");

        assertThat(result.deletedPolicyIds()).containsExactlyInAnyOrder(server.id(), tool.id());
        assertThat(policyRepository.findById(ENV, global.id())).isPresent();
    }

    @Test
    void delete_cascade_total_matches_architecture_example_one_server_five_tools_fifty_policies() {
        entityService.upsert(CALLER, create("api.example", EntityKind.RESOURCE, "apim"));
        for (int i = 1; i <= 5; i++) {
            entityService.upsert(CALLER, create("mcp.example.tool-" + i, EntityKind.RESOURCE, "apim"));
        }
        policyService.create(CALLER, new CreatePolicyCommand(ENV, "p-server", PolicyKind.RESOURCE, "api.example", ""));
        for (int i = 1; i <= 5; i++) {
            policyService.create(CALLER, new CreatePolicyCommand(ENV, "p-tool-" + i, PolicyKind.RESOURCE, "mcp.example.tool-" + i, ""));
        }

        CascadeResult result = entityService.delete(CALLER, "api.example");

        assertThat(result.deletedEntityIds()).hasSize(6);
        assertThat(result.deletedPolicyIds()).hasSize(6);
        assertThat(result.totalAffected()).isEqualTo(12);
    }

    @Test
    void delete_does_not_remove_aliased_mcp_when_target_is_a_descendant_not_the_api() {
        entityService.upsert(CALLER, create("api.bookings", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("api.bookings.foo", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-1", EntityKind.RESOURCE, "apim"));

        CascadeResult result = entityService.delete(CALLER, "api.bookings.foo");

        assertThat(result.deletedEntityIds()).containsExactly("api.bookings.foo");
        assertThat(entityRepository.findByEntityId(ENV, "mcp.bookings.tool-1")).isPresent();
    }

    @Test
    void delete_of_unknown_entityId_returns_empty_cascade() {
        CascadeResult result = entityService.delete(CALLER, "api.never-existed");
        assertThat(result.totalAffected()).isZero();
    }

    private static CreateOrReplaceEntityCommand create(String entityId, EntityKind kind, String source) {
        return new CreateOrReplaceEntityCommand(ENV, entityId, kind, Map.of(), List.of(), source);
    }

    @Test
    void upsert_emits_PUBLISH_AUTHZ_ENTITY_with_the_full_entity_payload() {
        events.clear();

        UpsertResult result = entityService.upsert(CALLER, create("api.123", EntityKind.RESOURCE, "apim"));

        assertThat(events.events()).hasSize(1);
        RecordingAuthzEventPublisher.Recorded event = events.events().get(0);
        assertThat(event.kind()).isEqualTo(EventKind.ENTITY_PUBLISHED);
        assertThat(event.environmentId()).isEqualTo(ENV);
        assertThat(event.id()).isEqualTo("api.123");
        assertThat(event.entity()).isEqualTo(result.entity());
    }

    @Test
    void upsert_replace_emits_a_second_PUBLISH_AUTHZ_ENTITY() {
        entityService.upsert(CALLER, create("api.123", EntityKind.RESOURCE, "apim"));
        events.clear();

        entityService.upsert(CALLER, create("api.123", EntityKind.RESOURCE, "apim"));

        assertThat(events.events()).hasSize(1);
        assertThat(events.events().get(0).kind()).isEqualTo(EventKind.ENTITY_PUBLISHED);
    }

    @Test
    void update_emits_PUBLISH_AUTHZ_ENTITY_so_the_gateway_picks_up_the_attribute_change() {
        entityService.upsert(CALLER, create("api.123", EntityKind.RESOURCE, "apim"));
        events.clear();

        entityService.update(CALLER, "api.123", new UpdateEntityCommand(Map.of("k", "v"), null));

        assertThat(events.events()).hasSize(1);
        RecordingAuthzEventPublisher.Recorded event = events.events().get(0);
        assertThat(event.kind()).isEqualTo(EventKind.ENTITY_PUBLISHED);
        assertThat(event.entity().attributes()).containsEntry("k", "v");
    }

    @Test
    void cascade_delete_emits_one_UNPUBLISH_per_affected_entity_then_one_per_affected_policy() {
        entityService.upsert(CALLER, create("api.bookings", EntityKind.RESOURCE, "apim"));
        entityService.upsert(CALLER, create("mcp.bookings.tool-1", EntityKind.RESOURCE, "apim"));
        var policy = policyService.create(CALLER, new CreatePolicyCommand(ENV, "p", PolicyKind.RESOURCE, "api.bookings", ""));
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
        var failingPublisher = new io.gravitee.apim.authorization.api.AuthzEventPublisher() {
            int calls = 0;

            @Override
            public void publishPolicyDeployed(io.gravitee.apim.authorization.domain.Policy policy) {}

            @Override
            public void unpublishPolicy(io.gravitee.apim.authorization.domain.Policy policy) {}

            @Override
            public void publishEntityUpserted(io.gravitee.apim.authorization.domain.Entity entity) {
                calls++;
                throw new io.gravitee.apim.authorization.event.AuthzEventPublishException(
                    "simulated publish failure",
                    new RuntimeException("underlying I/O")
                );
            }

            @Override
            public void unpublishEntity(io.gravitee.apim.authorization.domain.Entity entity) {}
        };
        EntityIdValidator validator = new EntityIdValidator();
        SchemaServiceImpl schemaService = new SchemaServiceImpl(entityRepository, policyRepository);
        EntityServiceImpl failingService = new EntityServiceImpl(
            entityRepository,
            policyRepository,
            validator,
            schemaService,
            failingPublisher,
            audit,
            DEFAULT_CASCADE_HARD_LIMIT
        );

        assertThatThrownBy(() -> failingService.upsert(CALLER, create("api.test", EntityKind.RESOURCE, "apim"))).isInstanceOf(
            io.gravitee.apim.authorization.event.AuthzEventPublishException.class
        );

        assertThat(failingPublisher.calls).isEqualTo(1);
    }

    @Test
    void upsert_propagates_AuthzEventPublishException_thrown_after_successful_retry_save() {
        // The upsert path retries save() once on DuplicateKey (entity-id race).
        // After the reorder save-then-publish, the first save throws and the publisher
        // is never invoked; the retry save succeeds and the (now first) publish call
        // is the one that fails. The previous test version assumed publish-before-save
        // and counted two publish calls — that ordering was the C2 finding we fixed.
        var racingRepo = new DuplicateOnFirstSaveEntityRepository(entityRepository);
        var failingPublisher = new io.gravitee.apim.authorization.api.AuthzEventPublisher() {
            int calls = 0;

            @Override
            public void publishPolicyDeployed(io.gravitee.apim.authorization.domain.Policy policy) {}

            @Override
            public void unpublishPolicy(io.gravitee.apim.authorization.domain.Policy policy) {}

            @Override
            public void publishEntityUpserted(io.gravitee.apim.authorization.domain.Entity entity) {
                calls++;
                throw new io.gravitee.apim.authorization.event.AuthzEventPublishException(
                    "simulated retry-time failure",
                    new RuntimeException("underlying I/O")
                );
            }

            @Override
            public void unpublishEntity(io.gravitee.apim.authorization.domain.Entity entity) {}
        };
        EntityIdValidator validator = new EntityIdValidator();
        SchemaServiceImpl schemaService = new SchemaServiceImpl(racingRepo, policyRepository);
        EntityServiceImpl racingService = new EntityServiceImpl(
            racingRepo,
            policyRepository,
            validator,
            schemaService,
            failingPublisher,
            audit,
            DEFAULT_CASCADE_HARD_LIMIT
        );

        assertThatThrownBy(() -> racingService.upsert(CALLER, create("api.race", EntityKind.RESOURCE, "apim"))).isInstanceOf(
            io.gravitee.apim.authorization.event.AuthzEventPublishException.class
        );

        assertThat(failingPublisher.calls).isEqualTo(1);
        assertThat(racingRepo.saveCalls).isEqualTo(2);
    }

    private static final class DuplicateOnFirstSaveEntityRepository implements io.gravitee.apim.authorization.api.EntityRepository {

        private final io.gravitee.apim.authorization.api.EntityRepository delegate;
        int saveCalls = 0;

        DuplicateOnFirstSaveEntityRepository(io.gravitee.apim.authorization.api.EntityRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Entity save(Entity entity) {
            saveCalls++;
            if (saveCalls == 1) {
                throw new org.springframework.dao.DuplicateKeyException("simulated unique-violation race");
            }
            return delegate.save(entity);
        }

        @Override
        public java.util.Optional<Entity> findById(String environmentId, String id) {
            return delegate.findById(environmentId, id);
        }

        @Override
        public java.util.Optional<Entity> findByEntityId(String environmentId, String entityId) {
            return delegate.findByEntityId(environmentId, entityId);
        }

        @Override
        public List<Entity> findAll(String environmentId) {
            return delegate.findAll(environmentId);
        }

        @Override
        public List<Entity> findByKind(String environmentId, EntityKind kind) {
            return delegate.findByKind(environmentId, kind);
        }

        @Override
        public List<Entity> findByEntityIdPrefix(String environmentId, String prefix) {
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
    void cascade_delete_above_hard_limit_throws_CascadeTooLargeException() {
        entityService.upsert(CALLER, create("api.huge", EntityKind.RESOURCE, "apim"));
        for (int i = 0; i < DEFAULT_CASCADE_HARD_LIMIT + 1; i++) {
            entityService.upsert(CALLER, create("mcp.huge.tool-" + i, EntityKind.RESOURCE, "apim"));
        }
        events.clear();

        assertThatThrownBy(() -> entityService.delete(CALLER, "api.huge")).isInstanceOf(
            io.gravitee.apim.authorization.service.exception.CascadeTooLargeException.class
        );

        assertThat(entityRepository.findByEntityId(ENV, "api.huge")).isPresent();
        assertThat(events.events()).isEmpty();
    }
}
