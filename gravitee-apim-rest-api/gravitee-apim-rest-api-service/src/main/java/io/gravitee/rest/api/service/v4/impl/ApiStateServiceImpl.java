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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.repository.management.model.Api.AuditEvent.API_UPDATED;
import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApiNotManagedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    private final ObjectMapper objectMapper;
    private final ApiMetadataService apiMetadataService;

    public ApiStateServiceImpl(
        @Lazy final ApiSearchService apiSearchService,
        @Lazy final ApiRepository apiRepository,
        final ApiMapper apiMapper,
        final GenericApiMapper genericApiMapper,
        @Lazy final ApiNotificationService apiNotificationService,
        @Lazy final PrimaryOwnerService primaryOwnerService,
        final AuditService auditService,
        @Lazy final EventService eventService,
        final ObjectMapper objectMapper,
        @Lazy final ApiMetadataService apiMetadataService
    ) {
        this.apiSearchService = apiSearchService;
        this.apiRepository = apiRepository;
        this.apiMapper = apiMapper;
        this.genericApiMapper = genericApiMapper;
        this.apiNotificationService = apiNotificationService;
        this.primaryOwnerService = primaryOwnerService;
        this.auditService = auditService;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.apiMetadataService = apiMetadataService;
    }

    @Override
    public GenericApiEntity deploy(
        ExecutionContext executionContext,
        String apiId,
        String authenticatedUser,
        ApiDeploymentEntity apiDeploymentEntity
    ) {
        log.debug("Deploy API: {}", apiId);

        Api api = apiSearchService.findRepositoryApiById(executionContext, apiId);

        if (DefinitionContext.isKubernetes(api.getOrigin())) {
            throw new ApiNotManagedException("The api is managed externally (" + api.getOrigin() + "). Unable to deploy it.");
        }

        this.deployApi(executionContext, authenticatedUser, apiDeploymentEntity, api);

        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext, api.getId());
        final GenericApiEntity deployedApi = genericApiMapper.toGenericApi(api, primaryOwner);
        GenericApiEntity apiWithMetadata = apiMetadataService.fetchMetadataForApi(executionContext, deployedApi);

        apiNotificationService.triggerDeployNotification(executionContext, apiWithMetadata);

        return deployedApi;
    }

    private void deployApi(ExecutionContext executionContext, String authenticatedUser, ApiDeploymentEntity apiDeploymentEntity, Api api) {
        try {
            // add deployment date
            api.setUpdatedAt(new Date());
            api.setDeployedAt(api.getUpdatedAt());
            api = apiRepository.update(api);

            // Clear useless field for history
            api.setPicture(null);

            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.USER.getValue(), authenticatedUser);

            addDeploymentLabelToProperties(executionContext, api.getId(), properties, apiDeploymentEntity);

            // And create event
            eventService.createApiEvent(executionContext, singleton(executionContext.getEnvironmentId()), PUBLISH_API, api, properties);
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to deploy API: {}", api.getId(), e);
            throw new TechnicalManagementException("An error occurs while trying to deploy API: " + api.getId(), e);
        }
    }

    private void addDeploymentLabelToProperties(
        ExecutionContext executionContext,
        String apiId,
        Map<String, String> properties,
        ApiDeploymentEntity apiDeploymentEntity
    ) {
        final EventQuery query = new EventQuery();
        query.setApi(apiId);
        query.setTypes(singleton(PUBLISH_API));

        final Optional<EventEntity> optEvent = eventService
            .search(executionContext, query)
            .stream()
            .max(comparing(EventEntity::getCreatedAt));

        String lastDeployNumber = optEvent.isPresent()
            ? optEvent.get().getProperties().getOrDefault(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "0")
            : "0";
        String newDeployNumber = Long.toString(Long.parseLong(lastDeployNumber) + 1);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), newDeployNumber);

        if (apiDeploymentEntity != null && StringUtils.isNotEmpty(apiDeploymentEntity.getDeploymentLabel())) {
            properties.put(Event.EventProperties.DEPLOYMENT_LABEL.getValue(), apiDeploymentEntity.getDeploymentLabel());
        }
    }

    @Override
    public ApiEntity start(ExecutionContext executionContext, String apiId, String userId) {
        try {
            log.debug("Start API {}", apiId);
            ApiEntity apiEntity = updateLifecycle(executionContext, apiId, LifecycleState.STARTED, userId);
            GenericApiEntity genericApiEntity = apiMetadataService.fetchMetadataForApi(executionContext, apiEntity);
            apiNotificationService.triggerStartNotification(executionContext, genericApiEntity);
            return apiEntity;
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to start API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to start API " + apiId, ex);
        }
    }

    @Override
    public ApiEntity stop(ExecutionContext executionContext, String apiId, String userId) {
        try {
            log.debug("Stop API {}", apiId);
            ApiEntity apiEntity = updateLifecycle(executionContext, apiId, LifecycleState.STOPPED, userId);
            GenericApiEntity genericApiEntity = apiMetadataService.fetchMetadataForApi(executionContext, apiEntity);
            apiNotificationService.triggerStopNotification(executionContext, genericApiEntity);
            return apiEntity;
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to stop API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to stop API " + apiId, ex);
        }
    }

    private ApiEntity updateLifecycle(ExecutionContext executionContext, String apiId, LifecycleState lifecycleState, String username)
        throws TechnicalException {
        Optional<Api> optApi = apiRepository.findById(apiId);
        if (optApi.isPresent()) {
            Api api = optApi.get();

            Api previousApi = new Api(api);
            api.setUpdatedAt(new Date());
            api.setLifecycleState(lifecycleState);
            Api updateApi = apiRepository.update(api);
            ApiEntity apiEntity = apiMapper.toEntity(updateApi, primaryOwnerService.getPrimaryOwner(executionContext, api.getId()));
            // Audit
            auditService.createApiAuditLog(
                executionContext,
                apiId,
                Collections.emptyMap(),
                API_UPDATED,
                api.getUpdatedAt(),
                previousApi,
                api
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

            if (!DefinitionContext.isKubernetes(updateApi.getOrigin())) {
                final ApiEntity deployedApi = deployLastPublishedAPI(executionContext, apiId, username, eventType);
                if (deployedApi != null) {
                    return deployedApi;
                }
            }
            return apiEntity;
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
    private ApiEntity deployLastPublishedAPI(ExecutionContext executionContext, String apiId, String userId, EventType eventType)
        throws TechnicalException {
        final EventQuery query = new EventQuery();
        query.setApi(apiId);
        query.setTypes(singleton(PUBLISH_API));

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

                // Clear useless field for history
                lastPublishedAPI.setPicture(null);

                // And create event
                eventService.createApiEvent(
                    executionContext,
                    singleton(executionContext.getEnvironmentId()),
                    eventType,
                    lastPublishedAPI,
                    properties
                );
                return null;
            } else {
                // this is the first time we start the api without previously deployed id.
                // let's do it.
                return (ApiEntity) this.deploy(executionContext, apiId, userId, new ApiDeploymentEntity());
            }
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
}
