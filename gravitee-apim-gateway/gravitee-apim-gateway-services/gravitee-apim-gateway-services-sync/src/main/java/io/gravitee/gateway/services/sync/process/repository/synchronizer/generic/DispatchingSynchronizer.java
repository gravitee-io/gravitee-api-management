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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.generic;

import io.gravitee.gateway.reactive.core.v4.generic.GenericEvent;
import io.gravitee.gateway.reactive.core.v4.generic.GenericEventObserver;
import io.gravitee.gateway.reactive.core.v4.generic.GenericEventObservers;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.GenericEventFetcher;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.LongSupplier;
import lombok.CustomLog;

/**
 * Hands each {@code GENERIC_EVENT} to the observer of its kind, without ever reading its payload: the kind, the action
 * and the document id come from the event properties.
 * <p>
 * The sync windows overlap by {@link DefaultSyncManager#TIMEFRAME_DELAY}, so an event is fetched several times. An event
 * an observer marked done is not handed out again; any other one is, as long as a window still covers it. There is no
 * retry list: an event still not done when it leaves the windows is only seen again at the next full sync (restart).
 * <p>
 * Never fails the sync cycle: a failing observer must not stop the APIs from syncing.
 */
@CustomLog
public class DispatchingSynchronizer implements RepositorySynchronizer {

    static final String PROPERTY_KIND = Event.EventProperties.GENERIC_EVENT_KIND.getValue();
    static final String PROPERTY_ACTION = Event.EventProperties.GENERIC_EVENT_ACTION.getValue();
    static final String PROPERTY_DOCUMENT_ID = Event.EventProperties.ID.getValue();
    private static final long DONE_RETENTION_MS = 4L * DefaultSyncManager.TIMEFRAME_DELAY;

    private final GenericEventFetcher genericEventFetcher;
    private final GenericEventObservers observers;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final LongSupplier clock;
    private final Map<String, Long> doneEventIds = new ConcurrentHashMap<>();

    public DispatchingSynchronizer(
        final GenericEventFetcher genericEventFetcher,
        final GenericEventObservers observers,
        final ThreadPoolExecutor syncFetcherExecutor
    ) {
        this(genericEventFetcher, observers, syncFetcherExecutor, System::currentTimeMillis);
    }

    DispatchingSynchronizer(
        final GenericEventFetcher genericEventFetcher,
        final GenericEventObservers observers,
        final ThreadPoolExecutor syncFetcherExecutor,
        final LongSupplier clock
    ) {
        this.genericEventFetcher = genericEventFetcher;
        this.observers = observers;
        this.syncFetcherExecutor = syncFetcherExecutor;
        this.clock = clock;
    }

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        return genericEventFetcher
            .fetchLatest(from, to, environments)
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .flatMap(events ->
                Flowable.fromIterable(events).sorted(
                    Comparator.comparing(Event::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                )
            )
            .filter(event -> !doneEventIds.containsKey(event.getId()))
            .doOnNext(this::dispatch)
            .ignoreElements()
            .doOnComplete(this::forgetOldDoneEvents)
            .onErrorComplete(error -> {
                log.warn("Unable to fetch generic events, the same window is fetched again at the next cycle", error);
                return true;
            });
    }

    private void dispatch(final Event event) {
        final Map<String, String> properties = event.getProperties() == null ? Map.of() : event.getProperties();
        final GenericEvent genericEvent = new GenericEvent(
            event.getId(),
            properties.get(PROPERTY_KIND),
            properties.get(PROPERTY_ACTION),
            properties.get(PROPERTY_DOCUMENT_ID),
            event.getPayload()
        );
        final Optional<GenericEventObserver> observer = observers.observerOf(genericEvent.kind());
        if (observer.isEmpty()) {
            log.warn(
                "⚠️ No observer for generic event [{}] of kind [{}], it will be offered again while the sync window covers it",
                event.getId(),
                genericEvent.kind()
            );
            return;
        }
        log.info(
            "📨 Generic event [{}] dispatched: kind [{}], action [{}], document [{}]",
            event.getId(),
            genericEvent.kind(),
            genericEvent.action(),
            genericEvent.documentId()
        );
        try {
            if (observer.get().onEvent(genericEvent)) {
                doneEventIds.put(event.getId(), createdAt(event));
            }
        } catch (Exception e) {
            log.warn(
                "Observer of kind [{}] failed on generic event [{}], it will be offered again while the sync window covers it",
                genericEvent.kind(),
                event.getId(),
                e
            );
        }
    }

    private void forgetOldDoneEvents() {
        final long threshold = clock.getAsLong() - DONE_RETENTION_MS;
        doneEventIds.values().removeIf(createdAt -> createdAt < threshold);
    }

    private long createdAt(final Event event) {
        final Date createdAt = event.getCreatedAt();
        return createdAt != null ? createdAt.getTime() : clock.getAsLong();
    }

    @Override
    public int order() {
        return Order.GENERIC_EVENT.index();
    }
}
