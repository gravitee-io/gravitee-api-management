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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.basicauth;

import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.services.sync.process.common.deployer.BasicAuthDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.BasicAuthCredentialsFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.BasicAuthCredentialsMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class BasicAuthSynchronizer implements RepositorySynchronizer {

    private final BasicAuthCredentialsFetcher basicAuthFetcher;
    private final SubscriptionCacheService subscriptionService;
    private final BasicAuthCredentialsMapper basicAuthMapper;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        if (from == -1) {
            return Completable.complete();
        }
        AtomicLong launchTime = new AtomicLong();
        return basicAuthFetcher
            .fetchLatest(from, to, environments)
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .flatMap(credentials ->
                Flowable.just(credentials)
                    .flatMapIterable(s -> s)
                    .flatMap(cred ->
                        Flowable.fromIterable(cred.getSubscriptions())
                            .flatMapIterable(subscriptionId -> subscriptionService.getAllById(subscriptionId))
                            .map(subscription -> basicAuthMapper.to(cred, java.util.Optional.of(subscription)))
                    )
                    .map(credential ->
                        SingleBasicAuthDeployable.builder()
                            .credential(credential)
                            .syncAction(credential.isActive() ? SyncAction.DEPLOY : SyncAction.UNDEPLOY)
                            .build()
                    )
            )
            .compose(upstream -> {
                BasicAuthDeployer deployer = deployerFactory.createBasicAuthDeployer();
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
                    .sequential();
            })
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count ->
                log.debug("BasicAuth credentials refreshed: {} items in {}ms", count, (System.currentTimeMillis() - launchTime.get()))
            )
            .ignoreElement();
    }

    private Flowable<SingleBasicAuthDeployable> deploy(final BasicAuthDeployer deployer, final SingleBasicAuthDeployable deployable) {
        return deployer.deploy(deployable).andThen(deployer.doAfterDeployment(deployable)).andThen(Flowable.just(deployable));
    }

    private Flowable<SingleBasicAuthDeployable> undeploy(final BasicAuthDeployer deployer, final SingleBasicAuthDeployable deployable) {
        return deployer.undeploy(deployable).andThen(deployer.doAfterUndeployment(deployable)).andThen(Flowable.just(deployable));
    }

    @Override
    public int order() {
        return Order.API_KEY.index() + 1;
    }
}
