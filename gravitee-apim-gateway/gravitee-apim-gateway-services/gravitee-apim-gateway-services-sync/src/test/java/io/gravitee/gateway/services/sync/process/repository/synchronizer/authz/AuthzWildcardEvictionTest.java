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
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzSchemaDeployer;
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

/**
 * D7 says a schema wildcard reaches every NAMED engine and never the bootstrap one. The shared
 * {@code deploy} used to drop the whole eviction set on a wildcard, on the premise that a wildcard can
 * never narrow a previously-applied scope away. That premise holds for policies and entities and is
 * false for schemas, so a schema retargeted from "default" to "*" would have stayed on the
 * cross-environment bootstrap engine forever.
 */
@ExtendWith(MockitoExtension.class)
class AuthzWildcardEvictionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private AuthzSchemaDeployer schemaDeployer;

    @Mock
    private AuthzPolicyDeployer policyDeployer;

    @Mock
    private AuthzEnginePort port;

    @BeforeEach
    void setUp() {
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(deployerFactory.createAuthzSchemaDeployer()).thenReturn(schemaDeployer);
        lenient().when(deployerFactory.createAuthzPolicyDeployer()).thenReturn(policyDeployer);
        lenient().when(schemaDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(schemaDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(policyDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(policyDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(port.commit()).thenReturn(Completable.complete());
    }

    @Test
    void a_schema_retargeted_from_default_to_wildcard_is_evicted_from_the_bootstrap_engine() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.just(List.of(schemaEvent("s-1", "[\"default\"]"))))
            .thenReturn(Flowable.just(List.of(schemaEvent("s-1", "[\"*\"]"))));

        AuthzSchemaSynchronizer synchronizer = schemaSynchronizer();
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzSchemaReactorDeployable> captor = ArgumentCaptor.forClass(AuthzSchemaReactorDeployable.class);
        verify(schemaDeployer, times(2)).deploy(captor.capture());
        assertThat(captor.getAllValues().get(1).removedTargetPdpIds())
            .as("the wildcard does not cover the bootstrap engine, so the schema must be evicted from it")
            .containsExactly("default");
    }

    @Test
    void a_document_hydrated_into_a_tagged_scope_is_not_evicted_when_republished_at_its_base() throws InterruptedException {
        // The two sides are recorded differently: hydration stores the routing scope it applied to
        // ("orders@eu"), while a publish carries the declared target ("orders"). addressFor strips the tag,
        // so both name one engine and the republish must not evict the document from it.
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.just(List.of(schemaEvent("s-4", "[\"orders@eu\"]"))))
            .thenReturn(Flowable.just(List.of(schemaEvent("s-4", "[\"orders\"]"))));

        AuthzSchemaSynchronizer synchronizer = schemaSynchronizer();
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzSchemaReactorDeployable> captor = ArgumentCaptor.forClass(AuthzSchemaReactorDeployable.class);
        verify(schemaDeployer, times(2)).deploy(captor.capture());
        assertThat(captor.getAllValues().get(1).removedTargetPdpIds())
            .as("a tagged scope and its base name the same engine, so nothing is dropped")
            .isEmpty();
    }

    @Test
    void a_schema_retargeted_from_a_tagged_bootstrap_engine_to_wildcard_is_still_evicted() throws InterruptedException {
        // A PDP created with a blank targetPdpId is normalised to "default", so "default@eu" is a legal
        // routing scope, and the wildcard reaches it no more than it reaches the bare bootstrap engine.
        // Matching the carve-out on the full string instead of the scope base would strand the document there
        // with nothing able to remove it.
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.just(List.of(schemaEvent("s-3", "[\"default@eu\"]"))))
            .thenReturn(Flowable.just(List.of(schemaEvent("s-3", "[\"*\"]"))));

        AuthzSchemaSynchronizer synchronizer = schemaSynchronizer();
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzSchemaReactorDeployable> captor = ArgumentCaptor.forClass(AuthzSchemaReactorDeployable.class);
        verify(schemaDeployer, times(2)).deploy(captor.capture());
        assertThat(captor.getAllValues().get(1).removedTargetPdpIds())
            .as("a tagged default is still the bootstrap engine, which the wildcard does not reach")
            .containsExactly("default@eu");
    }

    @Test
    void a_schema_targeted_at_both_wildcard_and_default_stays_on_the_bootstrap_engine() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.just(List.of(schemaEvent("s-2", "[\"default\"]"))))
            .thenReturn(Flowable.just(List.of(schemaEvent("s-2", "[\"*\",\"default\"]"))));

        AuthzSchemaSynchronizer synchronizer = schemaSynchronizer();
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzSchemaReactorDeployable> captor = ArgumentCaptor.forClass(AuthzSchemaReactorDeployable.class);
        verify(schemaDeployer, times(2)).deploy(captor.capture());
        assertThat(captor.getAllValues().get(1).removedTargetPdpIds())
            .as("D7 removes the implicit fan-out only; an explicitly named default is still wanted")
            .isEmpty();
    }

    @Test
    void a_policy_retargeted_from_default_to_wildcard_is_still_not_evicted() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.just(List.of(policyEvent("p-1", "[\"default\"]"))))
            .thenReturn(Flowable.just(List.of(policyEvent("p-1", "[\"*\"]"))));

        AuthzPolicySynchronizer synchronizer = new AuthzPolicySynchronizer(
            fetcher,
            new AuthzPolicyMapper(objectMapper),
            deployerFactory,
            port,
            new AuthzScopePlacement(),
            executor(),
            executor()
        );
        synchronizer.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        ArgumentCaptor<AuthzPolicyReactorDeployable> captor = ArgumentCaptor.forClass(AuthzPolicyReactorDeployable.class);
        verify(policyDeployer, times(2)).deploy(captor.capture());
        assertThat(captor.getAllValues().get(1).removedTargetPdpIds())
            .as("a policy wildcard covers the bootstrap engine, so nothing is dropped: retainAll(Set.of()) == clear()")
            .isEmpty();
    }

    private AuthzSchemaSynchronizer schemaSynchronizer() {
        return new AuthzSchemaSynchronizer(
            fetcher,
            new AuthzSchemaMapper(objectMapper),
            deployerFactory,
            port,
            new AuthzScopePlacement(),
            executor(),
            executor()
        );
    }

    private static ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
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

    private static Event policyEvent(String id, String targets) {
        return event(
            "evt-" + id + "-" + targets.hashCode(),
            EventType.PUBLISH_AUTHZ_POLICY,
            "{\"id\":\"" +
                id +
                "\",\"name\":\"P\",\"kind\":\"GLOBAL\",\"policyText\":\"permit(principal, action, resource);\",\"targetPdpIds\":" +
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
