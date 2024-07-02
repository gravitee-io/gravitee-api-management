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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.debug;

import io.gravitee.gateway.services.sync.process.common.deployer.DebugDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.DebugEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.DebugMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
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
@RequiredArgsConstructor
@Slf4j
public class DebugSynchronizer implements RepositorySynchronizer {

    private final DebugEventFetcher debugEventFetcher;
    private final DebugMapper debugMapperMapper;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        if (from == -1) {
            return Completable.complete();
        } else {
            AtomicLong launchTime = new AtomicLong();
            return debugEventFetcher
                .fetchLatest(from, to, environments)
                .subscribeOn(Schedulers.from(syncFetcherExecutor))
                .flatMap(events ->
                    Flowable
                        .just(events)
                        .flatMapIterable(e -> e)
                        .flatMapMaybe(event ->
                            debugMapperMapper
                                .to(event)
                                .map(reactable -> DebugDeployable.builder().id(event.getId()).reactable(reactable).build())
                        )
                )
                .compose(upstream -> {
                    DebugDeployer debugDeployer = deployerFactory.createDebugDeployer();
                    return upstream
                        .parallel(syncDeployerExecutor.getMaximumPoolSize())
                        .runOn(Schedulers.from(syncDeployerExecutor))
                        .flatMap(debugDeployable ->
                            debugDeployer
                                .deploy(debugDeployable)
                                .andThen(debugDeployer.doAfterDeployment(debugDeployable))
                                .andThen(Flowable.just(debugDeployable))
                                .onErrorResumeNext(throwable -> {
                                    log.error(throwable.getMessage(), throwable);
                                    return Flowable.empty();
                                })
                        )
                        .sequential();
                })
                .count()
                .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
                .doOnSuccess(count -> log.debug("{} debug events refreshed in {}ms", count, (System.currentTimeMillis() - launchTime.get()))
                )
                .ignoreElement();
        }
    }

    @Override
    public int order() {
        return Order.DEBUG.index();
    }
}
