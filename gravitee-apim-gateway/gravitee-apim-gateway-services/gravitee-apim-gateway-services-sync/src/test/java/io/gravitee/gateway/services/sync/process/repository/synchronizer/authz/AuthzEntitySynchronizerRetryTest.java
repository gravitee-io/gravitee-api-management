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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzEntityDeployer;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthzEntitySynchronizerRetryTest {

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
    void a_transiently_failed_entity_stage_is_retried_and_committed_on_the_next_cycle() throws InterruptedException {
        Event publish = entityEvent("evt-p", EventType.PUBLISH_AUTHZ_ENTITY, "ent-p");

        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("PDP busy")));
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // commit() is now always called; with nothing armed this cycle it short-circuits internally.
        verify(port).commit();

        when(deployer.deploy(any())).thenReturn(Completable.complete());
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(124L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(2)).deploy(any());
        verify(port, times(2)).commit();
    }

    @Test
    void a_transiently_failed_entity_undeploy_is_retried_and_committed_on_the_next_cycle() throws InterruptedException {
        Event unpublish = unpublishEvent("evt-u", "ent-u");

        when(deployer.undeploy(any())).thenReturn(Completable.error(new RuntimeException("engine not ready")));
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(unpublish)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // commit() is now always called; with nothing armed this cycle it short-circuits internally.
        verify(port).commit();

        when(deployer.undeploy(any())).thenReturn(Completable.complete());
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(124L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(2)).undeploy(any());
        verify(port, times(2)).commit();
    }

    @Test
    void an_unpublish_cancels_a_pending_entity_publish() throws InterruptedException {
        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("PDP busy")));
        Event publish = entityEvent("evt-p", EventType.PUBLISH_AUTHZ_ENTITY, "ent-x");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));
        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        when(deployer.undeploy(any())).thenReturn(Completable.error(new RuntimeException("engine not ready")));
        Event unpublish = unpublishEvent("evt-u", "ent-x");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(unpublish)));
        synchronizer.synchronize(124L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        when(deployer.deploy(any())).thenReturn(Completable.complete());
        when(deployer.undeploy(any())).thenReturn(Completable.complete());
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        synchronizer.synchronize(125L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(1)).deploy(any());
    }

    @Test
    void a_permanently_failing_entity_stage_stops_after_max_attempts() throws InterruptedException {
        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("permanently bad")));
        Event publish = entityEvent("evt-p", EventType.PUBLISH_AUTHZ_ENTITY, "ent-dead");

        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        for (int i = 0; i < AuthzEntitySynchronizer.MAX_PENDING_ATTEMPTS + 2; i++) {
            synchronizer.synchronize((long) (i + 2), Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        }

        when(deployer.deploy(any())).thenReturn(Completable.complete());
        synchronizer.synchronize(999L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(AuthzEntitySynchronizer.MAX_PENDING_ATTEMPTS + 1)).deploy(any());
    }

    private static Event entityEvent(String id, EventType type, String entityId) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload("{\"entityId\": \"" + entityId + "\", \"kind\": \"RESOURCE\", \"environmentId\": \"env-1\", \"attributes\": {}}");
        return event;
    }

    private static Event unpublishEvent(String id, String entityId) {
        Event event = new Event();
        event.setId(id);
        event.setType(EventType.UNPUBLISH_AUTHZ_ENTITY);
        event.setPayload("{\"entityId\": \"" + entityId + "\", \"environmentId\": \"env-1\"}");
        return event;
    }
}
