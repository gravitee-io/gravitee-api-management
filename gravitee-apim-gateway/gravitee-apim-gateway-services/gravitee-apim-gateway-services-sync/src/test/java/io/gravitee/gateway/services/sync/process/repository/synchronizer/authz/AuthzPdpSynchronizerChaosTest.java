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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzEntityDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzPolicyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
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
 * Chaos / failure-mode coverage for {@link AuthzPdpSynchronizer} (variant B gateway-sync relay).
 *
 * <p>Complements {@code AuthzPdpSynchronizerTest}, {@code AuthzPdpHydrationTest} and
 * {@code AuthzPdpSynchronizerGapsTest} by exercising abuse / failure scenarios that the
 * happy-path tests do not pin:
 * <ul>
 *   <li>GATEWAY RESTART recovery — a fresh synchronizer instance (empty pending set) re-provisions
 *       and re-hydrates an already-registered PDP from {@code events_latest} on initial sync;</li>
 *   <li>SYNC CUT-OFF — relay times out / has NO_HANDLERS, the scope is re-driven from the pending
 *       set on the next cycle and is never hydrated (fail-closed) meanwhile;</li>
 *   <li>CROSS-ENVIRONMENT ISOLATION — two environments reusing the SAME targetPdpId hydrate under their
 *       own {@code environmentId} namespace and one tenant's policy never lands under the other env;</li>
 *   <li>EVICTION correctness for reused scopes;</li>
 *   <li>MALFORMED / HOSTILE input — garbage payload, reserved-literal scope, oversized body.</li>
 * </ul>
 */
@ExtendWith(VertxExtension.class)
class AuthzPdpSynchronizerChaosTest {

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
        lenient().when(node.id()).thenReturn("gw-1");
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
    // (1) GATEWAY RESTART recovery
    // A fresh synchronizer (empty pending set, mimicking a process that just
    // came up) runs an INITIAL sync. The PDP is still in events_latest, so it
    // must be re-provisioned and its policies + entities re-hydrated end to end.
    // ---------------------------------------------------------------------

    @Test
    void gateway_restart_initial_sync_reprovisions_and_rehydrates_already_registered_pdp() throws InterruptedException {
        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-1", "env-1", "eu");
        Event policy = policyEvent("pol-1", "scope-1@eu");
        Event entity = entityEvent("ent-1", "scope-1@eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(publish))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policy))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_ENTITY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(entity))
        );

        // Brand-new instance: pending set is empty, just like after a JVM restart.
        AuthzPdpSynchronizer afterRestart = newSynchronizer();
        afterRestart.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(1);
        assertThat(received.peek().getString("op")).isEqualTo("provision");
        assertThat(received.peek().getString("targetPdpId")).isEqualTo("scope-1");
        assertThat(received.peek().getString("environmentId")).isEqualTo("env-1");
        verify(enginePort).addOrUpdatePolicy(eq("env-1"), eq("pol-1"), any(), any(), eq(Set.of("scope-1@eu")));
        verify(enginePort).addOrUpdateEntity(eq("env-1"), any(), any(), any(), eq(Set.of("scope-1@eu")));
        verify(enginePort, times(2)).commitScope("env-1", "scope-1@eu");
    }

    // ---------------------------------------------------------------------
    // (2) SYNC CUT-OFF — NO_HANDLERS (no PDP service listening at all).
    // The relay request gets NO_HANDLERS; the scope must NOT be hydrated this
    // cycle (fail-closed, never fail-open) and must be re-driven on the next
    // cycle once the PDP service consumer comes back, even though the event
    // window is then empty.
    // ---------------------------------------------------------------------

