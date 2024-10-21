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
package io.gravitee.rest.api.service.v4.impl.validation;

import static io.gravitee.rest.api.model.api.ApiLifecycleState.ARCHIVED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.DEPRECATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.sanitizer.HtmlSanitizer;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DefinitionVersionException;
import io.gravitee.rest.api.service.exceptions.DynamicPropertiesInvalidException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.LifecycleStateChangeNotAllowedException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.ApiServicePluginService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.exception.ApiTypeException;
import io.gravitee.rest.api.service.v4.validation.AnalyticsValidationService;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.EndpointGroupsValidationService;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import io.gravitee.rest.api.service.v4.validation.GroupValidationService;
import io.gravitee.rest.api.service.v4.validation.ListenerValidationService;
import io.gravitee.rest.api.service.v4.validation.PathParametersValidationService;
import io.gravitee.rest.api.service.v4.validation.PlanValidationService;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ApiValidationServiceImpl extends TransactionalService implements ApiValidationService {

    private final TagsValidationService tagsValidationService;
    private final GroupValidationService groupValidationService;
    private final ListenerValidationService listenerValidationService;
    private final EndpointGroupsValidationService endpointGroupsValidationService;
    private final FlowValidationService flowValidationService;
    private final ResourcesValidationService resourcesValidationService;
    private final AnalyticsValidationService analyticsValidationService;
    private final PlanSearchService planSearchService;
    private final PlanValidationService planValidationService;
    private final PathParametersValidationService pathParametersValidationService;
    private final ApiServicePluginService apiServicePluginService;

    public ApiValidationServiceImpl(
        final TagsValidationService tagsValidationService,
        final GroupValidationService groupValidationService,
        final ListenerValidationService listenerValidationService,
        final EndpointGroupsValidationService endpointGroupsValidationService,
        final FlowValidationService flowValidationService,
        final ResourcesValidationService resourcesValidationService,
        final AnalyticsValidationService loggingValidationService,
        final PlanSearchService planSearchService,
        final PlanValidationService planValidationService,
        final PathParametersValidationService pathParametersValidationService,
        ApiServicePluginService apiServicePluginService
    ) {
        this.tagsValidationService = tagsValidationService;
        this.groupValidationService = groupValidationService;
        this.listenerValidationService = listenerValidationService;
        this.endpointGroupsValidationService = endpointGroupsValidationService;
        this.flowValidationService = flowValidationService;
        this.resourcesValidationService = resourcesValidationService;
        this.analyticsValidationService = loggingValidationService;
        this.planSearchService = planSearchService;
        this.planValidationService = planValidationService;
        this.pathParametersValidationService = pathParametersValidationService;
        this.apiServicePluginService = apiServicePluginService;
    }

    @Override
    public void validateAndSanitizeNewApi(
        final ExecutionContext executionContext,
        final NewApiEntity newApiEntity,
        final PrimaryOwnerEntity primaryOwnerEntity
    ) {
        // Validate version
        this.validateDefinitionVersion(null, newApiEntity.getDefinitionVersion());
        // Validate API Type
        this.validateApiType(null, newApiEntity.getType());
        // Validate and clean tags
        newApiEntity.setTags(tagsValidationService.validateAndSanitize(executionContext, null, newApiEntity.getTags()));
        // Validate and clean groups
        newApiEntity.setGroups(
            groupValidationService.validateAndSanitize(executionContext, null, newApiEntity.getGroups(), primaryOwnerEntity)
        );
        // Validate and clean listeners
        newApiEntity.setListeners(
            listenerValidationService.validateAndSanitizeHttpV4(
                executionContext,
                null,
                newApiEntity.getListeners(),
                newApiEntity.getEndpointGroups()
            )
        );
        // Validate and clean endpoints
        newApiEntity.setEndpointGroups(
            endpointGroupsValidationService.validateAndSanitizeHttpV4(newApiEntity.getType(), newApiEntity.getEndpointGroups())
        );
        // Validate and clean logging
        newApiEntity.setAnalytics(
            analyticsValidationService.validateAndSanitize(executionContext, newApiEntity.getType(), newApiEntity.getAnalytics())
        );
        // Validate and clean flow
        newApiEntity.setFlows(flowValidationService.validateAndSanitize(newApiEntity.getType(), newApiEntity.getFlows()));

        pathParametersValidationService.validate(
            newApiEntity.getType(),
            (newApiEntity.getFlows() != null ? newApiEntity.getFlows().stream() : Stream.empty()),
            Stream.empty()
        );

        newApiEntity.setDescription(HtmlSanitizer.sanitize(newApiEntity.getDescription()));
    }

    @Override
    public void validateAndSanitizeUpdateApi(
        final ExecutionContext executionContext,
        final UpdateApiEntity updateApiEntity,
        final PrimaryOwnerEntity primaryOwnerEntity,
        final ApiEntity existingApiEntity
    ) {
        // Validate version
        this.validateDefinitionVersion(existingApiEntity.getDefinitionVersion(), updateApiEntity.getDefinitionVersion());
        // Validate API Type
        this.validateApiType(existingApiEntity.getType(), updateApiEntity.getType());
        // Validate and clean lifecycle state
        updateApiEntity.setLifecycleState(this.validateAndSanitizeLifecycleState(existingApiEntity, updateApiEntity));
        // Validate and clean tags
        updateApiEntity.setTags(
            tagsValidationService.validateAndSanitize(executionContext, existingApiEntity.getTags(), updateApiEntity.getTags())
        );
        // Validate and clean groups
        updateApiEntity.setGroups(
            groupValidationService.validateAndSanitize(
                executionContext,
                updateApiEntity.getId(),
                updateApiEntity.getGroups(),
                primaryOwnerEntity
            )
        );
        // Validate and clean listeners
        updateApiEntity.setListeners(
            listenerValidationService.validateAndSanitizeHttpV4(
                executionContext,
                updateApiEntity.getId(),
                updateApiEntity.getListeners(),
                updateApiEntity.getEndpointGroups()
            )
        );
        // Validate and clean endpoints
        updateApiEntity.setEndpointGroups(
            endpointGroupsValidationService.validateAndSanitizeHttpV4(updateApiEntity.getType(), updateApiEntity.getEndpointGroups())
        );
        // Validate and clean logging
        updateApiEntity.setAnalytics(
            analyticsValidationService.validateAndSanitize(executionContext, updateApiEntity.getType(), updateApiEntity.getAnalytics())
        );
        // Validate and clean flow
        updateApiEntity.setFlows(flowValidationService.validateAndSanitize(updateApiEntity.getType(), updateApiEntity.getFlows()));

        // Validate and clean plans
        updateApiEntity.setPlans(planValidationService.validateAndSanitize(updateApiEntity.getType(), updateApiEntity.getPlans()));

        // Validate path parameters
        pathParametersValidationService.validate(
            updateApiEntity.getType(),
            (updateApiEntity.getFlows() != null ? updateApiEntity.getFlows().stream() : Stream.empty()),
            getPlansFlows(updateApiEntity.getPlans())
        );

        // Validate and clean resources
        updateApiEntity.setResources(validateAndSanitize(updateApiEntity.getResources()));

        this.validateDynamicProperties(updateApiEntity.getServices() != null ? updateApiEntity.getServices().getDynamicProperty() : null);
    }

    @Override
    public void validateAndSanitizeImportApiForCreation(
        final ExecutionContext executionContext,
        final ApiEntity apiEntity,
        final PrimaryOwnerEntity primaryOwnerEntity
    ) {
        // Validate version
        this.validateDefinitionVersion(null, apiEntity.getDefinitionVersion());
        // Validate API Type
        this.validateApiType(null, apiEntity.getType());
        // Validate and clean lifecycle state. In creation, lifecycle state can't be set.
        apiEntity.setLifecycleState(null);
        // Validate and clean tags
        apiEntity.setTags(tagsValidationService.validateAndSanitize(executionContext, null, apiEntity.getTags()));
        // Validate and clean groups
        apiEntity.setGroups(groupValidationService.validateAndSanitize(executionContext, null, apiEntity.getGroups(), primaryOwnerEntity));
        // Validate and clean listeners
        apiEntity.setListeners(
            listenerValidationService.validateAndSanitizeHttpV4(
                executionContext,
                null,
                apiEntity.getListeners(),
                apiEntity.getEndpointGroups()
            )
        );
        // Validate and clean endpoints
        apiEntity.setEndpointGroups(
            endpointGroupsValidationService.validateAndSanitizeHttpV4(apiEntity.getType(), apiEntity.getEndpointGroups())
        );
        // Validate and clean logging
        apiEntity.setAnalytics(
            analyticsValidationService.validateAndSanitize(executionContext, apiEntity.getType(), apiEntity.getAnalytics())
        );
        // Validate and clean flow
        apiEntity.setFlows(flowValidationService.validateAndSanitize(apiEntity.getType(), apiEntity.getFlows()));

        // Validate and clean plans
        apiEntity.setPlans(planValidationService.validateAndSanitize(apiEntity.getType(), apiEntity.getPlans()));

        // Validate path parameters
        pathParametersValidationService.validate(
            apiEntity.getType(),
            (apiEntity.getFlows() != null ? apiEntity.getFlows().stream() : Stream.empty()),
            getPlansFlows(apiEntity.getPlans())
        );

        // Validate and clean resources
        apiEntity.setResources(validateAndSanitize(apiEntity.getResources()));

        // Sanitize Description
        apiEntity.setDescription(HtmlSanitizer.sanitize(apiEntity.getDescription()));

        this.validateDynamicProperties(apiEntity.getServices() != null ? apiEntity.getServices().getDynamicProperty() : null);
    }

    @Override
    public boolean canDeploy(ExecutionContext executionContext, String apiId) {
        return planSearchService
            .findByApi(executionContext, apiId)
            .stream()
            .anyMatch(planEntity ->
                PlanStatus.PUBLISHED.equals(planEntity.getPlanStatus()) || PlanStatus.DEPRECATED.equals(planEntity.getPlanStatus())
            );
    }

    public void validateDynamicProperties(Service dynamicProperties) {
        if (dynamicProperties == null) {
            return;
        }
        if (isBlank(dynamicProperties.getType())) {
            log.debug("Dynamic properties requires a type");
            throw new DynamicPropertiesInvalidException(dynamicProperties.getType());
        }

        dynamicProperties.setConfiguration(
            this.apiServicePluginService.validateApiServiceConfiguration(dynamicProperties.getType(), dynamicProperties.getConfiguration())
        );
    }

    public List<Resource> validateAndSanitize(List<Resource> resources) {
        return resourcesValidationService.validateAndSanitize(resources);
    }

    private Stream<Flow> getPlansFlows(Set<PlanEntity> plans) {
        if (plans == null) {
            return Stream.empty();
        }
        return plans.stream().flatMap(plan -> plan.getFlows() == null ? Stream.empty() : plan.getFlows().stream());
    }

    private void validateDefinitionVersion(final DefinitionVersion oldDefinitionVersion, final DefinitionVersion newDefinitionVersion) {
        if (newDefinitionVersion != DefinitionVersion.V4) {
            throw new InvalidDataException("Definition version is unsupported, should be V4 or higher");
        }
        if (oldDefinitionVersion != null && oldDefinitionVersion.asInteger() > newDefinitionVersion.asInteger()) {
            // not allowed downgrading definition version
            throw new DefinitionVersionException();
        }
    }

    private void validateApiType(final ApiType oldApiType, final ApiType newApiType) {
        if (newApiType == null) {
            throw new InvalidDataException("ApiType cannot be null.");
        }
        if (oldApiType != null && oldApiType != newApiType) {
            // not allowed changing API Type
            throw new ApiTypeException();
        }
    }

    private ApiLifecycleState validateAndSanitizeLifecycleState(final ApiEntity existingApiEntity, final UpdateApiEntity updateApiEntity) {
        // if lifecycle state not provided, return the existing one
        if (updateApiEntity.getLifecycleState() == null) {
            return existingApiEntity.getLifecycleState();
        } else if (DEPRECATED == existingApiEntity.getLifecycleState()) { //  Otherwise, we should first check that existingAPI and updateApi have the same lifecycleState and THEN check for deprecation status of the exiting API //  if we don't want a deprecated API to be updated, then we should have a specific check // TODO FCY: because of this, you can't update a deprecated API but the reason is not clear.
            throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
        } else if (existingApiEntity.getLifecycleState() == updateApiEntity.getLifecycleState()) {
            return existingApiEntity.getLifecycleState();
        } else if (
            (ARCHIVED == existingApiEntity.getLifecycleState() && (ARCHIVED != updateApiEntity.getLifecycleState())) ||
            ((UNPUBLISHED == existingApiEntity.getLifecycleState()) && (CREATED == updateApiEntity.getLifecycleState())) ||
            (CREATED == existingApiEntity.getLifecycleState()) &&
            (WorkflowState.IN_REVIEW == existingApiEntity.getWorkflowState())
        ) {
            throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
        }
        return updateApiEntity.getLifecycleState();
    }
}
