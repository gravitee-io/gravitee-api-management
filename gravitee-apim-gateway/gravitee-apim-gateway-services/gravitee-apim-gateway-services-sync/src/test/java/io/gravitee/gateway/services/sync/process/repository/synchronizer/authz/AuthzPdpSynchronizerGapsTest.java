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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzEntityDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzPolicyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.eventbus.MessageConsumer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Gap-fill + regression coverage for {@link AuthzPdpSynchronizer} (variant B gateway-sync relay).
 *
 * <p>Complements {@code AuthzPdpSynchronizerTest} (relay basics, tag filter, churn dedupe) and
 * {@code AuthzPdpHydrationTest} (backfill routing) by pinning: per-scope routing of multiple
 * provisions, tag-not-node.id targeting, fail-closed on blank scope, provision/evict idempotency,
 * hydration gating, swallowed-error behavior, and the confirmed relay-loss bug.
 */
@ExtendWith(VertxExtension.class)
class AuthzPdpSynchronizerGapsTest {

    private Vertx vertx;
    private AutoCloseable mocks;

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private Node node;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private AuthzEnginePort enginePort;

    private AuthzPdpSynchronizer synchronizer;
    private final ConcurrentLinkedQueue<JsonObject> received = new ConcurrentLinkedQueue<>();
    private MessageConsumer<JsonObject> consumer;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        vertx = Vertx.vertx();
        lenient().when(node.id()).thenReturn("ephemeral-node-id");
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        lenient().when(gatewayConfiguration.shardingTags()).thenReturn(java.util.Optional.of(java.util.List.of("eu")));
        lenient().when(enginePort.addOrUpdatePolicy(any(), any(), any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(enginePort.addOrUpdateEntity(any(), any(), any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(enginePort.commit()).thenReturn(Completable.complete());
        lenient().when(enginePort.commitScope(any(), any())).thenReturn(Completable.complete());
        consumer = registerReplyingConsumer();
        synchronizer = newSynchronizer();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (consumer != null) {
            consumer.unregister();
        }
        vertx.close();
        mocks.close();
    }

    private AuthzPdpSynchronizer newSynchronizer() {
        NoopDistributedSyncService distributedSyncService = new NoopDistributedSyncService();
        DeployerFactory deployerFactory = org.mockito.Mockito.mock(DeployerFactory.class);
        lenient().when(deployerFactory.createAuthzPolicyDeployer()).thenReturn(new AuthzPolicyDeployer(enginePort, distributedSyncService));
        lenient().when(deployerFactory.createAuthzEntityDeployer()).thenReturn(new AuthzEntityDeployer(enginePort, distributedSyncService));

        AuthzPolicySynchronizer policySynchronizer = new AuthzPolicySynchronizer(
            fetcher,
            new AuthzPolicyMapper(new ObjectMapper()),
            deployerFactory,
            enginePort,
            new AuthzScopePlacement(),
            executor(),
            executor()
        );
        AuthzEntitySynchronizer entitySynchronizer = new AuthzEntitySynchronizer(
            fetcher,
            new AuthzEntityMapper(new ObjectMapper()),
            deployerFactory,
            enginePort,
            new AuthzScopePlacement(),
            executor(),
            executor()
        );
        return new AuthzPdpSynchronizer(
            fetcher,
            new AuthzPdpMapper(new ObjectMapper()),
            policySynchronizer,
            entitySynchronizer,
            node,
            gatewayConfiguration,
            vertx,
            new AuthzHostedScopes(),
            executor(),
            executor()
        );
    }

    private static ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    private MessageConsumer<JsonObject> registerReplyingConsumer() {
        return vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> {
                received.add(message.body());
                message.reply(new JsonObject().put("ok", true));
            });
    }

    // ---------------------------------------------------------------------
    // Address / order contract
    // ---------------------------------------------------------------------

    @Test
    void provision_address_is_the_node_level_pdp_service_address() {
        assertThat(AuthzPdpSynchronizer.PROVISION_ADDRESS).isEqualTo("service:authz-pdp:provision");
    }

    @Test
    void pdp_order_index_is_eight_and_strictly_before_entity_and_policy() {
        assertThat(synchronizer.order()).isEqualTo(8);
        assertThat(Order.AUTHZ_PDP.index()).isLessThan(Order.AUTHZ_ENTITY.index());
        assertThat(Order.AUTHZ_ENTITY.index()).isLessThan(Order.AUTHZ_POLICY.index());
    }

    // ---------------------------------------------------------------------
    // Tag-based targeting (NOT node.id) — variant B live-validation fix
    // ---------------------------------------------------------------------

    @Test
    void targeting_is_tag_based_and_never_consults_ephemeral_node_id() throws InterruptedException {
        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-1", "eu");
        stubPdpFetch(publish);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(gatewayConfiguration, org.mockito.Mockito.atLeastOnce()).shardingTags();
        verify(node, never()).id();
        assertThat(received).hasSize(1);
    }

    @Test
    void blank_tag_provisions_on_an_untagged_node() throws InterruptedException {
        when(gatewayConfiguration.shardingTags()).thenReturn(java.util.Optional.empty());
        Event blankTag = pdpEvent("evt-blank", EventType.PUBLISH_AUTHZ_PDP, "scope-1", "   ");
        stubPdpFetch(blankTag);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(1);
        assertThat(received.peek().getString("op")).isEqualTo("provision");
        verify(gatewayConfiguration, never()).hasMatchingTags(any());
    }

    @Test
    void evict_for_a_non_matching_tag_is_dropped_so_a_regional_replica_on_this_node_survives() throws InterruptedException {
        // Node carries "eu" (setUp). Deleting the 'us' replica emits unpublishPdp(scope-us, tag=us). The
        // engine is keyed by the bare targetPdpId "scope-us", so relaying that evict on this eu node would
        // tear down the eu replica's engine that legitimately lives here. The tag gate must DROP a
        // non-matching-tag evict — a node only evicts scopes whose tag it carries (symmetric with provision).
        Event evict = pdpEvent("evt-evict", EventType.UNPUBLISH_AUTHZ_PDP, "scope-us", "us");
        stubPdpFetch(evict);

        synchronizer.synchronize(100L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Fail-closed on blank / missing scope (mapper drops it; nothing relayed)
    // ---------------------------------------------------------------------

    @Test
    void blank_scope_is_fail_closed_nothing_relayed_and_no_hydration() throws InterruptedException {
        Event blankScope = pdpEvent("evt-x", EventType.PUBLISH_AUTHZ_PDP, "", "eu");
        stubPdpFetch(blankScope);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any(), any(), any());
        verify(enginePort, never()).addOrUpdateEntity(any(), any(), any(), any(), any());
        verify(enginePort, never()).commitScope(any(), any());
    }

    // ---------------------------------------------------------------------
    // Per-scope routing of multiple provisions in one batch
    // ---------------------------------------------------------------------

    @Test
    void two_provisions_relay_two_scoped_commands_and_hydrate_each_scope_independently() throws InterruptedException {
        Event a = pdpEvent("evt-a", EventType.PUBLISH_AUTHZ_PDP, "scope-a", "eu");
        Event b = pdpEvent("evt-b", EventType.PUBLISH_AUTHZ_PDP, "scope-b", "eu");
        Event policyA = policyEvent("pol-a", "scope-a@eu");
        Event policyB = policyEvent("pol-b", "scope-b@eu");
        Event policyW = policyEvent("pol-w", "*");

        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(a, b))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policyA, policyB, policyW))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received)
            .extracting(m -> m.getString("targetPdpId"))
            .containsExactlyInAnyOrder("scope-a", "scope-b");
        assertThat(received).allMatch(m -> "provision".equals(m.getString("op")));
        // relay message carries the environmentId so the engine can compute the runtime key
        assertThat(received).allMatch(m -> "env-pdp".equals(m.getString("environmentId")));

