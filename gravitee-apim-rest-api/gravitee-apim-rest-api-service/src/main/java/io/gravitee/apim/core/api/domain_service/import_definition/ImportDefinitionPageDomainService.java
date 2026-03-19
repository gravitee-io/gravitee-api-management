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
package io.gravitee.apim.core.api.domain_service.import_definition;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.common.utils.TimeProvider;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@DomainService
class ImportDefinitionPageDomainService {

    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;

    ImportDefinitionPageDomainService(
        ApiDocumentationDomainService apiDocumentationDomainService,
        CreateApiDocumentationDomainService createApiDocumentationDomainService,
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService
    ) {
        this.apiDocumentationDomainService = apiDocumentationDomainService;
        this.createApiDocumentationDomainService = createApiDocumentationDomainService;
        this.updateApiDocumentationDomainService = updateApiDocumentationDomainService;
    }

    void upsertPages(String apiId, List<Page> pagesToImport, AuditInfo auditInfo) {
        if (pagesToImport == null || pagesToImport.isEmpty()) {
            return;
        }

        var savedPages = apiDocumentationDomainService.getApiPages(apiId, null);
        var pageMap = savedPages
            .stream()
            .filter(p -> p.getCrossId() != null)
            .collect(Collectors.toMap(Page::getCrossId, Function.identity()));
        var now = Date.from(TimeProvider.now().toInstant());

        for (var importedPage : pagesToImport) {
            var existingPage = importedPage.getCrossId() != null
                ? pageMap.get(importedPage.getCrossId())
                : savedPages
                    .stream()
                    .filter(p -> p.getType() == importedPage.getType() && importedPage.getName().equals(p.getName()))
                    .findFirst()
                    .orElse(null);
            var pageToSave = importedPage.toBuilder().referenceType(Page.ReferenceType.API).referenceId(apiId).updatedAt(now);
            if (existingPage == null) {
                createApiDocumentationDomainService.createPage(pageToSave.createdAt(now).build(), auditInfo);
            } else {
                // Match is by crossId; the imported page may carry a different id (e.g. after id generation on update).
                // Persistence must use the existing row id.
                var mergedForUpdate = pageToSave.id(existingPage.getId()).createdAt(existingPage.getCreatedAt()).build();
                updateApiDocumentationDomainService.updatePage(mergedForUpdate, existingPage, auditInfo);
            }
        }
    }
}
