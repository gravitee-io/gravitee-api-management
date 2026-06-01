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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzEntityDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzPolicyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * End-to-end coverage of the events path through the REAL deployers (not mocks): a DB authz event
 * must reach the engine port and commit, including auto-derived {@code api.}/{@code mcp.} resources
 * that the legacy API-derivation path used to handle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthzEventToEngineIntegrationTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private DeployerFactory deployerFactory;

    private RecordingPort port;

    @BeforeEach
    void setUp() {
        port = new RecordingPort();
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient()
            .when(deployerFactory.createAuthzEntityDeployer())
            .thenReturn(new AuthzEntityDeployer(port, new NoopDistributedSyncService()));
        lenient()
            .when(deployerFactory.createAuthzPolicyDeployer())
            .thenReturn(new AuthzPolicyDeployer(port, new NoopDistributedSyncService()));
    }

    @Test
    void api_resource_entity_from_DB_event_reaches_engine_and_commits() throws InterruptedException {
        AuthzEntitySynchronizer synchronizer = new AuthzEntitySynchronizer(
            fetcher,
            new AuthzEntityMapper(objectMapper),
            deployerFactory,
            port,
            executor(),
            executor()
        );
        Event publish = event(
            "evt-e",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\": \"api.bookings\", \"kind\": \"RESOURCE\", \"attributes\": {}, \"parents\": []}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(port.ops).containsExactly("addOrUpdateEntity:Resource::\"api.bookings\"", "commit");
    }

    @Test
    void api_resource_policy_from_DB_event_reaches_engine_and_commits() throws InterruptedException {
        AuthzPolicySynchronizer synchronizer = new AuthzPolicySynchronizer(
            fetcher,
            new AuthzPolicyMapper(objectMapper),
            deployerFactory,
            port,
            executor(),
            executor()
        );
        Event publish = event(
            "evt-p",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-1\", \"name\": \"Bookings\", \"kind\": \"RESOURCE\", \"entityId\": \"api.bookings\", \"policyText\": \"permit(p,a,r);\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        assertThat(port.ops).containsExactly("addOrUpdatePolicy:doc-1", "commit");
    }

    private static ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    private static Event event(String id, EventType type, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(payload);
        return event;
    }

    private static class RecordingPort implements AuthzEnginePort {

        final ConcurrentLinkedQueue<String> ops = new ConcurrentLinkedQueue<>();

        @Override
        public Completable addOrUpdateEntity(String uid, Map<String, Object> attributes, List<String> parents) {
            ops.add("addOrUpdateEntity:" + uid);
            return Completable.complete();
        }

        @Override
        public Completable removeEntity(String uid) {
            ops.add("removeEntity:" + uid);
            return Completable.complete();
        }

        @Override
        public Completable addOrUpdatePolicy(String docId, String name, String policyText) {
            ops.add("addOrUpdatePolicy:" + docId);
            return Completable.complete();
        }

        @Override
        public Completable removePolicy(String docId) {
            ops.add("removePolicy:" + docId);
            return Completable.complete();
        }

        @Override
        public Completable commit() {
            ops.add("commit");
            return Completable.complete();
        }
    }
}
