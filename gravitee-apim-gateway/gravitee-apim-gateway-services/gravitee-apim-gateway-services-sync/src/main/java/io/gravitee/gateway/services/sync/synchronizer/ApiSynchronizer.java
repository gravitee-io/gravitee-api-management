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
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.cache.ApiKeysCacheService;
import io.gravitee.gateway.services.sync.cache.SubscriptionsCacheService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.*;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    private PlanRepository planRepository;

    @Autowired
    private ApiKeysCacheService apiKeysCacheService;

    @Autowired
    private SubscriptionsCacheService subscriptionsCacheService;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private final Map<String, Environment> environmentMap = new ConcurrentHashMap<>();
    private final Map<String, io.gravitee.repository.management.model.Organization> organizationMap = new ConcurrentHashMap<>();

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
            .compose(this::fetchApiPlans)
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
                        logger.error("An error occurred when trying to synchronize api {} [{}].", api.getName(), api.getId(), e);
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
                        logger.error("An error occurred when trying to unregister api [{}].", apiId, e);
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

            enhanceWithOrgAndEnv(eventPayload.getEnvironmentId(), apiDefinition);

            return Maybe.just(apiDefinition);
        } catch (Exception e) {
            // Log the error and ignore this event.
            logger.error("Unable to extract api definition from event [{}].", apiEvent.getId(), e);
            return Maybe.empty();
        }
    }

    private void enhanceWithOrgAndEnv(String environmentId, io.gravitee.gateway.handlers.api.definition.Api definition) {
        Environment apiEnv = null;

        if (environmentId != null) {
            apiEnv =
                environmentMap.computeIfAbsent(
                    environmentId,
                    envId -> {
                        try {
                            final Environment environment = environmentRepository.findById(envId).get();

                            organizationMap.computeIfAbsent(
                                environment.getOrganizationId(),
                                orgId -> {
                                    try {
                                        return organizationRepository.findById(orgId).get();
                                    } catch (Exception e) {
                                        return null;
                                    }
                                }
                            );

                            return environment;
                        } catch (Exception e) {
                            logger.warn("An error occurred fetching the environment {} and its organization.", envId, e);
                            return null;
                        }
                    }
                );
        }

        if (apiEnv != null) {
            definition.setEnvironmentId(apiEnv.getId());
            definition.setEnvironmentHrid(apiEnv.getHrids() != null ? apiEnv.getHrids().stream().findFirst().orElse(null) : null);

            final io.gravitee.repository.management.model.Organization apiOrg = organizationMap.get(apiEnv.getOrganizationId());

            if (apiOrg != null) {
                definition.setOrganizationId(apiOrg.getId());
                definition.setOrganizationHrid(apiOrg.getHrids() != null ? apiOrg.getHrids().stream().findFirst().orElse(null) : null);
            }
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

    /**
     * Allows to start fetching plans in a bulk fashion way.
     * @param upstream the api upstream which will be chunked into packs of 50 in order to fetch plan v1.
     * @return he same flow of apis.
     */
    @NonNull
    private Flowable<io.gravitee.gateway.handlers.api.definition.Api> fetchApiPlans(
        Flowable<io.gravitee.gateway.handlers.api.definition.Api> upstream
    ) {
        return upstream
            .groupBy(io.gravitee.definition.model.Api::getDefinitionVersion)
            .flatMap(
                apisByDefinitionVersion -> {
                    if (apisByDefinitionVersion.getKey() == DefinitionVersion.V1) {
                        return apisByDefinitionVersion.buffer(getBulkSize()).flatMap(this::fetchV1ApiPlans);
                    } else {
                        return apisByDefinitionVersion.flatMapSingle(this::fetchV2ApiPlans);
                    }
                }
            );
    }

    private Flowable<io.gravitee.gateway.handlers.api.definition.Api> fetchV1ApiPlans(
        List<io.gravitee.gateway.handlers.api.definition.Api> apiDefinitions
    ) {
        final Map<String, io.gravitee.gateway.handlers.api.definition.Api> apisById = apiDefinitions
            .stream()
            .collect(Collectors.toMap(io.gravitee.definition.model.Api::getId, api -> api));

        // Get the api id to load plan only for V1 api definition.
        final List<String> apiV1Ids = new ArrayList<>(apisById.keySet());

        try {
            final Map<String, List<Plan>> plansByApi = planRepository
                .findByApis(apiV1Ids)
                .stream()
                .collect(Collectors.groupingBy(Plan::getApi));

            plansByApi.forEach(
                (key, value) -> {
                    final io.gravitee.gateway.handlers.api.definition.Api definition = apisById.get(key);

                    if (definition.getDefinitionVersion() == DefinitionVersion.V1) {
                        // Deploy only published plan
                        definition.setPlans(
                            value
                                .stream()
                                .filter(
                                    plan ->
                                        Plan.Status.PUBLISHED.equals(plan.getStatus()) || Plan.Status.DEPRECATED.equals(plan.getStatus())
                                )
                                .map(this::convert)
                                .collect(Collectors.toList())
                        );
                    }
                }
            );
        } catch (TechnicalException te) {
            logger.error("Unexpected error while loading plans of APIs: [{}]", apiV1Ids, te);
        }

        return Flowable.fromIterable(apiDefinitions);
    }

    private Single<io.gravitee.gateway.handlers.api.definition.Api> fetchV2ApiPlans(
        io.gravitee.gateway.handlers.api.definition.Api apiDefinition
    ) {
        apiDefinition.setPlans(
            apiDefinition
                .getPlans()
                .stream()
                .filter(plan -> "published".equalsIgnoreCase(plan.getStatus()) || "deprecated".equalsIgnoreCase(plan.getStatus()))
                .collect(Collectors.toList())
        );

        return Single.just(apiDefinition);
    }

    private io.gravitee.definition.model.Plan convert(Plan repoPlan) {
        io.gravitee.definition.model.Plan plan = new io.gravitee.definition.model.Plan();

        plan.setId(repoPlan.getId());
        plan.setName(repoPlan.getName());
        plan.setSecurityDefinition(repoPlan.getSecurityDefinition());
        plan.setSelectionRule(repoPlan.getSelectionRule());
        plan.setTags(repoPlan.getTags());

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
