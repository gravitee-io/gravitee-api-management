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
import io.gravitee.gateway.services.sync.process.distributed.mapper.AccessPointMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.LicenseMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.NodeMetadataMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SharedPolicyGroupMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.distributed.model.DistributedSyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.node.NodeMetadataDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
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
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
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
    private final AccessPointMapper accessPointMapper;
    private final SharedPolicyGroupMapper sharedPolicyGroupMapper;
    private final NodeMetadataMapper nodeMetadataMapper;

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
                log.debug("Node is primary, distributing API reactor event for {}", deployable.id());
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
            log.debug("Not a primary node, skipping API reactor event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final SingleSubscriptionDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing subscription event for {}", deployable.id());
                return subscriptionMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            log.debug("Not a primary node, skipping subscription event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final SingleApiKeyDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing API key event for {}", deployable.id());
                return apiKeyMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            log.debug("Not a primary node, skipping API key event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final OrganizationDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing organization event for {}", deployable.id());
                return organizationMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            log.debug("Not a primary node, skipping organization event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final DictionaryDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing dictionary event for {}", deployable.id());
                return dictionaryMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            log.debug("Not a primary node, skipping dictionary event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(LicenseDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing license event for organization {}", deployable.id());
                return licenseMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            log.debug("Not a primary node, skipping license event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(AccessPointDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing access point event for {}", deployable.id());
                return accessPointMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            log.debug("Not a primary node, skipping access point event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(SharedPolicyGroupReactorDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing shared policy group event for {}", deployable.id());
                return sharedPolicyGroupMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            log.debug("Not a primary node, skipping shared policy group event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final NodeMetadataDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing node metadata event for {}", deployable.id());
                return nodeMetadataMapper.to(deployable).flatMapCompletable(distributedEventRepository::createOrUpdate);
            }
            log.debug("Not a primary node, skipping node metadata event distribution");
            return Completable.complete();
        });
    }
}
