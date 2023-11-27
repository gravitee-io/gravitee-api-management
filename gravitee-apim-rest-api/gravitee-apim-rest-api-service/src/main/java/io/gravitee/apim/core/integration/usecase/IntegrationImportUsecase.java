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
import io.gravitee.apim.core.integration.model.Page;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.model.NewPageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiService;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
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
    private final ApiService apiService;
    private final PageService pageService;

    public IntegrationImportUsecase.Output execute(IntegrationImportUsecase.Input input) {
        var integrationId = input.integrationId();
        var userId = input.userId();

        var integration = integrationCrudService.get(integrationId);

        return new Output(
            integrationDomainService
                .fetchEntities(integration, input.entities())
                .flatMap(entities -> {
                    entities.forEach(integrationEntity -> importApi(integrationEntity, userId));
                    return Single.just(true);
                })
        );
    }

    @Builder
    public record Input(String integrationId, List<IntegrationEntity> entities, String userId) {}

    public record Output(Single<Boolean> status) {}

    private ApiEntity importApi(IntegrationEntity entity, String userId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setName(entity.getName());
        newApiEntity.setDescription(entity.getDescription());
        newApiEntity.setApiVersion(entity.getVersion());
        newApiEntity.setType(ApiType.FEDERATED);

        newApiEntity.setEndpointGroups(
            List.of(
                EndpointGroup
                    .builder()
                    .type("federated")
                    .endpoints(List.of(Endpoint.builder().name("endpoint").type("federated").build()))
                    .build()
            )
        );

        newApiEntity.setFlows(Collections.emptyList());

        newApiEntity.setListeners(
            List.of(
                HttpListener
                    .builder()
                    .entrypoints(Collections.emptyList())
                    .paths(List.of(Path.builder().host(entity.getHost()).path(entity.getPath()).overrideAccess(true).build()))
                    .entrypoints(List.of(Entrypoint.builder().type("federated").build()))
                    .build()
            )
        );

        ApiEntity apiEntity = apiService.create(executionContext, newApiEntity, userId);

        // Create page
        entity
            .getPages()
            .forEach(page -> {
                PageType pageType = PageType.valueOf(page.getPageType().name());
                createPage(apiEntity.getId(), entity.getName(), page.getContent(), pageType, userId);
            });

        log.info("API Imported {}", apiEntity.getId());
        return apiEntity;
    }

    private void createPage(String apiId, String apiName, String content, PageType pageType, String userId) {
        NewPageEntity newPageEntity = new NewPageEntity();
        newPageEntity.setType(pageType);
        newPageEntity.setName(apiName);
        newPageEntity.setContent(content);

        int order = pageService.findMaxApiPageOrderByApi(apiId) + 1;
        newPageEntity.setOrder(order);
        newPageEntity.setLastContributor(userId);

        pageService.createPage(GraviteeContext.getExecutionContext(), apiId, newPageEntity);
    }
}
