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

import static io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService.oneShotIndexation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewHttpApi;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.reactivex.rxjava3.core.Single;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ashraful Hasan (ashraful.hasan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class V4ApiServiceCockpitImpl implements V4ApiServiceCockpit {

    public static final String NEW_API_ENTITY_NODE = "/newApiEntity";
    public static final String PLAN_ENTITIES_NODE = "/planEntities";
    public static final String METADATA_NODE = "/metadata";

    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final ValidateApiDomainService validateApiDomainService;
    private final CreateApiDomainService createApiDomainService;
    private final ApiService apiServiceV4;
    private final ApiStateService apiStateService;
    private final GraviteeMapper graviteeMapper;
    private final ObjectMapper mapper;

    public V4ApiServiceCockpitImpl(
        ApiPrimaryOwnerFactory apiPrimaryOwnerFactory,
        ValidateApiDomainService validateApiDomainService,
        CreateApiDomainService createApiDomainService,
        ApiService apiServiceV4,
        ApiStateService apiStateService
    ) {
        this.apiPrimaryOwnerFactory = apiPrimaryOwnerFactory;
        this.validateApiDomainService = validateApiDomainService;
        this.createApiDomainService = createApiDomainService;
        this.apiServiceV4 = apiServiceV4;
        this.apiStateService = apiStateService;
        this.graviteeMapper = new GraviteeMapper();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Single<ApiEntity> createPublishApi(
        final String organizationId,
        final String environmentId,
        final String userId,
        final String apiDefinition
    ) throws JsonProcessingException {
        final JsonNode node = mapper.readTree(apiDefinition);
        final UpdateApiEntity updateApiEntity = getUpdateApiEntity(node);
        final ExecutionContext executionContext = new ExecutionContext(organizationId, environmentId);
        var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(organizationId, environmentId, userId);

        var auditInfo = new AuditInfo(organizationId, environmentId, AuditActor.builder().userId(userId).build());
        return Single
            .just(
                createApiDomainService.create(
                    deserializeApi(node, environmentId),
                    primaryOwner,
                    auditInfo,
                    api -> validateApiDomainService.validateAndSanitizeForCreation(api, primaryOwner, environmentId, organizationId),
                    oneShotIndexation(auditInfo)
                )
            )
            .flatMap(api -> publishApi(executionContext, api, userId, updateApiEntity))
            .flatMap(apiEntity -> syncDeployment(executionContext, apiEntity.getId(), userId));
    }

    private Api deserializeApi(JsonNode node, String environmentId) throws JsonProcessingException {
        final String newApiEntityNode = mapper.writeValueAsString(node.at(NEW_API_ENTITY_NODE));
        var newApi = graviteeMapper.readValue(newApiEntityNode, NewHttpApi.class);
        return ApiModelFactory.fromNewHttpApi(newApi, environmentId);
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

    private Single<ApiEntity> publishApi(ExecutionContext executionContext, Api api, String userId, UpdateApiEntity updateApiEntity) {
        final UpdateApiEntity apiToUpdate = createUpdateApiEntity(api, updateApiEntity);

        final ApiEntity update = apiServiceV4.update(executionContext, api.getId(), apiToUpdate, userId);
        return Single.just((ApiEntity) apiStateService.start(executionContext, update.getId(), userId));
    }

    private UpdateApiEntity createUpdateApiEntity(Api api, UpdateApiEntity updateApiEntity) {
        final UpdateApiEntity entity = new UpdateApiEntity();
        entity.setId(api.getId());
        entity.setName(api.getName());
        entity.setApiVersion(api.getVersion());
        entity.setDefinitionVersion(api.getDefinitionVersion());
        entity.setType(api.getType());
        entity.setDescription(api.getDescription());
        entity.setEndpointGroups(api.getApiDefinitionHttpV4().getEndpointGroups());
        entity.setAnalytics(api.getApiDefinitionHttpV4().getAnalytics());
        entity.setFlows(api.getApiDefinitionHttpV4().getFlows());
        entity.setFlowExecution(api.getApiDefinitionHttpV4().getFlowExecution());
        entity.setResponseTemplates(api.getApiDefinitionHttpV4().getResponseTemplates());
        entity.setServices(api.getApiDefinitionHttpV4().getServices());
        entity.setGroups(api.getGroups());
        entity.setVisibility(Visibility.PUBLIC);
        entity.setPicture(api.getPicture());
        entity.setCategories(api.getCategories());
        entity.setLabels(api.getLabels());
        entity.setLifecycleState(ApiLifecycleState.PUBLISHED);
        entity.setDisableMembershipNotifications(api.isDisableMembershipNotifications());
        entity.setBackground(api.getBackground());
        //setter values received from cockpit payload
        entity.setMetadata(updateApiEntity.getMetadata());
        entity.setProperties(updateApiEntity.getProperties());
        entity.setResources(updateApiEntity.getResources());
        entity.setPlans(updateApiEntity.getPlans());
        entity.setListeners(updateApiEntity.getListeners());

        return entity;
    }
}
