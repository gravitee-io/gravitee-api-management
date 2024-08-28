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
package io.gravitee.rest.api.service.impl;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiCRDEntity;
import io.gravitee.rest.api.model.api.ApiCRDStatusEntity;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ApiCRDService;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class ApiCDRServiceImpl extends AbstractService implements ApiCRDService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiCDRServiceImpl.class);

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private ApiService apiService;

    @Inject
    private ApiDuplicatorService apiDuplicatorService;

    @Inject
    private ParameterService parameterService;

    @Inject
    private PlanService planService;

    @Override
    public ApiCRDStatusEntity importApiDefinitionCRD(ExecutionContext executionContext, ApiCRDEntity api) {
        try {
            ApiEntity existingApiEntity = apiService
                .findByEnvironmentIdAndCrossId(executionContext.getEnvironmentId(), api.getCrossId())
                .orElse(null);

            String sanitizedApiDefinition = objectMapper.writeValueAsString(api);

            ApiEntity importedApi;
            if (existingApiEntity != null) {
                importedApi =
                    apiDuplicatorService.updateWithImportedDefinition(executionContext, existingApiEntity.getId(), sanitizedApiDefinition);
            } else {
                importedApi = apiDuplicatorService.createWithImportedDefinition(executionContext, sanitizedApiDefinition);
            }

            // Deploy the API if needed
            if (api.getDefinitionContext().isSyncFromManagement()) {
                apiService.deploy(
                    executionContext,
                    importedApi.getId(),
                    getAuthenticatedUsername(),
                    EventType.PUBLISH_API,
                    new ApiDeploymentEntity()
                );
            }

            // Update API State if it has changed
            updateApiState(existingApiEntity, importedApi);

            // Get plan IDs
            Map<String, String> plansByCrossId = planService
                .findByApi(executionContext, importedApi.getId())
                .stream()
                .collect(toMap(PlanEntity::getCrossId, PlanEntity::getId));

            return new ApiCRDStatusEntity(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                importedApi.getId(),
                importedApi.getCrossId(),
                api.getState(),
                null, // will be set in the APIResources
                plansByCrossId
            );
        } catch (JsonProcessingException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", api, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the API definition.");
        }
    }

    private void updateApiState(ApiEntity existingApiEntity, ApiEntity importedApi) {
        if (existingApiEntity == null || existingApiEntity.getState() != importedApi.getState()) {
            if (Lifecycle.State.STARTED == importedApi.getState()) {
                checkApiLifeCycle(importedApi);
                apiService.start(GraviteeContext.getExecutionContext(), importedApi.getId(), getAuthenticatedUsername());
            } else if (Lifecycle.State.STOPPED == importedApi.getState()) {
                checkApiLifeCycle(importedApi);
                apiService.stop(GraviteeContext.getExecutionContext(), importedApi.getId(), getAuthenticatedUsername());
            }
        }
    }

    private void checkApiLifeCycle(ApiEntity api) {
        if (ApiLifecycleState.ARCHIVED.equals(api.getLifecycleState())) {
            throw new BadRequestException(String.format("lifecycle state of deleted API [%s] cannot be changed", api.getId()));
        }

        if (api.getState() == Lifecycle.State.STOPPED) {
            final boolean apiReviewEnabled = parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.API_REVIEW_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            );
            if (apiReviewEnabled && api.getWorkflowState() != null && !WorkflowState.REVIEW_OK.equals(api.getWorkflowState())) {
                throw new BadRequestException("API cannot be started without being reviewed");
            }
        }
    }
}
