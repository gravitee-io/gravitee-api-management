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
package io.gravitee.rest.api.service.cockpit.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanMode;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.reactivex.rxjava3.core.Single;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Ashraful Hasan (ashraful.hasan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class V4ApiServiceCockpitImpl implements V4ApiServiceCockpit {

    public static final String KEYLESS = "Keyless";
    public static final String KEYLESS_TYPE = "key-less";
    public static final String NEW_API_ENTITY_NODE = "/newApiEntity";
    public static final String PLAN_ENTITIES_NODE = "/planEntities";
    public static final String METADATA_NODE = "/metadata";

    private final ApiService apiServiceV4;
    private final PlanService planServiceV4;
    private final ApiStateService apiStateService;
    private final GraviteeMapper graviteeMapper;
    private final ObjectMapper mapper;

    public V4ApiServiceCockpitImpl (ApiService apiServiceV4, PlanService planServiceV4, ApiStateService apiStateService) {
        this.apiServiceV4 = apiServiceV4;
        this.planServiceV4 = planServiceV4;
        this.apiStateService = apiStateService;
        this.graviteeMapper = new GraviteeMapper();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Single<ApiEntity> createPublishApi(String userId, String apiDefinition) throws JsonProcessingException {
        final JsonNode node = mapper.readTree(apiDefinition);
        final NewApiEntity newApiEntity = getNewApiEntity(node);
        final UpdateApiEntity updateApiEntity = getUpdateApiEntity(node);
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        return Single
            .just(apiServiceV4.create(executionContext, newApiEntity, userId))
            .flatMap(newApi -> publishPlan(executionContext, newApi.getId(), updateApiEntity))
            .flatMap(planEntity -> publishApi(executionContext, planEntity.getApiId(), userId, updateApiEntity))
            .flatMap(apiEntity -> syncDeployment(executionContext, apiEntity.getId(), userId));
    }

    private NewApiEntity getNewApiEntity(JsonNode node) throws JsonProcessingException {
        final String newApiEntityNode = mapper.writeValueAsString(node.at(NEW_API_ENTITY_NODE));
        return graviteeMapper.readValue(newApiEntityNode, NewApiEntity.class);
    }

    private UpdateApiEntity getUpdateApiEntity(JsonNode node) throws JsonProcessingException {
        final String newApiEntityNode = mapper.writeValueAsString(node.at(NEW_API_ENTITY_NODE));
        final String planEntitiesNode = mapper.writeValueAsString(node.at(PLAN_ENTITIES_NODE));
        final String metaDataNode = mapper.writeValueAsString(node.at(METADATA_NODE));

        final UpdateApiEntity updateApiEntity = graviteeMapper.readValue(newApiEntityNode, UpdateApiEntity.class);
        final PlanEntity[] planEntities = graviteeMapper.readValue(planEntitiesNode, PlanEntity[].class);
        final ApiMetadataEntity[] apiMetadataEntities = graviteeMapper.readValue(metaDataNode, ApiMetadataEntity[].class);

        updateApiEntity.setPlans(new HashSet<>(Arrays.asList(planEntities)));
        updateApiEntity.setMetadata(List.of(apiMetadataEntities));

        return updateApiEntity;
    }

    private Single<ApiEntity> syncDeployment(ExecutionContext executionContext, String apiId, String userId) {
        final ApiDeploymentEntity deploymentEntity = new ApiDeploymentEntity();

        return Single.just((ApiEntity) apiStateService.deploy(executionContext, apiId, userId, deploymentEntity));
    }

    private Single<PlanEntity> publishPlan(ExecutionContext executionContext, String apiId, UpdateApiEntity updateApiEntity) {
        final NewPlanEntity newPlanEntity = Optional
            .ofNullable(updateApiEntity)
            .map(mappedUpdateApiEntity -> {
                final Set<PlanEntity> plans = mappedUpdateApiEntity.getPlans();
                final PlanEntity plan = plans.iterator().next();
                plans.remove(plan);
                mappedUpdateApiEntity.setPlans(plans);
                return createNewPlan(apiId, plan);
            })
            .orElseGet(() -> createKeylessPlan(apiId));

        return Single
            .just(planServiceV4.create(executionContext, newPlanEntity))
            .map(planEntity -> planServiceV4.publish(executionContext, planEntity.getId()));
    }

    private Single<ApiEntity> publishApi(ExecutionContext executionContext, String apiId, String userId, UpdateApiEntity updateApiEntity) {
        final ApiEntity apiEntity = (ApiEntity) apiStateService.start(executionContext, apiId, userId);
        final UpdateApiEntity apiToUpdate = createUpdateApiEntity(apiEntity, updateApiEntity);

        return Single.just(apiServiceV4.update(executionContext, apiEntity.getId(), apiToUpdate, userId));
    }

    private NewPlanEntity createNewPlan(String apiId, PlanEntity planEntity) {
        final NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setApiId(apiId);
        newPlanEntity.setStatus(PlanStatus.STAGING);
        newPlanEntity.setType(planEntity.getType());
        newPlanEntity.setName(planEntity.getName());
        newPlanEntity.setDescription(planEntity.getDescription());
        newPlanEntity.setValidation(planEntity.getValidation());
        newPlanEntity.setSecurity(planEntity.getSecurity());
        newPlanEntity.setMode(planEntity.getMode());
        newPlanEntity.setCrossId(planEntity.getCrossId());
        newPlanEntity.setCharacteristics(planEntity.getCharacteristics());
        newPlanEntity.setExcludedGroups(planEntity.getExcludedGroups());
        newPlanEntity.setTags(planEntity.getTags());
        newPlanEntity.setGeneralConditions(planEntity.getGeneralConditions());
        newPlanEntity.setSelectionRule(planEntity.getSelectionRule());

        return newPlanEntity;
    }

    private NewPlanEntity createKeylessPlan(String apiId) {
        final NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setApiId(apiId);
        newPlanEntity.setType(PlanType.API);
        newPlanEntity.setName(KEYLESS);
        newPlanEntity.setDescription(KEYLESS);
        newPlanEntity.setValidation(PlanValidationType.MANUAL);
        PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType(KEYLESS_TYPE);
        newPlanEntity.setSecurity(planSecurity);
        newPlanEntity.setMode(PlanMode.STANDARD);
        newPlanEntity.setStatus(PlanStatus.STAGING);

        return newPlanEntity;
    }

    private UpdateApiEntity createUpdateApiEntity(ApiEntity apiEntity, UpdateApiEntity updateApiEntity) {
        final UpdateApiEntity entity = new UpdateApiEntity();
        entity.setId(apiEntity.getId());
        entity.setName(apiEntity.getName());
        entity.setApiVersion(apiEntity.getApiVersion());
        entity.setDefinitionVersion(apiEntity.getDefinitionVersion());
        entity.setType(apiEntity.getType());
        entity.setDescription(apiEntity.getDescription());
        entity.setEndpointGroups(apiEntity.getEndpointGroups());
        entity.setAnalytics(apiEntity.getAnalytics());
        entity.setFlows(apiEntity.getFlows());
        entity.setFlowExecution(apiEntity.getFlowExecution());
        entity.setResponseTemplates(apiEntity.getResponseTemplates());
        entity.setServices(apiEntity.getServices());
        entity.setGroups(apiEntity.getGroups());
        entity.setVisibility(apiEntity.getVisibility());
        entity.setPicture(apiEntity.getPicture());
        entity.setPictureUrl(apiEntity.getPictureUrl());
        entity.setCategories(apiEntity.getCategories());
        entity.setLabels(apiEntity.getLabels());
        entity.setLifecycleState(ApiLifecycleState.PUBLISHED);
        entity.setDisableMembershipNotifications(apiEntity.isDisableMembershipNotifications());
        entity.setBackground(apiEntity.getBackground());
        entity.setBackgroundUrl(apiEntity.getBackgroundUrl());
        //setter values received from cockpit payload
        entity.setMetadata(updateApiEntity.getMetadata());
        entity.setProperties(updateApiEntity.getProperties());
        entity.setResources(updateApiEntity.getResources());
        entity.setPlans(updateApiEntity.getPlans());
        entity.setListeners(updateApiEntity.getListeners());

        return entity;
    }
}
