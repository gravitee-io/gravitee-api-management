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
package io.gravitee.rest.api.service.v4.mapper;

import static io.gravitee.rest.api.model.WorkflowReferenceType.API;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.ApiAdapterDecorator;
import io.gravitee.apim.infra.adapter.PrimaryOwnerAdapter;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
    private final ParameterService parameterService;
    private final WorkflowService workflowService;
    private final CategoryMapper categoryMapper;

    public ApiMapper(
        final ObjectMapper objectMapper,
        @Lazy final PlanService planService,
        @Lazy final FlowService flowService,
        final ParameterService parameterService,
        final WorkflowService workflowService,
        final CategoryMapper categoryMapper
    ) {
        this.objectMapper = objectMapper;
        this.planService = planService;
        this.flowService = flowService;
        this.parameterService = parameterService;
        this.workflowService = workflowService;
        this.categoryMapper = categoryMapper;
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
                apiEntity.setAnalytics(apiDefinition.getAnalytics());
                apiEntity.setFailover(apiDefinition.getFailover());
                apiEntity.setListeners(apiDefinition.getListeners());
                apiEntity.setEndpointGroups(apiDefinition.getEndpointGroups());
                apiEntity.setServices(apiDefinition.getServices());
                apiEntity.setResources(apiDefinition.getResources());
                apiEntity.setProperties(apiDefinition.getProperties());
                apiEntity.setTags(apiDefinition.getTags());

                apiEntity.setFlowExecution(apiDefinition.getFlowExecution());
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
        apiEntity.setReferenceType(ReferenceContext.Type.ENVIRONMENT.name());
        apiEntity.setReferenceId(api.getEnvironmentId());
        apiEntity.setCategories(categoryMapper.toCategoryKey(api.getEnvironmentId(), api.getCategories()));
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

        apiEntity.setOriginContext(ApiAdapterDecorator.toOriginContext(api));

        apiEntity.setPrimaryOwner(primaryOwner);
        return apiEntity;
    }

    public NativeApiEntity toNativeEntity(final Api api, final PrimaryOwnerEntity primaryOwner) {
        NativeApiEntity apiEntity = new NativeApiEntity();

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
                var apiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.nativeapi.NativeApi.class);
                apiEntity.setDefinitionVersion(apiDefinition.getDefinitionVersion());
                apiEntity.setListeners(apiDefinition.getListeners());
                apiEntity.setEndpointGroups(apiDefinition.getEndpointGroups());
                apiEntity.setServices(apiDefinition.getServices());
                apiEntity.setResources(apiDefinition.getResources());
                apiEntity.setProperties(apiDefinition.getProperties());
                apiEntity.setTags(apiDefinition.getTags());
                apiEntity.setFlows(apiDefinition.getFlows());
            } catch (IOException ioe) {
                log.error("Unexpected error while generating API definition", ioe);
            }
        }

        if (api.getType() != null) {
            apiEntity.setType(ApiType.fromLabel(api.getType().getLabel()));
        }
        apiEntity.setGroups(api.getGroups());
        apiEntity.setDisableMembershipNotifications(api.isDisableMembershipNotifications());
        apiEntity.setReferenceType(ReferenceContext.Type.ENVIRONMENT.name());
        apiEntity.setReferenceId(api.getEnvironmentId());
        apiEntity.setCategories(categoryMapper.toCategoryKey(api.getEnvironmentId(), api.getCategories()));
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

        apiEntity.setOriginContext(ApiAdapterDecorator.toOriginContext(api));

        apiEntity.setPrimaryOwner(primaryOwner);
        return apiEntity;
    }

    public FederatedApiEntity federatedToEntity(final Api api, final PrimaryOwnerEntity primaryOwner) {
        api.setCategories(categoryMapper.toCategoryKey(api.getEnvironmentId(), api.getCategories()));
        return ApiAdapter.INSTANCE.toFederatedApiEntity(api, PrimaryOwnerAdapter.INSTANCE.fromRestEntity(primaryOwner));
    }

    public ApiEntity toEntity(
        final ExecutionContext executionContext,
        final Api api,
        final PrimaryOwnerEntity primaryOwner,
        final boolean readDatabaseFlows
    ) {
        ApiEntity apiEntity = toEntity(api, primaryOwner);

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

    public NativeApiEntity toNativeEntity(
        final ExecutionContext executionContext,
        final Api api,
        final PrimaryOwnerEntity primaryOwner,
        final boolean readDatabaseFlows
    ) {
        NativeApiEntity apiEntity = toNativeEntity(api, primaryOwner);

        Set<NativePlanEntity> plans = planService.findNativePlansByApi(executionContext, api.getId());
        apiEntity.setPlans(plans);

        if (readDatabaseFlows) {
            List<NativeFlow> flows = flowService.findNativeFlowByReference(FlowReferenceType.API, api.getId());
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

    public FederatedApiEntity federatedToEntity(
        final ExecutionContext executionContext,
        final Api api,
        final PrimaryOwnerEntity primaryOwner
    ) {
        api.setCategories(categoryMapper.toCategoryKey(executionContext.getEnvironmentId(), api.getCategories()));
        return ApiAdapter.INSTANCE.toFederatedApiEntity(api, PrimaryOwnerAdapter.INSTANCE.fromRestEntity(primaryOwner));
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
        String description = newApiEntity.getDescription();
        if (description != null) {
            repoApi.setDescription(description.trim());
        }
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
            apiDefinition.setAnalytics(newApiEntity.getAnalytics());
            apiDefinition.setFailover(newApiEntity.getFailover());
            apiDefinition.setFlowExecution(newApiEntity.getFlowExecution());
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
        String description = updateApiEntity.getDescription();
        if (description != null) {
            repoApi.setDescription(description.trim());
        }
        repoApi.setPicture(updateApiEntity.getPicture());
        repoApi.setBackground(updateApiEntity.getBackground());

        repoApi.setDefinitionVersion(updateApiEntity.getDefinitionVersion());
        repoApi.setDefinition(toApiDefinition(updateApiEntity));

        repoApi.setCategories(categoryMapper.toCategoryId(executionContext.getEnvironmentId(), updateApiEntity.getCategories()));

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
            apiDefinition.setType(updateApiEntity.getType());
            apiDefinition.setApiVersion(updateApiEntity.getApiVersion());
            apiDefinition.setDefinitionVersion(updateApiEntity.getDefinitionVersion());
            apiDefinition.setTags(updateApiEntity.getTags());
            apiDefinition.setListeners(updateApiEntity.getListeners());
            apiDefinition.setEndpointGroups(updateApiEntity.getEndpointGroups());
            apiDefinition.setAnalytics(updateApiEntity.getAnalytics());
            apiDefinition.setFailover(updateApiEntity.getFailover());
            if (updateApiEntity.getProperties() != null) {
                apiDefinition.setProperties(
                    updateApiEntity
                        .getProperties()
                        .stream()
                        .map(propertyEntity ->
                            new Property(
                                propertyEntity.getKey(),
                                propertyEntity.getValue(),
                                propertyEntity.isEncrypted(),
                                propertyEntity.isDynamic()
                            )
                        )
                        .collect(Collectors.toList())
                );
            }
            apiDefinition.setResources(updateApiEntity.getResources());
            apiDefinition.setFlowExecution(updateApiEntity.getFlowExecution());
            apiDefinition.setFlows(updateApiEntity.getFlows());
            apiDefinition.setResponseTemplates(updateApiEntity.getResponseTemplates());
            apiDefinition.setServices(updateApiEntity.getServices());

            return objectMapper.writeValueAsString(apiDefinition);
        } catch (JsonProcessingException jse) {
            log.error("Unexpected error while generating API definition", jse);
            throw new TechnicalManagementException("An error occurs while trying to parse API definition " + jse);
        }
    }

    public Api toRepository(final ExecutionContext executionContext, final ApiEntity apiEntity) {
        Api repoApi = new Api();
        if (apiEntity.getLifecycleState() != null) {
            repoApi.setApiLifecycleState(ApiLifecycleState.valueOf(apiEntity.getLifecycleState().name()));
        }
        repoApi.setBackground(apiEntity.getBackground());
        repoApi.setCategories(categoryMapper.toCategoryId(executionContext.getEnvironmentId(), apiEntity.getCategories()));
        repoApi.setCrossId(apiEntity.getCrossId());
        repoApi.setCreatedAt(apiEntity.getCreatedAt());
        repoApi.setDefinition(toApiDefinition(apiEntity));
        repoApi.setDefinitionVersion(apiEntity.getDefinitionVersion());
        repoApi.setDeployedAt(apiEntity.getDeployedAt());
        repoApi.setDescription(apiEntity.getDescription());
        repoApi.setDisableMembershipNotifications(apiEntity.isDisableMembershipNotifications());
        repoApi.setEnvironmentId(executionContext.getEnvironmentId());
        repoApi.setGroups(apiEntity.getGroups());
        repoApi.setId(apiEntity.getId());
        if (apiEntity.getLabels() != null) {
            repoApi.setLabels(new ArrayList<>(new HashSet<>(apiEntity.getLabels())));
        }
        if (apiEntity.getState() != null) {
            repoApi.setLifecycleState(LifecycleState.valueOf(apiEntity.getState().name()));
        }

        repoApi.setOrigin(apiEntity.getOriginContext().name());
        if (apiEntity.getOriginContext() instanceof OriginContext.Kubernetes kube) {
            repoApi.setMode(kube.mode().name().toLowerCase());
        } else if (apiEntity.getOriginContext() instanceof OriginContext.Integration integration) {
            repoApi.setIntegrationId(integration.integrationId());
        }

        repoApi.setName(apiEntity.getName());
        repoApi.setPicture(apiEntity.getPicture());
        repoApi.setType(apiEntity.getType());
        repoApi.setUpdatedAt(apiEntity.getUpdatedAt());
        if (apiEntity.getVisibility() != null) {
            repoApi.setVisibility(Visibility.valueOf(apiEntity.getVisibility().toString()));
        }
        repoApi.setVersion(apiEntity.getApiVersion());
        return repoApi;
    }

    private String toApiDefinition(final ApiEntity apiEntity) {
        try {
            io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();

            apiDefinition.setId(apiEntity.getId());
            apiDefinition.setName(apiEntity.getName());
            apiDefinition.setType(apiEntity.getType());
            apiDefinition.setApiVersion(apiEntity.getApiVersion());
            apiDefinition.setDefinitionVersion(apiEntity.getDefinitionVersion());
            apiDefinition.setTags(apiEntity.getTags());
            apiDefinition.setListeners(apiEntity.getListeners());
            apiDefinition.setEndpointGroups(apiEntity.getEndpointGroups());
            apiDefinition.setAnalytics(apiEntity.getAnalytics());
            apiDefinition.setFailover(apiEntity.getFailover());
            if (apiEntity.getProperties() != null) {
                apiDefinition.setProperties(
                    apiEntity
                        .getProperties()
                        .stream()
                        .map(propertyEntity -> new Property(propertyEntity.getKey(), propertyEntity.getValue()))
                        .collect(Collectors.toList())
                );
            }
            apiDefinition.setResources(apiEntity.getResources());
            apiDefinition.setFlowExecution(apiEntity.getFlowExecution());
            apiDefinition.setFlows(apiEntity.getFlows());
            apiDefinition.setResponseTemplates(apiEntity.getResponseTemplates());
            apiDefinition.setServices(apiEntity.getServices());

            return objectMapper.writeValueAsString(apiDefinition);
        } catch (JsonProcessingException jse) {
            log.error("Unexpected error while generating API definition", jse);
            throw new TechnicalManagementException("An error occurs while trying to parse API definition " + jse);
        }
    }
}
