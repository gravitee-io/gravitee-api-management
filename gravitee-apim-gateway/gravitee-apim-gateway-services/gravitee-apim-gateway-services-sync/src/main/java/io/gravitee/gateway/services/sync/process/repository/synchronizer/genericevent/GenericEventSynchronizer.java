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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent;

import static io.gravitee.repository.management.model.Event.EventProperties.GENERIC_EVENT_ID;

import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.genericevent.GenericEventManager;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.GenericEventDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Deploys the generic events published since the last cycle, then commits them so subscribers swap their state at once.
 * <p>
 * An event that fails to deploy is logged and skipped: it never fails the cycle, so one bad event cannot hold back the APIs.
 */
@CustomLog
@RequiredArgsConstructor
public class GenericEventSynchronizer implements RepositorySynchronizer {

    private static final Set<EventType> INIT_EVENT_TYPES = Set.of(EventType.PUBLISH_GENERIC_EVENT);
    private static final Set<EventType> INCREMENTAL_EVENT_TYPES = Set.of(
        EventType.PUBLISH_GENERIC_EVENT,
        EventType.UNPUBLISH_GENERIC_EVENT
    );

    private final LatestEventFetcher eventsFetcher;
    private final GenericEventMapper genericEventMapper;
    private final GenericEventManager genericEventManager;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();
        return eventsFetcher
            .fetchLatest(from, to, GENERIC_EVENT_ID, environments, from == -1 ? INIT_EVENT_TYPES : INCREMENTAL_EVENT_TYPES)
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .flatMap(events ->
                Flowable.fromIterable(events).flatMapMaybe(event ->
                    genericEventMapper.to(
                        event,
                        event.getType() == EventType.PUBLISH_GENERIC_EVENT ? SyncAction.DEPLOY : SyncAction.UNDEPLOY
                    )
                )
            )
            .compose(upstream -> {
                GenericEventDeployer deployer = deployerFactory.createGenericEventDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable ->
                        deployOrUndeploy(deployer, deployable)
                            .andThen(Flowable.just(deployable))
                            .onErrorResumeNext(throwable -> {
                                log.error(throwable.getMessage(), throwable);
                                return Flowable.empty();
                            })
                    )
                    .sequential(eventsFetcher.bulkItems());
            })
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .flatMapCompletable(count -> {
                var expired = genericEventManager.commit();
                String logMsg = String.format(
                    "%s generic events synchronized in %sms",
                    count,
                    (System.currentTimeMillis() - launchTime.get())
                );
                if (from == -1) {
                    log.info(logMsg);
                } else {
                    log.debug(logMsg);
                }
                return undistribute(expired);
            });
    }

    private static Completable deployOrUndeploy(GenericEventDeployer deployer, GenericEventDeployable deployable) {
        return deployable.syncAction() == SyncAction.DEPLOY
            ? deployer.deploy(deployable).andThen(deployer.doAfterDeployment(deployable))
            : deployer.undeploy(deployable).andThen(deployer.doAfterUndeployment(deployable));
    }

    /** Expired events leave without an unpublish, so secondary nodes are told here. */
    private Completable undistribute(List<GenericEvent> expired) {
        if (expired.isEmpty()) {
            return Completable.complete();
        }
        var deployer = deployerFactory.createGenericEventDeployer();
        return Flowable.fromIterable(expired).concatMapCompletable(event ->
            deployer
                .doAfterUndeployment(new GenericEventDeployable(GenericEventPayload.latestEventId(event), SyncAction.UNDEPLOY, event))
                .onErrorComplete(throwable -> {
                    log.error("Unable to distribute the expiry of generic event [{}] of kind [{}]", event.id(), event.kind(), throwable);
                    return true;
                })
        );
    }

    @Override
    public int order() {
        return Order.GENERIC_EVENT.index();
    }
}
