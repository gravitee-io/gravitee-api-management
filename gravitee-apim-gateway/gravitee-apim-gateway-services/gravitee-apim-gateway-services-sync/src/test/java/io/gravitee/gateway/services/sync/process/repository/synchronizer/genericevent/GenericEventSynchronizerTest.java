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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.genericevent.GenericEventManager;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.GenericEventDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenericEventSynchronizerTest {

    private static final Set<String> ENVIRONMENTS = Set.of("env-1");

    @Mock
    private LatestEventFetcher fetcher;

    @Mock
    private GenericEventManager genericEventManager;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private GenericEventDeployer deployer;

    private GenericEventSynchronizer cut;

    @BeforeEach
    void beforeEach() {
        cut = new GenericEventSynchronizer(
            fetcher,
            new GenericEventMapper(new ObjectMapper()),
            genericEventManager,
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(deployerFactory.createGenericEventDeployer()).thenReturn(deployer);
        lenient().when(deployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Test
    void should_use_the_generic_event_order() {
        assertThat(cut.order()).isEqualTo(Order.GENERIC_EVENT.index());
    }

    @Test
    void should_only_fetch_publish_events_on_initial_sync() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(-1L, Instant.now().toEpochMilli(), ENVIRONMENTS).test().await().assertComplete();

        verify(fetcher).fetchLatest(
            eq(-1L),
            any(),
            eq(Event.EventProperties.GENERIC_EVENT_ID),
            eq(ENVIRONMENTS),
            eq(Set.of(EventType.PUBLISH_GENERIC_EVENT))
        );
    }

    @Test
    void should_fetch_publish_and_unpublish_events_on_incremental_sync() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(123L, Instant.now().toEpochMilli(), ENVIRONMENTS).test().await().assertComplete();

        verify(fetcher).fetchLatest(
            eq(123L),
            any(),
            eq(Event.EventProperties.GENERIC_EVENT_ID),
            eq(ENVIRONMENTS),
            eq(Set.of(EventType.PUBLISH_GENERIC_EVENT, EventType.UNPUBLISH_GENERIC_EVENT))
        );
    }

    @Test
    void should_deploy_published_and_undeploy_unpublished_events_then_distribute_and_commit_once() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(
                List.of(
                    event("event-1", EventType.PUBLISH_GENERIC_EVENT, "hub-1", "{\"hubId\":\"hub-1\"}"),
                    event("event-2", EventType.UNPUBLISH_GENERIC_EVENT, "app-1", null)
                )
            )
        );

        cut.synchronize(123L, Instant.now().toEpochMilli(), ENVIRONMENTS).test().await().assertComplete();

        verify(deployer).deploy(argThat(d -> d.id().equals("event-1") && d.syncAction() == SyncAction.DEPLOY));
        verify(deployer).undeploy(argThat(d -> d.id().equals("event-2") && d.syncAction() == SyncAction.UNDEPLOY));
        verify(deployer).doAfterDeployment(argThat(d -> d.id().equals("event-1")));
        verify(deployer).doAfterUndeployment(argThat(d -> d.id().equals("event-2")));
        var order = inOrder(deployer, genericEventManager);
        order.verify(deployer).doAfterUndeployment(any());
        order.verify(genericEventManager).commit();
    }

    @Test
    void should_distribute_the_removal_of_events_that_expired_at_commit() throws InterruptedException {
        var expired = new GenericEvent("aihub.hub", "hub-1", "env-1", "org-1", Instant.parse("2026-01-01T00:00:00Z"), null, "{}");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        when(genericEventManager.commit()).thenReturn(List.of(expired));

        cut.synchronize(123L, Instant.now().toEpochMilli(), ENVIRONMENTS).test().await().assertComplete();

        verify(deployer).doAfterUndeployment(
            argThat(
                d ->
                    d.syncAction() == SyncAction.UNDEPLOY &&
                    d.genericEvent() == expired &&
                    d.id().equals(GenericEventPayload.latestEventId(expired))
            )
        );
    }

    @Test
    void should_complete_when_distributing_an_expiry_fails() throws InterruptedException {
        var expired = new GenericEvent("aihub.hub", "hub-1", "env-1", "org-1", Instant.parse("2026-01-01T00:00:00Z"), null, "{}");
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
        when(genericEventManager.commit()).thenReturn(List.of(expired));
        when(deployer.doAfterUndeployment(any())).thenReturn(Completable.error(new RuntimeException("store down")));

        cut.synchronize(123L, Instant.now().toEpochMilli(), ENVIRONMENTS).test().await().assertComplete();
    }

    @Test
    void should_skip_an_unreadable_event_and_still_deploy_the_others() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(
                List.of(
                    Event.builder().id("broken").type(EventType.PUBLISH_GENERIC_EVENT).payload("not json").build(),
                    event("event-1", EventType.PUBLISH_GENERIC_EVENT, "hub-1", "{\"hubId\":\"hub-1\"}")
                )
            )
        );

        cut.synchronize(123L, Instant.now().toEpochMilli(), ENVIRONMENTS).test().await().assertComplete();

        verify(deployer).deploy(argThat(d -> d.id().equals("event-1")));
        verify(genericEventManager).commit();
    }

    @Test
    void should_keep_syncing_when_a_event_fails_to_deploy() throws InterruptedException {
        when(deployer.deploy(argThat(d -> d.id().equals("event-1")))).thenReturn(Completable.error(new SyncException("boom")));
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(
            Flowable.just(
                List.of(
                    event("event-1", EventType.PUBLISH_GENERIC_EVENT, "hub-1", "{\"hubId\":\"hub-1\"}"),
                    event("event-2", EventType.PUBLISH_GENERIC_EVENT, "hub-2", "{\"hubId\":\"hub-2\"}")
                )
            )
        );

        cut.synchronize(123L, Instant.now().toEpochMilli(), ENVIRONMENTS).test().await().assertComplete();

        verify(deployer).deploy(argThat(d -> d.id().equals("event-2")));
        verify(genericEventManager).commit();
    }

    @Test
    void should_not_commit_when_fetching_fails() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.error(new RuntimeException("db down")));

        cut.synchronize(123L, Instant.now().toEpochMilli(), ENVIRONMENTS).test().await().assertError(RuntimeException.class);

        verify(genericEventManager, never()).commit();
    }

    private static Event event(String id, EventType type, String genericEventId, String data) {
        var payload =
            "{\"kind\":\"aihub.hub\",\"id\":\"" +
            genericEventId +
            "\",\"environmentId\":\"env-1\"" +
            (data == null ? "" : ",\"payload\":" + data) +
            "}";
        return Event.builder().id(id).type(type).payload(payload).build();
    }
}
