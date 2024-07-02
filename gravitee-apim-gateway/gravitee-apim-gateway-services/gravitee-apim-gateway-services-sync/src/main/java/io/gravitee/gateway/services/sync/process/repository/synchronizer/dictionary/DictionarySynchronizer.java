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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary;

import static io.gravitee.repository.management.model.Event.EventProperties.DICTIONARY_ID;

import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.DictionaryDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.DictionaryMapper;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
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
@Slf4j
@RequiredArgsConstructor
public class DictionarySynchronizer implements RepositorySynchronizer {

    private static final Set<EventType> INIT_EVENT_TYPES = Set.of(EventType.PUBLISH_DICTIONARY);
    private static final Set<EventType> INCREMENTAL_EVENT_TYPES = Set.of(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY);
    private final LatestEventFetcher eventsFetcher;
    private final DictionaryMapper dictionaryMapper;
    private final DeployerFactory deployerFactory;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    @Override
    public Completable synchronize(final Long from, final Long to, final Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();
        return eventsFetcher
            .fetchLatest(from, to, DICTIONARY_ID, environments, from == -1 ? INIT_EVENT_TYPES : INCREMENTAL_EVENT_TYPES)
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            // fetch per page
            .flatMap(events ->
                Flowable
                    .just(events)
                    .flatMapIterable(e -> e)
                    .groupBy(Event::getType)
                    .flatMap(eventsByType -> {
                        if (eventsByType.getKey() == EventType.PUBLISH_DICTIONARY) {
                            return prepareForDeployment(eventsByType);
                        } else if (eventsByType.getKey() == EventType.UNPUBLISH_DICTIONARY) {
                            return prepareForUndeployment(eventsByType);
                        } else {
                            return Flowable.empty();
                        }
                    })
            )
            // per deployable
            .compose(upstream -> {
                DictionaryDeployer dictionaryDeployer = deployerFactory.createDictionaryDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deploy(dictionaryDeployer, deployable);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeploy(dictionaryDeployer, deployable);
                        } else {
                            return Flowable.empty();
                        }
                    })
                    .sequential(eventsFetcher.bulkItems());
            })
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format(
                    "%s dictionaries synchronized in %sms",
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

    private Flowable<DictionaryDeployable> prepareForDeployment(final Flowable<Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(dictionaryMapper::to)
            .map(dictionary ->
                DictionaryDeployable.builder().id(dictionary.getId()).dictionary(dictionary).syncAction(SyncAction.DEPLOY).build()
            );
    }

    private Flowable<DictionaryDeployable> prepareForUndeployment(final Flowable<Event> eventsByType) {
        return eventsByType
            .flatMapMaybe(dictionaryMapper::toId)
            .map(dictionaryId -> DictionaryDeployable.builder().id(dictionaryId).syncAction(SyncAction.UNDEPLOY).build());
    }

    private static Flowable<DictionaryDeployable> deploy(
        final DictionaryDeployer dictionaryDeployer,
        final DictionaryDeployable deployable
    ) {
        return dictionaryDeployer
            .deploy(deployable)
            .andThen(dictionaryDeployer.doAfterDeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    private static Flowable<DictionaryDeployable> undeploy(
        final DictionaryDeployer dictionaryDeployer,
        final DictionaryDeployable deployable
    ) {
        return dictionaryDeployer
            .undeploy(deployable)
            .andThen(dictionaryDeployer.doAfterUndeployment(deployable))
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(throwable -> {
                log.error(throwable.getMessage(), throwable);
                return Flowable.empty();
            });
    }

    @Override
    public int order() {
        return Order.DICTIONARY.index();
    }
}
