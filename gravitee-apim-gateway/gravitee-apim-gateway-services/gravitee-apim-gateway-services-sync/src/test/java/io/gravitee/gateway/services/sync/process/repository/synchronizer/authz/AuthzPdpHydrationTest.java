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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@ExtendWith(VertxExtension.class)
class AuthzPdpHydrationTest {

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
    private MessageConsumer<JsonObject> provisionConsumer;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        vertx = Vertx.vertx();
        lenient().when(node.id()).thenReturn("gw-1");
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        lenient().when(gatewayConfiguration.shardingTags()).thenReturn(java.util.Optional.of(java.util.List.of()));
        lenient().when(enginePort.addOrUpdatePolicy(any(), any(), any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(enginePort.addOrUpdateEntity(any(), any(), any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(enginePort.commit()).thenReturn(Completable.complete());
        lenient().when(enginePort.commitScope(any(), any())).thenReturn(Completable.complete());
        provisionConsumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> message.reply(new JsonObject().put("ok", true)));

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
        synchronizer = new AuthzPdpSynchronizer(
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

    @AfterEach
    void tearDown() throws Exception {
        provisionConsumer.unregister();
        vertx.close();
        mocks.close();
    }

    @Test
    void provision_backfills_matching_policies_and_entities_then_commits() throws InterruptedException {
        Event provision = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-1", "");
        Event policyInScope = policyEvent("pol-1", "scope-1");
        Event policyWildcard = policyEvent("pol-2", "*");
        Event policyOtherScope = policyEvent("pol-3", "scope-9");
        Event entityInScope = entityEvent("ent-1", "scope-1");

        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(provision))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policyInScope, policyWildcard, policyOtherScope))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_ENTITY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(entityInScope))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-1"), any(), any(), eq(Set.of("scope-1")));
        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-2"), any(), any(), eq(Set.of("scope-1")));
        verify(enginePort, never()).addOrUpdatePolicy(any(), eq("pol-3"), any(), any(), any());
        verify(enginePort).addOrUpdateEntity(eq("env-pdp"), any(), any(), any(), eq(Set.of("scope-1")));
        verify(enginePort, times(2)).commitScope("env-pdp", "scope-1");
    }

