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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.HomepageDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import java.util.Date;
import java.util.Objects;
import lombok.Builder;

public class ApiUpdateDocumentationPageUsecase {

    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final HomepageDomainService homepageDomainService;
    private final ApiCrudService apiCrudService;
    private final PageCrudService pageCrudService;

    public ApiUpdateDocumentationPageUsecase(
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService,
        ApiDocumentationDomainService apiDocumentationDomainService,
        HomepageDomainService homepageDomainService,
        ApiCrudService apiCrudService,
        PageCrudService pageCrudService
    ) {
        this.updateApiDocumentationDomainService = updateApiDocumentationDomainService;
        this.apiDocumentationDomainService = apiDocumentationDomainService;
        this.homepageDomainService = homepageDomainService;
        this.apiCrudService = apiCrudService;
        this.pageCrudService = pageCrudService;
    }

    public Output execute(Input input) {
        this.apiCrudService.get(input.apiId);

        var oldPage = this.pageCrudService.get(input.pageId);
        this.apiDocumentationDomainService.validatePageAssociatedToApi(oldPage, input.apiId);

        Page.PageBuilder newPage = oldPage.toBuilder();

        if (!Objects.equals(oldPage.getName(), input.name)) {
            this.apiDocumentationDomainService.validateNameIsUnique(input.apiId, oldPage.getParentId(), input.name, oldPage.getType());
            newPage.name(input.name);
        }

        if (oldPage.isMarkdown() && !Objects.equals(oldPage.getContent(), input.content)) {
            this.apiDocumentationDomainService.validateContentIsSafe(input.content);
            newPage.content(input.content);
        }

        newPage.updatedAt(new Date());
        newPage.visibility(input.visibility);
        newPage.homepage(input.homepage);
        // TODO: Implement order logic -- APIM-3077
        newPage.order(input.order);

        var updatedPage = this.updateApiDocumentationDomainService.updatePage(newPage.build(), oldPage, input.auditInfo);

        if (updatedPage.isMarkdown() && updatedPage.isHomepage() && !oldPage.isHomepage()) {
            this.homepageDomainService.setPreviousHomepageToFalse(input.apiId, updatedPage.getId());
        }

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
        AuditInfo auditInfo
    ) {}

    public record Output(Page page) {}
}
