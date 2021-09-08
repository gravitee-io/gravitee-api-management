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
package io.gravitee.gateway.services.sync.synchronizer;

import static io.gravitee.gateway.services.sync.spring.SyncConfiguration.PARALLELISM;
import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;

import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.cache.ApiKeysCacheService;
import io.gravitee.gateway.services.sync.cache.SubscriptionsCacheService;
import io.gravitee.repository.management.model.*;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSynchronizer extends AbstractSynchronizer {

    private final Logger logger = LoggerFactory.getLogger(ApiSynchronizer.class);

    @Autowired
    private PlanFetcher planFetcher;

    @Autowired
    private ApiKeysCacheService apiKeysCacheService;

    @Autowired
    private SubscriptionsCacheService subscriptionsCacheService;

    @Autowired
    private ApiManager apiManager;

    @Value("${services.sync.bulk_items:100}")
    protected int bulkItems;

    public void synchronize(long lastRefreshAt, long nextLastRefreshAt, List<String> environments) {
        final long start = System.currentTimeMillis();
        final Long count;

        if (lastRefreshAt == -1) {
            count = initialSynchronizeApis(nextLastRefreshAt, environments);
        } else {
            count =
                this.searchLatestEvents(
                        getBulkSize(),
                        lastRefreshAt,
                        nextLastRefreshAt,
                        API_ID,
                        environments,
                        EventType.PUBLISH_API,
                        EventType.START_API,
                        EventType.UNPUBLISH_API,
                        EventType.STOP_API
                    )
                    .compose(this::processApiEvents)
                    .count()
                    .blockingGet();
        }

        if (lastRefreshAt == -1) {
            logger.info("{} apis synchronized in {}ms", count, (System.currentTimeMillis() - start));
        } else {
            logger.debug("{} apis synchronized in {}ms", count, (System.currentTimeMillis() - start));
        }
    }

    /**
     * Run the initial synchronization which focus on api PUBLISH and START events only.
     */
    private long initialSynchronizeApis(long nextLastRefreshAt, List<String> environments) {
        final Long count =
            this.searchLatestEvents(bulkItems, null, nextLastRefreshAt, API_ID, environments, EventType.PUBLISH_API, EventType.START_API)
                .compose(this::processApiRegisterEvents)
                .count()
                .blockingGet();

        return count;
    }

    @NonNull
    private Flowable<String> processApiEvents(Flowable<Event> upstream) {
        return upstream
            .groupBy(Event::getType)
            .flatMap(
                eventsByType -> {
                    if (eventsByType.getKey() == EventType.PUBLISH_API || eventsByType.getKey() == EventType.START_API) {
                        return eventsByType.compose(this::processApiRegisterEvents);
                    } else if (eventsByType.getKey() == EventType.UNPUBLISH_API || eventsByType.getKey() == EventType.STOP_API) {
                        return eventsByType.compose(this::processApiUnregisterEvents);
                    } else {
                        return Flowable.empty();
                    }
                }
            );
    }

    /**
     * Process events related to api registrations in an optimized way.
     * This process is divided into following steps:
     *  - Map each event payload to api definition
     *  - fetch api plans (bulk mode, for definition v1 only).
     *  - invoke ApiManager to register each api
     *
     * @param upstream the flow of events to process.
     * @return the flow of api ids registered.
     */
    @NonNull
    private Flowable<String> processApiRegisterEvents(Flowable<Event> upstream) {
        return upstream
            .flatMapMaybe(this::toApiDefinition)
            .compose(planFetcher::fetchApiPlans)
            .compose(this::fetchKeysAndSubscriptions)
            .compose(this::registerApi);
    }

    /**
     * Process events related to api unregistrations.
     * This process is divided into following steps:
     *  - Extract the api id to unregister from event
     *  - invoke ApiManager to unregister each api
     *
     * @param upstream the flow of events to process.
     * @return the flow of api ids unregistered.
     */
    @NonNull
    private Flowable<String> processApiUnregisterEvents(Flowable<Event> upstream) {
        return upstream.flatMapMaybe(this::toApiId).compose(this::unRegisterApi);
    }

    @NonNull
    private Flowable<String> registerApi(Flowable<io.gravitee.gateway.handlers.api.definition.Api> upstream) {
        return upstream
            .parallel(PARALLELISM)
            .runOn(Schedulers.from(executor))
            .doOnNext(
                api -> {
                    try {
                        apiManager.register(api);
                    } catch (Exception e) {
                        logger.error("An error occurred when trying to synchronize api {} [{}].", api.getName(), api.getId());
                    }
                }
            )
            .sequential()
            .map(io.gravitee.definition.model.Api::getId);
    }

    @NonNull
    private Flowable<String> unRegisterApi(Flowable<String> upstream) {
        return upstream
            .parallel(PARALLELISM)
            .runOn(Schedulers.from(executor))
            .doOnNext(
                apiId -> {
                    try {
                        apiManager.unregister(apiId);
                    } catch (Exception e) {
                        logger.error("An error occurred when trying to unregister api [{}].", apiId);
                    }
                }
            )
            .sequential();
    }

    private Maybe<io.gravitee.gateway.handlers.api.definition.Api> toApiDefinition(Event apiEvent) {
        try {
            // Read API definition from event
            io.gravitee.repository.management.model.Api eventPayload = objectMapper.readValue(
                apiEvent.getPayload(),
                io.gravitee.repository.management.model.Api.class
            );

            io.gravitee.definition.model.Api eventApiDefinition = objectMapper.readValue(
                eventPayload.getDefinition(),
                io.gravitee.definition.model.Api.class
            );

            // Update definition with required information for deployment phase
            final io.gravitee.gateway.handlers.api.definition.Api apiDefinition = new io.gravitee.gateway.handlers.api.definition.Api(
                eventApiDefinition
            );
            apiDefinition.setEnabled(eventPayload.getLifecycleState() == LifecycleState.STARTED);
            apiDefinition.setDeployedAt(eventPayload.getDeployedAt());

            return Maybe.just(apiDefinition);
        } catch (Exception e) {
            // Log the error and ignore this event.
            logger.error("Unable to extract api definition from event [{}].", apiEvent.getId());
            return Maybe.empty();
        }
    }

    private Maybe<String> toApiId(Event apiEvent) {
        final String apiId = apiEvent.getProperties().get(API_ID.getValue());

        if (apiId == null) {
            logger.error("Unable to extract api info from event [{}].", apiEvent.getId());
            return Maybe.empty();
        }
        return Maybe.just(apiId);
    }

    /**
     * Allows to start fetching api keys and subscription in a bulk fashion way.
     * @param upstream the api upstream which will be chunked into packs of 50 in order to fetch api keys and subscriptions.
     * @return the same flow of apis.
     */
    @NonNull
    private Flowable<io.gravitee.gateway.handlers.api.definition.Api> fetchKeysAndSubscriptions(
        Flowable<io.gravitee.gateway.handlers.api.definition.Api> upstream
    ) {
        return upstream
            .buffer(getBulkSize())
            .doOnNext(
                apis -> {
                    apiKeysCacheService.register(apis);
                    subscriptionsCacheService.register(apis);
                }
            )
            .flatMapIterable(apis -> apis);
    }

    private int getBulkSize() {
        return bulkItems > 0 ? bulkItems : DEFAULT_BULK_SIZE;
    }
}