    @Test
    void backfill_routes_only_matching_scopes_to_provisioned_scope_never_to_other_scope() throws InterruptedException {
        Event provision = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-x", "");
        Event policyX = policyEvent("pol-x", "scope-x");
        Event policyWildcard = policyEvent("pol-w", "*");
        Event policyOther = policyEvent("pol-other", "scope-other");
        Event policyMulti = policyEventMulti("pol-multi", "scope-other", "scope-x");

        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(provision))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(policyX, policyWildcard, policyOther, policyMulti))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-x"), any(), any(), eq(Set.of("scope-x")));
        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-w"), any(), any(), eq(Set.of("scope-x")));
        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-multi"), any(), any(), eq(Set.of("scope-x")));
        verify(enginePort, never()).addOrUpdatePolicy(any(), eq("pol-other"), any(), any(), any());
        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any(), any(), eq(Set.of("scope-other")));
    }

    @Test
    void backfill_does_not_apply_a_foreign_environment_policy_to_the_provisioned_scope() throws InterruptedException {
        Event provision = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-shared", "");
        Event ownPolicy = policyEventEnv("pol-own", "env-pdp", "scope-shared");
        Event foreignPolicy = policyEventEnv("pol-foreign", "env-other", "scope-shared");
        Event foreignWildcard = policyEventEnv("pol-foreign-w", "env-other", "*");

        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(provision))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(ownPolicy, foreignPolicy, foreignWildcard))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-pdp", "env-other")).test().await().assertComplete();

        verify(enginePort).addOrUpdatePolicy(eq("env-pdp"), eq("pol-own"), any(), any(), eq(Set.of("scope-shared")));
        verify(enginePort, never()).addOrUpdatePolicy(any(), eq("pol-foreign"), any(), any(), any());
        verify(enginePort, never()).addOrUpdatePolicy(any(), eq("pol-foreign-w"), any(), any(), any());
    }

    @Test
    void backfill_does_not_apply_a_foreign_environment_entity_to_the_provisioned_scope() throws InterruptedException {
        Event provision = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-shared", "");
        Event ownEntity = entityEventEnv("ent-own", "env-pdp", "scope-shared");
        Event foreignEntity = entityEventEnv("ent-foreign", "env-other", "scope-shared");

        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(provision))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_ENTITY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(ownEntity, foreignEntity))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-pdp", "env-other")).test().await().assertComplete();

        verify(enginePort).addOrUpdateEntity(eq("env-pdp"), any(), any(), any(), eq(Set.of("scope-shared")));
        verify(enginePort, times(1)).addOrUpdateEntity(any(), any(), any(), any(), any());
    }

    @Test
    void empty_scope_with_no_policies_or_entities_still_commits_once() throws InterruptedException {
        Event provision = pdpEvent("evt-empty", EventType.PUBLISH_AUTHZ_PDP, "scope-empty", "");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(provision))
        );
        // no policy / entity events at all for this scope

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any(), any(), any());
        verify(enginePort, never()).addOrUpdateEntity(any(), any(), any(), any(), any());
        // the scope must still seal generation 0 so it does not stay cold forever (once per synchronizer)
        verify(enginePort, times(2)).commitScope("env-pdp", "scope-empty");
    }

    @Test
    void evict_does_not_backfill() throws InterruptedException {
        Event evict = pdpEvent("evt-u", EventType.UNPUBLISH_AUTHZ_PDP, "scope-2", "");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(evict))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any(), any(), any());
        verify(enginePort, never()).addOrUpdateEntity(any(), any(), any(), any(), any());
        verify(enginePort, never()).commitScope(any(), any());
    }

    @Test
    void failing_hydration_is_abandoned_after_the_retry_cap_instead_of_redriving_forever() throws InterruptedException {
        Event provision = pdpEvent("evt-h", EventType.PUBLISH_AUTHZ_PDP, "scope-fail", "");
        // PDP event only on the first (initial) cycle; later cycles see no new events, so the scope
        // lives solely in pendingHydrations and exercises the re-drive bound rather than being
        // re-provisioned (which would reset the attempt counter every cycle).
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(provision)),
            Flowable.empty()
        );
        // Hydration commit fails every time, so the scope is re-queued into pendingHydrations.
        when(enginePort.commitScope(eq("env-pdp"), eq("scope-fail"))).thenReturn(Completable.error(new RuntimeException("boom")));

        // Initial sync hydrates once (fresh attempt); incremental cycles then re-drive the failure.
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        for (int i = 0; i < AuthzPdpSynchronizer.MAX_PENDING_ATTEMPTS + 5; i++) {
            synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        }

        // 1 fresh attempt + MAX_PENDING_ATTEMPTS re-drives, then abandoned — not unbounded.
        verify(enginePort, times(AuthzPdpSynchronizer.MAX_PENDING_ATTEMPTS + 1)).commitScope("env-pdp", "scope-fail");
    }

    private static Event pdpEvent(String id, EventType type, String targetPdpId, String tag) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(new JsonObject().put("targetPdpId", targetPdpId).put("tag", tag).put("environmentId", "env-pdp").encode());
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

    private static Event policyEventMulti(String docId, String... targetPdpIds) {
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
                .put("targetPdpIds", List.of(targetPdpIds))
                .encode()
        );
        return event;
    }

    private static Event entityEvent(String entityId, String targetPdpId) {
        Event event = new Event();
        event.setId(entityId);
        event.setType(EventType.PUBLISH_AUTHZ_ENTITY);
        event.setPayload(
            new JsonObject()
                .put("entityId", entityId)
                .put("kind", "RESOURCE")
                .put("environmentId", "env-pdp")
                .put("targetPdpIds", List.of(targetPdpId))
                .encode()
        );
        return event;
    }

    private static Event policyEventEnv(String docId, String environmentId, String targetPdpId) {
        Event event = new Event();
        event.setId(docId);
        event.setType(EventType.PUBLISH_AUTHZ_POLICY);
        event.setPayload(
            new JsonObject()
                .put("id", docId)
                .put("name", docId)
                .put("kind", "GLOBAL")
                .put("policyText", "permit();")
                .put("environmentId", environmentId)
                .put("targetPdpIds", List.of(targetPdpId))
                .encode()
        );
        return event;
    }

    private static Event entityEventEnv(String entityId, String environmentId, String targetPdpId) {
        Event event = new Event();
        event.setId(entityId);
        event.setType(EventType.PUBLISH_AUTHZ_ENTITY);
        event.setPayload(
            new JsonObject()
                .put("entityId", entityId)
                .put("kind", "RESOURCE")
                .put("environmentId", environmentId)
                .put("targetPdpIds", List.of(targetPdpId))
                .encode()
        );
        return event;
    }
}
