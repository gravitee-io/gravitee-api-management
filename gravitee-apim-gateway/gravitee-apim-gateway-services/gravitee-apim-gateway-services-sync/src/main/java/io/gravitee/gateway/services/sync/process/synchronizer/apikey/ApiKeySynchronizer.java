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
package io.gravitee.gateway.services.sync.process.synchronizer.apikey;

import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.services.sync.process.deployer.ApiKeyDeployer;
import io.gravitee.gateway.services.sync.process.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.fetcher.ApiKeyFetcher;
import io.gravitee.gateway.services.sync.process.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.model.SyncAction;
import io.gravitee.gateway.services.sync.process.synchronizer.Synchronizer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class ApiKeySynchronizer implements Synchronizer {

    private final ApiKeyFetcher apiKeyFetcher;
    private final SubscriptionCacheService subscriptionService;
    private final ApiKeyMapper apiKeyMapper;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final List<String> environments) {
        if (from == -1) {
            return Completable.complete();
        } else {
            AtomicLong launchTime = new AtomicLong();
            return apiKeyFetcher
                .fetchLatest(from, to)
                // append per page
                .flatMap(apiKeys ->
                    Flowable
                        .just(apiKeys)
                        .subscribeOn(Schedulers.from(syncFetcherExecutor))
                        .flatMapIterable(s -> s)
                        .flatMap(apiKey ->
                            Flowable
                                .fromIterable(apiKey.getSubscriptions())
                                .map(subscriptionId -> apiKeyMapper.to(apiKey, subscriptionService.getById(subscriptionId)))
                        )
                        .map(apiKey ->
                            SingleApikeyDeployable
                                .builder()
                                .apiKey(apiKey)
                                .syncAction(apiKey.isActive() ? SyncAction.DEPLOY : SyncAction.UNDEPLOY)
                                .build()
                        )
                )
                // per deployable
                .compose(upstream -> {
                    ApiKeyDeployer apiKeyDeployer = deployerFactory.createApiKeyDeployer();
                    return upstream
                        .parallel(syncDeployerExecutor.getMaximumPoolSize())
                        .runOn(Schedulers.from(syncDeployerExecutor))
                        .flatMap(deployable -> {
                            if (deployable.syncAction() == SyncAction.DEPLOY) {
                                return deploy(apiKeyDeployer, deployable);
                            } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                                return undeploy(apiKeyDeployer, deployable);
                            } else {
                                return Flowable.empty();
                            }
                        })
                        .sequential();
                })
                .count()
                .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
                .doOnSuccess(count ->
                    log.debug("ApiKeys of {} apis refreshed in {}ms", count, (System.currentTimeMillis() - launchTime.get()))
                )
                .ignoreElement();
        }
    }

    private Flowable<SingleApikeyDeployable> deploy(final ApiKeyDeployer apiKeyDeployer, final SingleApikeyDeployable deployable) {
        return apiKeyDeployer.deploy(deployable).andThen(apiKeyDeployer.doAtferDeployment(deployable)).andThen(Flowable.just(deployable));
    }

    private Flowable<SingleApikeyDeployable> undeploy(final ApiKeyDeployer apiKeyDeployer, final SingleApikeyDeployable deployable) {
        return apiKeyDeployer.undeploy(deployable).andThen(Flowable.just(deployable));
    }

    @Override
    public int order() {
        return 30;
    }
}
