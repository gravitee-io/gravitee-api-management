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
package io.gravitee.gateway.services.sync.process.distributed.localversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedLocalVersion;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * A lookup failure fails the sync cycle, because deploying the event anyway could silently remove a fix.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class DefaultLocalVersionResolver implements LocalVersionResolver {

    private final DistributedEventRepository distributedEventRepository;
    private final ClusterManager clusterManager;
    private final ObjectMapper objectMapper;
    private final ApiMapper apiMapper;
    private final DictionaryMapper dictionaryMapper;

    @Override
    public Maybe<ReactableApi<?>> resolveDeployment(final ReactableApi<?> api) {
        return activeLocalVersion(DistributedEventType.API, api.getId()).flatMapMaybe(localVersion -> {
            if (localVersion.isEmpty()) {
                return Maybe.just(api);
            }
            if (isNewer(api.getDeployedAt(), localVersion.get())) {
                return close(DistributedEventType.API, api.getId()).andThen(Maybe.just(api));
            }
            // A cold primary must load the local version with its subscriptions and keys. A stopped API has nothing to load.
            return localVersion.get().isRunning() ? toReactableApi(api.getId(), localVersion.get()) : Maybe.empty();
        });
    }

    @Override
    public Maybe<String> resolveUndeployment(final String apiId, final Date eventCreatedAt) {
        return resolveUndeployment(DistributedEventType.API, apiId, eventCreatedAt, apiId);
    }

    @Override
    public Maybe<Dictionary> resolveDeployment(final Dictionary dictionary) {
        return activeLocalVersion(DistributedEventType.DICTIONARY, dictionary.getId()).flatMapMaybe(localVersion -> {
            if (localVersion.isEmpty()) {
                return Maybe.just(dictionary);
            }
            if (isNewer(dictionary.getDeployedAt(), localVersion.get())) {
                return close(DistributedEventType.DICTIONARY, dictionary.getId()).andThen(Maybe.just(dictionary));
            }
            return toDictionary(dictionary.getId(), localVersion.get());
        });
    }

    @Override
    public Maybe<Dictionary> resolveUndeployment(final Dictionary dictionary, final Date eventCreatedAt) {
        return resolveUndeployment(DistributedEventType.DICTIONARY, dictionary.getId(), eventCreatedAt, dictionary);
    }

    private <T> Maybe<T> resolveUndeployment(final DistributedEventType type, final String id, final Date eventCreatedAt, final T result) {
        return activeLocalVersion(type, id).flatMapMaybe(localVersion -> {
            if (localVersion.isEmpty()) {
                return Maybe.just(result);
            }
            return isNewer(eventCreatedAt, localVersion.get()) ? close(type, id).andThen(Maybe.just(result)) : Maybe.empty();
        });
    }

    private Single<Optional<DistributedLocalVersion>> activeLocalVersion(final DistributedEventType type, final String id) {
        return distributedEventRepository
            .find(DistributedLocalVersion.recordsClusterId(clusterManager.clusterId()), type, id)
            .filter(event -> event.getSyncAction() == DistributedSyncAction.DEPLOY)
            .map(event -> Optional.of(objectMapper.readValue(event.getPayload(), DistributedLocalVersion.class)))
            .defaultIfEmpty(Optional.empty());
    }

    private static boolean isNewer(final Date controlPlaneDate, final DistributedLocalVersion localVersion) {
        return controlPlaneDate != null && controlPlaneDate.getTime() > localVersion.getBaseDeployedAt();
    }

    private Completable close(final DistributedEventType type, final String id) {
        return distributedEventRepository.createOrUpdate(
            DistributedEvent.builder()
                .clusterId(DistributedLocalVersion.recordsClusterId(clusterManager.clusterId()))
                .id(id)
                .type(type)
                .syncAction(DistributedSyncAction.UNDEPLOY)
                .updatedAt(new Date())
                .build()
        );
    }

    private Maybe<ReactableApi<?>> toReactableApi(final String apiId, final DistributedLocalVersion localVersion) {
        return apiMapper.to(localEvent(apiId, localVersion)).map(ApiReactorDeployable::reactableApi);
    }

    private Maybe<Dictionary> toDictionary(final String id, final DistributedLocalVersion localVersion) {
        return dictionaryMapper.to(localEvent(id, localVersion)).map(DictionaryDeployable::dictionary);
    }

    private static DistributedEvent localEvent(final String id, final DistributedLocalVersion localVersion) {
        return DistributedEvent.builder().id(id).payload(localVersion.getLocalPayload()).syncAction(DistributedSyncAction.DEPLOY).build();
    }
}
