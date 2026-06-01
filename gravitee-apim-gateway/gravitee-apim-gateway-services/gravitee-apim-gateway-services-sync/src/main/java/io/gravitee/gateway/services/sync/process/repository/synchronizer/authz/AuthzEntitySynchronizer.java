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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import io.gravitee.gateway.services.sync.process.common.deployer.AuthzEntityDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
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
public class AuthzEntitySynchronizer implements RepositorySynchronizer {

    private static final Set<EventType> INIT_EVENT_TYPES = Set.of(EventType.PUBLISH_AUTHZ_ENTITY);
    private static final Set<EventType> INCREMENTAL_EVENT_TYPES = Set.of(EventType.PUBLISH_AUTHZ_ENTITY, EventType.UNPUBLISH_AUTHZ_ENTITY);

    private final LatestEventFetcher eventsFetcher;
    private final AuthzEntityMapper mapper;
    private final DeployerFactory deployerFactory;
    private final AuthzEnginePort enginePort;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    public AuthzEntitySynchronizer(
        LatestEventFetcher eventsFetcher,
        AuthzEntityMapper mapper,
        DeployerFactory deployerFactory,
        AuthzEnginePort enginePort,
        ThreadPoolExecutor syncFetcherExecutor,
        ThreadPoolExecutor syncDeployerExecutor
    ) {
        this.eventsFetcher = eventsFetcher;
        this.mapper = mapper;
        this.deployerFactory = deployerFactory;
        this.enginePort = enginePort;
        this.syncFetcherExecutor = syncFetcherExecutor;
        this.syncDeployerExecutor = syncDeployerExecutor;
    }

    @Override
    public Completable synchronize(Long from, Long to, Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();
        AtomicLong processed = new AtomicLong();
        boolean initialSync = from == null || from.longValue() == -1L;

        return eventsFetcher
            .fetchLatest(
                from,
                to,
                Event.EventProperties.AUTHZ_ENTITY_ID,
                environments,
                initialSync ? INIT_EVENT_TYPES : INCREMENTAL_EVENT_TYPES
            )
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .compose(events -> processEvents(events, initialSync))
            .count()
            .doOnSubscribe(d -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(processed::set)
            .ignoreElement()
            .andThen(commitIfAny(processed))
            .doOnComplete(() -> {
                String msg = String.format(
                    "%s authz entities synchronized in %sms",
                    processed.get(),
                    System.currentTimeMillis() - launchTime.get()
                );
                if (initialSync) {
                    log.info(msg);
                } else {
                    log.debug(msg);
                }
            });
    }

    @Override
    public int order() {
        return Order.AUTHZ_ENTITY.index();
    }

    private Completable commitIfAny(AtomicLong processed) {
        return Completable.defer(() -> processed.get() == 0L ? Completable.complete() : enginePort.commit());
    }

    private Flowable<AuthzEntityReactorDeployable> processEvents(Flowable<List<Event>> eventsFlowable, boolean initialSync) {
        return eventsFlowable
            .flatMap(events ->
                Flowable.just(events)
                    .doOnNext(e -> log.debug("New authz entity events fetched"))
                    .flatMapIterable(e -> e)
                    .groupBy(Event::getType)
                    .flatMap(eventsByType -> {
                        EventType type = eventsByType.getKey();
                        if (type == EventType.PUBLISH_AUTHZ_ENTITY) {
                            return prepareForDeployment(eventsByType, initialSync);
                        } else if (type == EventType.UNPUBLISH_AUTHZ_ENTITY) {
                            return prepareForUndeployment(eventsByType);
                        }
                        return Flowable.empty();
                    })
            )
            .compose(upstream -> {
                AuthzEntityDeployer deployer = deployerFactory.createAuthzEntityDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deployEntity(deployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeployEntity(deployer, deployable);
                        }
                        return Flowable.just(deployable);
                    })
                    .sequential(bulkEvents());
            });
    }

    private Flowable<AuthzEntityReactorDeployable> prepareForDeployment(
        GroupedFlowable<EventType, Event> eventsByType,
        boolean initialSync
    ) {
        return eventsByType
            .flatMapMaybe(mapper::toDeploy)
            .buffer(bulkEvents())
            .flatMapIterable(d -> d);
    }

    private Flowable<AuthzEntityReactorDeployable> prepareForUndeployment(Flowable<Event> events) {
        return events.flatMapMaybe(mapper::toUndeploy);
    }

    private Flowable<AuthzEntityReactorDeployable> deployEntity(AuthzEntityDeployer deployer, AuthzEntityReactorDeployable deployable) {
        return deployer
            .deploy(deployable)
            .andThen(deployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(t -> {
                log.error(t.getMessage(), t);
                return Flowable.empty();
            });
    }

    private Flowable<AuthzEntityReactorDeployable> undeployEntity(AuthzEntityDeployer deployer, AuthzEntityReactorDeployable deployable) {
        return deployer
            .undeploy(deployable)
            .andThen(deployer.doAfterUndeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(t -> {
                log.error(t.getMessage(), t);
                return Flowable.empty();
            });
    }

    protected int bulkEvents() {
        return eventsFetcher.bulkItems();
    }
}
