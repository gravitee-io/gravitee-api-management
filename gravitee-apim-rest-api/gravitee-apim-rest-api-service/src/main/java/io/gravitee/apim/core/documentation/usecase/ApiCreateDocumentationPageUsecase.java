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
package io.gravitee.apim.core.documentation.usecase;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.DomainException;
import io.gravitee.apim.core.exception.InvalidPageParentException;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.sanitizer.SanitizeResult;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.Date;
import java.util.Objects;
import lombok.Builder;

public class ApiCreateDocumentationPageUsecase {

    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final PageCrudService pageCrudService;

    public ApiCreateDocumentationPageUsecase(
        CreateApiDocumentationDomainService createApiDocumentationDomainService,
        ApiDocumentationDomainService apiDocumentationDomainService,
        PageCrudService pageCrudService
    ) {
        this.createApiDocumentationDomainService = createApiDocumentationDomainService;
        this.apiDocumentationDomainService = apiDocumentationDomainService;
        this.pageCrudService = pageCrudService;
    }

    public Output execute(Input input) {
        var pageToCreate = input.page;
        pageToCreate.setId(UuidString.generateRandom());
        pageToCreate.setCreatedAt(new Date());
        pageToCreate.setUpdatedAt(pageToCreate.getCreatedAt());

        if (pageToCreate.isMarkdown()) {
            this.apiDocumentationDomainService.validateContentIsSafe(pageToCreate.getContent());
        }

        this.validateParentId(pageToCreate);

        return new Output(createApiDocumentationDomainService.createPage(pageToCreate, input.auditInfo()));
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
}
