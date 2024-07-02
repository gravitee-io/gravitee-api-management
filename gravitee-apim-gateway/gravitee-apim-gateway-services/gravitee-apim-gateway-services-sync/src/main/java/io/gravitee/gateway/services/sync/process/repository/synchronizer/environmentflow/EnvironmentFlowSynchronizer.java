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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.environmentflow;

import static io.gravitee.repository.management.model.Event.EventProperties.ENVIRONMENT_FLOW_ID;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.EnvironmentFlowDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.EnvironmentFlowMapper;
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
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class EnvironmentFlowSynchronizer implements RepositorySynchronizer {

    private static final Set<EventType> INIT_EVENT_TYPES = Set.of(EventType.DEPLOY_ENVIRONMENT_FLOW);
    private static final Set<EventType> INCREMENTAL_EVENT_TYPES = Set.of(
        EventType.DEPLOY_ENVIRONMENT_FLOW,
        EventType.UNDEPLOY_ENVIRONMENT_FLOW
    );
    private final LatestEventFetcher eventsFetcher;
    private final EnvironmentFlowMapper environmentFlowMapper;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    public EnvironmentFlowSynchronizer(
        final LatestEventFetcher eventsFetcher,
        final EnvironmentFlowMapper environmentFlowMapper,
        final DeployerFactory deployerFactory,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor
    ) {
        this.eventsFetcher = eventsFetcher;
        this.environmentFlowMapper = environmentFlowMapper;
        this.deployerFactory = deployerFactory;
        this.syncFetcherExecutor = syncFetcherExecutor;
        this.syncDeployerExecutor = syncDeployerExecutor;
    }

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();

        return eventsFetcher
            .fetchLatest(from, to, ENVIRONMENT_FLOW_ID, environments, from == -1 ? INIT_EVENT_TYPES : INCREMENTAL_EVENT_TYPES)
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .compose(this::processEvents)
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format(
                    "%s environment flows synchronized in %sms",
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
        return 3;
    }

    private Flowable<EnvironmentFlowReactorDeployable> processEvents(final Flowable<List<Event>> eventsFlowable) {
        return eventsFlowable
            // fetch per page
            .flatMap(events ->
                Flowable
                    .just(events)
                    .doOnNext(e -> log.debug("New environment flow events fetch"))
                    .flatMapIterable(e -> e)
                    .groupBy(Event::getType)
                    .flatMap(eventsByType -> {
                        if (eventsByType.getKey() == EventType.DEPLOY_ENVIRONMENT_FLOW) {
                            return prepareForDeployment(eventsByType);
                        } else if (eventsByType.getKey() == EventType.UNDEPLOY_ENVIRONMENT_FLOW) {
                            return prepareForUndeployment(eventsByType);
                        } else {
                            return Flowable.empty();
                        }
                    })
            )
            // per deployable
            .compose(upstream -> {
                EnvironmentFlowDeployer environmentFlowDeployer = deployerFactory.createEnvironmentFlowDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deployEnvironmentFlow(environmentFlowDeployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeployEnvironmentFlow(environmentFlowDeployer, deployable);
                        } else {
                            return Flowable.just(deployable);
                        }
                    })
                    .sequential(bulkEvents());
            });
    }

    private Flowable<EnvironmentFlowReactorDeployable> prepareForDeployment(final GroupedFlowable<EventType, Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(environmentFlowMapper::to)
            .map(reactableEnvironmentFlow ->
                EnvironmentFlowReactorDeployable
                    .builder()
                    .environmentFlowId(reactableEnvironmentFlow.getId())
                    .syncAction(SyncAction.DEPLOY)
                    .reactableEnvironmentFlow(reactableEnvironmentFlow)
                    .build()
            )
            .buffer(bulkEvents())
            .flatMapIterable(d -> d);
    }

    private Flowable<EnvironmentFlowReactorDeployable> prepareForUndeployment(final Flowable<Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(environmentFlowMapper::toId)
            .map(environmentFlowId ->
                EnvironmentFlowReactorDeployable.builder().syncAction(SyncAction.UNDEPLOY).environmentFlowId(environmentFlowId).build()
            );
    }

    private Flowable<EnvironmentFlowReactorDeployable> deployEnvironmentFlow(
        EnvironmentFlowDeployer environmentFlowDeployer,
        final EnvironmentFlowReactorDeployable deployable
    ) {
        return environmentFlowDeployer
            .deploy(deployable)
            .andThen(environmentFlowDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    private Flowable<EnvironmentFlowReactorDeployable> undeployEnvironmentFlow(
        final EnvironmentFlowDeployer environmentFlowDeployer,
        final EnvironmentFlowReactorDeployable deployable
    ) {
        return environmentFlowDeployer
            .undeploy(deployable)
            .andThen(environmentFlowDeployer.doAfterUndeployment(deployable))
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
