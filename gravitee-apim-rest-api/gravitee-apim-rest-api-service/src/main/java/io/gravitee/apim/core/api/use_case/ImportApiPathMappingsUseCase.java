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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiPathMappingsDomainService;
import io.gravitee.apim.core.api.domain_service.OpenApiPathMappingsExtractor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class ImportApiPathMappingsUseCase {

    private final ApiPathMappingsDomainService apiPathMappingsDomainService;
    private final PageCrudService pageCrudService;
    private final OpenApiPathMappingsExtractor openApiPathMappingsExtractor;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalPageContentQueryService portalPageContentQueryService;

    public record Input(String apiId, String pageId, boolean overwrite, AuditInfo auditInfo) {}

    public record Output(Set<String> pathMappings) {}

    public Output execute(Input input) {
        var api = apiPathMappingsDomainService.loadEditableHttpV4Api(input.apiId(), input.auditInfo());
        var extracted = openApiPathMappingsExtractor.extract(specification(input));
        Set<String> toStore;
        if (input.overwrite()) {
            toStore = new LinkedHashSet<>(extracted);
        } else {
            toStore = new LinkedHashSet<>(apiPathMappingsDomainService.currentMappings(api));
            toStore.addAll(extracted);
        }
        return new Output(apiPathMappingsDomainService.replace(api, toStore, input.auditInfo()));
    }

    private String specification(Input input) {
        var classicPage = pageCrudService.findById(input.pageId());
        if (classicPage.isPresent()) {
            return classicSwaggerSpecification(classicPage.get(), input);
        }
        return nextGenOpenApiSpecification(input);
    }

    private String classicSwaggerSpecification(Page page, Input input) {
        boolean ownedByApi = page.getReferenceType() == Page.ReferenceType.API && input.apiId().equals(page.getReferenceId());
        if (!ownedByApi) {
            throw new PageNotFoundException(input.pageId());
        }
        if (page.getType() != Page.Type.SWAGGER) {
            throw new ValidationDomainException("Page must be a SWAGGER documentation page");
        }
        return page.getContent();
    }

    private String nextGenOpenApiSpecification(Input input) {
        PortalNavigationItemId itemId;
        try {
            itemId = PortalNavigationItemId.of(input.pageId());
        } catch (IllegalArgumentException exception) {
            throw new PageNotFoundException(input.pageId());
        }
        var item = portalNavigationItemsQueryService.findByIdAndEnvironmentId(input.auditInfo().environmentId(), itemId);
        if (
            !(item instanceof PortalNavigationPage page) ||
            !new NavigationItemReference.ApiReference(input.apiId()).equals(item.getReference())
        ) {
            throw new PageNotFoundException(input.pageId());
        }
        var content = portalPageContentQueryService
            .findById(page.getPortalPageContentId())
            .orElseThrow(() -> new PageNotFoundException(input.pageId()));
        if (!input.auditInfo().environmentId().equals(content.getEnvironmentId())) {
            throw new PageNotFoundException(input.pageId());
        }
        if (!(content instanceof OpenApiPageContent openApi)) {
            throw new ValidationDomainException("Page must be an OpenAPI document");
        }
        return openApi.getContent().value();
    }
}
