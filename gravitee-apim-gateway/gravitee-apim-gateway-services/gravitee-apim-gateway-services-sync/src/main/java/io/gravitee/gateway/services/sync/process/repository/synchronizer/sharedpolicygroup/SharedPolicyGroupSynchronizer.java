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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.SharedPolicyGroupDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.SharedPolicyGroupMapper;
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
public class SharedPolicyGroupSynchronizer implements RepositorySynchronizer {

    private static final Set<EventType> INIT_EVENT_TYPES = Set.of(EventType.DEPLOY_SHARED_POLICY_GROUP);
    private static final Set<EventType> INCREMENTAL_EVENT_TYPES = Set.of(
        EventType.DEPLOY_SHARED_POLICY_GROUP,
        EventType.UNDEPLOY_SHARED_POLICY_GROUP
    );
    private final LatestEventFetcher eventsFetcher;
    private final SharedPolicyGroupMapper sharedPolicyGroupMapper;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    public SharedPolicyGroupSynchronizer(
        final LatestEventFetcher eventsFetcher,
        final SharedPolicyGroupMapper sharedPolicyGroupMapper,
        final DeployerFactory deployerFactory,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor
    ) {
        this.eventsFetcher = eventsFetcher;
        this.sharedPolicyGroupMapper = sharedPolicyGroupMapper;
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
                Event.EventProperties.SHARED_POLICY_GROUP_ID,
                environments,
                from == -1 ? INIT_EVENT_TYPES : INCREMENTAL_EVENT_TYPES
            )
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .compose(this::processEvents)
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format(
                    "%s shared policy groups synchronized in %sms",
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
        return Order.SHARED_POLICY_GROUP.index();
    }

    private Flowable<SharedPolicyGroupReactorDeployable> processEvents(final Flowable<List<Event>> eventsFlowable) {
        return eventsFlowable
            // fetch per page
            .flatMap(events ->
                Flowable
                    .just(events)
                    .doOnNext(e -> log.debug("New shared policy group events fetch"))
                    .flatMapIterable(e -> e)
                    .groupBy(Event::getType)
                    .flatMap(eventsByType -> {
                        if (eventsByType.getKey() == EventType.DEPLOY_SHARED_POLICY_GROUP) {
                            return prepareForDeployment(eventsByType);
                        } else if (eventsByType.getKey() == EventType.UNDEPLOY_SHARED_POLICY_GROUP) {
                            return prepareForUndeployment(eventsByType);
                        } else {
                            return Flowable.empty();
                        }
                    })
            )
            // per deployable
            .compose(upstream -> {
                SharedPolicyGroupDeployer sharedPolicyGroupDeployer = deployerFactory.createSharedPolicyGroupDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deploySharedPolicyGroup(sharedPolicyGroupDeployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeploySharedPolicyGroup(sharedPolicyGroupDeployer, deployable);
                        } else {
                            return Flowable.just(deployable);
                        }
                    })
                    .sequential(bulkEvents());
            });
    }

    private Flowable<SharedPolicyGroupReactorDeployable> prepareForDeployment(final GroupedFlowable<EventType, Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(sharedPolicyGroupMapper::to)
            .map(reactableSharedPolicyGroup ->
                SharedPolicyGroupReactorDeployable
                    .builder()
                    .sharedPolicyGroupId(reactableSharedPolicyGroup.getId())
                    .syncAction(SyncAction.DEPLOY)
                    .reactableSharedPolicyGroup(reactableSharedPolicyGroup)
                    .build()
            )
            .buffer(bulkEvents())
            .flatMapIterable(d -> d);
    }

    private Flowable<SharedPolicyGroupReactorDeployable> prepareForUndeployment(final Flowable<Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(sharedPolicyGroupMapper::toId)
            .map(sharedPolicyGroupId ->
                SharedPolicyGroupReactorDeployable
                    .builder()
                    .syncAction(SyncAction.UNDEPLOY)
                    .sharedPolicyGroupId(sharedPolicyGroupId)
                    .build()
            );
    }

    private Flowable<SharedPolicyGroupReactorDeployable> deploySharedPolicyGroup(
        SharedPolicyGroupDeployer sharedPolicyGroupDeployer,
        final SharedPolicyGroupReactorDeployable deployable
    ) {
        return sharedPolicyGroupDeployer
            .deploy(deployable)
            .andThen(sharedPolicyGroupDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    private Flowable<SharedPolicyGroupReactorDeployable> undeploySharedPolicyGroup(
        final SharedPolicyGroupDeployer sharedPolicyGroupDeployer,
        final SharedPolicyGroupReactorDeployable deployable
    ) {
        return sharedPolicyGroupDeployer
            .undeploy(deployable)
            .andThen(sharedPolicyGroupDeployer.doAfterUndeployment(deployable))
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
