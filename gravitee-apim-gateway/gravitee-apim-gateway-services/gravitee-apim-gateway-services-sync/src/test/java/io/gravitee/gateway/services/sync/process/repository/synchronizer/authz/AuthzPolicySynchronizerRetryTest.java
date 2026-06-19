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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthzPolicySynchronizerRetryTest {

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
    void a_transiently_failed_stage_is_retried_and_committed_on_the_next_cycle() throws InterruptedException {
        Event publish = policyEvent("evt-p", EventType.PUBLISH_AUTHZ_POLICY, "doc-p");

        // Cycle 1: the stage relay fails -> nothing committed, but the policy must be remembered.
        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("PDP busy")));
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));

        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // commit() is now always called; with nothing armed this cycle it short-circuits internally.
        verify(port).commit();

        // Cycle 2: window empty, engine healthy. The pending policy must be re-staged and committed.
        when(deployer.deploy(any())).thenReturn(Completable.complete());
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        synchronizer.synchronize(124L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        verify(deployer, times(2)).deploy(any());
        verify(port, times(2)).commit();
    }

    @Test
    void a_transiently_failed_undeploy_is_retried_and_committed_on_the_next_cycle() throws InterruptedException {
        Event unpublish = unpublishEvent("evt-u", "doc-u");

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
    void an_unpublish_cancels_a_pending_publish_so_it_is_not_resurrected() throws InterruptedException {
        // Cycle 1: deploy relay fails -> doc-x kept pending as a deploy.
        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("PDP busy")));
        Event publish = policyEvent("evt-p", EventType.PUBLISH_AUTHZ_POLICY, "doc-x");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));
        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Cycle 2: doc-x is UNPUBLISHED. The undeploy also fails, but the pending deploy must be cleared.
        when(deployer.undeploy(any())).thenReturn(Completable.error(new RuntimeException("engine not ready")));
        Event unpublish = unpublishEvent("evt-u", "doc-x");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(unpublish)));
        synchronizer.synchronize(124L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Cycle 3: empty window, engine healthy. Only the undeploy must be re-driven, never the stale deploy.
        when(deployer.deploy(any())).thenReturn(Completable.complete());
        when(deployer.undeploy(any())).thenReturn(Completable.complete());
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        synchronizer.synchronize(125L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // The deploy was only ever attempted once (cycle 1) and never re-driven after the unpublish.
        verify(deployer, times(1)).deploy(any());
    }

    @Test
    void a_publish_cancels_a_pending_unpublish() throws InterruptedException {
        when(deployer.undeploy(any())).thenReturn(Completable.error(new RuntimeException("engine not ready")));
        Event unpublish = unpublishEvent("evt-u", "doc-y");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(unpublish)));
        synchronizer.synchronize(123L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("PDP busy")));
        Event publish = policyEvent("evt-p", EventType.PUBLISH_AUTHZ_POLICY, "doc-y");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));
        synchronizer.synchronize(124L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        when(deployer.deploy(any())).thenReturn(Completable.complete());
        when(deployer.undeploy(any())).thenReturn(Completable.complete());
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        synchronizer.synchronize(125L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // The undeploy was only ever attempted once (cycle 1) and never re-driven after the publish.
        verify(deployer, times(1)).undeploy(any());
    }

    @Test
    void a_permanently_failing_stage_stops_after_max_attempts() throws InterruptedException {
        when(deployer.deploy(any())).thenReturn(Completable.error(new RuntimeException("permanently bad")));
        Event publish = policyEvent("evt-p", EventType.PUBLISH_AUTHZ_POLICY, "doc-dead");

        // Cycle 1: initial stage fails -> pending (first attempt).
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(publish)));
        synchronizer.synchronize(1L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // Subsequent cycles only ever re-drive from the pending set (window empty).
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        for (int i = 0; i < AuthzPolicySynchronizer.MAX_PENDING_ATTEMPTS + 2; i++) {
            synchronizer.synchronize((long) (i + 2), Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();
        }

        // After exhaustion the pending entry is abandoned: one more healthy cycle re-drives nothing.
        when(deployer.deploy(any())).thenReturn(Completable.complete());
        synchronizer.synchronize(999L, Instant.now().toEpochMilli(), Set.of("env-1")).test().await().assertComplete();

        // The bounded retries are exhausted, so deploy is never attempted on the final healthy cycle.
        // One initial stage + MAX_PENDING_ATTEMPTS re-drives, then abandoned.
        verify(deployer, times(AuthzPolicySynchronizer.MAX_PENDING_ATTEMPTS + 1)).deploy(any());
    }

    private static Event policyEvent(String id, EventType type, String docId) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(
            "{\"id\": \"" +
                docId +
                "\", \"name\": \"P\", \"kind\": \"GLOBAL\", \"environmentId\": \"env-1\", \"policyText\": \"permit(p,a,r);\"}"
        );
        return event;
    }

    private static Event unpublishEvent(String id, String docId) {
        Event event = new Event();
        event.setId(id);
        event.setType(EventType.UNPUBLISH_AUTHZ_POLICY);
        event.setPayload("{\"id\": \"" + docId + "\", \"environmentId\": \"env-1\"}");
        return event;
    }
}
