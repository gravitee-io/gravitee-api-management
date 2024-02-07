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

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService;
import io.gravitee.apim.core.documentation.domain_service.HomepageDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiCreateDocumentationPageUseCase {

    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final HomepageDomainService homepageDomainService;
    private final PageCrudService pageCrudService;
    private final PageQueryService pageQueryService;
    private final DocumentationValidationDomainService documentationValidationDomainService;

    public Output execute(Input input) {
        var pageToCreate = input.page;
        pageToCreate.setId(UuidString.generateRandom());
        pageToCreate.setCreatedAt(new Date());
        pageToCreate.setUpdatedAt(pageToCreate.getCreatedAt());
        pageToCreate.setName(documentationValidationDomainService.sanitizeDocumentationName(pageToCreate.getName()));

        if (pageToCreate.isMarkdown()) {
            this.documentationValidationDomainService.validateContent(pageToCreate.getContent(), pageToCreate.getReferenceId());
        }

        this.validateParentId(pageToCreate);
        this.validateNameIsUnique(pageToCreate);

        this.calculateOrder(pageToCreate);

        var createdPage = createApiDocumentationDomainService.createPage(pageToCreate, input.auditInfo());

        if (createdPage.isHomepage()) {
            this.homepageDomainService.setPreviousHomepageToFalse(createdPage.getReferenceId(), createdPage.getId());
        }

        if (createdPage.isFolder()) {
            createdPage.setHidden(true);
        }

        return new Output(createdPage);
    }

    @Builder
    public record Input(Page page, AuditInfo auditInfo) {}

    public record Output(Page createdPage) {}

    private void validateParentId(Page page) {
        var parentId = page.getParentId();

        if (Objects.nonNull(parentId) && !parentId.isEmpty()) {
            var foundParent = pageCrudService.findById(parentId);

            if (foundParent.isPresent()) {
                if (!foundParent.get().isFolder()) {
                    throw new InvalidPageParentException(parentId);
                }
                return;
            }
        }

        page.setParentId(null);
    }

    private void validateNameIsUnique(Page page) {
        this.apiDocumentationDomainService.validateNameIsUnique(page.getReferenceId(), page.getParentId(), page.getName(), page.getType());
    }

    private void calculateOrder(Page page) {
        var lastPage = pageQueryService
            .searchByApiIdAndParentId(page.getReferenceId(), page.getParentId())
            .stream()
            .max(Comparator.comparingInt(Page::getOrder));
        var nextOrder = lastPage.map(value -> value.getOrder() + 1).orElse(0);

        page.setOrder(nextOrder);
    }
}
