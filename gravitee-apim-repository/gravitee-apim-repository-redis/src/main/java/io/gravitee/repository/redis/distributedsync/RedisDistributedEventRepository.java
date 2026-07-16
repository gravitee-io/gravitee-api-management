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
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RedisDistributedEventRepository implements DistributedEventRepository {

    private static final String REDIS_INDEX_NAME = "distributed-event-search-idx";
    private static final String REDIS_KEY_PREFIX = "distributed_event" + REDIS_KEY_SEPARATOR;
    private static final String REDIS_SEARCH_RESULTS_FIELD = "results";
    private static final String REDIS_RESPONSE_ATTRIBUTES_FIELD = "extra_attributes";
    private static final long NO_CURSOR = -1;

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
        return send(redisAPI -> redisAPI.ftSearch(buildSearchArgs(criteria, page, size)))
            .filter(response -> response.type() == ResponseType.MULTI)
            .flattenStreamAsFlowable(this::mapSearchResults);
    }

    /**
     * Streams all matching events through a RediSearch cursor: {@code FT.AGGREGATE ... WITHCURSOR} runs the query
     * once and returns the first batch along with a cursor id, then each subsequent batch is fetched with
     * {@code FT.CURSOR READ} until the returned cursor id is 0. Every batch is served in constant time whatever
     * its position in the result set, so the overall cost stays linear with the number of events.
     * <p>
     * If the stream terminates before the cursor is exhausted (error or cancellation), the cursor is explicitly
     * deleted to free the server-side resource.
     */
    @Override
    public Flowable<DistributedEvent> searchAll(final DistributedEventCriteria criteria, final long batchSize) {
        return Flowable.defer(() -> {
            AtomicLong cursorId = new AtomicLong(NO_CURSOR);
            return send(redisAPI ->
                cursorId.get() == NO_CURSOR
                    ? redisAPI.ftAggregate(buildAggregateArgs(criteria, batchSize))
                    : redisAPI.ftCursor(List.of("READ", REDIS_INDEX_NAME, String.valueOf(cursorId.get())))
            )
                .flattenStreamAsFlowable(response -> {
                    // A cursor response is a 2-element array: the search results and the next cursor id (0 when exhausted)
                    cursorId.set(response.get(1).toLong());
                    return mapSearchResults(response.get(0));
                })
                .repeatUntil(() -> cursorId.get() == 0)
                .doFinally(() -> closeCursor(cursorId.get()));
        });
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
        return send(redisAPI -> redisAPI.hset(buildUpdateArgs(key, distributedEvent))).ignoreElement();
    }

    private Single<Response> send(final Function<RedisAPI, Future<Response>> command) {
        return Single.defer(() -> Single.fromCompletionStage(redisClient.redisApi().flatMap(command).toCompletionStage()));
    }

    private Stream<DistributedEvent> mapSearchResults(final Response searchResults) {
        return getSearchResults(searchResults)
            .stream()
            .filter(item -> item.type() == ResponseType.MULTI)
            .map(this::mapSearchResponse);
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

    private List<String> buildAggregateArgs(final DistributedEventCriteria criteria, final long batchSize) {
        return List.of(
            REDIS_INDEX_NAME,
            buildSearchQuery(criteria),
            "LOAD",
            "5",
            "@" + DistributedEvent.Fields.id,
            "@" + DistributedEvent.Fields.payload,
            "@" + DistributedEvent.Fields.type,
            "@" + DistributedEvent.Fields.syncAction,
            "@" + DistributedEvent.Fields.updatedAt,
            "WITHCURSOR",
            "COUNT",
            String.valueOf(batchSize)
        );
    }

    private void closeCursor(final long cursorId) {
        if (cursorId > 0) {
            redisClient
                .redisApi()
                .flatMap(redisAPI -> redisAPI.ftCursor(List.of("DEL", REDIS_INDEX_NAME, String.valueOf(cursorId))))
                .onFailure(throwable -> log.debug("Unable to close distributed-event cursor {}", cursorId, throwable));
        }
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