        // scope-a hydration: own policy + wildcard, routed under scope-a only, env-namespaced
        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-a"), any(), any(), eq(Set.of("scope-a@eu")));
        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-w"), any(), any(), eq(Set.of("scope-a@eu")));
        verify(enginePort, never()).addOrUpdatePolicy(any(), eq("pol-b"), any(), any(), eq(Set.of("scope-a@eu")));

        // scope-b hydration: own policy + wildcard, routed under scope-b only, env-namespaced
        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-b"), any(), any(), eq(Set.of("scope-b@eu")));
        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-w"), any(), any(), eq(Set.of("scope-b@eu")));
        verify(enginePort, never()).addOrUpdatePolicy(any(), eq("pol-a"), any(), any(), eq(Set.of("scope-b@eu")));

        // one commit per provisioned scope, per synchronizer (policy + entity backfill)
        verify(enginePort, times(2)).commitScope("env-pdp", "scope-a@eu");
        verify(enginePort, times(2)).commitScope("env-pdp", "scope-b@eu");
    }

    // ---------------------------------------------------------------------
    // Hydration is gated on a *matching* provision
    // ---------------------------------------------------------------------

    @Test
    void provision_for_non_matching_tag_is_not_relayed_and_not_hydrated() throws InterruptedException {
        // Node carries "eu" (setUp); a 'us' PDP does not match this node.
        Event us = pdpEvent("evt-us", EventType.PUBLISH_AUTHZ_PDP, "scope-us", "us");
        Event policyUs = policyEvent("pol-us", "scope-us@eu");

        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(us))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policyUs))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any(), any(), any());
        verify(enginePort, never()).commitScope(any(), any());
    }

    // ---------------------------------------------------------------------
    // Idempotency
    // ---------------------------------------------------------------------

    @Test
    void provision_then_evict_for_distinct_scopes_relays_both_independently() throws InterruptedException {
        Event prov = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-keep", "eu");
        Event evict = pdpEvent("evt-e", EventType.UNPUBLISH_AUTHZ_PDP, "scope-drop", "eu");
        stubPdpFetch(prov, evict);

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received)
            .extracting(m -> m.getString("op") + ":" + m.getString("targetPdpId"))
            .containsExactlyInAnyOrder("provision:scope-keep", "evict:scope-drop");
    }

    @Test
    void rerunning_initial_sync_is_idempotent_per_run_relaying_the_same_provision_again() throws InterruptedException {
        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-1", "eu");
        stubPdpFetch(publish);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        assertThat(received).hasSize(1);

        received.clear();
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(1);
        assertThat(received.peek().getString("targetPdpId")).isEqualTo("scope-1");
        assertThat(received.peek().getString("op")).isEqualTo("provision");
    }

    @Test
    void evict_relays_command_but_never_hydrates() throws InterruptedException {
        Event evict = pdpEvent("evt-e", EventType.UNPUBLISH_AUTHZ_PDP, "scope-gone", "eu");
        stubPdpFetch(evict);

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(1);
        assertThat(received.peek().getString("op")).isEqualTo("evict");
        verifyNoInteractions(enginePort);
    }

    // ---------------------------------------------------------------------
    // Swallowed-error behavior of hydration (onErrorResumeNext at hydrate)
    // ---------------------------------------------------------------------

    @Test
    void hydration_failure_is_swallowed_and_synchronize_still_completes() throws InterruptedException {
        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-1", "eu");
        Event policy = policyEvent("pol-1", "scope-1@eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(publish))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policy))
        );
        when(enginePort.addOrUpdatePolicy(any(), any(), any(), any(), any())).thenReturn(
            Completable.error(new RuntimeException("engine down"))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // provision was relayed before hydration failed
        assertThat(received).hasSize(1);
    }

    // ---------------------------------------------------------------------
    // F1.1 — durability: a failed provision relay is NOT silently dropped.
    // The scope whose relay/hydrate did not confirm is kept in a node-local
    // pending set and re-driven on the NEXT synchronize() cycle, independently
    // of the updatedAt event window. Once the relay confirms, the scope is
    // provisioned and hydrated. (Flipped from the prior pin that asserted the
    // PDP was permanently lost.)
    // ---------------------------------------------------------------------

    @Test
    void failed_provision_relay_is_re_driven_on_the_next_cycle_and_then_hydrated() throws InterruptedException {
        // Cycle 1: the relay always fails — nothing is hydrated, but the scope must be remembered.
        consumer.unregister();
        consumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> message.fail(500, "PDP service unavailable"));

        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-lost", "eu");
        Event policy = policyEvent("pol-1", "scope-lost@eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(publish))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policy))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Relay failed, so the scope was not yet provisioned/hydrated.
        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any(), any(), any());
        verify(enginePort, never()).commitScope(any(), any());

        // Cycle 2: the PDP service is healthy again and the incremental event window is EMPTY
        // (no new AUTHZ_PDP events). The pending scope must still be re-driven and hydrated.
        consumer.unregister();
        consumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> {
                received.add(message.body());
                message.reply(new JsonObject().put("ok", true));
            });
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Re-driven from the pending set: relayed with env, then hydrated.
        assertThat(received)
            .extracting(m -> m.getString("op") + ":" + m.getString("environmentId") + ":" + m.getString("targetPdpId"))
            .containsExactly("provision:env-pdp:scope-lost");
        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-1"), any(), any(), eq(Set.of("scope-lost@eu")));
        verify(enginePort, times(2)).commitScope("env-pdp", "scope-lost@eu");
    }

    @Test
    void regression_one_failed_relay_does_not_block_a_second_healthy_scope() throws InterruptedException {
        // First scope's relay fails, second scope is published in the same batch.
        // The failure is swallowed per-deployable, so the healthy scope must still be hydrated.
        consumer.unregister();
        consumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> {
                JsonObject body = message.body();
                if ("scope-bad".equals(body.getString("targetPdpId"))) {
                    message.fail(500, "down");
                } else {
                    received.add(body);
                    message.reply(new JsonObject().put("ok", true));
                }
            });

        Event bad = pdpEvent("evt-bad", EventType.PUBLISH_AUTHZ_PDP, "scope-bad", "eu");
        Event good = pdpEvent("evt-good", EventType.PUBLISH_AUTHZ_PDP, "scope-good", "eu");
        Event policyGood = policyEvent("pol-good", "scope-good@eu");
        Event policyBad = policyEvent("pol-bad", "scope-bad@eu");

        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(bad, good))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policyGood, policyBad))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // healthy scope relayed + hydrated, env-namespaced
        assertThat(received)
            .extracting(m -> m.getString("targetPdpId"))
            .contains("scope-good");
        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-good"), any(), any(), eq(Set.of("scope-good@eu")));
        // failed scope never hydrated (it stays in the pending set, not provisioned this cycle)
        verify(enginePort, never()).addOrUpdatePolicy(any(), eq("pol-bad"), any(), any(), eq(Set.of("scope-bad@eu")));
    }

    @Test
    void pending_entry_for_one_env_is_not_dropped_by_a_confirmation_in_another_env() throws InterruptedException {
        // Cycle 1: env-A relay fails (kept pending), env-B relay confirms in the same batch.
        // The env-B confirmation must NOT remove env-A's pending entry (runtime-key isolation).
        consumer.unregister();
        consumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> {
                if ("env-A".equals(message.body().getString("environmentId"))) {
                    message.fail(500, "down");
                } else {
                    received.add(message.body());
                    message.reply(new JsonObject().put("ok", true));
                }
            });

        Event publishEnvA = pdpEvent("evt-a", EventType.PUBLISH_AUTHZ_PDP, "scope-x", "eu", "env-A");
        Event publishEnvB = pdpEvent("evt-b", EventType.PUBLISH_AUTHZ_PDP, "scope-x", "eu", "env-B");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(publishEnvA, publishEnvB))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-A", "env-B")).test().await().assertComplete();

        // Cycle 2: window empty, all relays healthy. env-A must still be re-driven from pending.
        received.clear();
        consumer.unregister();
        consumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> {
                received.add(message.body());
                message.reply(new JsonObject().put("ok", true));
            });
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-A", "env-B")).test().await().assertComplete();

        assertThat(received)
            .extracting(m -> m.getString("op") + ":" + m.getString("environmentId") + ":" + m.getString("targetPdpId"))
            .containsExactly("provision:env-A:scope-x");
    }

    // ---------------------------------------------------------------------
    // T0-3 — a stale pending provision must NOT resurrect an evicted scope.
    // Cycle 1: provision relay fails (scope kept pending). Cycle 2: the scope
    // is UNPUBLISHED. The evict must clear the pending provision so a later
    // cycle does not re-provision a scope the control plane already removed.
    // ---------------------------------------------------------------------

    @Test
    void unpublish_clears_a_pending_provision_so_it_is_not_resurrected() throws InterruptedException {
        // Cycle 1: provision relay always fails -> scope remembered as pending.
        consumer.unregister();
        consumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> message.fail(500, "PDP service unavailable"));

        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-z", "eu");
        stubPdpFetch(publish);
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Cycle 2: the scope is UNPUBLISHED. The evict relay ALSO fails this cycle — the pending
        // provision must still be cleared (evict wins) so it cannot be resurrected later.
        Event unpublish = pdpEvent("evt-u", EventType.UNPUBLISH_AUTHZ_PDP, "scope-z", "eu");
        stubPdpFetch(unpublish);
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        received.clear();

        // Cycle 3: empty window, PDP service healthy. The evicted scope must NOT be resurrected
        // from the pending-provision set.
        consumer.unregister();
        consumer = registerReplyingConsumer();
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(Flowable.empty());
        synchronizer.synchronize(2L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).noneMatch(m -> "provision".equals(m.getString("op")) && "scope-z".equals(m.getString("targetPdpId")));
    }

    // ---------------------------------------------------------------------
    // T0-4 — a failed evict relay must be retried, otherwise a disabled PDP
    // keeps serving. Cycle 1: evict relay fails. Cycle 2 (empty window): the
    // evict is re-driven from a pending set.
    // ---------------------------------------------------------------------

    @Test
    void failed_evict_relay_is_re_driven_on_the_next_cycle() throws InterruptedException {
        // Cycle 1: evict relay fails.
        consumer.unregister();
        consumer = vertx.eventBus().consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> message.fail(500, "down"));

        Event unpublish = pdpEvent("evt-u", EventType.UNPUBLISH_AUTHZ_PDP, "scope-evict", "eu");
        stubPdpFetch(unpublish);
        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Cycle 2: PDP service healthy, AUTHZ_PDP window empty. The evict must be re-sent.
        consumer.unregister();
        consumer = registerReplyingConsumer();
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(124L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received)
            .extracting(m -> m.getString("op") + ":" + m.getString("environmentId") + ":" + m.getString("targetPdpId"))
            .containsExactly("evict:env-pdp:scope-evict");
    }

    // ---------------------------------------------------------------------
    // T1-2 — a hydration that fails is retried on the next cycle instead of
    // leaving the scope cold-started forever.
    // ---------------------------------------------------------------------

    @Test
    void failed_hydration_is_retried_on_the_next_cycle() throws InterruptedException {
        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-h", "eu");
        Event policy = policyEvent("pol-h", "scope-h@eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(publish))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policy))
        );
        // Cycle 1: the commit fails -> hydration must be remembered as pending.
        when(enginePort.commitScope(any(), any())).thenReturn(Completable.error(new RuntimeException("engine down")));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Cycle 2: AUTHZ_PDP window empty, engine healthy. The pending hydration must be re-driven.
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(Flowable.empty());
        when(enginePort.commitScope(any(), any())).thenReturn(Completable.complete());

        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // The scope was hydrated again on cycle 2 and finally committed.
        // Cycle 1: policy backfill commit errors before the entity backfill runs (1 commitScope).
        // Cycle 2: healthy, policy backfill commits then entity backfill commits (2 commitScope) = 3 total.
        verify(enginePort, times(2)).addOrUpdatePolicy(eq("env-pdp"), eq("pol-h"), any(), any(), eq(Set.of("scope-h@eu")));
        verify(enginePort, times(3)).commitScope("env-pdp", "scope-h@eu");
    }

    // ---------------------------------------------------------------------
    // T1-3 — a pending provision that keeps failing must not retry forever; it
    // is dropped after a bounded number of attempts so the pending set cannot
    // grow immortal on a permanently dead scope.
    // ---------------------------------------------------------------------

    @Test
    void a_permanently_failing_pending_provision_stops_retrying_after_max_attempts() throws InterruptedException {
        consumer.unregister();
        consumer = vertx.eventBus().consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> message.fail(500, "down"));

        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-dead", "eu");
        stubPdpFetch(publish);

        // Cycle 1: initial relay fails -> pending. This is the first attempt.
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Subsequent cycles only ever re-drive from the pending set (window empty).
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(Flowable.empty());
        for (int i = 0; i < AuthzPdpSynchronizer.MAX_PENDING_ATTEMPTS + 2; i++) {
            synchronizer.synchronize((long) (i + 1), Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        }

        // Now register a healthy consumer and run one more cycle. The scope must have been abandoned,
        // so nothing is relayed any more.
        consumer.unregister();
        consumer = registerReplyingConsumer();
        synchronizer.synchronize(999L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
    }

    @Test
    void a_pending_provision_is_dropped_when_its_tag_no_longer_matches_this_node() throws InterruptedException {
        consumer.unregister();
        consumer = vertx.eventBus().consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> message.fail(500, "down"));

        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-retag", "eu");
        stubPdpFetch(publish);
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // The node is re-sharded: it no longer carries the "eu" tag, so the pending provision must be
        // dropped, not re-driven, even though the relay would now succeed.
        when(gatewayConfiguration.shardingTags()).thenReturn(java.util.Optional.of(java.util.List.of("us")));
        consumer.unregister();
        consumer = registerReplyingConsumer();
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void stubPdpFetch(Event... events) {
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(events))
        );
    }

    private static Event pdpEvent(String id, EventType type, String targetPdpId, String tag) {
        return pdpEvent(id, type, targetPdpId, tag, "env-pdp");
    }

    private static Event pdpEvent(String id, EventType type, String targetPdpId, String tag, String environmentId) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        JsonObject payload = new JsonObject().put("targetPdpId", targetPdpId).put("environmentId", environmentId);
        if (tag != null) {
            payload.put("tag", tag);
        }
        event.setPayload(payload.encode());
        event.setProperties(Map.of(Event.EventProperties.AUTHZ_PDP_ID.getValue(), id));
        return event;
    }

    private static Event policyEvent(String docId, String targetPdpId) {
        Event event = new Event();
        event.setId(docId);
        event.setType(EventType.PUBLISH_AUTHZ_POLICY);
        event.setPayload(
            new JsonObject()
                .put("id", docId)
                .put("name", docId)
                .put("kind", "GLOBAL")
                .put("policyText", "permit();")
                .put("environmentId", "env-pdp")
                .put("targetPdpIds", List.of(targetPdpId))
                .encode()
        );
        return event;
    }
}
