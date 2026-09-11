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
package io.gravitee.repository.redis.distributedsync;

import static io.gravitee.repository.redis.distributedsync.RedisDistributedSyncRepositoryConfiguration.REDIS_KEY_SEPARATOR;

import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class RedisDistributedEventRepository implements DistributedEventRepository {

    private static final String REDIS_INDEX_NAME = "distributed-event-search-idx";
    private static final String REDIS_KEY_PREFIX = "distributed_event" + REDIS_KEY_SEPARATOR;
    private static final String REDIS_SEARCH_RESULTS_FIELD = "results";
    private static final String REDIS_RESPONSE_ATTRIBUTES_FIELD = "extra_attributes";
<<<<<<< HEAD
=======
    private static final String CLUSTER_ID_REQUIRED_MESSAGE = "Distributed event clusterId is required";
    private static final String CLUSTER_ID_PARAMETER_REQUIRED_MESSAGE = "clusterId is required";
    private static final long NO_CURSOR = -1;
    private static final String SCAN_BATCH_SIZE = "1000";
    // Keep well below the redis client waiting queue (max-waiting-handlers) to leave room for other commands
    private static final int UPDATE_MAX_CONCURRENCY = 32;

    // Upper bound on the number of distributed-event writes issued concurrently across ALL callers (both
    // createOrUpdate and updateAll funnel through createOrUpdateKey). Kept well below the Redis client waiting
    // queue (RedisConnectionFactory#buildRedisOptions sets max-waiting-handlers=1024), leaving room for
    // reads/state/scan commands, so a bulk sync never triggers "Redis waiting queue is full".
    private static final int DISTRIBUTION_WRITE_MAX_CONCURRENCY = 512;

    // A write failing because Redis is momentarily unreachable (restart / brief outage) is retried with an
    // exponential backoff instead of being surfaced as a failed distribution: the event key is idempotent
    // (HSET), so retrying is safe, and it lets a short Redis blip heal within the same sync cycle instead of
    // leaving the event unsynced until the api changes or the node restarts. Bounded so a long outage still
    // gives up and lets the sync window be replayed: 5 retries after the initial attempt at 200ms doubling
    // (200/400/800/1600/3200ms) ≈ ~6s of backoff waiting. Note this is only the waiting between attempts:
    // when an attempt hangs on a half-open connection it also spends up to WRITE_COMMAND_TIMEOUT_MS before
    // failing, so the worst-case time a permit is held is dominated by the timeout, not this backoff — see
    // WRITE_COMMAND_TIMEOUT_MS.
    static final int WRITE_RETRY_MAX_ATTEMPTS = 5;
    static final long WRITE_RETRY_INITIAL_BACKOFF_MS = 200;
    // Safety ceiling for the exponential backoff. At the current settings the computed delay tops out at
    // 3200ms, so this cap is not reached today; it only guards against a future change to the settings.
    static final long WRITE_RETRY_MAX_BACKOFF_MS = 5_000;
    // Per-attempt command timeout. Normal writes complete in milliseconds; this only fires for a command that
    // never resolves (half-open connection), so a permit can never be held indefinitely — the timeout is
    // retryable (see isRetryableWriteFailure) and bounds the hold.
    // Caveat on the half-open case: a timeout does NOT invalidate the connection. notifyConnectionFailure
    // skips TimeoutException by type, and .timeout() also emits on RxJava's computation scheduler (no Vert.x
    // context) so the notification would bail even without that skip. The retries therefore re-hit the same
    // stale connection, each timing out again, so the worst-case time a permit is held is ~66s
    // (6 attempts × 10s + ~6s backoff), NOT ~6s. Still bounded, so the sync window is eventually replayed;
    // making a timeout invalidate the connection (so the next attempt reconnects) is a possible follow-up.
    static final long WRITE_COMMAND_TIMEOUT_MS = 10_000;
>>>>>>> faaee94 (fix(redis): reconnect when Sentinel leaves a READONLY replica)

    private final RedisClient redisClient;

    public RedisDistributedEventRepository(final RedisClient redisClient) {
        this.redisClient = redisClient;
        this.redisClient.redisApi()
            .flatMap(redisAPI -> redisAPI.ftCreate(buildSearchIndex()))
            .onFailure(throwable -> {
                if (!"Index already exists".equals(throwable.getMessage())) {
                    log.error("Unable to create distributed-event index", throwable);
                }
            });
    }

    @Override
    public Flowable<DistributedEvent> search(final DistributedEventCriteria criteria, final Long page, final Long size) {
        return Single.defer(() ->
            Single.fromCompletionStage(
                redisClient
                    .redisApi()
                    .flatMap(redisAPI -> redisAPI.ftSearch(buildSearchArgs(criteria, page, size)))
                    .toCompletionStage()
                    .toCompletableFuture()
            )
        )
            .filter(response -> response.type() == ResponseType.MULTI)
            .flattenStreamAsFlowable(response ->
                getSearchResults(response)
                    .stream()
                    .filter(item -> item.type() == ResponseType.MULTI)
                    .map(this::mapSearchResponse)
            );
    }

    @Override
    public Completable createOrUpdate(final DistributedEvent distributedEvent) {
        return createOrUpdateKey(buildEventKey(distributedEvent), distributedEvent);
    }

    @Override
    public Completable updateAll(
        final DistributedEventType refType,
        final String refId,
        final DistributedSyncAction syncAction,
        final Date updatedAt
    ) {
        String matchingKey = REDIS_KEY_PREFIX + refType.name() + REDIS_KEY_SEPARATOR + refId + "*";
        AtomicInteger cursor = new AtomicInteger(0);
        return Single.defer(() ->
            Single.fromCompletionStage(
                redisClient
                    .redisApi()
                    .flatMap(redisAPI -> redisAPI.scan(List.of(String.valueOf(cursor.get()), "MATCH", matchingKey)))
                    .toCompletionStage()
            )
        )
            .filter(response -> response.type() == ResponseType.MULTI)
            .flattenStreamAsFlowable(response -> {
                Integer nextCursor = response.get(0).toInteger();
                cursor.set(nextCursor);
                Response keys = response.get(1);
                return keys.stream();
            })
            .map(Response::toString)
            .repeatUntil(() -> cursor.get() == 0)
            .flatMapCompletable(key ->
                createOrUpdateKey(key, DistributedEvent.builder().syncAction(syncAction).updatedAt(updatedAt).build())
            );
    }

    private Completable createOrUpdateKey(final String key, final DistributedEvent distributedEvent) {
        return Completable.defer(() ->
            Completable.fromCompletionStage(
                redisClient
                    .redisApi()
                    .flatMap(redisAPI -> redisAPI.hset(buildUpdateArgs(key, distributedEvent)))
                    .toCompletionStage()
            )
        );
    }

    private List<String> buildSearchIndex() {
        return List.of(
            REDIS_INDEX_NAME,
            "ON",
            "HASH",
            "PREFIX",
            "1",
            REDIS_KEY_PREFIX,
            "SCHEMA",
            DistributedEvent.Fields.type,
            "TAG",
            DistributedEvent.Fields.syncAction,
            "TAG",
            DistributedEvent.Fields.updatedAt,
            "NUMERIC",
            "SORTABLE"
        );
    }

    private List<String> buildSearchArgs(DistributedEventCriteria criteria, Long page, Long size) {
        var args = new ArrayList<String>();
        args.add(REDIS_INDEX_NAME);
        // add query
        args.add(buildSearchQuery(criteria));
        // add sort
        args.add("SORTBY");
        args.add(DistributedEvent.Fields.updatedAt);
        args.add("ASC");
        if (page > -1 && size > -1) {
            args.add("LIMIT");
            args.add(String.valueOf(page * size));
            args.add(String.valueOf(size));
        }
        return args;
    }

    private String buildSearchQuery(final DistributedEventCriteria criteria) {
        if (criteria == null) {
            return "*";
        }

        var query = new StringBuilder();
        if (criteria.getType() != null) {
            query.append("@" + DistributedEvent.Fields.type + ":{").append(criteria.getType()).append("}");
        }

        if (criteria.getSyncActions() != null) {
            var syncActions = criteria.getSyncActions().stream().map(Enum::name).collect(Collectors.joining("|"));
            query.append("@" + DistributedEvent.Fields.syncAction + ":{").append(syncActions).append("}");
        }

        if (criteria.getFrom() > 0) {
            query.append("@" + DistributedEvent.Fields.updatedAt + ":[").append(criteria.getFrom()).append(" +inf]");
        }

        if (criteria.getTo() > 0) {
            query.append("@" + DistributedEvent.Fields.updatedAt + ":[-inf ").append(criteria.getTo()).append("]");
        }

        // If no criteria have been applied, retrieve all events
        return query.isEmpty() ? "*" : query.toString();
    }

    private List<String> buildUpdateArgs(String key, DistributedEvent distributedEvent) {
        var args = new ArrayList<String>();
        args.add(key);
        if (distributedEvent.getId() != null) {
            args.add(DistributedEvent.Fields.id);
            args.add(distributedEvent.getId());
        }
        if (distributedEvent.getPayload() != null) {
            args.add(DistributedEvent.Fields.payload);
            args.add(distributedEvent.getPayload());
        }
        if (distributedEvent.getType() != null) {
            args.add(DistributedEvent.Fields.type);
            args.add(distributedEvent.getType().name());
        }
        if (distributedEvent.getSyncAction() != null) {
            args.add(DistributedEvent.Fields.syncAction);
            args.add(distributedEvent.getSyncAction().name());
        }
        if (distributedEvent.getUpdatedAt() != null) {
            args.add(DistributedEvent.Fields.updatedAt);
            args.add(String.valueOf(distributedEvent.getUpdatedAt().getTime()));
        }
        return args;
    }

    private DistributedEvent mapSearchResponse(Response item) {
        var distributedEventBuilder = DistributedEvent.builder();

        var attributes = getResultAttributes(item);

        // Get id
        var id = attributes.get(DistributedEvent.Fields.id);
        if (id != null) {
            distributedEventBuilder.id(id.toString());
        }

        // Get payload
        var payload = attributes.get(DistributedEvent.Fields.payload);
        if (payload != null) {
            distributedEventBuilder.payload(payload.toString());
        }
        // Get type
        var type = attributes.get(DistributedEvent.Fields.type);
        if (type != null) {
            distributedEventBuilder.type(DistributedEventType.valueOf(type.toString()));
        }
        // Get syncAction
        var syncAction = attributes.get(DistributedEvent.Fields.syncAction);
        if (syncAction != null) {
            distributedEventBuilder.syncAction(DistributedSyncAction.valueOf(syncAction.toString()));
        }
        // Get updatedAt
        var updatedAt = attributes.get(DistributedEvent.Fields.updatedAt);
        if (updatedAt != null) {
            distributedEventBuilder.updatedAt(Date.from(Instant.ofEpochMilli(updatedAt.toLong())));
        }
        return distributedEventBuilder.build();
    }

    private String buildEventKey(DistributedEvent distributedEvent) {
        var builder = new StringBuilder().append(REDIS_KEY_PREFIX);

        if (distributedEvent.getRefId() != null && distributedEvent.getRefType() != null) {
            builder.append(distributedEvent.getRefType().name()).append(REDIS_KEY_SEPARATOR).append(distributedEvent.getRefId());
        }

        return builder
            .append(REDIS_KEY_SEPARATOR)
            .append(distributedEvent.getType().name())
            .append(REDIS_KEY_SEPARATOR)
            .append(distributedEvent.getId())
            .toString();
    }

    /*
     * Starting from version 7.2 of Redis, warnings and additional attributes
     * have been added to the response and the actual result is nested into a
     * 'results' field of the Response object
     */
    private Response getSearchResults(Response response) {
        // Avoid exception by checking if the response can be handled as a map
        if (response.size() % 2 == 1) {
            return response;
        }

        return Optional.ofNullable(response.get(REDIS_SEARCH_RESULTS_FIELD)).orElse(response);
    }

    /*
     * Starting from version 7.2 of Redis, additional metadata has been added to the
     * response data and the actual attributes of the entry are stored in a 'extra_attributes'
     * property of the Response object
     */
    private Response getResultAttributes(Response response) {
        return Optional.ofNullable(response.get(REDIS_RESPONSE_ATTRIBUTES_FIELD)).orElse(response);
    }
}
