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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct;

import io.gravitee.gateway.services.sync.process.common.deployer.ApiProductDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiProductMapper;
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

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiProductSynchronizer implements RepositorySynchronizer {

    private static final Set<EventType> INIT_EVENT_TYPES = Set.of(EventType.DEPLOY_API_PRODUCT);

    private static final Set<EventType> INCREMENTAL_EVENT_TYPES = Set.of(EventType.DEPLOY_API_PRODUCT, EventType.UNDEPLOY_API_PRODUCT);

    private final LatestEventFetcher eventsFetcher;
    private final ApiProductMapper apiProductMapper;
    private final ApiProductPlanAppender apiProductPlanAppender;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    public ApiProductSynchronizer(
        final LatestEventFetcher eventsFetcher,
        final ApiProductMapper apiProductMapper,
        final ApiProductPlanAppender apiProductPlanAppender,
        final DeployerFactory deployerFactory,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor
    ) {
        this.eventsFetcher = eventsFetcher;
        this.apiProductMapper = apiProductMapper;
        this.apiProductPlanAppender = apiProductPlanAppender;
        this.deployerFactory = deployerFactory;
        this.syncFetcherExecutor = syncFetcherExecutor;
        this.syncDeployerExecutor = syncDeployerExecutor;
    }

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();

        return eventsFetcher
            .fetchLatest(
                from,
                to,
                Event.EventProperties.API_PRODUCT_ID,
                environments,
                from == -1 ? INIT_EVENT_TYPES : INCREMENTAL_EVENT_TYPES
            )
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .compose(events -> processEvents(events, environments))
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format(
                    "%s API products synchronized in %sms",
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

    @Override
    public int order() {
        return Order.API_PRODUCT.index();
    }

    private Flowable<ApiProductReactorDeployable> processEvents(
        final Flowable<List<Event>> eventsFlowable,
        final Set<String> environments
    ) {
        return eventsFlowable
            // fetch per page
            .flatMap(events ->
                Flowable.just(events)
                    .doOnNext(e -> log.debug("New API product events fetch"))
                    .flatMapIterable(e -> e)
                    .groupBy(Event::getType)
                    .flatMap(eventsByType -> {
                        if (eventsByType.getKey() == EventType.DEPLOY_API_PRODUCT) {
                            return prepareForDeployment(eventsByType, environments);
                        } else if (eventsByType.getKey() == EventType.UNDEPLOY_API_PRODUCT) {
                            return prepareForUndeployment(eventsByType);
                        } else {
                            return Flowable.empty();
                        }
                    })
            )
            // per deployable
            .compose(upstream -> {
                ApiProductDeployer apiProductDeployer = deployerFactory.createApiProductDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deployApiProduct(apiProductDeployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeployApiProduct(apiProductDeployer, deployable);
                        } else {
                            return Flowable.just(deployable);
                        }
                    })
                    .sequential(bulkEvents());
            });
    }

    private Flowable<ApiProductReactorDeployable> prepareForDeployment(
        final GroupedFlowable<EventType, Event> eventsByType,
        final Set<String> environments
    ) {
        return eventsByType
            .flatMapMaybe(apiProductMapper::to)
            .map(reactableApiProduct ->
                ApiProductReactorDeployable.builder()
                    .apiProductId(reactableApiProduct.getId())
                    .syncAction(SyncAction.DEPLOY)
                    .reactableApiProduct(reactableApiProduct)
                    .build()
            )
            .buffer(bulkEvents())
            .map(deployables -> apiProductPlanAppender.appends(deployables, environments))
            .flatMapIterable(d -> d);
    }

    private Flowable<ApiProductReactorDeployable> prepareForUndeployment(final Flowable<Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(apiProductMapper::toId)
            .map(apiProductId -> ApiProductReactorDeployable.builder().syncAction(SyncAction.UNDEPLOY).apiProductId(apiProductId).build());
    }

    private Flowable<ApiProductReactorDeployable> deployApiProduct(
        ApiProductDeployer apiProductDeployer,
        final ApiProductReactorDeployable deployable
    ) {
        return apiProductDeployer
            .deploy(deployable)
            .andThen(apiProductDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    private Flowable<ApiProductReactorDeployable> undeployApiProduct(
        final ApiProductDeployer apiProductDeployer,
        final ApiProductReactorDeployable deployable
    ) {
        return apiProductDeployer
            .undeploy(deployable)
            .andThen(apiProductDeployer.doAfterUndeployment(deployable))
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
