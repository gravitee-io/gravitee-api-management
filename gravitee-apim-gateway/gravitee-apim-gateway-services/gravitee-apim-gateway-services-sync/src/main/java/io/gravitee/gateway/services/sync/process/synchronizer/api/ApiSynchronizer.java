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
package io.gravitee.gateway.services.sync.process.synchronizer.api;

import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;

import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.process.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.synchronizer.Synchronizer;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
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
public class ApiSynchronizer extends AbstractApiSynchronizer implements Synchronizer {

    private static final Set<EventType> INIT_EVENT_TYPES = Set.of(EventType.PUBLISH_API, EventType.START_API);
    private static final Set<EventType> INCREMENTAL_EVENT_TYPES = Set.of(
        EventType.PUBLISH_API,
        EventType.START_API,
        EventType.UNPUBLISH_API,
        EventType.STOP_API
    );
    private final LatestEventFetcher eventsFetcher;

    public ApiSynchronizer(
        final LatestEventFetcher eventsFetcher,
        final ApiManager apiManager,
        final ApiMapper apiMapper,
        final PlanAppender planAppender,
        final SubscriptionAppender subscriptionAppender,
        final ApiKeyAppender apiKeyAppender,
        final DeployerFactory deployerFactory,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor
    ) {
        super(
            apiManager,
            apiMapper,
            planAppender,
            subscriptionAppender,
            apiKeyAppender,
            deployerFactory,
            syncFetcherExecutor,
            syncDeployerExecutor
        );
        this.eventsFetcher = eventsFetcher;
    }

    @Override
    public Completable synchronize(final Long from, final Long to, final List<String> environments) {
        AtomicLong launchTime = new AtomicLong();
        return eventsFetcher
            .fetchLatest(from, to, API_ID, environments, from == -1 ? INIT_EVENT_TYPES : INCREMENTAL_EVENT_TYPES)
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .compose(eventsFlowable -> processEvents(eventsFlowable, from, to))
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format("%s apis synchronized in %sms", count, (System.currentTimeMillis() - launchTime.get()));
                if (from == -1) {
                    log.info(logMsg);
                } else {
                    log.debug(logMsg);
                }
            })
            .ignoreElement();
    }

    @Override
    protected int bulkEvents() {
        return eventsFetcher.bulkItems();
    }

    @Override
    public int order() {
        return 1;
    }
}
