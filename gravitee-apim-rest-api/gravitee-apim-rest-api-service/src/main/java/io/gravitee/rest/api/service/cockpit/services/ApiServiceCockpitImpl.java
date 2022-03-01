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
package io.gravitee.rest.api.service.cockpit.services;

import static io.gravitee.rest.api.service.cockpit.services.ImportSwaggerDescriptorBuilder.buildForDocumentedApi;
import static io.gravitee.rest.api.service.cockpit.services.ImportSwaggerDescriptorBuilder.buildForMockedApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.*;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PageConverter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiServiceCockpitImpl implements ApiServiceCockpit {

    private final Logger logger = LoggerFactory.getLogger(ApiServiceCockpitImpl.class);

    private final ObjectMapper objectMapper;
    private final ApiService apiService;
    private final ApiConverter apiConverter;
    private final SwaggerService swaggerService;
    private final PageService pageService;
    private final ApiMetadataService apiMetadataService;
    private final PlanService planService;
    private final VirtualHostService virtualHostService;
    private final PageConverter pageConverter;

    public ApiServiceCockpitImpl(
        ObjectMapper objectMapper,
        ApiService apiService,
        SwaggerService swaggerService,
        PageService pageService,
        ApiMetadataService apiMetadataService,
        PlanService planService,
        VirtualHostService virtualHostService,
        ApiConverter apiConverter,
        PageConverter pageConverter
    ) {
        this.objectMapper = objectMapper;
        this.apiService = apiService;
        this.swaggerService = swaggerService;
        this.pageService = pageService;
        this.apiMetadataService = apiMetadataService;
        this.planService = planService;
        this.virtualHostService = virtualHostService;
        this.apiConverter = apiConverter;
        this.pageConverter = pageConverter;
    }

    @Override
    public ApiEntityResult createApi(
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        DeploymentMode mode,
        List<String> labels
    ) {
        if (mode == DeploymentMode.API_MOCKED) {
            logger.debug("Create Mocked Api [{}].", apiId);
            return createMockedApi(apiId, userId, swaggerDefinition, environmentId, labels);
        }

        if (mode == DeploymentMode.API_PUBLISHED) {
            logger.debug("Create Published Api [{}].", apiId);
            return createPublishedApi(apiId, userId, swaggerDefinition, environmentId, labels);
        }

        logger.debug("Create Documented Api [{}].", apiId);
        return createDocumentedApi(apiId, userId, swaggerDefinition, environmentId, labels);
    }

    @Override
    public ApiEntityResult updateApi(
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        DeploymentMode mode,
        List<String> labels
    ) {
        if (mode == DeploymentMode.API_DOCUMENTED) {
            logger.debug("Update Documented Api [{}].", apiId);
            return updateDocumentedApi(apiId, swaggerDefinition, labels);
        }

        if (mode == DeploymentMode.API_MOCKED) {
            logger.debug("Update Mocked Api [{}].", apiId);
            return updateMockedApi(apiId, userId, swaggerDefinition, environmentId, labels);
        }

        logger.debug("Update Published Api [{}].", apiId);
        return updatePublishedApi(apiId, userId, swaggerDefinition, environmentId, labels);
    }

    private ApiEntityResult createDocumentedApi(
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        List<String> labels
    ) {
        ImportSwaggerDescriptorEntity swaggerDescriptor = buildForDocumentedApi(swaggerDefinition);
        return createApiEntity(apiId, userId, swaggerDescriptor, labels);
    }

    private ApiEntityResult updateDocumentedApi(String apiId, String swaggerDefinition, List<String> labels) {
        return updateApiEntity(apiId, buildForDocumentedApi(swaggerDefinition), labels);
    }

    private ApiEntityResult createMockedApi(
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        List<String> labels
    ) {
        ImportSwaggerDescriptorEntity swaggerDescriptor = buildForMockedApi(swaggerDefinition);

        ApiEntityResult createApiResult = createApiEntity(apiId, userId, swaggerDescriptor, labels);

        if (createApiResult.isSuccess()) {
            this.planService.create(createKeylessPlan(apiId, environmentId));

            return ApiEntityResult.success(this.apiService.start(apiId, userId));
        }
        return createApiResult;
    }

    private ApiEntityResult updateMockedApi(
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        List<String> labels
    ) {
        ApiEntityResult apiEntityResult = updateApiEntity(apiId, buildForMockedApi(swaggerDefinition), labels);

        ApiDeploymentEntity apiDeployment = new ApiDeploymentEntity();
        apiDeployment.setDeploymentLabel("Model updated");

        Set<PlanEntity> plans = this.planService.findByApi(apiId);
        if (null == plans || plans.isEmpty()) {
            this.planService.create(createKeylessPlan(apiId, environmentId));
        }

        ApiEntity apiEntity = apiEntityResult.getApi();
        if (null != apiEntity && Lifecycle.State.STOPPED.equals(apiEntity.getState())) {
            this.apiService.start(apiId, userId);
        }

        return ApiEntityResult.success(apiService.deploy(apiId, userId, EventType.PUBLISH_API, apiDeployment));
    }

    private ApiEntityResult createPublishedApi(
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        List<String> labels
    ) {
        ImportSwaggerDescriptorEntity swaggerDescriptor = buildForMockedApi(swaggerDefinition);

        ApiEntityResult createApiResult = createApiEntity(apiId, userId, swaggerDescriptor, labels);

        if (createApiResult.isSuccess()) {
            this.planService.create(createKeylessPlan(apiId, environmentId));
            ApiEntity apiEntity = this.apiService.start(apiId, userId);

            publishSwaggerDocumentation(apiId);

            UpdateApiEntity updateEntity = apiConverter.toUpdateApiEntity(apiEntity);
            updateEntity.setVisibility(Visibility.PUBLIC);
            updateEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
            return ApiEntityResult.success(this.apiService.update(apiId, updateEntity));
        }

        return createApiResult;
    }

    private ApiEntityResult updatePublishedApi(
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        List<String> labels
    ) {
        ApiEntityResult updatedApiResult = updateMockedApi(apiId, userId, swaggerDefinition, environmentId, labels);

        if (updatedApiResult.isSuccess() && !ApiLifecycleState.PUBLISHED.equals(updatedApiResult.getApi().getLifecycleState())) {
            publishSwaggerDocumentation(apiId);
            UpdateApiEntity updateEntity = apiConverter.toUpdateApiEntity(updatedApiResult.getApi());
            updateEntity.setVisibility(Visibility.PUBLIC);
            updateEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
            return ApiEntityResult.success(this.apiService.update(apiId, updateEntity));
        }

        return updatedApiResult;
    }

    private ApiEntityResult updateApiEntity(String apiId, ImportSwaggerDescriptorEntity swaggerDescriptor, List<String> labels) {
        final SwaggerApiEntity api = swaggerService.createAPI(swaggerDescriptor, DefinitionVersion.V2);
        api.setPaths(null);
        api.setLabels(labels);

        return checkContextPath(api, apiId)
            .map(ApiEntityResult::failure)
            .orElseGet(() -> ApiEntityResult.success(this.apiService.updateFromSwagger(apiId, api, swaggerDescriptor)));
    }

    private ApiEntityResult createApiEntity(
        String apiId,
        String userId,
        ImportSwaggerDescriptorEntity swaggerDescriptor,
        List<String> labels
    ) {
        final SwaggerApiEntity api = swaggerService.createAPI(swaggerDescriptor, DefinitionVersion.V2);
        api.setPaths(null);
        api.setLabels(labels);

        final Optional<String> result = checkContextPath(api);
        if (result.isEmpty()) {
            final ObjectNode apiDefinition = objectMapper.valueToTree(api);
            apiDefinition.put("id", apiId);

            final ApiEntity createdApi = apiService.createWithApiDefinition(api, userId, apiDefinition);

            pageService.createAsideFolder(apiId, GraviteeContext.getCurrentEnvironment());
            pageService.createOrUpdateSwaggerPage(apiId, swaggerDescriptor, true);
            apiMetadataService.create(api.getMetadata(), createdApi.getId());
            return ApiEntityResult.success(createdApi);
        }

        return ApiEntityResult.failure(result.get());
    }

    Optional<String> checkContextPath(SwaggerApiEntity api) {
        try {
            virtualHostService.sanitizeAndValidate(api.getProxy().getVirtualHosts());
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e.getMessage());
        }
    }

    Optional<String> checkContextPath(SwaggerApiEntity api, String apiId) {
        try {
            virtualHostService.sanitizeAndValidate(api.getProxy().getVirtualHosts(), apiId);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e.getMessage());
        }
    }

    private NewPlanEntity createKeylessPlan(String apiId, String environmentId) {
        NewPlanEntity plan = new NewPlanEntity();
        plan.setId(UuidString.generateForEnvironment(environmentId, apiId));
        plan.setName("Keyless plan");
        plan.setSecurity(PlanSecurityType.KEY_LESS);
        plan.setApi(apiId);
        plan.setStatus(PlanStatus.PUBLISHED);

        return plan;
    }

    private void publishSwaggerDocumentation(String apiId) {
        List<PageEntity> apiDocs = pageService.search(
            new PageQuery.Builder().api(apiId).type(PageType.SWAGGER).build(),
            GraviteeContext.getCurrentEnvironment()
        );

        if (apiDocs.isEmpty()) {
            // This should not happen since we have created the documentation in previous step
            logger.error("No swagger documentation to publish");
            return;
        }

        if (apiDocs.size() > 1) {
            // This should not happen since we have created the documentation in previous step
            logger.error("More than one swagger documentation, this should not happen");
        }

        PageEntity page = apiDocs.get(0);

        if (!page.isPublished()) {
            UpdatePageEntity updatePage = pageConverter.toUpdatePageEntity(page);
            updatePage.setPublished(true);
            pageService.update(page.getId(), updatePage);
        }
    }
}
