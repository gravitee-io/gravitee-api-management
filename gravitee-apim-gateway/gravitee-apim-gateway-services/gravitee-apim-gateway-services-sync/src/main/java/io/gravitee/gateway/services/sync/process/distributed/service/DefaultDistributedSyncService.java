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
package io.gravitee.gateway.services.sync.process.distributed.service;

import io.gravitee.common.util.Version;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.LicenseMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.distributed.model.DistributedSyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SingleSubscriptionDeployable;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.DistributedSyncStateRepository;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class DefaultDistributedSyncService implements DistributedSyncService {

    private final Node node;
    private final ClusterManager clusterManager;
    private final String distributedSyncRepoType;
    private final DistributedEventRepository distributedEventRepository;
    private final DistributedSyncStateRepository distributedSyncStateRepository;
    private final ApiMapper apiMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final OrganizationMapper organizationMapper;
    private final DictionaryMapper dictionaryMapper;
    private final LicenseMapper licenseMapper;

    @Override
    public void validate() {
        if (distributedSyncRepoType == null || distributedSyncRepoType.isEmpty()) {
            throw new SyncException(
                "Distributed sync configuration invalid. No repository configured, check 'distributed-sync.type' value."
            );
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isPrimaryNode() {
        return clusterManager.self().primary();
    }

    @Override
    public Completable ready() {
        return distributedSyncStateRepository
            .ready()
            .onErrorResumeNext(throwable -> Completable.error(new DistributedSyncException("Distributed Sync repository is not ready.")));
    }

    @Override
    public Maybe<DistributedSyncState> state() {
        return Maybe.defer(() -> {
            if (isPrimaryNode()) {
                return distributedSyncStateRepository.findByClusterId(clusterManager.clusterId());
            }
            return Maybe.empty();
        });
    }

    @Override
    public Completable storeState(final long fromTime, final long toTime) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                return distributedSyncStateRepository.createOrUpdate(
                    DistributedSyncState
                        .builder()
                        .clusterId(clusterManager.clusterId())
                        .nodeId(node.id())
                        .nodeVersion(Version.RUNTIME_VERSION.toString())
                        .from(fromTime)
                        .to(toTime)
                        .build()
                );
            }
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final ApiReactorDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                return apiMapper
                    .to(deployable)
                    .flatMapCompletable(distributedEventRepository::createOrUpdate)
                    .andThen(
                        Completable.defer(() -> {
                            if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                                return distributedEventRepository.updateAll(
                                    DistributedEventType.API,
                                    deployable.apiId(),
                                    DistributedSyncAction.UNDEPLOY,
                                    new Date()
                                );
                            }
                            return Completable.complete();
                        })
                    );
            }
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final SingleSubscriptionDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                return subscriptionMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final SingleApiKeyDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                return apiKeyMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final OrganizationDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                return organizationMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final DictionaryDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                return dictionaryMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(LicenseDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                return licenseMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            return Completable.complete();
        });
    }
}
