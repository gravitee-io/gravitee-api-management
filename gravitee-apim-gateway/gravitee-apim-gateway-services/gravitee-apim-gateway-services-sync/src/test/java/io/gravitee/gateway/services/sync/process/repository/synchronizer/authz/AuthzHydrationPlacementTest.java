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
import java.util.TreeSet;
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
 * Regression for the placement/hydration eviction gap: a document applied to a scope ONLY via the
 * {@link AuthzPdpSynchronizer} hydration backfill (outside the normal PUBLISH path) must still be
 * evicted when a later PUBLISH narrows it away. This works only because hydration feeds the same
 * {@link AuthzScopePlacement} the entity synchronizer reads for its {@code dropped = applied −
 * newTarget} computation. Without the shared placement the hydrated scope would be invisible and the
 * document would linger.
 */
@ExtendWith(VertxExtension.class)
class AuthzHydrationPlacementTest {

    private static final String ENV = "env-1";
    private static final String ENTITY_ID = "api.bookings";
    private static final String ENGINE_UID = "Resource::\"api.bookings\"";

    private Vertx vertx;
    private AutoCloseable mocks;
    private MessageConsumer<JsonObject> provisionConsumer;

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private Node node;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private DeployerFactory deployerFactory;

    private RecordingPort port;
    private AuthzScopePlacement entityPlacement;
    private AuthzHostedScopes hostedScopes;
    private AuthzPdpSynchronizer pdpSynchronizer;
    private AuthzEntitySynchronizer entitySynchronizer;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        vertx = Vertx.vertx();
        port = new RecordingPort();
        entityPlacement = new AuthzScopePlacement();
        hostedScopes = new AuthzHostedScopes();
        AuthzScopePlacement policyPlacement = new AuthzScopePlacement();

        lenient().when(node.id()).thenReturn("node-id");
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(gatewayConfiguration.shardingTags()).thenReturn(java.util.Optional.of(java.util.List.of()));
        lenient()
            .when(deployerFactory.createAuthzEntityDeployer())
            .thenReturn(new AuthzEntityDeployer(port, new NoopDistributedSyncService()));
        lenient()
            .when(deployerFactory.createAuthzPolicyDeployer())
            .thenReturn(new AuthzPolicyDeployer(port, new NoopDistributedSyncService()));

        provisionConsumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> message.reply(new JsonObject().put("ok", true)));

        entitySynchronizer = new AuthzEntitySynchronizer(
            fetcher,
            new AuthzEntityMapper(new ObjectMapper()),
            deployerFactory,
            port,
            entityPlacement,
            executor(),
            executor()
        );
        AuthzPolicySynchronizer policySynchronizer = new AuthzPolicySynchronizer(
            fetcher,
            new AuthzPolicyMapper(new ObjectMapper()),
            deployerFactory,
            port,
            policyPlacement,
            executor(),
            executor()
        );
        pdpSynchronizer = new AuthzPdpSynchronizer(
            fetcher,
            new AuthzPdpMapper(new ObjectMapper()),
            policySynchronizer,
            entitySynchronizer,
            node,
            gatewayConfiguration,
            vertx,
            hostedScopes,
            executor(),
            executor()
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provisionConsumer != null) {
            provisionConsumer.unregister();
        }
        vertx.close();
        mocks.close();
    }

    @Test
    void a_scope_hydrated_with_an_entity_is_evicted_when_a_later_publish_narrows_it_away() throws InterruptedException {
        // Cycle 1: provision scope-b and hydrate it with the entity (targets scope-b only). This applies
        // the entity to scope-b through the engine port AND records scope-b in the shared placement.
        Event pdpPublish = pdpEvent("evt-pdp", "scope-b");
        Event entityForScopeB = entityEvent("scope-b");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(pdpPublish))
        );
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(Flowable.empty());
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_ENTITY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(entityForScopeB))
        );

        pdpSynchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of(ENV)).test().await().assertComplete();

        assertThat(entityPlacement.applied(ENV + ":" + ENTITY_ID)).containsExactly("scope-b");

        // Cycle 2: the entity is re-published targeting scope-a only (scope-b dropped). The entity
        // synchronizer must evict it from scope-b — which is only possible because hydration recorded
        // scope-b in the shared placement.
        port.ops.clear();
        Event entityNarrowedToScopeA = entityEvent("scope-a");
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_ENTITY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(entityNarrowedToScopeA))
        );

        entitySynchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of(ENV)).test().await().assertComplete();

        assertThat(port.ops).contains("removeEntity:" + ENGINE_UID + ":[scope-b]");
        assertThat(entityPlacement.applied(ENV + ":" + ENTITY_ID)).containsExactly("scope-a");
    }

    private static ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    private static Event pdpEvent(String id, String targetPdpId) {
        Event event = new Event();
        event.setId(id);
        event.setType(EventType.PUBLISH_AUTHZ_PDP);
        event.setPayload(new JsonObject().put("targetPdpId", targetPdpId).put("environmentId", ENV).encode());
        event.setProperties(Map.of(Event.EventProperties.AUTHZ_PDP_ID.getValue(), id));
        return event;
    }

    private static Event entityEvent(String targetPdpId) {
        Event event = new Event();
        event.setId(ENTITY_ID);
        event.setType(EventType.PUBLISH_AUTHZ_ENTITY);
        event.setPayload(
            new JsonObject()
                .put("entityId", ENTITY_ID)
                .put("kind", "RESOURCE")
                .put("attributes", new JsonObject())
                .put("parents", List.of())
                .put("environmentId", ENV)
                .put("targetPdpIds", List.of(targetPdpId))
                .encode()
        );
        return event;
    }

    private static class RecordingPort implements AuthzEnginePort {

        final ConcurrentLinkedQueue<String> ops = new ConcurrentLinkedQueue<>();

        @Override
        public Completable addOrUpdateEntity(
            String environmentId,
            String uid,
            Map<String, Object> attributes,
            List<String> parents,
            Set<String> targetPdpIds
        ) {
            ops.add("addOrUpdateEntity:" + uid + ":" + new TreeSet<>(targetPdpIds));
            return Completable.complete();
        }

        @Override
        public Completable removeEntity(String environmentId, String uid, Set<String> targetPdpIds) {
            ops.add("removeEntity:" + uid + ":" + new TreeSet<>(targetPdpIds));
            return Completable.complete();
        }

        @Override
        public Completable addOrUpdatePolicy(String environmentId, String docId, String name, String policyText, Set<String> targetPdpIds) {
            ops.add("addOrUpdatePolicy:" + docId);
            return Completable.complete();
        }

        @Override
        public Completable removePolicy(String environmentId, String docId, Set<String> targetPdpIds) {
            ops.add("removePolicy:" + docId);
            return Completable.complete();
        }

        @Override
        public Completable commit() {
            ops.add("commit");
            return Completable.complete();
        }

        @Override
        public Completable commitScope(String environmentId, String targetPdpId) {
            ops.add("commitScope:" + targetPdpId);
            return Completable.complete();
        }
    }
}
