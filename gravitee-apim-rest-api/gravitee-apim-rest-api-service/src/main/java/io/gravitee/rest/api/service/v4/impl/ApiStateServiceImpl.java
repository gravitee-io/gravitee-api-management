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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.repository.management.model.Api.AuditEvent.API_UPDATED;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.event.ApiEvent;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class ApiStateServiceImpl implements ApiStateService {

    private final ApiSearchService apiSearchService;
    private final ApiRepository apiRepository;
    private final ApiMapper apiMapper;
    private final GenericApiMapper genericApiMapper;
    private final ApiNotificationService apiNotificationService;
    private final PrimaryOwnerService primaryOwnerService;
    private final AuditService auditService;
    private final EventService eventService;
    private final EventLatestRepository eventLatestRepository;
    private final ObjectMapper objectMapper;
    private final ApiMetadataService apiMetadataService;
    private final ApiValidationService apiValidationService;
    private final PlanSearchService planSearchService;
    private final ApiConverter apiConverter;
    private final SynchronizationService synchronizationService;
    private final EventManager eventManager;
    private final SearchEngineService searchEngineService;

    public ApiStateServiceImpl(
        @Lazy final ApiSearchService apiSearchService,
        @Lazy final ApiRepository apiRepository,
        final ApiMapper apiMapper,
        final GenericApiMapper genericApiMapper,
        @Lazy final ApiNotificationService apiNotificationService,
        @Lazy final PrimaryOwnerService primaryOwnerService,
        final AuditService auditService,
        @Lazy final EventService eventService,
        @Lazy final EventLatestRepository eventLatestRepository,
        final ObjectMapper objectMapper,
        @Lazy final ApiMetadataService apiMetadataService,
        @Lazy final ApiValidationService apiValidationService,
        final PlanSearchService planSearchService,
        final ApiConverter apiConverter,
        final SynchronizationService synchronizationService,
        final EventManager eventManager,
        final SearchEngineService searchEngineService
    ) {
        this.apiSearchService = apiSearchService;
        this.apiRepository = apiRepository;
        this.apiMapper = apiMapper;
        this.genericApiMapper = genericApiMapper;
        this.apiNotificationService = apiNotificationService;
        this.primaryOwnerService = primaryOwnerService;
        this.auditService = auditService;
        this.eventService = eventService;
        this.eventLatestRepository = eventLatestRepository;
        this.objectMapper = objectMapper;
        this.apiMetadataService = apiMetadataService;
        this.apiValidationService = apiValidationService;
        this.planSearchService = planSearchService;
        this.apiConverter = apiConverter;
        this.synchronizationService = synchronizationService;
        this.eventManager = eventManager;
        this.searchEngineService = searchEngineService;
    }

    @Override
    public GenericApiEntity deploy(
        ExecutionContext executionContext,
        String apiId,
        String authenticatedUser,
        ApiDeploymentEntity apiDeploymentEntity
    ) {
        Api api = apiSearchService.findRepositoryApiById(executionContext, apiId);
        return deploy(executionContext, api, api, authenticatedUser, apiDeploymentEntity);
    }

    @Override
    public GenericApiEntity deploy(
        ExecutionContext executionContext,
        Api apiToDeploy,
        String authenticatedUser,
        ApiDeploymentEntity apiDeploymentEntity
    ) {
        Api api = apiSearchService.findRepositoryApiById(executionContext, apiToDeploy.getId());
        return deploy(executionContext, api, apiToDeploy, authenticatedUser, apiDeploymentEntity);
    }

    /**
     * Deploys an api
     * @param apiFromDb is the api coming from database, on which the deployedAt date will be set
     * @param apiToDeploy is the api to deploy in an Event. It can defer from apiFromDb in case an ApiService is still running in background
     */
    private GenericApiEntity deploy(
        ExecutionContext executionContext,
        Api apiFromDb,
        Api apiToDeploy,
        String authenticatedUser,
        ApiDeploymentEntity apiDeploymentEntity
    ) {
        log.debug("Deploy API: {}", apiToDeploy.getId());

        if (!apiValidationService.canDeploy(executionContext, apiToDeploy.getId())) {
            throw new ApiNotDeployableException(
                "The api {" + apiToDeploy.getId() + "} can not be deployed without at least one published plan"
            );
        }

        // FIXME: improvement: what about updating deployedAt only when the user trigger it manually ?
        this.updateDeploymentDate(apiFromDb);
        if (apiToDeploy.getDeployedAt() == null) {
            apiToDeploy.setDeployedAt(apiFromDb.getDeployedAt());
        }
        this.deployApi(executionContext, authenticatedUser, apiDeploymentEntity, apiToDeploy);

        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext.getOrganizationId(), apiToDeploy.getId());
        final GenericApiEntity deployedApi = genericApiMapper.toGenericApi(apiToDeploy, primaryOwner);
        GenericApiEntity apiWithMetadata = apiMetadataService.fetchMetadataForApi(executionContext, deployedApi);

        apiNotificationService.triggerDeployNotification(executionContext, apiWithMetadata);

        return deployedApi;
    }

    private void updateDeploymentDate(Api api) {
        try {
            // add deployment date
            api.setUpdatedAt(new Date());
            api.setDeployedAt(api.getUpdatedAt());
            apiRepository.update(api);
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to deploy API: {}", api.getId(), e);
            throw new TechnicalManagementException("An error occurs while trying to deploy API: " + api.getId(), e);
        }
    }

    private void deployApi(ExecutionContext executionContext, String authenticatedUser, ApiDeploymentEntity apiDeploymentEntity, Api api) {
        // Clear useless field for history
        api.setPicture(null);

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.USER.getValue(), authenticatedUser);

        addDeploymentLabelToProperties(executionContext, api.getId(), properties, apiDeploymentEntity);

        // And create event
        eventService.createApiEvent(
            executionContext,
            singleton(executionContext.getEnvironmentId()),
            executionContext.getOrganizationId(),
            EventType.PUBLISH_API,
            api,
            properties
        );
    }

    private void addDeploymentLabelToProperties(
        ExecutionContext executionContext,
        String apiId,
        Map<String, String> properties,
        ApiDeploymentEntity apiDeploymentEntity
    ) {
        EventCriteria criteria = EventCriteria.builder()
            .types(
                Set.of(
                    io.gravitee.repository.management.model.EventType.PUBLISH_API,
                    io.gravitee.repository.management.model.EventType.STOP_API,
                    io.gravitee.repository.management.model.EventType.START_API,
                    io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                )
            )
            .property(Event.EventProperties.API_ID.getValue(), apiId)
            .build();

        String lastDeployNumber = eventLatestRepository
            .search(criteria, Event.EventProperties.DEPLOYMENT_NUMBER, 0L, 1L)
            .stream()
            .findFirst()
            .map(eventEntity -> eventEntity.getProperties().getOrDefault(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "0"))
            .orElse("0");

        String newDeployNumber = Long.toString(Long.parseLong(lastDeployNumber) + 1);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), newDeployNumber);

        if (apiDeploymentEntity != null && StringUtils.isNotEmpty(apiDeploymentEntity.getDeploymentLabel())) {
            properties.put(Event.EventProperties.DEPLOYMENT_LABEL.getValue(), apiDeploymentEntity.getDeploymentLabel());
        }
    }

    @Override
    public GenericApiEntity start(ExecutionContext executionContext, String apiId, String userId) {
        if (!apiValidationService.canDeploy(executionContext, apiId)) {
            throw new ApiNotDeployableException("The API {" + apiId + "} can not be started without at least one published plan");
        }

        try {
            log.debug("Start API {}", apiId);
            GenericApiEntity api = updateLifecycle(executionContext, apiId, LifecycleState.STARTED, userId);
            GenericApiEntity genericApiEntity = apiMetadataService.fetchMetadataForApi(executionContext, api);
            apiNotificationService.triggerStartNotification(executionContext, genericApiEntity);
            searchEngineService.index(executionContext, genericApiEntity, false);
            return api;
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to start API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to start API " + apiId, ex);
        }
    }

    @Override
    public boolean startV2DynamicProperties(String apiId) {
        try {
            log.debug("Start V2 Dynamic properties for API {}", apiId);
            Api apiToUpdate = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

            eventManager.publishEvent(ApiEvent.START_DYNAMIC_PROPERTY_V2, apiToUpdate);
            return true;
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("Fail to start Dynamic properties for API " + apiId, ex);
        }
    }

    @Override
    public boolean startV4DynamicProperties(String apiId) {
        try {
            log.debug("starting V4 Dynamic properties for API {}", apiId);
            Api apiToUpdate = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

            eventManager.publishEvent(ApiEvent.START_DYNAMIC_PROPERTY_V4, apiToUpdate);
            return true;
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("Fail to start Dynamic properties for API " + apiId, ex);
        }
    }

    @Override
    public GenericApiEntity stop(ExecutionContext executionContext, String apiId, String userId) {
        return stopApi(executionContext, apiId, userId, true);
    }

    @Override
    public GenericApiEntity stopWithoutNotification(ExecutionContext executionContext, String apiId, String userId) {
        return stopApi(executionContext, apiId, userId, false);
    }

    @Override
    public boolean stopV2DynamicProperties(String apiId) {
        try {
            log.debug("Stopping V2 Dynamic properties for API {}", apiId);
            Api apiToUpdate = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

            eventManager.publishEvent(ApiEvent.STOP_DYNAMIC_PROPERTY_V2, apiToUpdate);
            return true;
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("Fail to stop Dynamic properties for API " + apiId, ex);
        }
    }

    @Override
    public boolean stopV4DynamicProperties(String apiId) {
        try {
            log.debug("stopping V4 Dynamic properties for API {}", apiId);
            Api apiToUpdate = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

            eventManager.publishEvent(ApiEvent.STOP_DYNAMIC_PROPERTY_V4, apiToUpdate);
            return true;
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("Fail to stop Dynamic properties for API " + apiId, ex);
        }
    }

    private GenericApiEntity stopApi(ExecutionContext executionContext, String apiId, String userId, boolean sendNotification) {
        try {
            log.debug("Stop API {}", apiId);
            GenericApiEntity apiEntity = updateLifecycle(executionContext, apiId, LifecycleState.STOPPED, userId);
            GenericApiEntity genericApiEntity = apiMetadataService.fetchMetadataForApi(executionContext, apiEntity);
            if (sendNotification) {
                apiNotificationService.triggerStopNotification(executionContext, genericApiEntity);
            }

            searchEngineService.index(executionContext, genericApiEntity, false);

            return apiEntity;
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to stop API " + apiId, ex);
        }
    }

    private GenericApiEntity updateLifecycle(
        ExecutionContext executionContext,
        String apiId,
        LifecycleState lifecycleState,
        String username
    ) throws TechnicalException {
        Optional<Api> optApi = apiRepository.findById(apiId);
        if (optApi.isPresent()) {
            Api api = optApi.get();

            Api previousApi = new Api(api);
            api.setUpdatedAt(new Date());
            api.setLifecycleState(lifecycleState);
            Api updateApi = apiRepository.update(api);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(emptyMap())
                    .event(API_UPDATED)
                    .createdAt(api.getUpdatedAt())
                    .oldValue(previousApi)
                    .newValue(api)
                    .build(),
                apiId
            );

            EventType eventType = null;
            switch (lifecycleState) {
                case STARTED:
                    eventType = EventType.START_API;
                    break;
                case STOPPED:
                    eventType = EventType.STOP_API;
                    break;
                default:
                    break;
            }

            final GenericApiEntity deployedApi = deployLastPublishedAPI(executionContext, apiId, username, eventType);
            if (deployedApi != null) {
                return deployedApi;
            }

            return genericApiMapper.toGenericApi(
                updateApi,
                primaryOwnerService.getPrimaryOwner(executionContext.getOrganizationId(), api.getId())
            );
        } else {
            throw new ApiNotFoundException(apiId);
        }
    }

    /**
     * Allows to deploy the last published API
     * @param apiId the API id
     * @param userId the user id
     * @param eventType the event type
     * @return The persisted API or null
     * @throws TechnicalException if an exception occurs while saving the API
     */
    private GenericApiEntity deployLastPublishedAPI(ExecutionContext executionContext, String apiId, String userId, EventType eventType)
        throws TechnicalException {
        final EventQuery query = new EventQuery();
        query.setApi(apiId);
        query.setTypes(singleton(EventType.PUBLISH_API));
        if (executionContext.hasOrganizationId()) {
            query.setOrganizationIds(Set.of(executionContext.getOrganizationId()));
        }
        if (executionContext.hasEnvironmentId()) {
            query.setEnvironmentIds(Set.of(executionContext.getEnvironmentId()));
        }

        final Optional<EventEntity> optEvent = eventService
            .search(executionContext, query)
            .stream()
            .max(comparing(EventEntity::getCreatedAt));

        try {
            if (optEvent.isPresent()) {
                EventEntity event = optEvent.get();
                JsonNode node = objectMapper.readTree(event.getPayload());
                Api lastPublishedAPI = objectMapper.convertValue(node, Api.class);
                lastPublishedAPI.setLifecycleState(convert(eventType));
                lastPublishedAPI.setUpdatedAt(new Date());
                lastPublishedAPI.setDeployedAt(new Date());
                Map<String, String> properties = new HashMap<>();
                properties.put(Event.EventProperties.USER.getValue(), userId);
                properties.put(
                    Event.EventProperties.DEPLOYMENT_NUMBER.getValue(),
                    Optional.ofNullable(event.getProperties())
                        .map(p -> p.get(Event.EventProperties.DEPLOYMENT_NUMBER.getValue()))
                        .orElse("0")
                );
                // Clear useless field for history
                lastPublishedAPI.setPicture(null);

                // And create event
                eventService.createApiEvent(
                    executionContext,
                    singleton(executionContext.getEnvironmentId()),
                    executionContext.getOrganizationId(),
                    eventType,
                    lastPublishedAPI,
                    properties
                );
                return null;
            } else {
                // this is the first time we start the api without previously deployed id.
                // let's do it.
                return this.deploy(executionContext, apiId, userId, new ApiDeploymentEntity());
            }
        } catch (AbstractManagementException e) {
            log.info("Unable to deploy last published API {} due to : {}", apiId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("An error occurs while trying to deploy last published API {}", apiId, e);
            throw new TechnicalException("An error occurs while trying to deploy last published API " + apiId, e);
        }
    }

    private LifecycleState convert(EventType eventType) {
        LifecycleState lifecycleState;
        switch (eventType) {
            case START_API:
                lifecycleState = LifecycleState.STARTED;
                break;
            case STOP_API:
                lifecycleState = LifecycleState.STOPPED;
                break;
            default:
                throw new IllegalArgumentException("Unknown EventType " + eventType + " to convert EventType into Lifecycle");
        }
        return lifecycleState;
    }

    @Override
    public boolean isSynchronized(ExecutionContext executionContext, GenericApiEntity genericApiEntity) {
        try {
            // The state of the api is managed by kubernetes. There is no synchronization allowed from management.
            if (genericApiEntity.getOriginContext() instanceof OriginContext.Kubernetes) {
                return true;
            }

            // 1 - Check if the api definition is sync with last one in events
            List<Event> events = eventLatestRepository.search(
                EventCriteria.builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.STOP_API,
                            io.gravitee.repository.management.model.EventType.START_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), genericApiEntity.getId()))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            );

            if (!events.isEmpty()) {
                // According to page size, we know that we have only one element in the list
                Event lastEvent = events.get(0);
                boolean sync = false;
                if (
                    io.gravitee.repository.management.model.EventType.PUBLISH_API.equals(lastEvent.getType()) ||
                    io.gravitee.repository.management.model.EventType.START_API.equals(lastEvent.getType()) ||
                    io.gravitee.repository.management.model.EventType.STOP_API.equals(lastEvent.getType())
                ) {
                    Api payloadEntity = objectMapper.readValue(lastEvent.getPayload(), Api.class);
                    if (
                        payloadEntity != null &&
                        getOrV2(genericApiEntity::getDefinitionVersion) != getOrV2(payloadEntity::getDefinitionVersion)
                    ) {
                        return false;
                    }

                    if (
                        genericApiEntity.getDefinitionVersion() == DefinitionVersion.V2 ||
                        genericApiEntity.getDefinitionVersion() == DefinitionVersion.V1
                    ) {
                        io.gravitee.rest.api.model.api.ApiEntity apiEntity = (io.gravitee.rest.api.model.api.ApiEntity) genericApiEntity;

                        io.gravitee.rest.api.model.api.ApiEntity deployedApiEntity = apiConverter.toApiEntity(
                            executionContext,
                            payloadEntity,
                            null,
                            false
                        );

                        removePathsRuleDescriptionFromApiV1(deployedApiEntity);
                        removePathsRuleDescriptionFromApiV1(apiEntity);

                        sync = synchronizationService.checkSynchronization(
                            io.gravitee.rest.api.model.api.ApiEntity.class,
                            deployedApiEntity,
                            apiEntity
                        );
                    } else if (genericApiEntity instanceof ApiEntity httpApiEntity) {
                        ApiEntity deployedApiEntity = apiMapper.toEntity(executionContext, payloadEntity, false);

                        sync = synchronizationService.checkSynchronization(ApiEntity.class, deployedApiEntity, httpApiEntity);
                    } else if (genericApiEntity instanceof NativeApiEntity nativeApiEntity) {
                        NativeApiEntity deployedApiEntity = apiMapper.toNativeEntity(executionContext, payloadEntity, null, false);

                        sync = synchronizationService.checkSynchronization(NativeApiEntity.class, deployedApiEntity, nativeApiEntity);
                    }

                    // 2 - If API definition is synchronized, check if there is any modification for API's plans
                    // but only for published or closed plan
                    if (sync) {
                        Set<GenericPlanEntity> plans = planSearchService.findByApi(executionContext, genericApiEntity.getId());
                        sync = plans
                            .stream()
                            .noneMatch(
                                plan ->
                                    plan.getPlanStatus() != PlanStatus.STAGING &&
                                    plan.getNeedRedeployAt().after(genericApiEntity.getDeployedAt())
                            );
                    }
                }
                return sync;
            }
        } catch (Exception e) {
            String errorMsg = String.format(
                "An error occurs while trying to check API synchronization state '%s'",
                genericApiEntity.getId()
            );
            log.error(errorMsg, genericApiEntity.getId(), e);
        }

        return false;
    }

    private void removePathsRuleDescriptionFromApiV1(final io.gravitee.rest.api.model.api.ApiEntity api) {
        if (api.getPaths() != null) {
            api
                .getPaths()
                .values()
                .stream()
                .flatMap(CollectionUtils::stream)
                .forEach(rule -> rule.setDescription(""));
        }
    }

    private void removeFlowsIdsFromApiV2(io.gravitee.rest.api.model.api.ApiEntity api) {
        api.getFlows().forEach(flow -> flow.setId(null));
        api.getPlans().forEach(plan -> plan.getFlows().forEach(flow -> flow.setId(null)));
    }

    private DefinitionVersion getOrV2(Supplier<DefinitionVersion> getDefinitionVersion) {
        return getDefinitionVersion.get() != null ? getDefinitionVersion.get() : DefinitionVersion.V2;
    }
}
