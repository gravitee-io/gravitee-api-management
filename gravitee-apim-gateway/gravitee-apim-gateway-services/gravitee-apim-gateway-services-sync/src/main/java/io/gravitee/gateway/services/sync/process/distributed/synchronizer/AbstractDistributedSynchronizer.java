/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.process.distributed.synchronizer;

import io.gravitee.gateway.services.sync.process.common.deployer.Deployer;
import io.gravitee.gateway.services.sync.process.common.model.Deployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.distributed.DistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDistributedSynchronizer<T extends Deployable, Y extends Deployer<T>> implements DistributedSynchronizer {

    protected static final Set<DistributedSyncAction> INIT_SYNC_ACTIONS = Set.of(DistributedSyncAction.DEPLOY);
    protected static final Set<DistributedSyncAction> INCREMENTAL_SYNC_ACTIONS = Set.of(
        DistributedSyncAction.DEPLOY,
        DistributedSyncAction.UNDEPLOY
    );
    private final DistributedEventFetcher distributedEventFetcher;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to) {
        boolean initialSync = from == null || from == -1;
        AtomicLong launchTime = new AtomicLong();
        return distributedEventFetcher
            .fetchLatest(from, to, distributedEventType(), syncActions(initialSync))
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .flatMapMaybe(this::mapTo)
            // per deployable
            .compose(upstream -> {
                Y deployer = createDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deploy(deployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeploy(deployer, deployable);
                        } else {
                            return Flowable.empty();
                        }
                    })
                    .sequential(distributedEventFetcher.bulkItems());
            })
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format(
                    "%s %s(s) synchronized in %sms",
                    count,
                    distributedEventType().name().toLowerCase(),
                    (System.currentTimeMillis() - launchTime.get())
                );
                if (initialSync) {
                    log.info(logMsg);
                } else {
                    log.debug(logMsg);
                }
            })
            .ignoreElement();
    }

    protected Set<DistributedSyncAction> syncActions(final boolean initialSync) {
        return initialSync ? INIT_SYNC_ACTIONS : INCREMENTAL_SYNC_ACTIONS;
    }

    protected abstract DistributedEventType distributedEventType();

    protected abstract Maybe<T> mapTo(final DistributedEvent distributedEvent);

    protected abstract Y createDeployer();

    private Flowable<T> deploy(final Y apiDeployer, final T deployable) {
        return apiDeployer.deploy(deployable).andThen(apiDeployer.doAfterDeployment(deployable)).andThen(Flowable.just(deployable));
    }

    private Flowable<T> undeploy(final Y apiDeployer, final T deployable) {
        return apiDeployer.undeploy(deployable).andThen(apiDeployer.doAfterUndeployment(deployable)).andThen(Flowable.just(deployable));
    }
}
