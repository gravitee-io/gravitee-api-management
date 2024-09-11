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
import io.gravitee.apim.core.documentation.domain_service.PageSourceDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.exception.ApiPageSourceNotDefinedException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Date;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;

@UseCase
@AllArgsConstructor
public class ApiUpdateFetchedPageContentUseCase {

    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final ApiCrudService apiCrudService;
    private final PageCrudService pageCrudService;
    private final DocumentationValidationDomainService documentationValidationDomainService;
    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;

    public Output execute(Input input) {
        var api = this.apiCrudService.get(input.apiId);

        var oldPage = this.pageCrudService.get(input.pageId);

        this.apiDocumentationDomainService.validatePageAssociatedToApi(oldPage, api.getId());

        if (Objects.isNull(oldPage.getSource())) {
            throw new ApiPageSourceNotDefinedException(oldPage.getId(), oldPage.getReferenceId());
        }

        var pageToUpdate =
            this.documentationValidationDomainService.validateAndSanitizeForUpdate(oldPage, input.auditInfo.organizationId(), false);

        if (Objects.equals(pageToUpdate.getContent(), oldPage.getContent())) {
            return new Output(oldPage);
        }

        var updatedPage =
            this.updateApiDocumentationDomainService.updatePage(
                    pageToUpdate.toBuilder().updatedAt(new Date()).build(),
                    oldPage,
                    input.auditInfo
                );

        return new Output(updatedPage);
    }

    @Builder
    public record Input(String pageId, String apiId, AuditInfo auditInfo) {}

    public record Output(Page page) {}
}
