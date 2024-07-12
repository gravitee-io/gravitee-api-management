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

import static io.gravitee.rest.api.service.cockpit.services.ImportSwaggerDescriptorBuilder.buildForDocumentedApi;
import static io.gravitee.rest.api.service.cockpit.services.ImportSwaggerDescriptorBuilder.buildForMockedApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.*;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.cockpit.model.ContextPathValidationResult;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PageConverter;
import java.util.List;
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
    private final PageConverter pageConverter;
    private final VerifyApiPathDomainService verifyApiPathDomainService;

    public ApiServiceCockpitImpl(
        ObjectMapper objectMapper,
        ApiService apiService,
        SwaggerService swaggerService,
        PageService pageService,
        ApiMetadataService apiMetadataService,
        PlanService planService,
        ApiConverter apiConverter,
        PageConverter pageConverter,
        VerifyApiPathDomainService verifyApiPathDomainService
    ) {
        this.objectMapper = objectMapper;
        this.apiService = apiService;
        this.swaggerService = swaggerService;
        this.pageService = pageService;
        this.apiMetadataService = apiMetadataService;
        this.planService = planService;
        this.apiConverter = apiConverter;
        this.pageConverter = pageConverter;
        this.verifyApiPathDomainService = verifyApiPathDomainService;
    }

    @Override
    public ApiEntityResult createApi(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        DeploymentMode mode,
        List<String> labels
    ) {
        if (mode == DeploymentMode.API_MOCKED) {
            logger.debug("Create Mocked Api [{}].", apiId);
            return createMockedApi(executionContext, apiId, userId, swaggerDefinition, environmentId, labels);
        }

        if (mode == DeploymentMode.API_PUBLISHED) {
            logger.debug("Create Published Api [{}].", apiId);
            return createPublishedApi(executionContext, apiId, userId, swaggerDefinition, environmentId, labels);
        }

        logger.debug("Create Documented Api [{}].", apiId);
        return createDocumentedApi(executionContext, apiId, userId, swaggerDefinition, labels);
    }

    @Override
    public ApiEntityResult updateApi(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        DeploymentMode mode,
        List<String> labels
    ) {
        if (mode == DeploymentMode.API_DOCUMENTED) {
            logger.debug("Update Documented Api [{}].", apiId);
            return updateDocumentedApi(executionContext, apiId, swaggerDefinition, labels);
        }

        if (mode == DeploymentMode.API_MOCKED) {
            logger.debug("Update Mocked Api [{}].", apiId);
            return updateMockedApi(executionContext, apiId, userId, swaggerDefinition, environmentId, labels);
        }

        logger.debug("Update Published Api [{}].", apiId);
        return updatePublishedApi(executionContext, apiId, userId, swaggerDefinition, environmentId, labels);
    }

    private ApiEntityResult createDocumentedApi(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        String swaggerDefinition,
        List<String> labels
    ) {
        ImportSwaggerDescriptorEntity swaggerDescriptor = buildForDocumentedApi(swaggerDefinition);
        return createApiEntity(executionContext, apiId, userId, swaggerDescriptor, labels);
    }

    private ApiEntityResult updateDocumentedApi(
        ExecutionContext executionContext,
        String apiId,
        String swaggerDefinition,
        List<String> labels
    ) {
        return updateApiEntity(executionContext, apiId, buildForDocumentedApi(swaggerDefinition), labels);
    }

    private ApiEntityResult createMockedApi(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        List<String> labels
    ) {
        ImportSwaggerDescriptorEntity swaggerDescriptor = buildForMockedApi(swaggerDefinition);

        ApiEntityResult createApiResult = createApiEntity(executionContext, apiId, userId, swaggerDescriptor, labels);

        if (createApiResult.isSuccess()) {
            String createdApiId = createApiResult.getApi().getId();
            this.planService.create(executionContext, createKeylessPlan(createdApiId, environmentId));

            return ApiEntityResult.success(this.apiService.start(executionContext, createdApiId, userId));
        }
        return createApiResult;
    }

    private ApiEntityResult updateMockedApi(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        List<String> labels
    ) {
        ApiEntityResult apiEntityResult = updateApiEntity(executionContext, apiId, buildForMockedApi(swaggerDefinition), labels);

        ApiDeploymentEntity apiDeployment = new ApiDeploymentEntity();
        apiDeployment.setDeploymentLabel("Model updated");

        Set<PlanEntity> plans = this.planService.findByApi(executionContext, apiId);
        if (null == plans || plans.isEmpty()) {
            this.planService.create(executionContext, createKeylessPlan(apiId, environmentId));
        }

        ApiEntity apiEntity = apiEntityResult.getApi();
        if (null != apiEntity && Lifecycle.State.STOPPED.equals(apiEntity.getState())) {
            this.apiService.start(executionContext, apiId, userId);
        }

        return ApiEntityResult.success(apiService.deploy(executionContext, apiId, userId, EventType.PUBLISH_API, apiDeployment));
    }

    private ApiEntityResult createPublishedApi(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        List<String> labels
    ) {
        ImportSwaggerDescriptorEntity swaggerDescriptor = buildForMockedApi(swaggerDefinition);

        ApiEntityResult createApiResult = createApiEntity(executionContext, apiId, userId, swaggerDescriptor, labels);

        if (createApiResult.isSuccess()) {
            String createdApiId = createApiResult.getApi().getId();
            this.planService.create(executionContext, createKeylessPlan(createdApiId, environmentId));
            ApiEntity apiEntity = this.apiService.start(executionContext, createdApiId, userId);

            publishSwaggerDocumentation(executionContext, createdApiId);

            UpdateApiEntity updateEntity = apiConverter.toUpdateApiEntity(apiEntity);
            updateEntity.setVisibility(Visibility.PUBLIC);
            updateEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
            return ApiEntityResult.success(this.apiService.update(executionContext, createdApiId, updateEntity));
        }

        return createApiResult;
    }

    private ApiEntityResult updatePublishedApi(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        String swaggerDefinition,
        String environmentId,
        List<String> labels
    ) {
        ApiEntityResult updatedApiResult = updateMockedApi(executionContext, apiId, userId, swaggerDefinition, environmentId, labels);

        if (updatedApiResult.isSuccess() && !ApiLifecycleState.PUBLISHED.equals(updatedApiResult.getApi().getLifecycleState())) {
            publishSwaggerDocumentation(executionContext, apiId);
            UpdateApiEntity updateEntity = apiConverter.toUpdateApiEntity(updatedApiResult.getApi());
            updateEntity.setVisibility(Visibility.PUBLIC);
            updateEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
            return ApiEntityResult.success(this.apiService.update(executionContext, apiId, updateEntity));
        }

        return updatedApiResult;
    }

    private ApiEntityResult updateApiEntity(
        ExecutionContext executionContext,
        String apiId,
        ImportSwaggerDescriptorEntity swaggerDescriptor,
        List<String> labels
    ) {
        final SwaggerApiEntity api = swaggerService.createAPI(executionContext, swaggerDescriptor, DefinitionVersion.V2);
        api.setPaths(null);
        api.setLabels(labels);

        // apply update to proxy paths
        final ContextPathValidationResult result = checkContextPath(executionContext.getEnvironmentId(), api, apiId);
        if (result.hasError()) {
            return ApiEntityResult.failure(result.getError());
        }
        api.getProxy().setVirtualHosts(result.toVirtualHosts());

        return ApiEntityResult.success(this.apiService.updateFromSwagger(executionContext, apiId, api, swaggerDescriptor));
    }

    private ApiEntityResult createApiEntity(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        ImportSwaggerDescriptorEntity swaggerDescriptor,
        List<String> labels
    ) {
        final SwaggerApiEntity api = swaggerService.createAPI(executionContext, swaggerDescriptor, DefinitionVersion.V2);
        api.setPaths(null);
        api.setLabels(labels);

        final ContextPathValidationResult result = checkContextPath(executionContext.getEnvironmentId(), api);
        if (result.hasError()) {
            return ApiEntityResult.failure(result.getError());
        }

        // Update virtual hosts from sanitized paths
        api.getProxy().setVirtualHosts(result.toVirtualHosts());

        final ObjectNode apiDefinition = objectMapper.valueToTree(api);
        apiDefinition.put("crossId", apiId);
        api.setCrossId(apiId);

        final ApiEntity createdApi = apiService.createWithApiDefinition(executionContext, api, userId, apiDefinition);

        pageService.createAsideFolder(executionContext, createdApi.getId());
        pageService.createOrUpdateSwaggerPage(executionContext, createdApi.getId(), swaggerDescriptor, true);
        apiMetadataService.create(executionContext, api.getMetadata(), createdApi.getId());
        return ApiEntityResult.success(createdApi);
    }

    ContextPathValidationResult checkContextPath(String environmentId, SwaggerApiEntity api) {
        return checkContextPath(environmentId, api, null);
    }

    ContextPathValidationResult checkContextPath(String environmentId, SwaggerApiEntity api, String apiId) {
        try {
            var validationResult = verifyApiPathDomainService.validateAndSanitize(
                new VerifyApiPathDomainService.Input(
                    environmentId,
                    apiId,
                    api
                        .getProxy()
                        .getVirtualHosts()
                        .stream()
                        .map(h -> Path.builder().path(h.getPath()).host(h.getHost()).overrideAccess(h.isOverrideEntrypoint()).build())
                        .toList()
                )
            );

            validationResult
                .severe()
                .ifPresent(errors -> {
                    throw new InvalidPathsException(errors.iterator().next().getMessage());
                });

            var sanitizedPaths = validationResult
                .map(VerifyApiPathDomainService.Input::paths)
                .value()
                .stream()
                .flatMap(List::stream)
                .toList();

            return ContextPathValidationResult.builder().sanitizedPaths(sanitizedPaths).build();
        } catch (InvalidPathsException e) {
            String ctxPath = api.getProxy().getVirtualHosts().stream().findFirst().map(VirtualHost::getPath).orElse("");
            return ContextPathValidationResult
                .builder()
                .error("The path [" + ctxPath + "] automatically generated from the name is already covered by another API.")
                .build();
        } catch (Exception e) {
            return ContextPathValidationResult.builder().error(e.getMessage()).build();
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

    private void publishSwaggerDocumentation(ExecutionContext executionContext, String apiId) {
        List<PageEntity> apiDocs = pageService.search(
            executionContext.getEnvironmentId(),
            new PageQuery.Builder().api(apiId).type(PageType.SWAGGER).build()
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
            pageService.update(executionContext, page.getId(), updatePage);
        }
    }
}
