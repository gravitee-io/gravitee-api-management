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

import io.gravitee.repository.distributedsync.api.DistributedSyncStateRepository;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import io.gravitee.repository.distributedsync.model.DistributedSyncStatus;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RedisDistributedSyncStateRepository implements DistributedSyncStateRepository {

    private static final String REDIS_KEY_PREFIX = "distributed_sync_state" + REDIS_KEY_SEPARATOR;
    private final RedisClient redisClient;

    public RedisDistributedSyncStateRepository(final RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public Completable ready() {
        return Completable.defer(() -> Completable.fromCompletionStage(redisClient.redisApi().toCompletionStage()));
    }

    @Override
    public Maybe<DistributedSyncState> findByClusterId(final String clusterId) {
        return Maybe.defer(() ->
            Maybe.fromCompletionStage(
                redisClient
                    .redisApi()
                    .flatMap(redisAPI -> redisAPI.hgetall(REDIS_KEY_PREFIX + clusterId))
                    .toCompletionStage()
            )
        )
            .filter(response -> response.type() == ResponseType.MULTI && response.size() != 0)
            .map(RedisDistributedSyncStateRepository::toState);
    }

    private static DistributedSyncState toState(final Response item) {
        DistributedSyncState.DistributedSyncStateBuilder builder = DistributedSyncState.builder();
        stringField(item, DistributedSyncState.Fields.clusterId, builder::clusterId);
        stringField(item, DistributedSyncState.Fields.nodeId, builder::nodeId);
        stringField(item, DistributedSyncState.Fields.nodeVersion, builder::nodeVersion);
        longField(item, DistributedSyncState.Fields.from, builder::from);
        longField(item, DistributedSyncState.Fields.to, builder::to);
        stringField(item, DistributedSyncState.Fields.status, value -> builder.status(parseStatus(value)));
        longField(item, DistributedSyncState.Fields.resyncId, builder::resyncId);
        longField(item, DistributedSyncState.Fields.heartbeatAt, builder::heartbeatAt);
        longField(item, DistributedSyncState.Fields.tombstoneHorizon, builder::tombstoneHorizon);
        stringField(item, DistributedSyncState.Fields.topology, builder::topology);
        return builder.build();
    }

    private static void stringField(final Response item, final String field, final Consumer<String> setter) {
        Response value = item.get(field);
        if (value != null) {
            setter.accept(value.toString());
        }
    }

    private static void longField(final Response item, final String field, final LongConsumer setter) {
        Response value = item.get(field);
        if (value != null) {
            setter.accept(value.toLong());
        }
    }

    /**
     * A status written by a newer gateway is read as {@code null} (i.e. active) rather than failing the read.
     */
    private static DistributedSyncStatus parseStatus(final String value) {
        for (DistributedSyncStatus status : DistributedSyncStatus.values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return null;
    }

    @Override
    public Completable createOrUpdate(final DistributedSyncState distributedSyncState) {
        return Completable.defer(() -> {
            List<String> args = new ArrayList<>(
                List.of(
                    REDIS_KEY_PREFIX + distributedSyncState.getClusterId(),
                    DistributedSyncState.Fields.clusterId,
                    distributedSyncState.getClusterId(),
                    DistributedSyncState.Fields.nodeId,
                    distributedSyncState.getNodeId(),
                    DistributedSyncState.Fields.nodeVersion,
                    distributedSyncState.getNodeVersion(),
                    DistributedSyncState.Fields.from,
                    String.valueOf(distributedSyncState.getFrom()),
                    DistributedSyncState.Fields.to,
                    String.valueOf(distributedSyncState.getTo())
                )
            );
            addIfPresent(args, DistributedSyncState.Fields.status, distributedSyncState.getStatus());
            addIfPresent(args, DistributedSyncState.Fields.resyncId, distributedSyncState.getResyncId());
            addIfPresent(args, DistributedSyncState.Fields.heartbeatAt, distributedSyncState.getHeartbeatAt());
            addIfPresent(args, DistributedSyncState.Fields.tombstoneHorizon, distributedSyncState.getTombstoneHorizon());
            addIfPresent(args, DistributedSyncState.Fields.topology, distributedSyncState.getTopology());
            return Completable.fromCompletionStage(
                redisClient
                    .redisApi()
                    .flatMap(redisAPI -> redisAPI.hset(args))
                    .toCompletionStage()
            );
        });
    }

    private static void addIfPresent(final List<String> args, final String field, final Object value) {
        if (value != null) {
            args.add(field);
            args.add(value instanceof Enum<?> e ? e.name() : String.valueOf(value));
        }
    }
}
