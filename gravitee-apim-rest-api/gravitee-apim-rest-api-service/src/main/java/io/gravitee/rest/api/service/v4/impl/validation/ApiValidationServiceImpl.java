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
package io.gravitee.rest.api.service.v4.impl.validation;

import static io.gravitee.rest.api.model.api.ApiLifecycleState.ARCHIVED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.DEPRECATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DefinitionVersionException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.LifecycleStateChangeNotAllowedException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.exception.ApiTypeException;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.EndpointGroupsValidationService;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import io.gravitee.rest.api.service.v4.validation.GroupValidationService;
import io.gravitee.rest.api.service.v4.validation.ListenerValidationService;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiValidationServiceImpl extends TransactionalService implements ApiValidationService {

    private final TagsValidationService tagsValidationService;
    private final GroupValidationService groupValidationService;
    private final ListenerValidationService listenerValidationService;
    private final EndpointGroupsValidationService endpointGroupsValidationService;
    private final FlowValidationService flowValidationService;
    private final ResourcesValidationService resourcesValidationService;

    public ApiValidationServiceImpl(
        final TagsValidationService tagsValidationService,
        final GroupValidationService groupValidationService,
        final ListenerValidationService listenerValidationService,
        final EndpointGroupsValidationService endpointGroupsValidationService,
        final FlowValidationService flowValidationService,
        final ResourcesValidationService resourcesValidationService
    ) {
        this.tagsValidationService = tagsValidationService;
        this.groupValidationService = groupValidationService;
        this.listenerValidationService = listenerValidationService;
        this.endpointGroupsValidationService = endpointGroupsValidationService;
        this.flowValidationService = flowValidationService;
        this.resourcesValidationService = resourcesValidationService;
    }

    @Override
    public void validateAndSanitizeNewApi(
        final ExecutionContext executionContext,
        final NewApiEntity newApiEntity,
        final PrimaryOwnerEntity currentPrimaryOwnerEntity
    ) {
        // Validate version
        this.validateDefinitionVersion(null, newApiEntity.getDefinitionVersion());
        // Validate API Type
        this.validateApiType(null, newApiEntity.getType());
        // Validate and clean tags
        newApiEntity.setTags(tagsValidationService.validateAndSanitize(executionContext, null, newApiEntity.getTags()));
        // Validate and clean groups
        newApiEntity.setGroups(
            groupValidationService.validateAndSanitize(executionContext, null, newApiEntity.getGroups(), currentPrimaryOwnerEntity)
        );
        // Validate and clean listeners
        newApiEntity.setListeners(listenerValidationService.validateAndSanitize(executionContext, null, newApiEntity.getListeners()));
        // Validate and clean endpoints
        newApiEntity.setEndpointGroups(endpointGroupsValidationService.validateAndSanitize(newApiEntity.getEndpointGroups()));
        // Validate and clean flow
        newApiEntity.setFlows(flowValidationService.validateAndSanitize(newApiEntity.getFlows()));
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
            listenerValidationService.validateAndSanitize(executionContext, updateApiEntity.getId(), updateApiEntity.getListeners())
        );
        // Validate and clean endpoints
        updateApiEntity.setEndpointGroups(endpointGroupsValidationService.validateAndSanitize(updateApiEntity.getEndpointGroups()));
        // Validate and clean flow
        updateApiEntity.setFlows(flowValidationService.validateAndSanitize(updateApiEntity.getFlows()));
        // Validate and clean resources
        updateApiEntity.setResources(resourcesValidationService.validateAndSanitize(updateApiEntity.getResources()));
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
