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
package io.gravitee.apim.core.documentation.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService;
import io.gravitee.apim.core.documentation.domain_service.HomepageDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.AccessControl;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class ApiUpdateDocumentationPageUseCase {

    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final HomepageDomainService homepageDomainService;
    private final ApiCrudService apiCrudService;
    private final PageCrudService pageCrudService;
    private final PageQueryService pageQueryService;
    private final DocumentationValidationDomainService documentationValidationDomainService;

    public Output execute(Input input) {
        var api = this.apiCrudService.get(input.apiId);

        var oldPage = this.pageCrudService.get(input.pageId);
        this.apiDocumentationDomainService.validatePageAssociatedToApi(oldPage, input.apiId);

        Page.PageBuilder newPage = oldPage.toBuilder();

        var name = documentationValidationDomainService.sanitizeDocumentationName(input.name);
        if (!Objects.equals(oldPage.getName(), name)) {
            this.apiDocumentationDomainService.validateNameIsUnique(input.apiId, oldPage.getParentId(), name, oldPage.getType());
            newPage.name(name);
        }

        if (oldPage.isMarkdown() && !Objects.equals(oldPage.getContent(), input.content)) {
            this.documentationValidationDomainService.validateContent(input.content, input.apiId, input.auditInfo().organizationId());
            newPage.content(input.content);
        } else if (oldPage.isSwagger() && !Objects.equals(oldPage.getContent(), input.content)) {
            this.documentationValidationDomainService.parseOpenApiContent(input.content);
            newPage.content(input.content);
        } else if (oldPage.isAsyncApi() && !Objects.equals(oldPage.getContent(), input.content)) {
            newPage.content(input.content);
        }

        newPage.updatedAt(new Date());
        newPage.visibility(input.visibility);
        newPage.homepage(input.homepage);
        newPage.order(input.order);
        newPage.configuration(input.configuration);

        if (Objects.nonNull(input.excludedAccessControls)) {
            newPage.excludedAccessControls(input.excludedAccessControls);
        }

        if (Objects.nonNull(input.accessControls)) {
            newPage.accessControls(this.documentationValidationDomainService.sanitizeAccessControls(input.accessControls));
        }

        var updatedPage = this.updateApiDocumentationDomainService.updatePage(newPage.build(), oldPage, input.auditInfo);

        if (!updatedPage.isFolder() && updatedPage.isHomepage() && !oldPage.isHomepage()) {
            this.homepageDomainService.setPreviousHomepageToFalse(input.apiId, updatedPage.getId());
        }

        if (updatedPage.getOrder() != oldPage.getOrder()) {
            this.updatePageOrders(oldPage.getOrder(), updatedPage, input.auditInfo);
        }

        updatedPage =
            updatedPage
                .withHidden(this.apiDocumentationDomainService.pageIsHidden(updatedPage))
                .withGeneralConditions(this.apiDocumentationDomainService.pageIsUsedAsGeneralConditions(updatedPage, api));

        return new Output(updatedPage);
    }

    @Builder
    public record Input(
        String apiId,
        String pageId,
        String name,
        int order,
        Page.Visibility visibility,
        String content,
        boolean homepage,
        AuditInfo auditInfo,
        Set<AccessControl> accessControls,
        Boolean excludedAccessControls,
        Map<String, String> configuration
    ) {}

    public record Output(Page page) {}

    private void updatePageOrders(int oldOrder, Page updatedPage, AuditInfo auditInfo) {
        var newOrder = updatedPage.getOrder();
        var shouldMoveDown = newOrder < oldOrder;
        var orderIncrement = shouldMoveDown ? 1 : -1;

        this.pageQueryService.searchByApiIdAndParentId(updatedPage.getReferenceId(), updatedPage.getParentId())
            .stream()
            .filter(page -> !Objects.equals(page.getId(), updatedPage.getId()))
            .filter(page ->
                shouldMoveDown
                    ? this.toBeMovedDown(oldOrder, newOrder, page.getOrder())
                    : this.toBeMovedUp(oldOrder, newOrder, page.getOrder())
            )
            .forEach(page -> {
                var updatedOrder = page.getOrder() + orderIncrement;
                this.updateApiDocumentationDomainService.updatePage(page.toBuilder().order(updatedOrder).build(), page, auditInfo);
            });
    }

    private boolean toBeMovedUp(int oldOrder, int newOrder, int pageOrder) {
        return oldOrder < pageOrder && pageOrder <= newOrder;
    }

    private boolean toBeMovedDown(int oldOrder, int newOrder, int pageOrder) {
        return newOrder <= pageOrder && pageOrder < oldOrder;
    }
}
