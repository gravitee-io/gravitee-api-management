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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint;

import io.gravitee.gateway.services.sync.process.common.deployer.AccessPointDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.AccessPointFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.AccessPointMapper;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AccessPointSynchronizer implements RepositorySynchronizer {

    private final AccessPointFetcher accessPointFetcher;
    private final AccessPointMapper accessPointMapper;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(Long from, Long to, Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();
        return accessPointFetcher
            .fetchLatest(from, to, environments, from == -1 ? AccessPointStatus.CREATED : null)
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .flatMap(accessPoints ->
                Flowable
                    .just(accessPoints)
                    .flatMapIterable(e -> e)
                    .groupBy(AccessPoint::getStatus)
                    .flatMap(accessPointsByStatus -> {
                        if (accessPointsByStatus.getKey() == AccessPointStatus.CREATED) {
                            return prepareForDeployment(accessPointsByStatus);
                        } else if (accessPointsByStatus.getKey() == AccessPointStatus.DELETED) {
                            return prepareForUndeployment(accessPointsByStatus);
                        } else {
                            return Flowable.empty();
                        }
                    })
            )
            .compose(upstream -> {
                AccessPointDeployer accessPointDeployer = deployerFactory.createAccessPointDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deploy(accessPointDeployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeploy(accessPointDeployer, deployable);
                        } else {
                            return Flowable.empty();
                        }
                    })
                    .sequential(accessPointFetcher.bulkItems());
            })
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format(
                    "%s access points synchronized in %sms",
                    count,
                    (System.currentTimeMillis() - launchTime.get())
                );
                if (from == -1) {
                    log.info(logMsg);
                } else {
                    log.debug(logMsg);
                }
            })
            .ignoreElement();
    }

    private Flowable<AccessPointDeployable> prepareForDeployment(final Flowable<AccessPoint> accessPoints) {
        return accessPoints.map(accessPoint ->
            AccessPointDeployable.builder().reactableAccessPoint(accessPointMapper.to(accessPoint)).syncAction(SyncAction.DEPLOY).build()
        );
    }

    private Flowable<AccessPointDeployable> prepareForUndeployment(final Flowable<AccessPoint> accessPoints) {
        return accessPoints.map(accessPoint ->
            AccessPointDeployable.builder().reactableAccessPoint(accessPointMapper.to(accessPoint)).syncAction(SyncAction.UNDEPLOY).build()
        );
    }

    private static Flowable<AccessPointDeployable> deploy(
        final AccessPointDeployer accessPointDeployer,
        final AccessPointDeployable deployable
    ) {
        return accessPointDeployer
            .deploy(deployable)
            .andThen(accessPointDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    private static Flowable<AccessPointDeployable> undeploy(
        final AccessPointDeployer accessPointDeployer,
        final AccessPointDeployable deployable
    ) {
        return accessPointDeployer
            .undeploy(deployable)
            .andThen(accessPointDeployer.doAfterUndeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    @Override
    public int order() {
        return Order.ACCESS_POINT.index();
    }
}
