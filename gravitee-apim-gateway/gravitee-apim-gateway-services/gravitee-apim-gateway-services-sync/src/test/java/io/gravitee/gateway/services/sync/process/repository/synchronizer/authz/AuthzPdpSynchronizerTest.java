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

@ExtendWith(VertxExtension.class)
class AuthzPdpSynchronizerTest {

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
        lenient().when(enginePort.commit()).thenReturn(Completable.complete());
        lenient().when(enginePort.commitScope(any(), any())).thenReturn(Completable.complete());
        consumer = vertx
            .eventBus()
            .consumer(AuthzPdpSynchronizer.PROVISION_ADDRESS, message -> {
                received.add(message.body());
                message.reply(new JsonObject().put("ok", true));
            });
        synchronizer = newSynchronizer();
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

    @AfterEach
    void tearDown() throws Exception {
        consumer.unregister();
        vertx.close();
        mocks.close();
    }

    @Test
    void order_is_before_entity_and_policy() {
        assertThat(synchronizer.order()).isEqualTo(Order.AUTHZ_PDP.index());
        assertThat(Order.AUTHZ_PDP.index()).isLessThan(Order.AUTHZ_ENTITY.index());
        assertThat(Order.AUTHZ_PDP.index()).isLessThan(Order.AUTHZ_POLICY.index());
    }

    @Test
    void publish_sends_provision_message_with_scope_id() throws InterruptedException {
        Event publish = pdpEvent("evt-p", EventType.PUBLISH_AUTHZ_PDP, "scope-1", "eu");
        stubPdpFetch(publish);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(1);
        JsonObject msg = received.peek();
        assertThat(msg.getString("op")).isEqualTo("provision");
        assertThat(msg.getString("targetPdpId")).isEqualTo("scope-1");
    }

    @Test
    void unpublish_sends_evict_message() throws InterruptedException {
        Event unpublish = pdpEvent("evt-u", EventType.UNPUBLISH_AUTHZ_PDP, "scope-2", "eu");
        stubPdpFetch(unpublish);

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(1);
        assertThat(received.peek().getString("op")).isEqualTo("evict");
        assertThat(received.peek().getString("targetPdpId")).isEqualTo("scope-2");
    }

    @Test
    void no_events_sends_nothing() throws InterruptedException {
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

        assertThat(received).isEmpty();
    }

    @Test
    void provisions_pdp_tagged_for_this_node_but_not_other_tags() throws InterruptedException {
        // Node carries tag "eu" (setUp). A PDP tagged "us" is not provisioned here.
        Event eu = pdpEvent("evt-eu", EventType.PUBLISH_AUTHZ_PDP, "scope-eu", "eu");
        Event us = pdpEvent("evt-us", EventType.PUBLISH_AUTHZ_PDP, "scope-us", "us");
        stubPdpFetch(eu, us);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(1);
        assertThat(received.peek().getString("targetPdpId")).isEqualTo("scope-eu");
    }

    @Test
    void incremental_publish_for_unmatched_tag_is_dropped_not_converted_to_an_evict() throws InterruptedException {
        // Node carries "eu" (setUp); a PUBLISH for a 'us'-tagged PDP does not match this node. It must be
        // simply dropped — NOT converted into an evict. Converting would tear down a same-targetPdpId
        // regional replica that legitimately lives on this node under a different tag (the engine is keyed
        // by the bare targetPdpId). A genuine re-tag is a delete+recreate whose UNPUBLISH carries the old
        // tag and cleans up the node that actually matched it.
        Event publish = pdpEvent("evt-retag", EventType.PUBLISH_AUTHZ_PDP, "scope-x", "us");
        stubPdpFetch(publish);

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
    }

    @Test
    void initial_publish_for_unmatched_tag_evicts_nothing() throws InterruptedException {
        // Node carries "eu" (setUp); a 'us' PDP does not match.
        // On the initial cycle the node starts empty — a non-matching PDP is simply not provisioned,
        // and must not generate a spurious evict.
        Event publish = pdpEvent("evt-us", EventType.PUBLISH_AUTHZ_PDP, "scope-us", "us");
        stubPdpFetch(publish);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
    }

