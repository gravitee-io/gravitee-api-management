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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzPolicyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.service.AuthzRegistry;
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
class AuthzPolicySynchronizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private AuthzPolicyDeployer deployer;

    @Mock
    private AuthzEnginePort port;

    private AuthzRegistry authzRegistry;

    private AuthzPolicySynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        authzRegistry = new AuthzRegistry(null);
        synchronizer = new AuthzPolicySynchronizer(
            fetcher,
            new AuthzPolicyMapper(objectMapper),
            deployerFactory,
            port,
            authzRegistry,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );

        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(deployerFactory.createAuthzPolicyDeployer()).thenReturn(deployer);
        lenient().when(deployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
        lenient().when(port.commit()).thenReturn(Completable.complete());
    }

    @Test
    void order_is_AUTHZ_POLICY() {
        assertThat(synchronizer.order()).isEqualTo(Order.AUTHZ_POLICY.index());
    }

    @Test
    void no_events_skips_commit() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

        verify(port, never()).commit();
    }

    @Test
    void INIT_deploys_GLOBAL_policy_and_skips_RESOURCE_to_avoid_appender_race() throws InterruptedException {
        Event globalEvt = event(
            "evt-g",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-g\", \"name\": \"Global\", \"kind\": \"GLOBAL\", \"policyText\": \"permit(p,a,r);\"}"
        );
        Event resourceEvt = event(
            "evt-r",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-r\", \"name\": \"R\", \"kind\": \"RESOURCE\", \"entityId\": \"api.x\", \"policyText\": \"permit(...);\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(globalEvt, resourceEvt)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(1)).deploy(any());
        verify(port).commit();
    }

    @Test
    void INCREMENTAL_deploys_both_GLOBAL_and_RESOURCE() throws InterruptedException {
        authzRegistry.registerForApi("api.x", List.of("api.x"));
        Event globalEvt = event(
            "evt-g",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-g\", \"name\": \"Global\", \"kind\": \"GLOBAL\", \"policyText\": \"permit(p,a,r);\"}"
        );
        Event resourceEvt = event(
            "evt-r",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-r\", \"name\": \"R\", \"kind\": \"RESOURCE\", \"entityId\": \"api.x\", \"policyText\": \"permit(...);\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(globalEvt, resourceEvt)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(2)).deploy(any());
        verify(port).commit();
    }

    @Test
    void INCREMENTAL_handles_publish_and_unpublish_in_one_batch() throws InterruptedException {
        Event publish = event(
            "evt-p",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-p\", \"name\": \"P\", \"kind\": \"GLOBAL\", \"policyText\": \"permit(p,a,r);\"}"
        );
        Event unpublish = event("evt-u", EventType.UNPUBLISH_AUTHZ_POLICY, "{\"id\": \"doc-u\", \"environmentId\": \"env-1\"}");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish, unpublish)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer).deploy(any());
        verify(deployer).undeploy(any());
        verify(port).commit();
    }

    @Test
    void commit_fires_after_all_deploy_ops_complete() throws InterruptedException {
        Event publish = event(
            "evt-p",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-p\", \"name\": \"P\", \"kind\": \"GLOBAL\", \"policyText\": \"permit(p,a,r);\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        var inOrder = inOrder(deployer, port);
        inOrder.verify(deployer).deploy(any());
        inOrder.verify(port).commit();
    }

    @Test
    void smoke_100_policies_in_one_cycle_yields_single_commit() throws InterruptedException {
        List<Event> events = new java.util.ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            events.add(
                event(
                    "evt-" + i,
                    EventType.PUBLISH_AUTHZ_POLICY,
                    "{\"id\": \"doc-" + i + "\", \"name\": \"P" + i + "\", \"kind\": \"GLOBAL\", \"policyText\": \"permit(p,a,r);\"}"
                )
            );
        }
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(events));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(100)).deploy(any());
        verify(port, times(1)).commit();
    }

    @Test
    void unparseable_event_payload_is_dropped_without_breaking_the_batch() throws InterruptedException {
        Event bad = event("evt-bad", EventType.PUBLISH_AUTHZ_POLICY, "not-json");
        Event good = event(
            "evt-good",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-good\", \"name\": \"G\", \"kind\": \"GLOBAL\", \"policyText\": \"permit(p,a,r);\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(bad, good)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(1)).deploy(any());
        verify(port).commit();
    }

    @Test
    void cold_sync_skips_all_resource_policies() throws InterruptedException {
        authzRegistry.registerForApi("api.bookings", List.of("api.bookings"));
        Event autoDerived = event(
            "evt-auto",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-auto\", \"name\": \"A\", \"kind\": \"RESOURCE\", \"entityId\": \"api.bookings\", \"policyText\": \"permit(...);\"}"
        );
        Event custom = event(
            "evt-custom",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-custom\", \"name\": \"C\", \"kind\": \"RESOURCE\", \"entityId\": \"team.acme.docs\", \"policyText\": \"permit(...);\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(autoDerived, custom)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, never()).deploy(any());
        verify(port, never()).commit();
    }

    @Test
    void incremental_skips_auto_derived_resource_policy_when_api_not_hosted() throws InterruptedException {
        Event autoDerived = event(
            "evt-auto",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-auto\", \"name\": \"A\", \"kind\": \"RESOURCE\", \"entityId\": \"api.bookings\", \"policyText\": \"permit(...);\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(autoDerived)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, never()).deploy(any());
        verify(port, never()).commit();
    }

    @Test
    void incremental_keeps_auto_derived_resource_policy_when_api_hosted() throws InterruptedException {
        authzRegistry.registerForApi("api.bookings", List.of("api.bookings"));
        Event autoDerived = event(
            "evt-auto",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-auto\", \"name\": \"A\", \"kind\": \"RESOURCE\", \"entityId\": \"api.bookings\", \"policyText\": \"permit(...);\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(autoDerived)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzPolicyReactorDeployable> captor = ArgumentCaptor.forClass(AuthzPolicyReactorDeployable.class);
        verify(deployer).deploy(captor.capture());
        assertThat(captor.getValue().docId()).isEqualTo("doc-auto");
        verify(port).commit();
    }

    @Test
    void incremental_keeps_custom_resource_policies() throws InterruptedException {
        Event custom = event(
            "evt-custom",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\": \"doc-custom\", \"name\": \"C\", \"kind\": \"RESOURCE\", \"entityId\": \"team.acme.docs\", \"policyText\": \"permit(...);\"}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(custom)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzPolicyReactorDeployable> captor = ArgumentCaptor.forClass(AuthzPolicyReactorDeployable.class);
        verify(deployer).deploy(captor.capture());
        assertThat(captor.getValue().docId()).isEqualTo("doc-custom");
        verify(port).commit();
    }

    private static Event event(String id, EventType type, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(payload);
        return event;
    }
}
