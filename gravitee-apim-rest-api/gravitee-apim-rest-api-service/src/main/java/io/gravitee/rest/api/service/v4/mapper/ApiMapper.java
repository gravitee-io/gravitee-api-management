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
package io.gravitee.rest.api.service.v4.mapper;

import static io.gravitee.rest.api.model.WorkflowReferenceType.API;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("ApiMapperV4")
@Slf4j
public class ApiMapper {

    private final ObjectMapper objectMapper;
    private final PlanService planService;
    private final FlowService flowService;
    private final CategoryService categoryService;
    private final ParameterService parameterService;
    private final WorkflowService workflowService;

    public ApiMapper(
        final ObjectMapper objectMapper,
        final PlanService planService,
        final FlowService flowService,
        final CategoryService categoryService,
        final ParameterService parameterService,
        final WorkflowService workflowService
    ) {
        this.objectMapper = objectMapper;
        this.planService = planService;
        this.flowService = flowService;
        this.categoryService = categoryService;
        this.parameterService = parameterService;
        this.workflowService = workflowService;
    }

    public ApiEntity toEntity(final Api api, final PrimaryOwnerEntity primaryOwner) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setCrossId(api.getCrossId());
        apiEntity.setName(api.getName());
        apiEntity.setApiVersion(api.getVersion());
        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setDeployedAt(api.getDeployedAt());
        apiEntity.setCreatedAt(api.getCreatedAt());
        apiEntity.setDescription(api.getDescription());

