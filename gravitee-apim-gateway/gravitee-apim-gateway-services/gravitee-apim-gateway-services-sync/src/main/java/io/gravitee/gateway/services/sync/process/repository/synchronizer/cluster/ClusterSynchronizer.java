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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.cluster;

import io.gravitee.gateway.services.sync.process.common.deployer.ClusterDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.ClusterMapper;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.flowables.GroupedFlowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;

@CustomLog
public class ClusterSynchronizer implements RepositorySynchronizer {

    private static final Set<EventType> INIT_EVENT_TYPES = Set.of(EventType.DEPLOY_CLUSTER);
    private static final Set<EventType> INCREMENTAL_EVENT_TYPES = Set.of(EventType.DEPLOY_CLUSTER, EventType.UNDEPLOY_CLUSTER);

    private final LatestEventFetcher eventsFetcher;
    private final ClusterMapper clusterMapper;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    public ClusterSynchronizer(
        final LatestEventFetcher eventsFetcher,
        final ClusterMapper clusterMapper,
        final DeployerFactory deployerFactory,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor
    ) {
        this.eventsFetcher = eventsFetcher;
        this.clusterMapper = clusterMapper;
        this.deployerFactory = deployerFactory;
        this.syncFetcherExecutor = syncFetcherExecutor;
        this.syncDeployerExecutor = syncDeployerExecutor;
    }

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();

        return eventsFetcher
            .fetchLatest(from, to, Event.EventProperties.CLUSTER_ID, environments, from == -1 ? INIT_EVENT_TYPES : INCREMENTAL_EVENT_TYPES)
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .compose(this::processEvents)
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format("%s clusters synchronized in %sms", count, (System.currentTimeMillis() - launchTime.get()));
                if (from == -1) {
                    log.info(logMsg);
                } else {
                    log.debug(logMsg);
                }
            })
            .ignoreElement();
    }

    @Override
    public int order() {
        return Order.CLUSTER.index();
    }

    private Flowable<ClusterReactorDeployable> processEvents(final Flowable<List<Event>> eventsFlowable) {
        return eventsFlowable
            .flatMap(events ->
                Flowable.just(events)
                    .doOnNext(e -> log.debug("New cluster events fetch"))
                    .flatMapIterable(e -> e)
                    .groupBy(Event::getType)
                    .flatMap(eventsByType -> {
                        if (eventsByType.getKey() == EventType.DEPLOY_CLUSTER) {
                            return prepareForDeployment(eventsByType);
                        } else if (eventsByType.getKey() == EventType.UNDEPLOY_CLUSTER) {
                            return prepareForUndeployment(eventsByType);
                        } else {
                            return Flowable.empty();
                        }
                    })
            )
            .compose(upstream -> {
                ClusterDeployer clusterDeployer = deployerFactory.createClusterDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deployCluster(clusterDeployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeployCluster(clusterDeployer, deployable);
                        } else {
                            return Flowable.just(deployable);
                        }
                    })
                    .sequential(bulkEvents());
            });
    }

    private Flowable<ClusterReactorDeployable> prepareForDeployment(final GroupedFlowable<EventType, Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(clusterMapper::to)
            .map(reactableCluster ->
                ClusterReactorDeployable.builder()
                    .clusterId(reactableCluster.getId())
                    .syncAction(SyncAction.DEPLOY)
                    .reactableCluster(reactableCluster)
                    .build()
            )
            .buffer(bulkEvents())
            .flatMapIterable(d -> d);
    }

    private Flowable<ClusterReactorDeployable> prepareForUndeployment(final Flowable<Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(clusterMapper::toId)
            .map(clusterId -> ClusterReactorDeployable.builder().syncAction(SyncAction.UNDEPLOY).clusterId(clusterId).build());
    }

    private Flowable<ClusterReactorDeployable> deployCluster(ClusterDeployer clusterDeployer, final ClusterReactorDeployable deployable) {
        return clusterDeployer
            .deploy(deployable)
            .andThen(clusterDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    private Flowable<ClusterReactorDeployable> undeployCluster(
        final ClusterDeployer clusterDeployer,
        final ClusterReactorDeployable deployable
    ) {
        return clusterDeployer
            .undeploy(deployable)
            .andThen(clusterDeployer.doAfterUndeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    protected int bulkEvents() {
        return eventsFetcher.bulkItems();
    }
}
