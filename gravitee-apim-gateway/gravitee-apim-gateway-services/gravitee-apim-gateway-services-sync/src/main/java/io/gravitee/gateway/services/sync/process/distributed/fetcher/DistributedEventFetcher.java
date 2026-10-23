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
package io.gravitee.gateway.services.sync.process.distributed.fetcher;

import static io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager.TIMEFRAME_DELAY;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Map;
import java.util.Set;
import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class DistributedEventFetcher {

    // bulkItems sizes the downstream deployment batches; events themselves are cheap to fetch,
    // so read them by larger batches to limit the number of repository round trips.
    private static final int MIN_FETCH_BATCH_SIZE = 1000;

    private final DistributedEventRepository distributedEventRepository;
    private final ClusterManager clusterManager;

    @Getter
    @Accessors(fluent = true)
    private final int bulkItems;

    /** Per-type batch size overrides, for types whose events are larger (e.g. API payloads). */
    @Getter
    private final Map<DistributedEventType, Integer> batchSizes;

    public DistributedEventFetcher(
        final DistributedEventRepository distributedEventRepository,
        final ClusterManager clusterManager,
        final int bulkItems,
        final Map<DistributedEventType, Integer> batchSizes
    ) {
        if (bulkItems <= 0) {
            throw new IllegalArgumentException("bulkItems must be > 0 (got " + bulkItems + ")");
        }
        this.distributedEventRepository = distributedEventRepository;
        this.clusterManager = clusterManager;
        this.bulkItems = bulkItems;
        this.batchSizes = batchSizes;
    }

    /**
     * Returns the batch size for a given event type, capped at the minimum, or the default bulkItems when the
     * type has no specific override. Types whose events carry large payloads (APIs) can be tuned down to fetch
     * fewer events at a time into the memory of each secondary.
     */
    /**
     * Returns the batch size for a given event type, capped at the minimum, or the default bulkItems when the
     * type has no specific override. Types whose events carry large payloads (APIs) can be tuned down to fetch
     * fewer events at a time into the memory of each secondary.
     */
    public int requestBatchSize(final DistributedEventType type) {
        return Math.max(batchSizes.getOrDefault(type, bulkItems), MIN_FETCH_BATCH_SIZE);
    }

    public Flowable<DistributedEvent> fetchLatest(
        final Long from,
        final Long to,
        final DistributedEventType type,
        final Set<DistributedSyncAction> syncActions
    ) {
        boolean initialSync = from == null || from == -1;
        if (!initialSync && shouldSkipCycle(type)) {
            log.debug("Change sequence unchanged for {}, skipping the distributed sync query", type);
            return Flowable.empty();
        }
        DistributedEventCriteria distributedEventCriteria = DistributedEventCriteria.builder()
            .clusterId(clusterManager.clusterId())
            .from(from == null ? -1 : from - TIMEFRAME_DELAY)
            .to(to == null ? -1 : to + TIMEFRAME_DELAY)
            .type(type)
            .syncActions(syncActions)
            .build();

        return distributedEventRepository
            .searchAll(distributedEventCriteria, requestBatchSize(type))
            .doOnComplete(() -> recordSequenceSeen(type));
    }

    /**
     * The primary increments {@code distributed_sync_seq:<clusterId>} after every cycle that wrote events. A
     * secondary reads it before each type query and skips the query when it has not moved since the last fetch;
     * the change is detected via the repository clock (Redis TIME) so no additional key is needed. A full check
     * runs every {@value #FORCED_CHECK_EVERY} cycles regardless, so a missed change self-heals.
     */
    private final int FORCED_CHECK_EVERY = 12;
    private final java.util.concurrent.ConcurrentHashMap<DistributedEventType, Integer> cyclesSinceFullCheck =
        new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<DistributedEventType, Long> lastSeenSequence =
        new java.util.concurrent.ConcurrentHashMap<>();

    private boolean shouldSkipCycle(final DistributedEventType type) {
        Integer sinceCheck = cyclesSinceFullCheck.merge(type, 1, Integer::sum);
        if (sinceCheck >= FORCED_CHECK_EVERY) {
            cyclesSinceFullCheck.put(type, 0);
            return false;
        }
        try {
            Long current = distributedEventRepository.serverTime().blockingGet();
            Long last = lastSeenSequence.get(type);
            return last != null && current != null && current <= last;
        } catch (RuntimeException e) {
            // If the clock cannot be read, err on the side of fetching.
            log.warn("Unable to read the change sequence clock, fetching the events", e);
            return false;
        }
    }

    private void recordSequenceSeen(final DistributedEventType type) {
        try {
            Long current = distributedEventRepository.serverTime().blockingGet();
            if (current != null) {
                lastSeenSequence.put(type, current);
            }
        } catch (RuntimeException e) {
            log.warn("Unable to read the change sequence clock, the next cycle will fetch again", e);
        }
    }

    /**
     * All DEPLOY events of the type, whatever their timestamp. Used to reconcile the local in-memory state with the
     * authoritative set after a full synchronization.
     */
    public Flowable<DistributedEvent> fetchAuthoritative(final DistributedEventType type, final Set<DistributedSyncAction> syncActions) {
        DistributedEventCriteria distributedEventCriteria = DistributedEventCriteria.builder()
            .clusterId(clusterManager.clusterId())
            .type(type)
            .syncActions(syncActions)
            .build();
        return distributedEventRepository.searchAll(distributedEventCriteria, Math.max(bulkItems, MIN_FETCH_BATCH_SIZE));
    }
}