        if (api.getDefinition() != null) {
            try {
                var apiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.Api.class);
                apiEntity.setDefinitionVersion(apiDefinition.getDefinitionVersion());
                apiEntity.setListeners(apiDefinition.getListeners());
                apiEntity.setEndpointGroups(apiDefinition.getEndpointGroups());
                apiEntity.setServices(apiDefinition.getServices());
                apiEntity.setResources(apiDefinition.getResources());
                apiEntity.setProperties(apiDefinition.getProperties());
                apiEntity.setTags(apiDefinition.getTags());

                apiEntity.setFlowMode(apiDefinition.getFlowMode());
                apiEntity.setFlows(apiDefinition.getFlows());

                apiEntity.setResponseTemplates(apiDefinition.getResponseTemplates());
            } catch (IOException ioe) {
                log.error("Unexpected error while generating API definition", ioe);
            }
        }

        if (api.getType() != null) {
            apiEntity.setType(ApiType.fromLabel(api.getType().getLabel()));
        }
        apiEntity.setGroups(api.getGroups());
        apiEntity.setDisableMembershipNotifications(api.isDisableMembershipNotifications());
        apiEntity.setReferenceType(GraviteeContext.ReferenceContextType.ENVIRONMENT.name());
        apiEntity.setReferenceId(api.getEnvironmentId());
        apiEntity.setCategories(api.getCategories());
        apiEntity.setPicture(api.getPicture());
        apiEntity.setBackground(api.getBackground());
        apiEntity.setLabels(api.getLabels());

        final LifecycleState state = api.getLifecycleState();
        if (state != null) {
            apiEntity.setState(Lifecycle.State.valueOf(state.name()));
        }
        if (api.getVisibility() != null) {
            apiEntity.setVisibility(io.gravitee.rest.api.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        final ApiLifecycleState lifecycleState = api.getApiLifecycleState();
        if (lifecycleState != null) {
            apiEntity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.valueOf(lifecycleState.name()));
        }

        apiEntity.setPrimaryOwner(primaryOwner);
        return apiEntity;
    }

    public ApiEntity toEntity(
        final ExecutionContext executionContext,
        final Api api,
        final PrimaryOwnerEntity primaryOwner,
        List<CategoryEntity> categories,
        final boolean readDatabaseFlows
    ) {
        ApiEntity apiEntity = toEntity(api, primaryOwner);

        Set<PlanEntity> plans = planService.findByApi(executionContext, api.getId());
        apiEntity.setPlans(plans);

        if (readDatabaseFlows) {
            List<Flow> flows = flowService.findByReference(FlowReferenceType.API, api.getId());
            apiEntity.setFlows(flows);
        }

        // TODO: extract calls to external service from convert method
        final Set<String> apiCategories = api.getCategories();
        if (apiCategories != null) {
            if (categories == null) {
                categories = categoryService.findAll(executionContext.getEnvironmentId());
            }
            final Set<String> newApiCategories = new HashSet<>(apiCategories.size());
            for (final String apiView : apiCategories) {
                final Optional<CategoryEntity> optionalView = categories.stream().filter(c -> apiView.equals(c.getId())).findAny();
                optionalView.ifPresent(category -> newApiCategories.add(category.getKey()));
            }
            apiEntity.setCategories(newApiCategories);
        }

        if (
            parameterService.findAsBoolean(
                executionContext,
                Key.API_REVIEW_ENABLED,
                api.getEnvironmentId(),
                ParameterReferenceType.ENVIRONMENT
            )
        ) {
            final List<Workflow> workflows = workflowService.findByReferenceAndType(API, api.getId(), REVIEW);
            if (workflows != null && !workflows.isEmpty()) {
                apiEntity.setWorkflowState(WorkflowState.valueOf(workflows.get(0).getState()));
            }
        }

        return apiEntity;
    }

    public Api toRepository(final ExecutionContext executionContext, final NewApiEntity newApiEntity) {
        Api repoApi = new Api();
        String generatedApiId = UuidString.generateRandom();
        repoApi.setId(generatedApiId);
        repoApi.setEnvironmentId(executionContext.getEnvironmentId());
        // Set date fields
        repoApi.setCreatedAt(new Date());
        repoApi.setUpdatedAt(repoApi.getCreatedAt());
        repoApi.setApiLifecycleState(ApiLifecycleState.CREATED);
        // Be sure that lifecycle is set to STOPPED by default and visibility is private
        repoApi.setLifecycleState(LifecycleState.STOPPED);
        repoApi.setVisibility(Visibility.PRIVATE);

        repoApi.setName(newApiEntity.getName().trim());
        repoApi.setVersion(newApiEntity.getApiVersion().trim());
        repoApi.setDescription(newApiEntity.getDescription().trim());

        repoApi.setDefinitionVersion(newApiEntity.getDefinitionVersion());
        repoApi.setDefinition(toApiDefinition(generatedApiId, newApiEntity));
        repoApi.setType(newApiEntity.getType());
        repoApi.setGroups(newApiEntity.getGroups());
        return repoApi;
    }

    private String toApiDefinition(final String apiId, final NewApiEntity newApiEntity) {
        try {
            io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();

            apiDefinition.setId(apiId);
            apiDefinition.setName(newApiEntity.getName());
            apiDefinition.setType(newApiEntity.getType());
            apiDefinition.setDefinitionVersion(newApiEntity.getDefinitionVersion());
            apiDefinition.setApiVersion(newApiEntity.getApiVersion());
            apiDefinition.setTags(newApiEntity.getTags());
            apiDefinition.setListeners(newApiEntity.getListeners());
            apiDefinition.setEndpointGroups(newApiEntity.getEndpointGroups());
            apiDefinition.setFlowMode(newApiEntity.getFlowMode());
            apiDefinition.setFlows(newApiEntity.getFlows());

            return objectMapper.writeValueAsString(apiDefinition);
        } catch (JsonProcessingException jse) {
            log.error("Unexpected error while generating API definition", jse);
            throw new TechnicalManagementException("An error occurs while trying to parse API definition " + jse);
        }
    }

    public Api toRepository(final ExecutionContext executionContext, final UpdateApiEntity updateApiEntity) {
        Api repoApi = new Api();
        String apiId = updateApiEntity.getId();
        repoApi.setId(apiId.trim());
        repoApi.setCrossId(updateApiEntity.getCrossId());
        repoApi.setEnvironmentId(executionContext.getEnvironmentId());
        repoApi.setType(updateApiEntity.getType());
        repoApi.setUpdatedAt(new Date());
        if (updateApiEntity.getLifecycleState() != null) {
            repoApi.setApiLifecycleState(ApiLifecycleState.valueOf(updateApiEntity.getLifecycleState().name()));
        }
        if (updateApiEntity.getVisibility() != null) {
            repoApi.setVisibility(Visibility.valueOf(updateApiEntity.getVisibility().toString()));
        }

        repoApi.setName(updateApiEntity.getName().trim());
        repoApi.setVersion(updateApiEntity.getApiVersion().trim());
        repoApi.setDescription(updateApiEntity.getDescription().trim());
        repoApi.setPicture(updateApiEntity.getPicture());
        repoApi.setBackground(updateApiEntity.getBackground());

        repoApi.setDefinitionVersion(updateApiEntity.getDefinitionVersion());
        repoApi.setDefinition(toApiDefinition(updateApiEntity));

        final Set<String> apiCategories = updateApiEntity.getCategories();
        if (apiCategories != null) {
            final List<CategoryEntity> categories = categoryService.findAll(executionContext.getEnvironmentId());
            final Set<String> newApiCategories = new HashSet<>(apiCategories.size());
            for (final String apiCategory : apiCategories) {
                final Optional<CategoryEntity> optionalCategory = categories
                    .stream()
                    .filter(c -> apiCategory.equals(c.getKey()) || apiCategory.equals(c.getId()))
                    .findAny();
                optionalCategory.ifPresent(category -> newApiCategories.add(category.getId()));
            }
            repoApi.setCategories(newApiCategories);
        }

        if (updateApiEntity.getLabels() != null) {
            repoApi.setLabels(new ArrayList<>(new HashSet<>(updateApiEntity.getLabels())));
        }

        repoApi.setGroups(updateApiEntity.getGroups());
        repoApi.setDisableMembershipNotifications(updateApiEntity.isDisableMembershipNotifications());

        return repoApi;
    }

    private String toApiDefinition(final UpdateApiEntity updateApiEntity) {
        try {
            io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();

            apiDefinition.setId(updateApiEntity.getId());
            apiDefinition.setName(updateApiEntity.getName());
            apiDefinition.setDefinitionVersion(updateApiEntity.getDefinitionVersion());
            apiDefinition.setType(updateApiEntity.getType());
            apiDefinition.setApiVersion(updateApiEntity.getApiVersion());
            apiDefinition.setTags(updateApiEntity.getTags());
            apiDefinition.setListeners(updateApiEntity.getListeners());
            apiDefinition.setEndpointGroups(updateApiEntity.getEndpointGroups());
            apiDefinition.setProperties(
                updateApiEntity
                    .getProperties()
                    .stream()
                    .map(propertyEntity -> new Property(propertyEntity.getKey(), propertyEntity.getValue()))
                    .collect(Collectors.toList())
            );
            apiDefinition.setResources(updateApiEntity.getResources());
            apiDefinition.setFlowMode(updateApiEntity.getFlowMode());
            apiDefinition.setFlows(updateApiEntity.getFlows());

            return objectMapper.writeValueAsString(apiDefinition);
        } catch (JsonProcessingException jse) {
            log.error("Unexpected error while generating API definition", jse);
            throw new TechnicalManagementException("An error occurs while trying to parse API definition " + jse);
        }
    }
}
