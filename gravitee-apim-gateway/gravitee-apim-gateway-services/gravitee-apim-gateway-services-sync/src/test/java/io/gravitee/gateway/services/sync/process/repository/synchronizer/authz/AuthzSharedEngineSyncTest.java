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
import static org.mockito.Mockito.mock;
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
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.eventbus.MessageConsumer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
 * Regional replicas ({@code stock@eu}, {@code stock@us}) share one PDP engine keyed {@code env:stock}.
 * Everything below runs through the real {@link EventBusAuthzEnginePort} against an in-process fake engine,
 * so the revision marks the shared-engine rules depend on come from real hydration and staging rather than
 * from hand-written {@code markApplied} calls. That is the seam where {@code serves()} (keyed by engine),
 * the placement delta (compared on the engine) and {@code appliedOnAnotherScope} (keyed by routing scope)
 * meet, and a mocked port cannot exercise it.
 */
@ExtendWith(VertxExtension.class)
class AuthzSharedEngineSyncTest {

    private static final String ENV = "env-1";
    private static final String ENGINE_ADDRESS = "service:authz-pdp:sync:scope:env-1:stock";

    private Vertx vertx;
    private AutoCloseable mocks;

    @Mock
    private LatestEventFetcher fetcher;

    private AuthzHostedScopes hostedScopes;
    private AuthzAppliedRevisions revisions;
    private AuthzPdpSynchronizer pdpSynchronizer;
    private AuthzPolicySynchronizer policySynchronizer;
    private FakeEngine engine;
    private final List<String> provisionOps = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean provisionRelayFails;
    private MessageConsumer<JsonObject> provisionConsumer;
    private MessageConsumer<JsonObject> engineConsumer;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        vertx = Vertx.vertx();
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        hostedScopes = new AuthzHostedScopes();
        revisions = new AuthzAppliedRevisions();
        engine = registerFakeEngine();
        provisionConsumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> {
                if (provisionRelayFails) {
                    message.fail(500, "pdp unreachable");
                    return;
                }
                JsonObject body = message.body();
                provisionOps.add(body.getString("op") + ":" + body.getString("targetPdpId"));
                message.reply(new JsonObject().put("ok", true));
            });
        wireSynchronizers();
    }

    @AfterEach
    void tearDown() throws Exception {
        provisionConsumer.unregister();
        engineConsumer.unregister();
        vertx.close();
        mocks.close();
    }

    @Test
    void a_document_survives_a_replica_delete_and_the_narrowing_publish_that_follows() throws InterruptedException {
        stubPolicyFetch(policyPublish("pol-1", 100L, "stock@eu", "stock@us"));

        // 1. stock@eu is provisioned and hydrated with the policy that targets both replicas.
        stubPdpFetch(pdpEvent("pdp-eu", EventType.PUBLISH_AUTHZ_PDP, "eu"));
        runPdpCycle(-1L);
        assertThat(engine.served()).containsExactly("pol-1");

        // 2. stock@us is provisioned on the very same engine and hydrated in turn.
        stubPdpFetch(pdpEvent("pdp-us", EventType.PUBLISH_AUTHZ_PDP, "us"));
        runPdpCycle(1L);
        assertThat(hostedScopes.hostedFor(ENV)).containsExactlyInAnyOrder("stock@eu", "stock@us");

        // 3. stock@us is deleted. The engine stock@eu shares must stay up, with its documents.
        stubPdpFetch(pdpEvent("pdp-us", EventType.UNPUBLISH_AUTHZ_PDP, "us"));
        provisionOps.clear();
        runPdpCycle(2L);
        assertThat(provisionOps).doesNotContain("evict:stock");
        assertThat(engine.served()).containsExactly("pol-1");

        // 4. The cascade narrows the policy to the surviving replica. stock@eu still applies pol-1, so the
        //    removal for stock@us must never reach the shared engine.
        stubPolicyFetch(policyPublish("pol-1", 300L, "stock@eu"));
        engine.ops().clear();
        runPolicyCycle(3L);
        assertThat(engine.ops()).doesNotContain("removePolicy:pol-1");
        assertThat(engine.served()).containsExactly("pol-1");

        // 5. The policy itself is deleted. Nothing applies it any more, so now it goes.
        stubPolicyFetch(policyUnpublish("pol-1", "stock@eu"));
        runPolicyCycle(4L);
        assertThat(engine.ops()).contains("removePolicy:pol-1");
        assertThat(engine.served()).isEmpty();
    }

    @Test
    void deleting_the_last_replica_removes_the_document_from_the_shared_engine() throws InterruptedException {
        stubPolicyFetch(policyPublish("pol-1", 100L, "stock@eu"));
        stubPdpFetch(pdpEvent("pdp-eu", EventType.PUBLISH_AUTHZ_PDP, "eu"));
        runPdpCycle(-1L);
        assertThat(engine.served()).containsExactly("pol-1");

        provisionOps.clear();
        stubPdpFetch(pdpEvent("pdp-eu", EventType.UNPUBLISH_AUTHZ_PDP, "eu"));
        runPdpCycle(1L);

        assertThat(provisionOps).containsExactly("evict:stock");
        assertThat(hostedScopes.hostedFor(ENV)).isEmpty();
    }

    @Test
    void a_suppressed_evict_whose_provision_relay_fails_still_lets_the_cascade_clear_the_reused_engine() throws InterruptedException {
        stubPolicyFetch(policyPublish("pol-1", 100L, "stock@us"));

        // 1. This node hosts stock@us alone, and the shared engine holds its policy.
        stubPdpFetch(pdpEvent("pdp-us", EventType.PUBLISH_AUTHZ_PDP, "us"));
        runPdpCycle(-1L);
        assertThat(engine.served()).containsExactly("pol-1");

        // 2. A re-tag arrives as one batch: the delete of stock@us next to the create of stock@eu. The create
        //    reuses the engine, so the evict is suppressed and the engine stays up. The create's relay fails.
        provisionRelayFails = true;
        stubPdpFetch(pdpEvent("pdp-us", EventType.UNPUBLISH_AUTHZ_PDP, "us"), pdpEvent("pdp-eu", EventType.PUBLISH_AUTHZ_PDP, "eu"));
        runPdpCycle(1L);

        // 3. Same cycle, the cascade moves the policy off stock@us. The engine is alive and still holds the
        //    document, so the removal has to reach it.
        stubPolicyFetch(policyPublish("pol-1", 300L, "default"));
        engine.ops().clear();
        runPolicyCycle(2L);

        // 4. The pending provision confirms and stock@eu is hydrated. Hydration only ever adds, and the
        //    policy no longer targets this engine, so a document left behind now is left behind for good.
        provisionRelayFails = false;
        stubPdpFetch();
        runPdpCycle(3L);

        assertThat(hostedScopes.hostedFor(ENV)).containsExactly("stock@eu");
        // The op, not only the served set: the served set alone cannot tell a removal that routed from one
        // the serves() gate dropped.
        assertThat(engine.ops()).contains("removePolicy:pol-1");
        assertThat(engine.served()).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Wiring
    // ---------------------------------------------------------------------

    private void wireSynchronizers() {
        ObjectMapper objectMapper = new ObjectMapper();
        EventBusAuthzEnginePort port = new EventBusAuthzEnginePort(vertx, hostedScopes, revisions);
        DeployerFactory deployerFactory = mock(DeployerFactory.class);
        NoopDistributedSyncService distributedSyncService = new NoopDistributedSyncService();
        lenient().when(deployerFactory.createAuthzPolicyDeployer()).thenReturn(new AuthzPolicyDeployer(port, distributedSyncService));
        lenient().when(deployerFactory.createAuthzEntityDeployer()).thenReturn(new AuthzEntityDeployer(port, distributedSyncService));

        policySynchronizer = new AuthzPolicySynchronizer(
            fetcher,
            new AuthzPolicyMapper(objectMapper),
            deployerFactory,
            port,
            new AuthzScopePlacement(),
            executor(),
            executor()
        );
        AuthzSchemaSynchronizer schemaSynchronizer = new AuthzSchemaSynchronizer(
            fetcher,
            new AuthzSchemaMapper(objectMapper),
            deployerFactory,
            port,
            new AuthzScopePlacement(),
            executor(),
            executor()
        );
        AuthzEntitySynchronizer entitySynchronizer = new AuthzEntitySynchronizer(
            fetcher,
            new AuthzEntityMapper(objectMapper),
            deployerFactory,
            port,
            new AuthzScopePlacement(),
            executor(),
            executor()
        );
        pdpSynchronizer = new AuthzPdpSynchronizer(
            fetcher,
            new AuthzPdpMapper(objectMapper),
            schemaSynchronizer,
            policySynchronizer,
            entitySynchronizer,
            mock(Node.class),
            untaggedGateway(),
            vertx,
            hostedScopes,
            executor(),
            executor(),
            revisions
        );
    }

    /** An untagged gateway is a catch-all, so this one node hosts both regional replicas. */
    private static GatewayConfiguration untaggedGateway() {
        GatewayConfiguration configuration = mock(GatewayConfiguration.class);
        lenient().when(configuration.shardingTags()).thenReturn(Optional.empty());
        return configuration;
    }

    private static ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    // ---------------------------------------------------------------------
    // Fake engine
    // ---------------------------------------------------------------------

    /** Mutations stage, a commit makes the staged documents the served set, and every op is recorded. */
    private record FakeEngine(Set<String> served, List<String> ops) {}

    private FakeEngine registerFakeEngine() {
        Set<String> staged = ConcurrentHashMap.newKeySet();
        Set<String> served = ConcurrentHashMap.newKeySet();
        List<String> ops = Collections.synchronizedList(new ArrayList<>());
        engineConsumer = vertx
            .eventBus()
            .consumer(ENGINE_ADDRESS, msg -> {
                JsonObject body = msg.body();
                String op = body.getString("op");
                String docId = body.getString("docId", body.getString("uid"));
                ops.add(docId == null ? op : op + ":" + docId);
                switch (op) {
                    case "addOrUpdatePolicy", "addOrUpdateEntity", "addOrUpdateSchema" -> staged.add(docId);
                    case "removePolicy", "removeEntity", "removeSchema" -> staged.remove(docId);
                    case "commit" -> {
                        served.clear();
                        served.addAll(staged);
                    }
                    default -> {}
                }
                msg.reply(new JsonObject().put("commitGeneration", 1L));
            });
        return new FakeEngine(served, ops);
    }

    // ---------------------------------------------------------------------
    // Cycles and events
    // ---------------------------------------------------------------------

    private void runPdpCycle(long from) throws InterruptedException {
        pdpSynchronizer.synchronize(from, Instant.now().toEpochMilli(), Set.of(ENV)).test().await().assertComplete();
    }

    private void runPolicyCycle(long from) throws InterruptedException {
        policySynchronizer.synchronize(from, Instant.now().toEpochMilli(), Set.of(ENV)).test().await().assertComplete();
    }

    private void stubPdpFetch(Event... events) {
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(events))
        );
    }

    private void stubPolicyFetch(Event... events) {
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_POLICY_ID), any(), any())).thenReturn(
            Flowable.just(List.of(events))
        );
    }

    private static Event pdpEvent(String id, EventType type, String tag) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(new JsonObject().put("targetPdpId", "stock").put("environmentId", ENV).put("tag", tag).encode());
        event.setProperties(Map.of(Event.EventProperties.AUTHZ_PDP_ID.getValue(), id));
        return event;
    }

    private static Event policyPublish(String docId, long updatedAt, String... targetPdpIds) {
        Event event = policyEvent(docId, EventType.PUBLISH_AUTHZ_POLICY, targetPdpIds);
        event.setUpdatedAt(new Date(updatedAt));
        return event;
    }

    private static Event policyUnpublish(String docId, String... targetPdpIds) {
        return policyEvent(docId, EventType.UNPUBLISH_AUTHZ_POLICY, targetPdpIds);
    }

    private static Event policyEvent(String docId, EventType type, String... targetPdpIds) {
        Event event = new Event();
        event.setId(docId);
        event.setType(type);
        event.setPayload(
            new JsonObject()
                .put("id", docId)
                .put("name", docId)
                .put("kind", "GLOBAL")
                .put("policyText", "permit(principal, action, resource);")
                .put("environmentId", ENV)
                .put("targetPdpIds", List.of(targetPdpIds))
                .encode()
        );
        event.setProperties(Map.of(Event.EventProperties.AUTHZ_POLICY_ID.getValue(), docId));
        return event;
    }
}
