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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.ActionOnApi;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.cache.ApiKeysCacheService;
import io.gravitee.gateway.services.sync.cache.SubscriptionsCacheService;
import io.gravitee.gateway.services.sync.synchronizer.api.EventToReactableApiAdapter;
import io.gravitee.gateway.services.sync.synchronizer.api.PlanFetcher;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSynchronizer extends AbstractSynchronizer {

    private static final int WAIT_TASK_COMPLETION_DELAY = 100;
    private final Logger logger = LoggerFactory.getLogger(ApiSynchronizer.class);

    private final ApiKeysCacheService apiKeysCacheService;

    private final SubscriptionsCacheService subscriptionsCacheService;

    private final SubscriptionService subscriptionService;

    private final ApiManager apiManager;

    private final GatewayConfiguration gatewayConfiguration;

    private final ThreadPoolExecutor executor;

    private final EventToReactableApiAdapter eventToReactableApiAdapter;
    private final PlanFetcher planFetcher;

    public ApiSynchronizer(
        EventLatestRepository eventLatestRepository,
        ThreadPoolExecutor executor,
        int bulkItems,
        ApiKeysCacheService apiKeysCacheService,
        SubscriptionsCacheService subscriptionsCacheService,
        SubscriptionService subscriptionService,
        ApiManager apiManager,
        EventToReactableApiAdapter eventToReactableApiAdapter,
        PlanFetcher planFetcher,
        GatewayConfiguration gatewayConfiguration
    ) {
        super(eventLatestRepository, bulkItems);
        this.apiKeysCacheService = apiKeysCacheService;
        this.subscriptionsCacheService = subscriptionsCacheService;
        this.subscriptionService = subscriptionService;
        this.apiManager = apiManager;
        this.executor = executor;
        this.eventToReactableApiAdapter = eventToReactableApiAdapter;
        this.planFetcher = planFetcher;
        this.gatewayConfiguration = gatewayConfiguration;
    }

    public void synchronize(Long lastRefreshAt, Long nextLastRefreshAt, List<String> environments) {
        final long start = System.currentTimeMillis();
        final Long count;

        if (lastRefreshAt == -1) {
            count = initialSynchronizeApis(nextLastRefreshAt, environments);
            waitForAllTasksCompletion();
            logger.info("{} apis synchronized in {}ms", count, (System.currentTimeMillis() - start));
        } else {
            count =
                this.searchLatestEvents(
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
            logger.debug("{} apis synchronized in {}ms", count, (System.currentTimeMillis() - start));
        }
    }

    private void waitForAllTasksCompletion() {
        // This is the very first sync process. Need to wait for all background task to finish before continuing (api keys, subscriptions, ...).
        if (executor.getActiveCount() > 0) {
            logger.info("There are still sync tasks running in background. Waiting for them to finish before continuing...");
        }

        while (executor.getActiveCount() > 0 || !executor.getQueue().isEmpty()) {
            try {
                Thread.sleep(WAIT_TASK_COMPLETION_DELAY);
            } catch (InterruptedException e) {
                logger.warn("An error occurred waiting for first api sync process to finish", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Run the initial synchronization which focus on api PUBLISH and START events only.
     */
    private long initialSynchronizeApis(long nextLastRefreshAt, List<String> environments) {
        return this.searchLatestEvents(null, nextLastRefreshAt, API_ID, environments, EventType.PUBLISH_API, EventType.START_API)
            .compose(this::processApiRegisterEvents)
            .count()
            .blockingGet();
    }

    @NonNull
    public Flowable<String> processApiEvents(Flowable<Event> upstream) {
        return upstream
            .groupBy(Event::getType)
            .flatMap(eventsByType -> {
                if (eventsByType.getKey() == EventType.PUBLISH_API || eventsByType.getKey() == EventType.START_API) {
                    return eventsByType.compose(this::processApiRegisterEvents);
                } else if (eventsByType.getKey() == EventType.UNPUBLISH_API || eventsByType.getKey() == EventType.STOP_API) {
                    return eventsByType.compose(this::processApiUnregisterEvents);
                } else {
                    return Flowable.empty();
                }
            });
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
            .flatMapMaybe(eventToReactableApiAdapter::toReactableApi)
            .<ActionOnApi, ReactableApi<?>>groupBy(apiManager::requiredActionFor, reactableApi -> reactableApi)
            .flatMap(groupedFlowable -> {
                if (groupedFlowable.getKey() == ActionOnApi.DEPLOY) {
                    return groupedFlowable
                        .compose(g -> planFetcher.fetchApiPlans(g, getBulkSize()))
                        .compose(this::filterByTags)
                        .compose(this::fetchKeysAndSubscriptions)
                        .compose(this::registerApi)
                        .compose(this::dispatchSubscriptionsForApis);
                } else if (groupedFlowable.getKey() == ActionOnApi.UNDEPLOY) {
                    return groupedFlowable.map(ReactableApi::getId).compose(this::unRegisterApi);
                } else {
                    return groupedFlowable.map(ReactableApi::getId);
                }
            });
    }

    /**
     * Process events related to api un-registrations.
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
    private Flowable<String> registerApi(Flowable<ReactableApi<?>> upstream) {
        return upstream
            .parallel(PARALLELISM)
            .runOn(Schedulers.from(executor))
            .doOnNext(api -> {
                try {
                    apiManager.register(api);
                } catch (Exception e) {
                    logger.error("An error occurred when trying to synchronize api {} [{}].", api.getName(), api.getId(), e);
                }
            })
            .sequential()
            .map(ReactableApi::getId);
    }

    @NonNull
    private Flowable<String> unRegisterApi(Flowable<String> upstream) {
        return upstream
            .parallel(PARALLELISM)
            .runOn(Schedulers.from(executor))
            .doOnNext(apiId -> {
                try {
                    apiManager.unregister(apiId);
                } catch (Exception e) {
                    logger.error("An error occurred when trying to unregister api [{}].", apiId, e);
                }
            })
            .sequential();
    }

    private Maybe<String> toApiId(Event apiEvent) {
        final String apiId = apiEvent.getProperties().get(API_ID.getValue());

        if (apiId == null) {
            logger.error("Unable to extract api info from event [{}].", apiEvent.getId());
            return Maybe.empty();
        }
        return Maybe.just(apiId);
    }

    @NonNull
    private Flowable<ReactableApi<?>> filterByTags(Flowable<ReactableApi<?>> upstream) {
        return upstream
            .filter(api -> gatewayConfiguration.hasMatchingTags(api.getTags()))
            .map(api -> {
                if (api.getDefinitionVersion() != DefinitionVersion.V4) {
                    var plans = ((io.gravitee.definition.model.Api) api.getDefinition()).getPlans();
                    if (plans != null) {
                        ((io.gravitee.definition.model.Api) api.getDefinition()).setPlans(
                                plans
                                    .stream()
                                    .filter(p -> p.getStatus() != null)
                                    .filter(p -> filterPlanStatus(p.getStatus()))
                                    .filter(p -> filterShardingTag(p.getName(), api.getName(), p.getTags()))
                                    .collect(Collectors.toList())
                            );
                    }
                    return api;
                }

                var plans = ((Api) api.getDefinition()).getPlans();
                if (plans != null) {
                    ((Api) api.getDefinition()).setPlans(
                            plans
                                .stream()
                                .filter(p -> p.getStatus() != null)
                                .filter(p -> filterPlanStatus(p.getStatus().getLabel()))
                                .filter(p -> filterShardingTag(p.getName(), api.getName(), p.getTags()))
                                .collect(Collectors.toList())
                        );
                }
                return api;
            })
            .filter(api -> {
                if (api.getDefinition() instanceof Api) {
                    return !((Api) api.getDefinition()).getPlans().isEmpty();
                } else if (api.getDefinition() instanceof io.gravitee.definition.model.Api) {
                    return !((io.gravitee.definition.model.Api) api.getDefinition()).getPlans().isEmpty();
                }
                return false;
            });
    }

    private boolean filterPlanStatus(final String planStatus) {
        return (
            PlanStatus.PUBLISHED.getLabel().equalsIgnoreCase(planStatus) || PlanStatus.DEPRECATED.getLabel().equalsIgnoreCase(planStatus)
        );
    }

    protected boolean filterShardingTag(final String planName, final String apiName, final Set<String> tags) {
        if (tags != null && !tags.isEmpty()) {
            boolean hasMatchingTags = gatewayConfiguration.hasMatchingTags(tags);
            if (!hasMatchingTags) {
                logger.debug("Plan name[{}] api[{}] has been ignored because not in configured sharding tags", planName, apiName);
            }
            return hasMatchingTags;
        }
        return true;
    }

    /**
     * Allows to start fetching api keys and subscription in a bulk fashion way.
     * @param upstream the api upstream which will be chunked into packs of 50 in order to fetch api keys and subscriptions.
     * @return the same flow of apis.
     */
    @NonNull
    private Flowable<ReactableApi<?>> fetchKeysAndSubscriptions(Flowable<ReactableApi<?>> upstream) {
        return upstream
            .buffer(getBulkSize())
            .doOnNext(apis -> {
                apiKeysCacheService.register(apis);
                subscriptionsCacheService.register(apis);
            })
            .flatMapIterable(apis -> apis);
    }

    /**
     * Dispatch all the subscriptions belonging to the apis
     * @param upstream the apis id upstream which will be used to dispatch subscriptions of type {@link io.gravitee.gateway.api.service.Subscription.Type#SUBSCRIPTION}
     * @return the same flow of apis id.
     */
    @NonNull
    private Flowable<String> dispatchSubscriptionsForApis(Flowable<String> upstream) {
        return upstream.buffer(getBulkSize()).doOnNext(subscriptionService::dispatchFor).flatMapIterable(apis -> apis);
    }
}