    @Test
    void no_handlers_fails_closed_then_pending_redrive_provisions_and_hydrates_when_service_returns() throws InterruptedException {
        // Cycle 1: NO consumer at all -> NO_HANDLERS for every relay.
        consumer.unregister();
        consumer = null;

        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-cut", "env-1", "eu");
        Event policy = policyEvent("pol-1", "scope-cut@eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(publish))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policy))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Fail-closed: nothing relayed (no listener), nothing hydrated, no commit.
        assertThat(received).isEmpty();
        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any(), any(), any());
        verify(enginePort, never()).commitScope(any(), any());

        // Cycle 2: PDP service is back and the AUTHZ_PDP window is now EMPTY.
        consumer = registerReplyingConsumer();
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received)
            .extracting(m -> m.getString("op") + ":" + m.getString("environmentId") + ":" + m.getString("targetPdpId"))
            .containsExactly("provision:env-1:scope-cut");
        verify(enginePort).addOrUpdatePolicy(eq("env-1"), eq("pol-1"), any(), any(), eq(Set.of("scope-cut@eu")));
        verify(enginePort, times(2)).commitScope("env-1", "scope-cut@eu");
    }

    // ---------------------------------------------------------------------
    // (3) CROSS-ENVIRONMENT ISOLATION / DATA LEAK
    // Two environments reuse the SAME targetPdpId. Each PDP must be provisioned and
    // each scope hydrated under its own environmentId namespace. A policy that
    // belongs to env-a must NEVER be added under env-b and vice versa.
    // ---------------------------------------------------------------------

    @Test
    void two_environments_reusing_same_scope_id_hydrate_under_their_own_env_namespace_without_leaking() throws InterruptedException {
        Event pdpEnvA = pdpEvent("pdp-a", EventType.PUBLISH_AUTHZ_PDP, "shared-scope", "env-a", "eu");
        Event pdpEnvB = pdpEvent("pdp-b", EventType.PUBLISH_AUTHZ_PDP, "shared-scope", "env-b", "eu");
        // Both env's policy events target the same shared-scope id (this is the leak risk).
        Event policyEnvA = policyEvent("pol-a", "env-a", "shared-scope@eu");
        Event policyEnvB = policyEvent("pol-b", "env-b", "shared-scope@eu");

        // The fetch is keyed on environments; emulate per-environment isolation by returning only
        // the PDP / policy that belongs to the environment set being asked for (the sync loop drives
        // one environment partition at a time).
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenAnswer(invocation -> {
            Set<String> envs = invocation.getArgument(3);
            if (envs != null && envs.contains("env-a")) {
                return Flowable.just(List.of(pdpEnvA));
            }
            if (envs != null && envs.contains("env-b")) {
                return Flowable.just(List.of(pdpEnvB));
            }
            return Flowable.empty();
        });
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenAnswer(invocation -> {
            Set<String> envs = invocation.getArgument(3);
            if (envs != null && envs.contains("env-a")) {
                return Flowable.just(List.of(policyEnvA));
            }
            if (envs != null && envs.contains("env-b")) {
                return Flowable.just(List.of(policyEnvB));
            }
            return Flowable.empty();
        });

        // env-a sync
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-a")).test().await().assertComplete();
        // env-b sync (separate environments set, as the sync loop drives per environment partition)
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-b")).test().await().assertComplete();

        // Both PDPs provisioned with their own env, same scope id.
        assertThat(received)
            .extracting(m -> m.getString("environmentId") + ":" + m.getString("targetPdpId"))
            .containsExactlyInAnyOrder("env-a:shared-scope", "env-b:shared-scope");

        // env-a policy lands ONLY under env-a; env-b policy lands ONLY under env-b.
        verify(enginePort).addOrUpdatePolicy(eq("env-a"), eq("pol-a"), any(), any(), eq(Set.of("shared-scope@eu")));
        verify(enginePort).addOrUpdatePolicy(eq("env-b"), eq("pol-b"), any(), any(), eq(Set.of("shared-scope@eu")));
        // No cross-tenant leak: env-a never sees pol-b, env-b never sees pol-a.
        verify(enginePort, never()).addOrUpdatePolicy(eq("env-a"), eq("pol-b"), any(), any(), any());
        verify(enginePort, never()).addOrUpdatePolicy(eq("env-b"), eq("pol-a"), any(), any(), any());
    }

    @Test
    void same_scope_in_different_environments_is_relayed_as_two_distinct_provisions_not_collapsed() throws InterruptedException {
        Event pdpEnvA = pdpEvent("pdp-a", EventType.PUBLISH_AUTHZ_PDP, "shared-scope", "env-a", "eu");
        Event pdpEnvB = pdpEvent("pdp-b", EventType.PUBLISH_AUTHZ_PDP, "shared-scope", "env-b", "eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(pdpEnvA, pdpEnvB))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-a", "env-b")).test().await().assertComplete();

        // Same targetPdpId, two environments: both provision commands must be relayed (env-namespaced
        // isolation), neither silently dropped as a "duplicate scope".
        assertThat(received).hasSize(2);
        assertThat(received)
            .allMatch(m -> "provision".equals(m.getString("op")))
            .extracting(m -> m.getString("environmentId"))
            .containsExactlyInAnyOrder("env-a", "env-b");
    }

    // ---------------------------------------------------------------------
    // (4) EVICTION correctness for a reused scope
    // A genuine evict for a scope that is NOT being re-published must be relayed
    // (engine + consumers torn down). Distinct from the churn-dedupe case where a
    // live publish in the same batch suppresses a stale evict for the same scope.
    // ---------------------------------------------------------------------

    @Test
    void evict_for_scope_with_no_live_republish_in_batch_is_relayed() throws InterruptedException {
        Event evict = pdpEvent("pdp-gone", EventType.UNPUBLISH_AUTHZ_PDP, "scope-gone", "env-1", "eu");
        Event otherLive = pdpEvent("pdp-other", EventType.PUBLISH_AUTHZ_PDP, "scope-other", "env-1", "eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(evict, otherLive))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // The evict for scope-gone is NOT suppressed by a publish for a *different* scope.
        assertThat(received)
            .extracting(m -> m.getString("op") + ":" + m.getString("targetPdpId"))
            .containsExactlyInAnyOrder("evict:scope-gone", "provision:scope-other");
        // Evict must not trigger any hydration.
        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any(), any(), eq(Set.of("scope-gone@eu")));
    }

    // ---------------------------------------------------------------------
    // (5) MALFORMED / HOSTILE input — fail-closed, never crash the cycle.
    // ---------------------------------------------------------------------

    @Test
    void garbage_payload_is_dropped_and_does_not_break_a_valid_provision_in_the_same_batch() throws InterruptedException {
        Event garbage = rawPayloadEvent("evt-garbage", EventType.PUBLISH_AUTHZ_PDP, "}{not json at all%%");
        Event valid = pdpEvent("evt-ok", EventType.PUBLISH_AUTHZ_PDP, "scope-ok", "env-1", "eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(garbage, valid))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Garbage is swallowed by the mapper; the valid provision still relays.
        assertThat(received)
            .extracting(m -> m.getString("targetPdpId"))
            .containsExactly("scope-ok");
    }

    @Test
    void empty_object_payload_missing_scope_is_fail_closed() throws InterruptedException {
        Event emptyObj = rawPayloadEvent("evt-empty", EventType.PUBLISH_AUTHZ_PDP, "{}");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(emptyObj))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
        verify(enginePort, never()).commitScope(any(), any());
    }

    @Test
    void blank_after_trim_scope_is_fail_closed() throws InterruptedException {
        Event blankAfterTrim = pdpEvent("evt-ws", EventType.PUBLISH_AUTHZ_PDP, "   \t  ", "env-1", "eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(blankAfterTrim))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
        verify(enginePort, never()).commitScope(any(), any());
    }

    @Test
    void wildcard_literal_as_scope_id_is_relayed_verbatim_not_treated_as_broadcast_target() throws InterruptedException {
        // The PDP wire carries a singular targetPdpId. A literal "*" must be relayed as-is (it is the
        // PDP service / target-resolver's job to interpret star), the synchronizer must neither drop
        // it nor expand it here.
        Event star = pdpEvent("evt-star", EventType.PUBLISH_AUTHZ_PDP, "*", "env-1", "eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(star))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(1);
        assertThat(received.peek().getString("targetPdpId")).isEqualTo("*");
        assertThat(received.peek().getString("op")).isEqualTo("provision");
    }

    @Test
    void oversized_scope_id_payload_is_relayed_without_throwing() throws InterruptedException {
        String huge = "s".repeat(200_000);
        Event oversized = pdpEvent("evt-huge", EventType.PUBLISH_AUTHZ_PDP, huge, "env-1", "eu");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(oversized))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // The synchronizer is not the validation boundary for scope length; it must not crash the
        // cycle on a large body. It relays exactly what it was given.
        assertThat(received).hasSize(1);
        assertThat(received.peek().getString("targetPdpId")).hasSize(200_000);
    }

    @Test
    void missing_environment_id_in_event_is_fail_closed_and_not_relayed() throws InterruptedException {
        // environmentId is required: a PDP event without it cannot be routed to a tenant-scoped engine
        // address, so it must be dropped (fail-closed) rather than relayed under a null env.
        Event noEnv = pdpEventNoEnv("evt-noenv", EventType.PUBLISH_AUTHZ_PDP, "scope-noenv", "eu");
        Event policy = policyEvent("pol-noenv", null, "scope-noenv");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(noEnv))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policy))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any(), any(), any());
        verify(enginePort, never()).commitScope(any(), any());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static Event pdpEvent(String id, EventType type, String targetPdpId, String environmentId, String tag) {
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

    private static Event pdpEventNoEnv(String id, EventType type, String targetPdpId, String tag) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        JsonObject payload = new JsonObject().put("targetPdpId", targetPdpId);
        if (tag != null) {
            payload.put("tag", tag);
        }
        event.setPayload(payload.encode());
        event.setProperties(Map.of(Event.EventProperties.AUTHZ_PDP_ID.getValue(), id));
        return event;
    }

    private static Event rawPayloadEvent(String id, EventType type, String rawPayload) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(rawPayload);
        event.setProperties(Map.of(Event.EventProperties.AUTHZ_PDP_ID.getValue(), id));
        return event;
    }

    private static Event policyEvent(String docId, String targetPdpId) {
        return policyEvent(docId, "env-1", targetPdpId);
    }

    private static Event policyEvent(String docId, String environmentId, String targetPdpId) {
        Event event = new Event();
        event.setId(docId);
        event.setType(EventType.PUBLISH_AUTHZ_POLICY);
        JsonObject payload = new JsonObject()
            .put("id", docId)
            .put("name", docId)
            .put("kind", "GLOBAL")
            .put("policyText", "permit();")
            .put("targetPdpIds", List.of(targetPdpId));
        if (environmentId != null) {
            payload.put("environmentId", environmentId);
        }
        event.setPayload(payload.encode());
        return event;
    }

    private static Event entityEvent(String entityId, String targetPdpId) {
        return entityEvent(entityId, "env-1", targetPdpId);
    }

    private static Event entityEvent(String entityId, String environmentId, String targetPdpId) {
        Event event = new Event();
        event.setId(entityId);
        event.setType(EventType.PUBLISH_AUTHZ_ENTITY);
        JsonObject payload = new JsonObject().put("entityId", entityId).put("kind", "RESOURCE").put("targetPdpIds", List.of(targetPdpId));
        if (environmentId != null) {
            payload.put("environmentId", environmentId);
        }
        event.setPayload(payload.encode());
        return event;
    }
}
