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

package io.gravitee.apim.core.integration.usecase;

import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.domain_service.IntegrationDomainService;
import io.gravitee.apim.core.integration.model.IntegrationEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.NewPageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class IntegrationImportUsecase {

    private final IntegrationDomainService integrationDomainService;
    private final IntegrationCrudService integrationCrudService;
    private final SwaggerService swaggerService;
    private final ApiService apiService;
    private final PageService pageService;
    private final ApiConverter apiConverter;

    public IntegrationImportUsecase.Output execute(IntegrationImportUsecase.Input input) {
        var integrationId = input.integrationId();
        var userId = input.userId();

        var integration = integrationCrudService.get(integrationId);

        return new Output(
            integrationDomainService
                .fetchEntities(integration, input.entities())
                .flatMap(entities -> {
                    entities.forEach(integrationEntity -> importApi(integrationEntity.getName(), integrationEntity.getContent(), userId));
                    return Single.just(true);
                })
        );
    }

    @Builder
    public record Input(String integrationId, List<IntegrationEntity> entities, String userId) {}

    public record Output(Single<Boolean> status) {}

    private ApiEntity importApi(String apiName, String apiContent, String userId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        // Create api
        ApiEntity apiEntity = createFromSwagger(executionContext, apiContent, userId);
        // Update visibility and publication
        apiEntity = updateVisibility(executionContext, apiEntity);
        // Create page
        createPage(apiEntity.getId(), apiName, apiContent, PageType.SWAGGER);

        log.info("API Imported {}", apiEntity.getId());
        return apiEntity;
    }

    private ApiEntity updateVisibility(ExecutionContext executionContext, ApiEntity apiEntity) {
        if (apiEntity.getVisibility() != Visibility.PUBLIC || apiEntity.getLifecycleState() != ApiLifecycleState.PUBLISHED) {
            UpdateApiEntity updateApiEntity = apiConverter.toUpdateApiEntity(apiEntity);
            updateApiEntity.setVisibility(Visibility.PUBLIC);
            updateApiEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
            apiEntity = apiService.update(executionContext, apiEntity.getId(), updateApiEntity, false);
        }
        return apiEntity;
    }

    private ApiEntity createFromSwagger(ExecutionContext executionContext, String payload, String userId) {
        final ImportSwaggerDescriptorEntity importSwaggerDescriptorEntity = new ImportSwaggerDescriptorEntity();
        importSwaggerDescriptorEntity.setPayload(payload);
        importSwaggerDescriptorEntity.setWithPathMapping(true);
        final SwaggerApiEntity swaggerApiEntity = swaggerService.createAPI(
            executionContext,
            importSwaggerDescriptorEntity,
            DefinitionVersion.V2
        );
        return apiService.createFromSwagger(executionContext, swaggerApiEntity, userId, importSwaggerDescriptorEntity);
    }

    private void createPage(String apiId, String apiName, String content, PageType pageType) {
        NewPageEntity pageEntity = new NewPageEntity();
        pageEntity.setType(pageType);
        pageEntity.setPublished(true);
        pageEntity.setVisibility(Visibility.PUBLIC);
        pageEntity.setName(apiName);
        pageEntity.setContent(content);
        pageService.createPage(GraviteeContext.getExecutionContext(), apiId, pageEntity);
    }
}
