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
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.reactivex.rxjava3.core.Single;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author Ashraful Hasan (ashraful.hasan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class V4ApiServiceCockpitImpl implements V4ApiServiceCockpit {

    public static final String NEW_API_ENTITY_NODE = "/newApiEntity";
    public static final String PLAN_ENTITIES_NODE = "/planEntities";
    public static final String METADATA_NODE = "/metadata";

    private final ApiService apiServiceV4;
    private final ApiStateService apiStateService;
    private final GraviteeMapper graviteeMapper;
    private final ObjectMapper mapper;

    public V4ApiServiceCockpitImpl(ApiService apiServiceV4, ApiStateService apiStateService) {
        this.apiServiceV4 = apiServiceV4;
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
                .flatMap(apiEntity -> publishApi(executionContext, apiEntity, userId, updateApiEntity))
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

    private Single<ApiEntity> publishApi(ExecutionContext executionContext, ApiEntity apiEntity, String userId, UpdateApiEntity updateApiEntity) {
        final UpdateApiEntity apiToUpdate = createUpdateApiEntity(apiEntity, updateApiEntity);

        final ApiEntity update = apiServiceV4.update(executionContext, apiEntity.getId(), apiToUpdate, userId);
        return Single.just((ApiEntity) apiStateService.start(executionContext, update.getId(), userId));
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
        entity.setVisibility(Visibility.PUBLIC);
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
