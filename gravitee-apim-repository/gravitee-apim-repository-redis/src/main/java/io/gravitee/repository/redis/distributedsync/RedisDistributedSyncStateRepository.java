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
import io.gravitee.repository.redis.vertx.RedisClient;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
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
            .map(item -> {
                DistributedSyncState.DistributedSyncStateBuilder distributedSyncStateBuilder = DistributedSyncState.builder();
                // Get cluster id
                Response clusterIdResponse = item.get(DistributedSyncState.Fields.clusterId);
                if (clusterIdResponse != null) {
                    distributedSyncStateBuilder.clusterId(clusterIdResponse.toString());
                }
                // Get node id
                Response nodeIdResponse = item.get(DistributedSyncState.Fields.nodeId);
                if (nodeIdResponse != null) {
                    distributedSyncStateBuilder.nodeId(nodeIdResponse.toString());
                }
                // Get node version
                Response nodeVersionResponse = item.get(DistributedSyncState.Fields.nodeVersion);
                if (nodeVersionResponse != null) {
                    distributedSyncStateBuilder.nodeVersion(nodeVersionResponse.toString());
                }
                // Get from time
                Response fromResponse = item.get(DistributedSyncState.Fields.from);
                if (fromResponse != null) {
                    distributedSyncStateBuilder.from(fromResponse.toLong());
                }
                // Get to time
                Response toResponse = item.get(DistributedSyncState.Fields.to);
                if (toResponse != null) {
                    distributedSyncStateBuilder.to(toResponse.toLong());
                }
                return distributedSyncStateBuilder.build();
            });
    }

    @Override
    public Completable createOrUpdate(final DistributedSyncState distributedSyncState) {
        return Completable.defer(() ->
            Completable.fromCompletionStage(
                redisClient
                    .redisApi()
                    .flatMap(redisAPI ->
                        redisAPI.hset(
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
                        )
                    )
                    .toCompletionStage()
            )
        );
    }
}
