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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzPolicyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
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
class AuthzPolicySynchronizerScopeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private AuthzPolicyDeployer deployer;

    @Mock
    private AuthzEnginePort port;

    private AuthzPolicySynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new AuthzPolicySynchronizer(
            fetcher,
            new AuthzPolicyMapper(objectMapper),
            deployerFactory,
            port,
            new AuthzScopePlacement(),
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
    void routes_mixed_scope_batch_end_to_end() throws InterruptedException {
        Event evtA = event(
            "evt-a",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\":\"p-a\",\"name\":\"A\",\"kind\":\"GLOBAL\",\"policyText\":\"permit(principal, action, resource);\",\"targetPdpIds\":[\"api-a\"]}"
        );
        Event evtB = event(
            "evt-b",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\":\"p-b\",\"name\":\"B\",\"kind\":\"GLOBAL\",\"policyText\":\"permit(principal, action, resource);\",\"targetPdpIds\":[\"*\"]}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(evtA, evtB)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzPolicyReactorDeployable> captor = ArgumentCaptor.forClass(AuthzPolicyReactorDeployable.class);
        verify(deployer, times(2)).deploy(captor.capture());

        List<AuthzPolicyReactorDeployable> deployables = captor.getAllValues();
        assertThat(deployables).anyMatch(d -> d.targetPdpIds().equals(Set.of("api-a")));
        assertThat(deployables).anyMatch(d -> d.targetPdpIds().equals(Set.of("*")));

        verify(port).commit();
    }

    @Test
    void unpublish_without_targetPdpIds_falls_back_to_last_placement_for_eviction() throws InterruptedException {
        // Cycle 1: deploy the policy to scope "api-a" — the synchronizer records the placement.
        Event publish = event(
            "evt-pub",
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\":\"p-1\",\"name\":\"P\",\"kind\":\"GLOBAL\",\"policyText\":\"permit(principal, action, resource);\",\"targetPdpIds\":[\"api-a\"]}"
        );
        // Cycle 2: the UNPUBLISH carries no targetPdpIds (e.g. the PDP was already deleted), so eviction
        // must fall back to the recorded placement instead of removing from an empty scope set (orphan).
        Event unpublish = event("evt-unpub", EventType.UNPUBLISH_AUTHZ_POLICY, "{\"id\":\"p-1\",\"kind\":\"GLOBAL\"}");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.just(List.of(publish)))
            .thenReturn(Flowable.just(List.of(unpublish)));

        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzPolicyReactorDeployable> captor = ArgumentCaptor.forClass(AuthzPolicyReactorDeployable.class);
        verify(deployer).undeploy(captor.capture());
        assertThat(captor.getValue().targetPdpIds()).isEqualTo(Set.of("api-a"));
    }

    private static Event event(String id, EventType type, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(payload);
        return event;
    }
}
