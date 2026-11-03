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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@DomainService
class ImportDefinitionPageDomainService {

    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    private final ImportDefinitionPageParentValidationDomainService pageParentValidationDomainService;

    ImportDefinitionPageDomainService(
        ApiDocumentationDomainService apiDocumentationDomainService,
        CreateApiDocumentationDomainService createApiDocumentationDomainService,
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService,
        ImportDefinitionPageParentValidationDomainService pageParentValidationDomainService
    ) {
        this.apiDocumentationDomainService = apiDocumentationDomainService;
        this.createApiDocumentationDomainService = createApiDocumentationDomainService;
        this.updateApiDocumentationDomainService = updateApiDocumentationDomainService;
        this.pageParentValidationDomainService = pageParentValidationDomainService;
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

        var destinationIds = new HashMap<String, String>();
        var preparedPages = pagesToImport
            .stream()
            .map(importedPage -> {
                var existingPage = findSavedPageForImport(pageMap, savedPages, importedPage);
                var pageToSave = importedPage
                    .toBuilder()
                    .id(existingPage.map(Page::getId).orElse(importedPage.getId()))
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(apiId)
                    .createdAt(existingPage.isPresent() ? existingPage.get().getCreatedAt() : now)
                    .updatedAt(now)
                    .build();
                if (importedPage.getId() != null && !importedPage.getId().isEmpty()) {
                    destinationIds.put(importedPage.getId(), pageToSave.getId());
                }
                return new PreparedPage(pageToSave, existingPage);
            })
            .toList();

        preparedPages.forEach(prepared -> {
            var page = prepared.page();
            if (page.getParentId() != null && !page.getParentId().isEmpty()) {
                page.setParentId(destinationIds.getOrDefault(page.getParentId(), page.getParentId()));
            }
        });
        pageParentValidationDomainService.validate(preparedPages.stream().map(PreparedPage::page).toList());

        for (var prepared : preparedPages) {
            if (prepared.existingPage().isEmpty()) {
                createApiDocumentationDomainService.createPage(prepared.page(), auditInfo);
            } else {
                updateApiDocumentationDomainService.updatePage(prepared.page(), prepared.existingPage().get(), auditInfo);
            }
        }
    }

    private record PreparedPage(Page page, Optional<Page> existingPage) {}

    /** Match by {@code crossId} when present and found; else by matching type and name. */
    private static Optional<Page> findSavedPageForImport(Map<String, Page> savedByCrossId, List<Page> savedPages, Page importedPage) {
        if (importedPage.getCrossId() != null) {
            var byCrossId = Optional.ofNullable(savedByCrossId.get(importedPage.getCrossId()));
            if (byCrossId.isPresent()) return byCrossId;
        }
        return savedPages
            .stream()
            .filter(p -> p.getType() == importedPage.getType() && importedPage.getName().equals(p.getName()))
            .findFirst();
    }
}
