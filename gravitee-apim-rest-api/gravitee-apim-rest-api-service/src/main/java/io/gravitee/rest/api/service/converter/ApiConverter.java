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
package io.gravitee.rest.api.service.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.PropertiesEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiConverter.class);

    @Autowired
    private ObjectMapper objectMapper;

    public ApiEntity toApiEntity(Api api, PrimaryOwnerEntity primaryOwnerEntity) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setCrossId(api.getCrossId());
        apiEntity.setName(api.getName());
        apiEntity.setDeployedAt(api.getDeployedAt());
        apiEntity.setCreatedAt(api.getCreatedAt());
        apiEntity.setGroups(api.getGroups());
        apiEntity.setDisableMembershipNotifications(api.isDisableMembershipNotifications());
        apiEntity.setReferenceType(GraviteeContext.ReferenceContextType.ENVIRONMENT.name());
        apiEntity.setReferenceId(api.getEnvironmentId());
        apiEntity.setCategories(api.getCategories());
        apiEntity.setDefinitionContext(new DefinitionContext(api.getOrigin(), api.getMode()));

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
                if (apiDefinition.getProxy().getVirtualHosts() != null && !apiDefinition.getProxy().getVirtualHosts().isEmpty()) {
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
        updateApiEntity.setDefinitionContext(apiEntity.getDefinitionContext());
        return updateApiEntity;
    }
}
