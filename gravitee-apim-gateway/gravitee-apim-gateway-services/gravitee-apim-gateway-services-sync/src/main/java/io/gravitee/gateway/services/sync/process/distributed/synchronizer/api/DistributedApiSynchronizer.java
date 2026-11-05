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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.api;

import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.AbstractDistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class DistributedApiSynchronizer extends AbstractDistributedSynchronizer<ApiReactorDeployable, ApiDeployer> {

    private final DeployerFactory deployerFactory;
    private final ApiMapper apiMapper;

    public DistributedApiSynchronizer(
        final DistributedEventFetcher distributedEventFetcher,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor,
        final DeployerFactory deployerFactory,
        final ApiMapper apiMapper
    ) {
        super(distributedEventFetcher, syncFetcherExecutor, syncDeployerExecutor);
        this.deployerFactory = deployerFactory;
        this.apiMapper = apiMapper;
    }

    @Override
    protected DistributedEventType distributedEventType() {
        return DistributedEventType.API;
    }

    protected Maybe<ApiReactorDeployable> mapTo(final DistributedEvent distributedEvent) {
        return apiMapper.to(distributedEvent);
    }

    @Override
    protected ApiDeployer createDeployer() {
        return deployerFactory.createApiDeployer();
    }

    @Override
    public Completable synchronize(final Long from, final Long to) {
        boolean fullSync = from == null || from == -1;
        return super.synchronize(from, to).andThen(Completable.defer(() -> fullSync ? reconcile() : Completable.complete()));
    }

    /**
     * Mark-and-sweep after a full synchronization: every API still in the local manager that is NOT in the
     * authoritative DEPLOY set published by the primary is a ghost (removed while this gateway was down or
     * missed earlier). It is undeployed so secondaries never serve entities the primary no longer knows about.
     */
    private Completable reconcile() {
        return distributedEventFetcher
            .fetchAuthoritative(DistributedEventType.API, Set.of(DistributedSyncAction.DEPLOY))
            .map(DistributedEvent::getId)
            .collect(HashSet::new, Set::add)
            .flatMapCompletable(authoritativeIds -> {
                Collection<ReactableApi<?>> deployedApis = deployerFactory.apiManager() == null
                    ? List.of()
                    : deployerFactory.apiManager().apis();
                if (deployedApis == null || deployedApis.isEmpty()) {
                    return Completable.complete();
                }
                List<String> ghosts = deployedApis
                    .stream()
                    .map(ReactableApi::getId)
                    .filter(id -> !authoritativeIds.contains(id))
                    .toList();
                if (ghosts.isEmpty()) {
                    return Completable.complete();
                }
                log.warn("Removing {} API(s) no longer present in the distributed sync catalog: {}", ghosts.size(), ghosts);
                ApiDeployer deployer = createDeployer();
                return Flowable.fromIterable(ghosts).flatMapCompletable(apiId ->
                    deployer
                        .undeploy(ApiReactorDeployable.builder().apiId(apiId).syncAction(SyncAction.UNDEPLOY).build())
                        .onErrorResumeNext(t -> {
                            log.error("Unable to remove stale API [{}] during reconciliation", apiId, t);
                            return Completable.complete();
                        })
                );
            });
    }

    @Override
    public int order() {
        return Order.API.index();
    }
}
