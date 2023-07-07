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
    private final RedisClient redisClient;

    public RedisDistributedEventRepository(final RedisClient redisClient) {
        this.redisClient = redisClient;
        this.redisClient.redisApi()
            .flatMap(redisAPI ->
                redisAPI.ftCreate(
                    List.of(
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
                    )
                )
            )
            .onFailure(throwable -> {
                if (!"Index already exists".equals(throwable.getMessage())) {
                    log.error("Unable to create distributed-event index", throwable);
                }
            });
    }

    @Override
    public Flowable<DistributedEvent> search(final DistributedEventCriteria criteria, final Long page, final Long size) {
        return Single
            .defer(() ->
                Single.fromCompletionStage(
                    redisClient
                        .redisApi()
                        .flatMap(redisAPI -> {
                            StringBuilder query = new StringBuilder();
                            if (criteria != null) {
                                if (criteria.getType() != null) {
                                    query.append("@" + DistributedEvent.Fields.type + ":{").append(criteria.getType()).append("}");
                                }
                                if (criteria.getSyncActions() != null) {
                                    String syncActionsTags = criteria
                                        .getSyncActions()
                                        .stream()
                                        .map(Enum::name)
                                        .collect(Collectors.joining("|"));
                                    query.append("@" + DistributedEvent.Fields.syncAction + ":{").append(syncActionsTags).append("}");
                                }
                                if (criteria.getFrom() > 0) {
                                    query
                                        .append("@" + DistributedEvent.Fields.updatedAt + ":[")
                                        .append(criteria.getFrom())
                                        .append(" +inf]");
                                }
                                if (criteria.getTo() > 0) {
                                    query.append("@" + DistributedEvent.Fields.updatedAt + ":[-inf ").append(criteria.getTo()).append("]");
                                }
                            }
                            // If no criteria has been applied, retrieved all events
                            if (query.length() == 0) {
                                query.append("*");
                            }
                            List<String> args = new ArrayList<>();
                            // add index
                            args.add(REDIS_INDEX_NAME);
                            // add query
                            args.add(query.toString());
                            // add sort
                            args.add("SORTBY");
                            args.add(DistributedEvent.Fields.updatedAt);
                            args.add("ASC");
                            if (page > -1 && size > -1) {
                                args.add("LIMIT");
                                args.add(String.valueOf(page * size));
                                args.add(String.valueOf(size));
                            }
                            return redisAPI.ftSearch(args);
                        })
                        .toCompletionStage()
                        .toCompletableFuture()
                )
            )
            .filter(response -> response.type() == ResponseType.MULTI)
            .flattenStreamAsFlowable(response ->
                response
                    .stream()
                    .filter(item -> item.type() == ResponseType.MULTI)
                    .map(item -> {
                        DistributedEvent.DistributedEventBuilder distributedEventBuilder = DistributedEvent.builder();
                        // Get id
                        Response idResponse = item.get(DistributedEvent.Fields.id);
                        if (idResponse != null) {
                            distributedEventBuilder.id(idResponse.toString());
                        }
                        // Get payload
                        Response payloadResponse = item.get(DistributedEvent.Fields.payload);
                        if (payloadResponse != null) {
                            distributedEventBuilder.payload(payloadResponse.toString());
                        }
                        // Get type
                        Response typeResponse = item.get(DistributedEvent.Fields.type);
                        if (typeResponse != null) {
                            distributedEventBuilder.type(DistributedEventType.valueOf(typeResponse.toString()));
                        }
                        // Get syncAction
                        Response syncActionResponse = item.get(DistributedEvent.Fields.syncAction);
                        if (syncActionResponse != null) {
                            distributedEventBuilder.syncAction(DistributedSyncAction.valueOf(syncActionResponse.toString()));
                        }
                        // Get updatedAt
                        Response updatedAtResponse = item.get(DistributedEvent.Fields.updatedAt);
                        if (updatedAtResponse != null) {
                            distributedEventBuilder.updatedAt(Date.from(Instant.ofEpochMilli(updatedAtResponse.toLong())));
                        }
                        return distributedEventBuilder.build();
                    })
            );
    }

    @Override
    public Completable createOrUpdate(final DistributedEvent distributedEvent) {
        String key = REDIS_KEY_PREFIX;
        if (distributedEvent.getRefId() != null && distributedEvent.getRefType() != null) {
            key += distributedEvent.getRefType().name() + REDIS_KEY_SEPARATOR + distributedEvent.getRefId();
        }
        key += REDIS_KEY_SEPARATOR + distributedEvent.getType().name() + REDIS_KEY_SEPARATOR + distributedEvent.getId();
        return createOrUpdateKey(key, distributedEvent);
    }

    @Override
    public Completable updateAll(
        final DistributedEventType refType,
        final String refId,
        final DistributedSyncAction syncAction,
        final Date updateAt
    ) {
        String matchingKey = REDIS_KEY_PREFIX + refType.name() + REDIS_KEY_SEPARATOR + refId + "*";
        AtomicInteger cursor = new AtomicInteger(0);
        return Single
            .defer(() ->
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
            .flatMapCompletable(key -> createOrUpdateKey(key, DistributedEvent.builder().syncAction(syncAction).updatedAt(updateAt).build())
            );
    }

    private Completable createOrUpdateKey(final String key, final DistributedEvent distributedEvent) {
        return Completable.defer(() ->
            Completable.fromCompletionStage(
                redisClient
                    .redisApi()
                    .flatMap(redisAPI -> {
                        List<String> args = new ArrayList<>();
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
                        return redisAPI.hset(args);
                    })
                    .toCompletionStage()
            )
        );
    }
}
