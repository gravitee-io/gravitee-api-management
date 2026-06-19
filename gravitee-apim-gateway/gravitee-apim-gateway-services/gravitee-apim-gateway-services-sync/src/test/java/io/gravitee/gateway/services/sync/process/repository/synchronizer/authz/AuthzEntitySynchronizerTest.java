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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzEntityDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class AuthzEntitySynchronizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private AuthzEntityDeployer deployer;

    @Mock
    private AuthzEnginePort port;

    private AuthzEntitySynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new AuthzEntitySynchronizer(
            fetcher,
            new AuthzEntityMapper(objectMapper),
            deployerFactory,
            port,
            new AuthzScopePlacement(),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );

        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(deployerFactory.createAuthzEntityDeployer()).thenReturn(deployer);
        lenient().when(deployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
        lenient().when(port.commit()).thenReturn(Completable.complete());
    }

    @Test
    void order_is_AUTHZ_ENTITY() {
        assertThat(synchronizer.order()).isEqualTo(Order.AUTHZ_ENTITY.index());
    }

    @Test
    void no_events_still_calls_commit_to_retry_any_pending() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

        // commit() is always called; it short-circuits internally when nothing is armed, so a previous
        // cycle's failed-and-re-armed commit still gets retried even on a cycle with no new events.
        verify(port).commit();
    }

    @Test
    void publish_event_drives_deployer_then_engine_commit() throws InterruptedException {
        Event publish = event(
            "evt-1",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\": \"custom.bookings\", \"kind\": \"RESOURCE\", \"attributes\": {}, \"parents\": []}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer).deploy(any(AuthzEntityReactorDeployable.class));
        verify(port).commit();
    }

    @Test
    void unpublish_without_targetPdpIds_falls_back_to_last_placement_for_eviction() throws InterruptedException {
        // Cycle 1: deploy the entity to scope "api-a" — the synchronizer records the placement.
        Event publish = event(
            "evt-pub",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\":\"custom.x\",\"kind\":\"RESOURCE\",\"attributes\":{},\"parents\":[],\"environmentId\":\"env-1\",\"targetPdpIds\":[\"api-a\"]}"
        );
        // Cycle 2: the UNPUBLISH carries no targetPdpIds, so eviction must fall back to the recorded
        // placement instead of removing from an empty scope set (which would orphan the entity).
        Event unpublish = event(
            "evt-unpub",
            EventType.UNPUBLISH_AUTHZ_ENTITY,
            "{\"entityId\":\"custom.x\",\"kind\":\"RESOURCE\",\"environmentId\":\"env-1\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.just(List.of(publish)))
            .thenReturn(Flowable.just(List.of(unpublish)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzEntityReactorDeployable> captor = ArgumentCaptor.forClass(AuthzEntityReactorDeployable.class);
        verify(deployer).undeploy(captor.capture());
        assertThat(captor.getValue().targetPdpIds()).isEqualTo(Set.of("api-a"));
    }

    @Test
    void unpublish_event_on_INIT_is_filtered_out() throws InterruptedException {
        Event publish = event("evt-1", EventType.PUBLISH_AUTHZ_ENTITY, "{\"entityId\": \"custom.x\", \"kind\": \"RESOURCE\"}");
        when(fetcher.fetchLatest(any(), any(), any(), any(), eqSet(EventType.PUBLISH_AUTHZ_ENTITY))).thenReturn(
            Flowable.just(List.of(publish))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer).deploy(any());
        verify(deployer, never()).undeploy(any());
    }

    @Test
    void incremental_sync_handles_both_publish_and_unpublish() throws InterruptedException {
        Event publish = event("evt-1", EventType.PUBLISH_AUTHZ_ENTITY, "{\"entityId\": \"custom.x\", \"kind\": \"RESOURCE\"}");
        Event unpublish = event("evt-2", EventType.UNPUBLISH_AUTHZ_ENTITY, "{\"entityId\": \"custom.y\", \"environmentId\": \"env-1\"}");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish, unpublish)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer).deploy(any());
        verify(deployer).undeploy(any());
        verify(port).commit();
    }

    @Test
    void smoke_100_entities_in_one_cycle_yields_single_commit() throws InterruptedException {
        List<Event> events = new java.util.ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            events.add(
                event(
                    "evt-" + i,
                    EventType.PUBLISH_AUTHZ_ENTITY,
                    "{\"entityId\": \"custom.api-" + i + "\", \"kind\": \"RESOURCE\", \"attributes\": {}, \"parents\": []}"
                )
            );
        }
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(events));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(100)).deploy(any(AuthzEntityReactorDeployable.class));
        verify(port, times(1)).commit();
    }

    @Test
    void unparseable_event_payload_is_dropped_without_failing_the_batch() throws InterruptedException {
        Event bad = event("evt-bad", EventType.PUBLISH_AUTHZ_ENTITY, "not-json");
        Event good = event("evt-good", EventType.PUBLISH_AUTHZ_ENTITY, "{\"entityId\": \"custom.x\", \"kind\": \"RESOURCE\"}");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(bad, good)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer).deploy(any());
        verify(port).commit();
    }

    @Test
    void cold_sync_deploys_auto_derived_resource_entities() throws InterruptedException {
        Event apiResource = event("evt-api", EventType.PUBLISH_AUTHZ_ENTITY, "{\"entityId\": \"api.bookings\", \"kind\": \"RESOURCE\"}");
        Event mcpResource = event(
            "evt-mcp",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\": \"mcp.bookings.get\", \"kind\": \"RESOURCE\"}"
        );
        Event agentResource = event("evt-agent", EventType.PUBLISH_AUTHZ_ENTITY, "{\"entityId\": \"agent.bot\", \"kind\": \"RESOURCE\"}");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(List.of(apiResource, mcpResource, agentResource))
        );

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(3)).deploy(any());
        verify(port).commit();
    }

    @Test
    void cold_sync_keeps_principal_entities() throws InterruptedException {
        Event principal = event(
            "evt-principal",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\": \"idp.am.alice\", \"kind\": \"PRINCIPAL\", \"attributes\": {}, \"parents\": []}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(principal)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzEntityReactorDeployable> captor = ArgumentCaptor.forClass(AuthzEntityReactorDeployable.class);
        verify(deployer).deploy(captor.capture());
        assertThat(captor.getValue().entityId()).isEqualTo("idp.am.alice");
        verify(port).commit();
    }

    @Test
    void cold_sync_keeps_custom_resource_entities() throws InterruptedException {
        Event customResource = event(
            "evt-custom",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\": \"team.acme.docs\", \"kind\": \"RESOURCE\", \"attributes\": {}, \"parents\": []}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(customResource)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzEntityReactorDeployable> captor = ArgumentCaptor.forClass(AuthzEntityReactorDeployable.class);
        verify(deployer).deploy(captor.capture());
        assertThat(captor.getValue().entityId()).isEqualTo("team.acme.docs");
        verify(port).commit();
    }

    @Test
    void incremental_deploys_auto_derived_resource_regardless_of_hosting() throws InterruptedException {
        Event apiResource = event(
            "evt-api",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\": \"api.bookings\", \"kind\": \"RESOURCE\", \"attributes\": {}, \"parents\": []}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(apiResource)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer).deploy(any());
        verify(port).commit();
    }

    @Test
    void incremental_deploys_auto_derived_resource_even_when_api_hosted() throws InterruptedException {
        Event apiResource = event(
            "evt-api",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\": \"api.bookings\", \"kind\": \"RESOURCE\", \"attributes\": {}, \"parents\": []}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(apiResource)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzEntityReactorDeployable> captor = ArgumentCaptor.forClass(AuthzEntityReactorDeployable.class);
        verify(deployer).deploy(captor.capture());
        assertThat(captor.getValue().entityId()).isEqualTo("api.bookings");
        verify(port).commit();
    }

    @Test
    void retarget_evicts_dropped_scope_via_placement() throws InterruptedException {
        ArgumentCaptor<AuthzEntityReactorDeployable> captor = ArgumentCaptor.forClass(AuthzEntityReactorDeployable.class);

        // Cycle 1: entity targets scope-a.
        Event toA = event(
            "evt-1",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\":\"custom.x\",\"kind\":\"RESOURCE\",\"environmentId\":\"env-1\",\"targetPdpIds\":[\"scope-a\"]}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(toA)));
        synchronizer.synchronize(1L, 2L, Set.of("env-1")).test().await().assertComplete();

        // Cycle 2: re-targeted to scope-b. Only the latest PUBLISH survives the fetcher — the fix must
        // still evict scope-a, derived from the applied placement (not from any per-event delta).
        Event toB = event(
            "evt-2",
            EventType.PUBLISH_AUTHZ_ENTITY,
            "{\"entityId\":\"custom.x\",\"kind\":\"RESOURCE\",\"environmentId\":\"env-1\",\"targetPdpIds\":[\"scope-b\"]}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(toB)));
        synchronizer.synchronize(2L, 3L, Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(2)).deploy(captor.capture());
        assertThat(captor.getAllValues().get(0).removedTargetPdpIds()).isEmpty();
        assertThat(captor.getAllValues().get(1).targetPdpIds()).containsExactly("scope-b");
        assertThat(captor.getAllValues().get(1).removedTargetPdpIds()).containsExactly("scope-a");
    }

    private static Event event(String id, EventType type, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(payload);
        return event;
    }

    private static Set<EventType> eqSet(EventType... types) {
        return org.mockito.ArgumentMatchers.argThat(actual -> actual != null && actual.equals(Set.of(types)));
    }
}
