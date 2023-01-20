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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.manager.ActionOnApi;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.cache.ApiKeysCacheService;
import io.gravitee.gateway.services.sync.cache.SubscriptionsCacheService;
import io.gravitee.gateway.services.sync.synchronizer.api.EventToReactableApiAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.Plan;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSynchronizer extends AbstractSynchronizer {

    private final Logger logger = LoggerFactory.getLogger(ApiSynchronizer.class);

    private final PlanRepository planRepository;

    private final ApiKeysCacheService apiKeysCacheService;

    private final SubscriptionsCacheService subscriptionsCacheService;

    private final SubscriptionService subscriptionService;

    private final ApiManager apiManager;

    private final ObjectMapper objectMapper;

    private final ExecutorService executor;

    private final EventToReactableApiAdapter eventToReactableApiAdapter;

    public ApiSynchronizer(
        EventRepository eventRepository,
        ObjectMapper objectMapper,
        ExecutorService executor,
        int bulkItems,
        PlanRepository planRepository,
        ApiKeysCacheService apiKeysCacheService,
        SubscriptionsCacheService subscriptionsCacheService,
        SubscriptionService subscriptionService,
        ApiManager apiManager,
        EventToReactableApiAdapter eventToReactableApiAdapter
    ) {
        super(eventRepository, bulkItems);
        this.planRepository = planRepository;
        this.apiKeysCacheService = apiKeysCacheService;
        this.subscriptionsCacheService = subscriptionsCacheService;
        this.subscriptionService = subscriptionService;
        this.apiManager = apiManager;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.eventToReactableApiAdapter = eventToReactableApiAdapter;
    }

    public void synchronize(Long lastRefreshAt, Long nextLastRefreshAt, List<String> environments) {
        final long start = System.currentTimeMillis();
        final Long count;

        if (lastRefreshAt == -1) {
            count = initialSynchronizeApis(nextLastRefreshAt, environments);
        } else {
            count =
                this.searchLatestEvents(
                        lastRefreshAt,
                        nextLastRefreshAt,
                        true,
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
            this.searchLatestEvents(null, nextLastRefreshAt, true, API_ID, environments, EventType.PUBLISH_API, EventType.START_API)
                .compose(this::processApiRegisterEvents)
                .count()
                .blockingGet();

        return count;
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
            .<ActionOnApi, ReactableApi<?>>groupBy(reactableApi -> apiManager.requiredActionFor(reactableApi), reactableApi -> reactableApi)
            .flatMap(groupedFlowable -> {
                if (groupedFlowable.getKey() == ActionOnApi.DEPLOY) {
                    return groupedFlowable
                        .compose(this::fetchApiPlans)
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
        return upstream
            .buffer(getBulkSize())
            .doOnNext(apis -> {
                subscriptionService.dispatchFor(apis);
            })
            .flatMapIterable(apis -> apis);
    }

    /**
     * Allows to start fetching plans in a bulk fashion way.
     * @param upstream the api upstream which will be chunked into packs of 50 in order to fetch plan v1.
     * @return he same flow of apis.
     */
    @NonNull
    private Flowable<ReactableApi<?>> fetchApiPlans(Flowable<ReactableApi<?>> upstream) {
        return upstream
            .groupBy(ReactableApi::getDefinitionVersion)
            .flatMap(apisByDefinitionVersion -> {
                if (apisByDefinitionVersion.getKey() == DefinitionVersion.V1) {
                    return apisByDefinitionVersion.buffer(getBulkSize()).flatMap(this::fetchV1ApiPlans);
                } else {
                    return apisByDefinitionVersion;
                }
            });
    }

    private Flowable<ReactableApi<?>> fetchV1ApiPlans(List<ReactableApi<?>> apiDefinitions) {
        final Map<String, io.gravitee.gateway.handlers.api.definition.Api> apisById = apiDefinitions
            .stream()
            .map(reactableApi -> (io.gravitee.gateway.handlers.api.definition.Api) reactableApi)
            .collect(Collectors.toMap(io.gravitee.gateway.handlers.api.definition.Api::getId, api -> api));

        // Get the api id to load plan only for V1 api definition.
        final List<String> apiV1Ids = new ArrayList<>(apisById.keySet());

        try {
            final Map<String, List<Plan>> plansByApi = planRepository
                .findByApis(apiV1Ids)
                .stream()
                .collect(Collectors.groupingBy(Plan::getApi));

            plansByApi.forEach((key, value) -> {
                final io.gravitee.gateway.handlers.api.definition.Api api = apisById.get(key);

                if (api.getDefinitionVersion() == DefinitionVersion.V1) {
                    // Deploy only published plan
                    api
                        .getDefinition()
                        .setPlans(
                            value
                                .stream()
                                .filter(plan ->
                                    Plan.Status.PUBLISHED.equals(plan.getStatus()) || Plan.Status.DEPRECATED.equals(plan.getStatus())
                                )
                                .map(this::convert)
                                .collect(Collectors.toList())
                        );
                }
            });
        } catch (TechnicalException te) {
            logger.error("Unexpected error while loading plans of APIs: [{}]", apiV1Ids, te);
        }

        return Flowable.fromIterable(apiDefinitions);
    }

    private io.gravitee.definition.model.Plan convert(Plan repoPlan) {
        io.gravitee.definition.model.Plan plan = new io.gravitee.definition.model.Plan();

        plan.setId(repoPlan.getId());
        plan.setName(repoPlan.getName());
        plan.setSecurityDefinition(repoPlan.getSecurityDefinition());
        plan.setSelectionRule(repoPlan.getSelectionRule());
        plan.setTags(repoPlan.getTags());
        plan.setStatus(repoPlan.getStatus().name());
        plan.setApi(repoPlan.getApi());

        if (repoPlan.getSecurity() != null) {
            plan.setSecurity(repoPlan.getSecurity().name());
        } else {
            // TODO: must be handle by a migration script
            plan.setSecurity("api_key");
        }

        try {
            if (repoPlan.getDefinition() != null && !repoPlan.getDefinition().trim().isEmpty()) {
                HashMap<String, List<Rule>> paths = objectMapper.readValue(
                    repoPlan.getDefinition(),
                    new TypeReference<HashMap<String, List<Rule>>>() {}
                );

                plan.setPaths(paths);
            }
        } catch (IOException ioe) {
            logger.error("Unexpected error while converting plan: {}", plan, ioe);
        }

        return plan;
    }
}
