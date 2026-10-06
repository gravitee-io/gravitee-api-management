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
package io.gravitee.repository.distributedsync.api;

import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DistributedEventRepository {
    /**
     * Search for {@link DistributedEvent} matching the corresponding criteria.
     *
     * @param criteria Criteria to search for {@link DistributedEvent}.
     * @param page optional page number starting from 0, <code>null</code> means no paging.
     * @param size optional number of events to retrieve, <code>null</code> means no limit.
     *
     * @return the {@link Flowable} of the latest events.
     */
    Flowable<DistributedEvent> search(final DistributedEventCriteria criteria, final Long page, final Long size);

    /**
     * Search for all {@link DistributedEvent} matching the corresponding criteria, fetched from the repository by batch of <code>batchSize</code>.
     * <p>
     * Events are emitted without any ordering guarantee. Implementations are encouraged to override this method
     * when the underlying storage offers a more efficient way to stream large result sets than offset-based paging.
     *
     * @param criteria Criteria to search for {@link DistributedEvent}.
     * @param batchSize number of events to fetch per query.
     *
     * @return the {@link Flowable} of all matching events.
     */
    default Flowable<DistributedEvent> searchAll(final DistributedEventCriteria criteria, final long batchSize) {
        return Flowable.defer(() -> {
            AtomicLong page = new AtomicLong();
            AtomicBoolean lastPage = new AtomicBoolean();
            return Flowable.defer(() -> search(criteria, page.getAndIncrement(), batchSize))
                .switchIfEmpty(Flowable.fromAction(() -> lastPage.set(true)))
                .repeatUntil(lastPage::get);
        });
    }

    /**
     * This method allows to create or update a distributed event if it does not exist in database or update it if it's present (replace old values by new ones).
     *
     * @param distributedEvent {@link DistributedEvent} to store
     * @return {@link Completable}
     */
    Completable createOrUpdate(final DistributedEvent distributedEvent);

    /**
     * Same as {@link #createOrUpdate(DistributedEvent)}, but the stored {@code updatedAt} is the time the write is
     * actually attempted rather than the time the event was built.
     * <p>
     * Readers select events by an {@code updatedAt} window: an event stamped long before it becomes visible
     * (queued behind other writes, or retried) can fall behind a window a reader has already consumed and be
     * missed for good. Stamping at write time bounds that gap to a single write attempt.
     *
     * @param distributedEvent {@link DistributedEvent} to store
     * @return {@link Completable}
     */
    default Completable createOrUpdateAtWriteTime(final DistributedEvent distributedEvent) {
        return createOrUpdate(distributedEvent);
    }

    /**
     * This method allows to update all distributed event associated to reference id and type.
     *
     * @param clusterId cluster identifier used to scope distributed events
     * @param refType {@link DistributedEventType} used to filter distributed events
     * @param refId {@link String} used to filter distributed events
     * @param syncAction {@link DistributedSyncAction} to update
     * @param updateAt {@link Date} to update
     * @return {@link Completable}
     */
    Completable updateAll(
        final String clusterId,
        final DistributedEventType refType,
        final String refId,
        final DistributedSyncAction syncAction,
        final Date updateAt
    );

    /**
     * Same as {@link #updateAll(String, DistributedEventType, String, DistributedSyncAction, Date)}, with each
     * event stamped at the time its own write is attempted (see {@link #createOrUpdateAtWriteTime}).
     */
    default Completable updateAllAtWriteTime(
        final String clusterId,
        final DistributedEventType refType,
        final String refId,
        final DistributedSyncAction syncAction
    ) {
        return updateAll(clusterId, refType, refId, syncAction, new Date());
    }

    /**
     * Makes sure the structures needed to search events exist (e.g. a search index), creating them if they were
     * lost.
     */
    default Completable ensureSearchable() {
        return Completable.complete();
    }

    /**
     * Whether every stored event is visible to searches. A search structure being (re)built after a restart of the
     * underlying store returns partial results, which a node starting from scratch must not trust.
     */
    default Single<Boolean> fullyIndexed() {
        return Single.just(true);
    }

    /**
     * Marks as {@link DistributedSyncAction#UNDEPLOY} every {@link DistributedSyncAction#DEPLOY} event of the given
     * types last written before {@code before}. Used after a full republish: whatever was not rewritten is no longer
     * part of the catalog.
     *
     * @return the number of events marked
     */
    default Single<Long> undeployNotUpdatedSince(final String clusterId, final Set<DistributedEventType> types, final Date before) {
        return Single.error(new UnsupportedOperationException("Removing stale distributed events is not supported by this repository"));
    }

    /**
     * Deletes the {@link DistributedSyncAction#UNDEPLOY} events last written before {@code before}.
     *
     * @return the number of events deleted
     */
    /**
     * Returns the current time of the Redis server in milliseconds. Secondaries use it for the sync window so
     * clock skew between nodes does not lose events.
     */
    default Single<Long> serverTime() {
        return Single.just(System.currentTimeMillis());
    }

    /**
     * Signals that the primary wrote events: a monotonically increasing sequence so secondaries can skip the
     * repository queries of the types that haven not changed.
     */
    default Completable advanceChangeSequence(final String clusterId) {
        return Completable.complete();
    }

    /**
     * Acquires or renews an exclusive write lease on the given key. Only the primary that holds the lease may
     * write distributed events, so a fencing token prevents two primaries (split-brain, two clusters sharing
     * Redis) from writing concurrently.
     */
    default Completable applyFencedWrite(final String key, final String value, final long ttlMs) {
        return Completable.complete();
    }

    /**
     * Creates or updates an event at the current time, fencing the write against the given lease value. When
     * the lease is non-null, a Redis Lua script stamps the event with the server clock and refuses the write
     * if the lease does not match. Subclasses that do not support fencing delegate to
     * {@link #createOrUpdateAtWriteTime(DistributedEvent)}.
     */
    default Completable createOrUpdateFenced(final DistributedEvent event, final String leaseValue) {
        if (leaseValue == null) {
            return createOrUpdateAtWriteTime(event);
        }
        return createOrUpdateAtWriteTime(event);
    }

    default Single<Long> deleteUndeployedBefore(final String clusterId, final Date before) {
        return Single.just(0L);
    }
}
