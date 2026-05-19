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
import io.gravitee.gamma.authorization.audit.RecordingAuthzAuditPort;
import io.gravitee.gamma.authorization.domain.Policy;
import io.gravitee.gamma.authorization.domain.PolicyKind;
import io.gravitee.gamma.authorization.domain.PolicyStatus;
import io.gravitee.gamma.authorization.event.RecordingAuthzEventPublisher;
import io.gravitee.gamma.authorization.event.RecordingAuthzEventPublisher.EventKind;
import io.gravitee.gamma.authorization.repository.InMemoryEntityRepository;
import io.gravitee.gamma.authorization.repository.InMemoryPolicyRepository;
import io.gravitee.gamma.authorization.service.exception.InvalidStatusTransitionException;
import io.gravitee.gamma.authorization.service.exception.PolicyNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyServiceImplTest {

    private static final Instant FIXED = Instant.parse("2025-01-01T00:00:00Z");
    private static final String ENV = "env-1";
    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", ENV, "alice");
    private static final AuthzCallerContext CALLER_ENV2 = AuthzCallerContext.ofUser("org-1", "env-2", "alice");

    private InMemoryPolicyRepository repository;
    private InMemoryEntityRepository entityRepository;
    private SchemaServiceImpl schemaService;
    private PolicyServiceImpl service;
    private RecordingAuthzEventPublisher events;
    private RecordingAuthzAuditPort audit;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(FIXED, ZoneOffset.UTC));
        repository = new InMemoryPolicyRepository();
        entityRepository = new InMemoryEntityRepository();
        schemaService = new SchemaServiceImpl(entityRepository, repository);
        events = new RecordingAuthzEventPublisher();
        audit = new RecordingAuthzAuditPort();
        service = new PolicyServiceImpl(repository, new EntityIdValidator(), schemaService, events, audit);
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void create_persists_a_global_policy_with_generated_id_and_timestamps() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "global", PolicyKind.GLOBAL, null, "permit"));

        assertThat(created.id()).isNotBlank();
        assertThat(created.kind()).isEqualTo(PolicyKind.GLOBAL);
        assertThat(created.status()).isEqualTo(PolicyStatus.DRAFT);
        assertThat(created.createdAt()).isEqualTo(FIXED);
        assertThat(created.updatedAt()).isEqualTo(FIXED);
        assertThat(repository.findById(ENV, created.id())).contains(created);
    }

    @Test
    void create_persists_a_resource_policy() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "r", PolicyKind.RESOURCE, "api-1", ""));

        assertThat(created.kind()).isEqualTo(PolicyKind.RESOURCE);
        assertThat(created.entityId()).isEqualTo("api-1");
    }

    @Test
    void create_allows_multiple_global_policies_in_the_same_environment() {
        service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, ""));
        Policy second = service.create(CALLER, new CreatePolicyCommand(ENV, "g2", PolicyKind.GLOBAL, null, ""));

        assertThat(second.environmentId()).isEqualTo(ENV);
        assertThat(repository.findByKind(ENV, PolicyKind.GLOBAL)).hasSize(2);
    }

    @Test
    void create_allows_a_global_policy_per_environment() {
        service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, ""));
        Policy other = service.create(CALLER_ENV2, new CreatePolicyCommand("env-2", "g2", PolicyKind.GLOBAL, null, ""));
        assertThat(other.environmentId()).isEqualTo("env-2");
    }

    @Test
    void create_allows_multiple_resource_policies_for_the_same_entity() {
        service.create(CALLER, new CreatePolicyCommand(ENV, "r1", PolicyKind.RESOURCE, "api-1", ""));
        Policy second = service.create(CALLER, new CreatePolicyCommand(ENV, "r2", PolicyKind.RESOURCE, "api-1", ""));

        assertThat(repository.findByEntityId(ENV, "api-1")).hasSize(2);
        assertThat(second.id()).isNotBlank();
    }

    @Test
    void update_modifies_name_and_text_and_bumps_updated_at_without_touching_status() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "name", PolicyKind.GLOBAL, null, "old"));

        Instant later = FIXED.plusSeconds(60);
        TimeProvider.overrideClock(Clock.fixed(later, ZoneOffset.UTC));

        Policy updated = service.update(CALLER, created.id(), new UpdatePolicyCommand("renamed", "new"));

        assertThat(updated.name()).isEqualTo("renamed");
        assertThat(updated.policyText()).isEqualTo("new");
        assertThat(updated.status()).isEqualTo(PolicyStatus.DRAFT);
        assertThat(updated.updatedAt()).isEqualTo(later);
        assertThat(updated.createdAt()).isEqualTo(FIXED);
    }

    @Test
    void update_leaves_null_fields_unchanged() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, "t"));

        Policy updated = service.update(CALLER, created.id(), new UpdatePolicyCommand(null, null));

        assertThat(updated.name()).isEqualTo("n");
        assertThat(updated.policyText()).isEqualTo("t");
        assertThat(updated.status()).isEqualTo(PolicyStatus.DRAFT);
    }

    @Test
    void update_does_not_change_status_even_when_policy_is_deployed() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, ""));
        service.deploy(CALLER, created.id());

        Policy updated = service.update(CALLER, created.id(), new UpdatePolicyCommand("renamed", null));

        assertThat(updated.status()).isEqualTo(PolicyStatus.DEPLOYED);
        assertThat(updated.name()).isEqualTo("renamed");
    }

    @Test
    void update_rejects_unknown_id() {
        assertThatThrownBy(() -> service.update(CALLER, "missing", new UpdatePolicyCommand("x", null))).isInstanceOf(
            PolicyNotFoundException.class
        );
    }

    @Test
    void deploy_transitions_draft_to_deployed_and_bumps_updated_at() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, ""));

        Instant later = FIXED.plusSeconds(60);
        TimeProvider.overrideClock(Clock.fixed(later, ZoneOffset.UTC));

        Policy deployed = service.deploy(CALLER, created.id());

        assertThat(deployed.status()).isEqualTo(PolicyStatus.DEPLOYED);
        assertThat(deployed.updatedAt()).isEqualTo(later);
        assertThat(deployed.createdAt()).isEqualTo(FIXED);
    }

    @Test
    void deploy_transitions_disabled_to_deployed() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, ""));
        service.deploy(CALLER, created.id());
        service.disable(CALLER, created.id());

        Policy redeployed = service.deploy(CALLER, created.id());

        assertThat(redeployed.status()).isEqualTo(PolicyStatus.DEPLOYED);
    }

    @Test
    void deploy_is_idempotent_when_already_deployed() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, ""));
        Policy deployed = service.deploy(CALLER, created.id());

        Instant later = FIXED.plusSeconds(60);
        TimeProvider.overrideClock(Clock.fixed(later, ZoneOffset.UTC));

        Policy second = service.deploy(CALLER, created.id());

        assertThat(second).isEqualTo(deployed);
        assertThat(second.updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void deploy_rejects_unknown_id() {
        assertThatThrownBy(() -> service.deploy(CALLER, "missing")).isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void disable_transitions_deployed_to_disabled_and_bumps_updated_at() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, ""));
        service.deploy(CALLER, created.id());

        Instant later = FIXED.plusSeconds(60);
        TimeProvider.overrideClock(Clock.fixed(later, ZoneOffset.UTC));

        Policy disabled = service.disable(CALLER, created.id());

        assertThat(disabled.status()).isEqualTo(PolicyStatus.DISABLED);
        assertThat(disabled.updatedAt()).isEqualTo(later);
    }

    @Test
    void disable_is_idempotent_when_already_disabled() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, ""));
        service.deploy(CALLER, created.id());
        Policy disabled = service.disable(CALLER, created.id());

        Instant later = FIXED.plusSeconds(60);
        TimeProvider.overrideClock(Clock.fixed(later, ZoneOffset.UTC));

        Policy second = service.disable(CALLER, created.id());

        assertThat(second).isEqualTo(disabled);
        assertThat(second.updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void disable_rejects_draft_policy() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, ""));

        assertThatThrownBy(() -> service.disable(CALLER, created.id())).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void disable_rejects_unknown_id() {
        assertThatThrownBy(() -> service.disable(CALLER, "missing")).isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void delete_returns_true_when_removed_and_false_otherwise() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, ""));
        assertThat(service.delete(CALLER, created.id())).isTrue();
        assertThat(service.delete(CALLER, created.id())).isFalse();
    }

    @Test
    void find_by_id_returns_optional() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "n", PolicyKind.GLOBAL, null, ""));
        assertThat(service.findById(ENV, created.id())).contains(created);
        assertThat(service.findById(ENV, "missing")).isEmpty();
    }

    @Test
    void find_by_kind_filters_results() {
        service.create(CALLER, new CreatePolicyCommand(ENV, "g", PolicyKind.GLOBAL, null, ""));
        service.create(CALLER, new CreatePolicyCommand(ENV, "r", PolicyKind.RESOURCE, "api-1", ""));

        assertThat(service.findByKind(ENV, PolicyKind.GLOBAL)).hasSize(1);
        assertThat(service.findByKind(ENV, PolicyKind.RESOURCE)).hasSize(1);
    }

    @Test
    void find_by_entity_id_filters_results() {
        service.create(CALLER, new CreatePolicyCommand(ENV, "r1", PolicyKind.RESOURCE, "api-1", ""));
        service.create(CALLER, new CreatePolicyCommand(ENV, "r2", PolicyKind.RESOURCE, "api-2", ""));

        assertThat(service.findByEntityId(ENV, "api-1")).hasSize(1);
        assertThat(service.findByEntityId(ENV, "api-2")).hasSize(1);
        assertThat(service.findByEntityId(ENV, "missing")).isEmpty();
    }

    @Test
    void find_all_returns_every_policy_for_environment() {
        service.create(CALLER, new CreatePolicyCommand(ENV, "g", PolicyKind.GLOBAL, null, ""));
        service.create(CALLER, new CreatePolicyCommand(ENV, "r", PolicyKind.RESOURCE, "api-1", ""));
        assertThat(service.findAll(ENV)).hasSize(2);
    }

    @Test
    void update_throws_NullPointerException_for_null_caller() {
        assertThatThrownBy(() -> service.update(null, "id", new UpdatePolicyCommand(null, null)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("caller");
    }

    @Test
    void delete_throws_NullPointerException_for_null_id() {
        assertThatThrownBy(() -> service.delete(CALLER, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("id");
    }

    @Test
    void findById_throws_IllegalArgumentException_for_blank_environmentId() {
        assertThatThrownBy(() -> service.findById("", "id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("environmentId");
    }

    @Test
    void findByEntityId_throws_IllegalArgumentException_for_blank_entityId() {
        assertThatThrownBy(() -> service.findByEntityId(ENV, "  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("entityId");
    }

    @Test
    void findByKind_throws_NullPointerException_for_null_kind() {
        assertThatThrownBy(() -> service.findByKind(ENV, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("kind");
    }

    @Test
    void create_rejects_null_environmentId_at_command_construction() {
        assertThatThrownBy(() -> new CreatePolicyCommand(null, "n", PolicyKind.GLOBAL, null, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("environmentId");
    }

    @Test
    void create_does_not_emit_an_event_because_DRAFT_policies_never_reach_the_gateway() {
        service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));

        assertThat(events.events()).isEmpty();
    }

    @Test
    void deploy_emits_PUBLISH_AUTHZ_POLICY_with_the_policy_payload() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));
        events.clear();

        service.deploy(CALLER, created.id());

        assertThat(events.events()).hasSize(1);
        RecordingAuthzEventPublisher.Recorded event = events.events().get(0);
        assertThat(event.kind()).isEqualTo(EventKind.POLICY_PUBLISHED);
        assertThat(event.environmentId()).isEqualTo(ENV);
        assertThat(event.id()).isEqualTo(created.id());
        assertThat(event.policy()).isNotNull();
        assertThat(event.policy().status()).isEqualTo(PolicyStatus.DEPLOYED);
    }

    @Test
    void deploy_idempotent_on_already_deployed_policy_does_not_emit_a_second_event() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));
        service.deploy(CALLER, created.id());
        events.clear();

        service.deploy(CALLER, created.id());

        assertThat(events.events()).isEmpty();
    }

    @Test
    void disable_emits_UNPUBLISH_AUTHZ_POLICY() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));
        service.deploy(CALLER, created.id());
        events.clear();

        service.disable(CALLER, created.id());

        assertThat(events.events()).hasSize(1);
        RecordingAuthzEventPublisher.Recorded event = events.events().get(0);
        assertThat(event.kind()).isEqualTo(EventKind.POLICY_UNPUBLISHED);
        assertThat(event.id()).isEqualTo(created.id());
    }

    @Test
    void disable_idempotent_on_already_disabled_policy_does_not_emit_a_second_event() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));
        service.deploy(CALLER, created.id());
        service.disable(CALLER, created.id());
        events.clear();

        service.disable(CALLER, created.id());

        assertThat(events.events()).isEmpty();
    }

    @Test
    void update_on_a_DEPLOYED_policy_re_publishes_so_the_gateway_picks_up_the_new_text() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));
        service.deploy(CALLER, created.id());
        events.clear();

        service.update(CALLER, created.id(), new UpdatePolicyCommand("renamed", "forbid"));

        assertThat(events.events()).hasSize(1);
        assertThat(events.events().get(0).kind()).isEqualTo(EventKind.POLICY_PUBLISHED);
        assertThat(events.events().get(0).policy().policyText()).isEqualTo("forbid");
    }

    @Test
    void update_on_a_DRAFT_policy_does_not_emit_an_event() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));
        events.clear();

        service.update(CALLER, created.id(), new UpdatePolicyCommand("renamed", null));

        assertThat(events.events()).isEmpty();
    }

    @Test
    void delete_emits_UNPUBLISH_AUTHZ_POLICY_only_when_prior_status_was_DEPLOYED() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));
        service.deploy(CALLER, created.id());
        events.clear();

        service.delete(CALLER, created.id());

        assertThat(events.events()).hasSize(1);
        assertThat(events.events().get(0).kind()).isEqualTo(EventKind.POLICY_UNPUBLISHED);
        assertThat(events.events().get(0).id()).isEqualTo(created.id());
    }

    @Test
    void delete_does_not_emit_when_prior_status_was_DRAFT() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));
        events.clear();

        service.delete(CALLER, created.id());

        assertThat(events.events()).isEmpty();
    }

    @Test
    void delete_does_not_emit_when_prior_status_was_DISABLED() {
        Policy created = service.create(CALLER, new CreatePolicyCommand(ENV, "g1", PolicyKind.GLOBAL, null, "permit"));
        service.deploy(CALLER, created.id());
        service.disable(CALLER, created.id());
        events.clear();

        service.delete(CALLER, created.id());

        assertThat(events.events()).isEmpty();
    }
}
