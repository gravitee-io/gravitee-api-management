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
package io.gravitee.rest.api.service.converter;

import static io.gravitee.rest.api.model.WorkflowReferenceType.API;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.PropertiesEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiConverter.class);

    private ObjectMapper objectMapper;
    private PlanService planService;
    private FlowService flowService;
    private CategoryMapper categoryMapper;
    private ParameterService parameterService;
    private WorkflowService workflowService;

    public ApiConverter(
        final ObjectMapper objectMapper,
        @Lazy final PlanService planService,
        @Lazy final FlowService flowService,
        final CategoryMapper categoryMapper,
        final ParameterService parameterService,
        final WorkflowService workflowService
    ) {
        this.objectMapper = objectMapper;
        this.planService = planService;
        this.flowService = flowService;
        this.categoryMapper = categoryMapper;
        this.parameterService = parameterService;
        this.workflowService = workflowService;
    }

    public ApiEntity toApiEntity(Api api, PrimaryOwnerEntity primaryOwnerEntity) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setCrossId(api.getCrossId());
        apiEntity.setEnvironmentId(api.getEnvironmentId());
        apiEntity.setName(api.getName());
        apiEntity.setDeployedAt(api.getDeployedAt());
        apiEntity.setCreatedAt(api.getCreatedAt());
        apiEntity.setGroups(api.getGroups());
        apiEntity.setDisableMembershipNotifications(api.isDisableMembershipNotifications());
        apiEntity.setReferenceType(ReferenceContext.Type.ENVIRONMENT.name());
        apiEntity.setReferenceId(api.getEnvironmentId());
        apiEntity.setCategories(categoryMapper.toCategoryKey(api.getEnvironmentId(), api.getCategories()));
        apiEntity.setDefinitionContext(new DefinitionContext(api.getOrigin(), api.getMode(), api.getSyncFrom()));

        if (api.getDefinition() != null) {
            try {
                var apiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class);
                apiEntity.setProxy(apiDefinition.getProxy());
                apiEntity.setPaths(apiDefinition.getPaths());
                apiEntity.setServices(apiDefinition.getServices());
                apiEntity.setResources(apiDefinition.getResources());
                apiEntity.setProperties(apiDefinition.getProperties());
                apiEntity.setTags(apiDefinition.getTags());
                if (apiDefinition.getDefinitionVersion() != null) {
                    apiEntity.setGraviteeDefinitionVersion(apiDefinition.getDefinitionVersion().getLabel());
                }
                if (apiDefinition.getExecutionMode() != null) {
                    apiEntity.setExecutionMode(apiDefinition.getExecutionMode());
                }
                if (apiDefinition.getFlowMode() != null) {
                    apiEntity.setFlowMode(apiDefinition.getFlowMode());
                }
                if (apiDefinition.getFlows() != null) {
                    apiEntity.setFlows(apiDefinition.getFlows());
                }

                // Issue https://github.com/gravitee-io/issues/issues/3356
                if (
                    apiDefinition.getProxy() != null &&
                    apiDefinition.getProxy().getVirtualHosts() != null &&
                    !apiDefinition.getProxy().getVirtualHosts().isEmpty()
                ) {
                    apiEntity.setContextPath(apiDefinition.getProxy().getVirtualHosts().get(0).getPath());
                }

                if (apiDefinition.getPathMappings() != null) {
                    apiEntity.setPathMappings(new HashSet<>(apiDefinition.getPathMappings().keySet()));
                }
                apiEntity.setResponseTemplates(apiDefinition.getResponseTemplates());
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while generating API definition", ioe);
            }
        }

        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setVersion(api.getVersion());
        apiEntity.setDescription(api.getDescription());
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

        if (primaryOwnerEntity != null) {
            apiEntity.setPrimaryOwner(primaryOwnerEntity);
        }

        return apiEntity;
    }

    public ApiEntity toApiEntity(ExecutionContext executionContext, Api api, PrimaryOwnerEntity primaryOwner, boolean readDatabaseFlows) {
        ApiEntity apiEntity = toApiEntity(api, primaryOwner);
        if (apiEntity.getDefinitionContext() == null) {
            // Set context to management for backward compatibility.
            apiEntity.setDefinitionContext(
                new DefinitionContext(Api.ORIGIN_MANAGEMENT, Api.MODE_FULLY_MANAGED, Api.ORIGIN_MANAGEMENT.toUpperCase())
            );
        }

        Set<PlanEntity> plans = planService.findByApi(executionContext, api.getId());
        apiEntity.setPlans(plans);

        if (readDatabaseFlows) {
            List<Flow> flows = flowService.findByReference(FlowReferenceType.API, api.getId());
            apiEntity.setFlows(flows);
        }

        apiEntity.setCategories(categoryMapper.toCategoryKey(executionContext.getEnvironmentId(), api.getCategories()));

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

    public UpdateApiEntity toUpdateApiEntity(ApiEntity apiEntity) {
        return toUpdateApiEntity(apiEntity, false);
    }

    public UpdateApiEntity toUpdateApiEntity(ApiEntity apiEntity, boolean resetCrossId) {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setCrossId(resetCrossId ? null : apiEntity.getCrossId());
        updateApiEntity.setProxy(apiEntity.getProxy());
        updateApiEntity.setVersion(apiEntity.getVersion());
        updateApiEntity.setName(apiEntity.getName());
        updateApiEntity.setProperties(new PropertiesEntity(apiEntity.getProperties()));
        updateApiEntity.setDescription(apiEntity.getDescription());
        updateApiEntity.setGroups(apiEntity.getGroups());
        updateApiEntity.setPaths(apiEntity.getPaths());
        updateApiEntity.setPicture(apiEntity.getPicture());
        updateApiEntity.setBackground(apiEntity.getBackground());
        updateApiEntity.setResources(apiEntity.getResources());
        updateApiEntity.setTags(apiEntity.getTags());
        updateApiEntity.setServices(apiEntity.getServices());
        updateApiEntity.setVisibility(apiEntity.getVisibility());
        updateApiEntity.setLabels(apiEntity.getLabels());
        updateApiEntity.setPathMappings(apiEntity.getPathMappings());
        updateApiEntity.setLifecycleState(apiEntity.getLifecycleState());
        updateApiEntity.setPlans(apiEntity.getPlans());
        updateApiEntity.setFlows(apiEntity.getFlows());
        updateApiEntity.setGraviteeDefinitionVersion(apiEntity.getGraviteeDefinitionVersion());
        updateApiEntity.setFlowMode(apiEntity.getFlowMode());
        updateApiEntity.setResponseTemplates(apiEntity.getResponseTemplates());
        updateApiEntity.setCategories(apiEntity.getCategories());
        updateApiEntity.setDisableMembershipNotifications(apiEntity.isDisableMembershipNotifications());
        return updateApiEntity;
    }
}