    @Test
    void live_publish_suppresses_evict_for_same_scope_from_another_pdp_id() throws InterruptedException {
        Event deletedPdp = pdpEvent("pdp-old", EventType.UNPUBLISH_AUTHZ_PDP, "scope-shared", "eu");
        Event livePdp = pdpEvent("pdp-new", EventType.PUBLISH_AUTHZ_PDP, "scope-shared", "eu");
        stubPdpFetch(deletedPdp, livePdp);

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(1);
        JsonObject msg = received.peek();
        assertThat(msg.getString("op")).isEqualTo("provision");
        assertThat(msg.getString("targetPdpId")).isEqualTo("scope-shared");
        assertThat(received).noneMatch(m -> "evict".equals(m.getString("op")));
    }

    @Test
    void evict_in_one_env_is_not_suppressed_by_publish_of_same_scope_in_another_env() throws InterruptedException {
        Event evictEnvA = pdpEvent("pdp-a", EventType.UNPUBLISH_AUTHZ_PDP, "scope-x", "eu", "env-A");
        Event publishEnvB = pdpEvent("pdp-b", EventType.PUBLISH_AUTHZ_PDP, "scope-x", "eu", "env-B");
        stubPdpFetch(evictEnvA, publishEnvB);

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-A", "env-B")).test().await().assertComplete();

        assertThat(received).hasSize(2);
        assertThat(received).anyMatch(
            m ->
                "evict".equals(m.getString("op")) &&
                "env-A".equals(m.getString("environmentId")) &&
                "scope-x".equals(m.getString("targetPdpId"))
        );
        assertThat(received).anyMatch(
            m ->
                "provision".equals(m.getString("op")) &&
                "env-B".equals(m.getString("environmentId")) &&
                "scope-x".equals(m.getString("targetPdpId"))
        );
    }

    @Test
    void provisions_untagged_pdp_only_on_an_untagged_node() throws InterruptedException {
        // Untagged node: an untagged (or blank-tag) PDP belongs here.
        when(gatewayConfiguration.shardingTags()).thenReturn(java.util.Optional.empty());
        Event noTag = pdpEvent("evt-x", EventType.PUBLISH_AUTHZ_PDP, "scope-x", null);
        Event blankTag = pdpEvent("evt-y", EventType.PUBLISH_AUTHZ_PDP, "scope-y", "   ");
        stubPdpFetch(noTag, blankTag);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).hasSize(2);
        assertThat(received).allMatch(m -> "provision".equals(m.getString("op")));
        assertThat(received).anyMatch(m -> "scope-x".equals(m.getString("targetPdpId")));
        assertThat(received).anyMatch(m -> "scope-y".equals(m.getString("targetPdpId")));
    }

    @Test
    void does_not_provision_an_untagged_pdp_on_a_tagged_node() throws InterruptedException {
        // Node carries "eu" (setUp): an untagged PDP is NOT cloned here — that is the default scope's role.
        Event noTag = pdpEvent("evt-x", EventType.PUBLISH_AUTHZ_PDP, "scope-x", null);
        stubPdpFetch(noTag);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
    }

    @Test
    void does_not_relay_a_provision_for_the_default_scope() throws InterruptedException {
        // The default engine is bootstrapped on every node by the PDP service; a "default" provision
        // event must be ignored (it would otherwise bind a duplicate consumer on the unscoped address).
        Event def = pdpEvent("evt-def", EventType.PUBLISH_AUTHZ_PDP, "default", "eu");
        stubPdpFetch(def);

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(received).isEmpty();
    }

    private void stubPdpFetch(Event... events) {
        when(fetcher.fetchLatest(any(), any(), eq(Event.EventProperties.AUTHZ_PDP_ID), any(), any())).thenReturn(
            Flowable.just(List.of(events))
        );
    }

    private static Event pdpEvent(String id, EventType type, String targetPdpId, String tag) {
        return pdpEvent(id, type, targetPdpId, tag, "env-1");
    }

    private static Event pdpEvent(String id, EventType type, String targetPdpId, String tag, String environmentId) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        JsonObject payload = new JsonObject().put("targetPdpId", targetPdpId);
        if (tag != null) {
            payload.put("tag", tag);
        }
        if (environmentId != null) {
            payload.put("environmentId", environmentId);
        }
        event.setPayload(payload.encode());
        event.setProperties(Map.of(Event.EventProperties.AUTHZ_PDP_ID.getValue(), id));
        return event;
    }
}
