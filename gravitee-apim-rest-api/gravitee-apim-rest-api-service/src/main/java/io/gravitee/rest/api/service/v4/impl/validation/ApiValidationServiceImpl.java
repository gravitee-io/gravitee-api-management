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

import static io.gravitee.rest.api.model.api.ApiLifecycleState.*;

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
import io.gravitee.rest.api.service.v4.validation.*;
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
        this.validateDefinitionVersion(updateApiEntity, existingApiEntity);
        // Validate API Type
        this.validateApiType(updateApiEntity, existingApiEntity);
        // Validate and clean lifecycle state
        updateApiEntity.setLifecycleState(this.validateAndSanitizeLifecycleState(updateApiEntity, existingApiEntity));

        // Validate and clean tags
        updateApiEntity.setTags(tagsValidationService.validateAndSanitize(executionContext, null, updateApiEntity.getTags()));
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

    private void validateDefinitionVersion(final UpdateApiEntity updateApiEntity, final ApiEntity existingApiEntity) {
        if (updateApiEntity.getDefinitionVersion() == null) {
            throw new InvalidDataException("Invalid definition version for api '" + updateApiEntity.getId() + "'");
        }
        if (updateApiEntity.getDefinitionVersion().asInteger() < existingApiEntity.getDefinitionVersion().asInteger()) {
            // not allowed to downgrade definition version
            throw new DefinitionVersionException();
        }
    }

    private void validateApiType(final UpdateApiEntity updateApiEntity, final ApiEntity existingApiEntity) {
        if (updateApiEntity.getType() == null) {
            throw new InvalidDataException("Invalid definition type for api '" + updateApiEntity.getId() + "'");
        }
        if (updateApiEntity.getType() != existingApiEntity.getType()) {
            // not allowed to change API Type
            throw new ApiTypeException();
        }
    }

    private ApiLifecycleState validateAndSanitizeLifecycleState(final UpdateApiEntity updateApiEntity, final ApiEntity existingApiEntity) {
        // if lifecycle state not provided, return the existing one
        if (updateApiEntity.getLifecycleState() == null) {
            return existingApiEntity.getLifecycleState();
        }
        // TODO FCY: because of this, you can't update a deprecated API but the reason is not clear.
        //  if we don't want a deprecated API to be updated, then we should have a specific check
        //  Otherwise, we should first check that existingAPI and updateApi have the same lifecycleState and THEN check for deprecation status of the exiting API
        if (DEPRECATED.equals(existingApiEntity.getLifecycleState())) {
            throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
        }
        if (existingApiEntity.getLifecycleState() == updateApiEntity.getLifecycleState()) {
            return existingApiEntity.getLifecycleState();
        }
        if (ARCHIVED.equals(existingApiEntity.getLifecycleState()) && !ARCHIVED.equals(updateApiEntity.getLifecycleState())) {
            throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
        } else if (UNPUBLISHED.equals(existingApiEntity.getLifecycleState()) && CREATED.equals(updateApiEntity.getLifecycleState())) {
            throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
        } else if (
            CREATED.equals(existingApiEntity.getLifecycleState()) && WorkflowState.IN_REVIEW.equals(existingApiEntity.getWorkflowState())
        ) {
            throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
        }
        return updateApiEntity.getLifecycleState();
    }
}
