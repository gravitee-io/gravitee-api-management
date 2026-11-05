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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventSubscriber;
import io.gravitee.gateway.genericevent.DefaultGenericEventManager;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.GenericEventDeployer;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runs the sync chain as a gateway does, from the latest events to a subscriber, with only the repository faked.
 */
class GenericEventSyncFlowTest {

    private static final Set<String> ENVIRONMENTS = Set.of("env-1");
    private static final Instant T0 = Instant.parse("2026-11-04T10:00:00Z");

    private final InMemoryEventLatestRepository repository = new InMemoryEventLatestRepository();
    private DefaultGenericEventManager manager;
    private GenericEventSynchronizer synchronizer;

    @BeforeEach
    void beforeEach() {
        startGateway();
    }

    @Test
    void should_replay_the_synced_events_to_a_late_subscriber() {
        repository.publish("aihub.hub", "hub-1", "{\"v\":1}", T0);
        sync(-1L, T0);

        var subscriber = new Recorder();
        manager.subscribe("aihub", subscriber);

        assertThat(subscriber.calls).containsExactly("upsert hub-1 {\"v\":1}", "commit");
    }

    @Test
    void should_deliver_an_update_once_although_the_sync_windows_overlap() {
        repository.publish("aihub.hub", "hub-1", "{\"v\":1}", T0);
        sync(-1L, T0);
        var subscriber = new Recorder();
        manager.subscribe("aihub", subscriber);
        subscriber.calls.clear();

        repository.publish("aihub.hub", "hub-1", "{\"v\":2}", T0.plusSeconds(5));
        sync(T0.toEpochMilli(), T0.plusSeconds(5));
        sync(T0.plusSeconds(5).toEpochMilli(), T0.plusSeconds(10));

        assertThat(subscriber.calls).containsExactly("upsert hub-1 {\"v\":2}", "commit");
    }

    @Test
    void should_remove_an_unpublished_event() {
        repository.publish("aihub.hub", "hub-1", "{\"v\":1}", T0);
        sync(-1L, T0);
        var subscriber = new Recorder();
        manager.subscribe("aihub", subscriber);
        subscriber.calls.clear();

        repository.unpublish("aihub.hub", "hub-1", T0.plusSeconds(5));
        sync(T0.toEpochMilli(), T0.plusSeconds(5));

        assertThat(subscriber.calls).containsExactly("remove hub-1", "commit");
        assertThat(manager.size()).isZero();
    }

    @Test
    void should_rebuild_the_current_events_after_a_restart() {
        repository.publish("aihub.hub", "hub-1", "{\"v\":1}", T0);
        repository.publish("aihub.hub", "hub-2", "{\"v\":1}", T0);
        repository.publish("aihub.hub", "hub-1", "{\"v\":2}", T0.plusSeconds(5));
        repository.unpublish("aihub.hub", "hub-2", T0.plusSeconds(5));

        startGateway();
        var subscriber = new Recorder();
        manager.subscribe("aihub", subscriber);
        sync(-1L, T0.plusSeconds(10));

        assertThat(subscriber.calls).containsExactly("commit", "upsert hub-1 {\"v\":2}", "commit");
    }

    private void startGateway() {
        manager = new DefaultGenericEventManager();
        var deployerFactory = mock(DeployerFactory.class);
        when(deployerFactory.createGenericEventDeployer()).thenAnswer(invocation ->
            new GenericEventDeployer(manager, new NoopDistributedSyncService())
        );
        synchronizer = new GenericEventSynchronizer(
            new LatestEventFetcher(repository, 10),
            new GenericEventMapper(new ObjectMapper()),
            manager,
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );
    }

    private void sync(Long from, Instant to) {
        synchronizer.synchronize(from, to.toEpochMilli(), ENVIRONMENTS).blockingAwait();
    }

    static class Recorder implements GenericEventSubscriber {

        final List<String> calls = new ArrayList<>();

        @Override
        public void onUpsert(GenericEvent event) {
            calls.add("upsert " + event.id() + " " + event.payload());
        }

        @Override
        public void onRemove(GenericEvent event) {
            calls.add("remove " + event.id());
        }

        @Override
        public void onCommit() {
            calls.add("commit");
        }
    }

    /** Holds one row per environment, kind and id, written the way the control plane writes it. */
    static class InMemoryEventLatestRepository implements EventLatestRepository {

        private final Map<String, Event> rows = new LinkedHashMap<>();

        void publish(String kind, String id, String payload, Instant at) {
            write(EventType.PUBLISH_GENERIC_EVENT, kind, id, ",\"payload\":" + payload, at);
        }

        void unpublish(String kind, String id, Instant at) {
            write(EventType.UNPUBLISH_GENERIC_EVENT, kind, id, "", at);
        }

        private void write(EventType type, String kind, String id, String payload, Instant at) {
            var envelope =
                "{\"kind\":\"%s\",\"id\":\"%s\",\"environmentId\":\"env-1\",\"organizationId\":\"org-1\",\"updatedAt\":\"%s\"%s}".formatted(
                    kind,
                    id,
                    at,
                    payload
                );
            var latestId = UUID.nameUUIDFromBytes(("env-1:" + kind + ":" + id).getBytes(StandardCharsets.UTF_8)).toString();
            rows.put(
                latestId,
                Event.builder()
                    .id(latestId)
                    .type(type)
                    .payload(envelope)
                    .environments(ENVIRONMENTS)
                    .properties(Map.of(Event.EventProperties.GENERIC_EVENT_ID.getValue(), latestId))
                    .createdAt(Date.from(at))
                    .updatedAt(Date.from(at))
                    .build()
            );
        }

        @Override
        public List<Event> search(EventCriteria criteria, Event.EventProperties group, Long page, Long size) {
            return rows
                .values()
                .stream()
                .filter(event -> criteria.getTypes().contains(event.getType()))
                .filter(event -> criteria.getFrom() <= 0 || event.getUpdatedAt().getTime() >= criteria.getFrom())
                .filter(event -> criteria.getTo() <= 0 || event.getUpdatedAt().getTime() < criteria.getTo())
                .sorted(Comparator.comparing(Event::getUpdatedAt))
                .skip(page * size)
                .limit(size)
                .toList();
        }

        @Override
        public Event createOrUpdate(Event event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String eventId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Event> findByEnvironmentId(String environmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Event> findByOrganizationId(String organizationId) {
            throw new UnsupportedOperationException();
        }
    }
}
