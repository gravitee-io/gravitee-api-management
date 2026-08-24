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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzSchemaDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mirrors {@code AuthzPolicySynchronizerTest} and {@code AuthzEntitySynchronizerTest} so the schema route
 * is held to the same contract as its siblings. Retarget eviction has its own coverage in
 * {@code AuthzWildcardEvictionTest}, which owns the D7 carve-out cases.
 */
@ExtendWith(MockitoExtension.class)
class AuthzSchemaSynchronizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private AuthzSchemaDeployer deployer;

    @Mock
    private AuthzEnginePort port;

    private AuthzSchemaSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new AuthzSchemaSynchronizer(
            fetcher,
            new AuthzSchemaMapper(objectMapper),
            deployerFactory,
            port,
            new AuthzScopePlacement(),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );

        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(deployerFactory.createAuthzSchemaDeployer()).thenReturn(deployer);
        lenient().when(deployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
        lenient().when(port.commit()).thenReturn(Completable.complete());
    }

    @Test
    void order_is_AUTHZ_SCHEMA() {
        assertThat(synchronizer.order()).isEqualTo(Order.AUTHZ_SCHEMA.index());
    }

    @Test
    void schema_is_ordered_before_policies_and_entities() {
        // A schema staged after the policies that depend on it takes effect a full cycle late.
        assertThat(synchronizer.order()).isLessThan(Order.AUTHZ_POLICY.index()).isLessThan(Order.AUTHZ_ENTITY.index());
    }

    @Test
    void no_events_still_calls_commit_to_retry_any_pending() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

        verify(port).commit();
    }

    @Test
    void publish_and_unpublish_are_handled_in_one_batch() throws InterruptedException {
        Event publish = schemaEvent("doc-p", "[\"orders\"]");
        Event unpublish = event("evt-u", EventType.UNPUBLISH_AUTHZ_SCHEMA, "{\"id\":\"doc-u\",\"environmentId\":\"env-1\"}");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish, unpublish)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer).deploy(any());
        verify(deployer).undeploy(any());
        verify(port).commit();
    }

    @Test
    void commit_fires_after_the_deploy_completes() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(schemaEvent("doc-p", "[\"orders\"]")))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        var inOrder = inOrder(deployer, port);
        inOrder.verify(deployer).deploy(any());
        inOrder.verify(port).commit();
    }

    @Test
    void unparseable_event_payload_is_dropped_without_breaking_the_batch() throws InterruptedException {
        Event bad = event("evt-bad", EventType.PUBLISH_AUTHZ_SCHEMA, "not-json");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(bad, schemaEvent("doc-good", "[\"orders\"]")))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(1)).deploy(any());
        verify(port).commit();
    }

    @Test
    void a_whole_cycle_of_documents_yields_a_single_commit() throws InterruptedException {
        List<Event> events = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            events.add(schemaEvent("doc-" + i, "[\"orders\"]"));
        }
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(events));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(50)).deploy(any());
        verify(port, times(1)).commit();
    }

    @Test
    void the_deployable_carries_the_documents_declared_targets() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(schemaEvent("doc-t", "[\"orders\",\"stock\"]")))
        );

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzSchemaReactorDeployable> captor = ArgumentCaptor.forClass(AuthzSchemaReactorDeployable.class);
        verify(deployer).deploy(captor.capture());
        assertThat(captor.getValue().targetPdpIds()).containsExactlyInAnyOrder("orders", "stock");
        assertThat(captor.getValue().schemaText()).isEqualTo("entity User;");
    }

    private static Event schemaEvent(String id, String targets) {
        return event(
            "evt-" + id + "-" + targets.hashCode(),
            EventType.PUBLISH_AUTHZ_SCHEMA,
            "{\"id\":\"" +
                id +
                "\",\"name\":\"S\",\"environmentId\":\"env-1\",\"schemaText\":\"entity User;\",\"targetPdpIds\":" +
                targets +
                "}"
        );
    }

    private static Event event(String id, EventType type, String payload) {
        Event e = new Event();
        e.setId(id);
        e.setType(type);
        e.setPayload(payload);
        e.setUpdatedAt(new java.util.Date());
        return e;
    }
}
