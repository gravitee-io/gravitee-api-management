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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.Date;
import java.util.Map;

public class ApiPublishDocumentationPageUseCase {

    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    private final ApiCrudService apiCrudService;
    private final PageCrudService pageCrudService;

    public ApiPublishDocumentationPageUseCase(
        ApiDocumentationDomainService apiDocumentationDomainService,
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService,
        ApiCrudService apiCrudService,
        PageCrudService pageCrudService
    ) {
        this.apiDocumentationDomainService = apiDocumentationDomainService;
        this.updateApiDocumentationDomainService = updateApiDocumentationDomainService;
        this.apiCrudService = apiCrudService;
        this.pageCrudService = pageCrudService;
    }

    public Output execute(Input input) {
        this.apiCrudService.get(input.apiId);

        var page = this.pageCrudService.get(input.pageId);
        this.apiDocumentationDomainService.validatePageAssociatedToApi(page, input.apiId);

        if (page.isPublished()) {
            throw new ValidationDomainException("Page is already published.", Map.of("id", page.getId()));
        }

        var newPage = page.toBuilder().published(true).updatedAt(new Date()).build();
        var updatedPage = this.updateApiDocumentationDomainService.updatePage(newPage, page, input.auditInfo);

        updatedPage = updatedPage.withHidden(this.apiDocumentationDomainService.pageIsHidden(updatedPage));

        return new Output(updatedPage);
    }

    public record Input(String apiId, String pageId, AuditInfo auditInfo) {}

    public record Output(Page page) {}
}
