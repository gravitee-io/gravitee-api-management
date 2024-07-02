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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.SubscriptionDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.SubscriptionFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.repository.management.model.Subscription;
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
public class SubscriptionSynchronizer implements RepositorySynchronizer {

    private final SubscriptionFetcher subscriptionFetcher;
    private final SubscriptionMapper subscriptionMapper;
    private final DeployerFactory deployerFactory;
    private final PlanService planCache;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        if (from == -1) {
            return Completable.complete();
        } else {
            AtomicLong launchTime = new AtomicLong();
            return subscriptionFetcher
                .fetchLatest(from, to, environments)
                .subscribeOn(Schedulers.from(syncFetcherExecutor))
                // append per page
                .flatMap(subscriptions ->
                    Flowable
                        .just(subscriptions)
                        .flatMapIterable(s -> s)
                        .filter(subscription -> planCache.isDeployed(subscription.getApi(), subscription.getPlan()))
                        .flatMapMaybe(subscription -> Maybe.fromCallable(() -> subscriptionMapper.to(subscription)))
                        .map(subscription ->
                            SingleSubscriptionDeployable
                                .builder()
                                .subscription(subscription)
                                .syncAction(
                                    subscription.getStatus().equals(Subscription.Status.ACCEPTED.name())
                                        ? SyncAction.DEPLOY
                                        : SyncAction.UNDEPLOY
                                )
                                .build()
                        )
                )
                // per deployable
                .compose(upstream -> {
                    SubscriptionDeployer subscriptionDeployer = deployerFactory.createSubscriptionDeployer();
                    return upstream
                        .parallel(syncDeployerExecutor.getMaximumPoolSize())
                        .runOn(Schedulers.from(syncDeployerExecutor))
                        .flatMap(deployable -> {
                            if (deployable.syncAction() == SyncAction.DEPLOY) {
                                return deploy(subscriptionDeployer, deployable);
                            } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                                return undeploy(subscriptionDeployer, deployable);
                            } else {
                                return Flowable.empty();
                            }
                        })
                        .sequential();
                })
                .count()
                .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
                .doOnSuccess(count ->
                    log.debug("Subscriptions of {} apis refreshed in {}ms", count, (System.currentTimeMillis() - launchTime.get()))
                )
                .ignoreElement();
        }
    }

    private Flowable<SingleSubscriptionDeployable> deploy(
        final SubscriptionDeployer subscriptionDeployer,
        final SingleSubscriptionDeployable deployable
    ) {
        return subscriptionDeployer
            .deploy(deployable)
            .andThen(subscriptionDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    private Flowable<SingleSubscriptionDeployable> undeploy(
        final SubscriptionDeployer subscriptionDeployer,
        final SingleSubscriptionDeployable deployable
    ) {
        return subscriptionDeployer
            .undeploy(deployable)
            .andThen(subscriptionDeployer.doAfterUndeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    @Override
    public int order() {
        return Order.SUBSCRIPTION.index();
    }
}
