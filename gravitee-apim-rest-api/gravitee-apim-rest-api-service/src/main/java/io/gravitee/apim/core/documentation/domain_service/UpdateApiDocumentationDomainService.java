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
package io.gravitee.apim.core.documentation.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.PageAuditEvent;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.crud_service.PageRevisionCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.Indexer.IndexationContext;
import io.gravitee.apim.core.search.model.IndexablePage;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class UpdateApiDocumentationDomainService {

    private final PageCrudService pageCrudService;
    private final PageRevisionCrudService pageRevisionCrudService;
    private final AuditDomainService auditDomainService;
    private final Indexer indexer;

    public Page updatePage(Page page, Page oldPage, AuditInfo auditInfo) {
        var updatedPage = pageCrudService.updateDocumentationPage(page);

        if (updatedPage.isSwaggerOrMarkdown() || updatedPage.isAsyncApi()) {
            if (
                !Objects.equals(updatedPage.getName(), oldPage.getName()) || !Objects.equals(updatedPage.getContent(), oldPage.getContent())
            ) {
                pageRevisionCrudService.create(updatedPage);
            }
            if (page.isPublished()) {
                indexer.index(new IndexationContext(auditInfo.organizationId(), auditInfo.environmentId()), new IndexablePage(page));
            } else {
                indexer.delete(new IndexationContext(auditInfo.organizationId(), auditInfo.environmentId()), new IndexablePage(page));
            }
        }

        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .apiId(page.getReferenceId())
                .event(PageAuditEvent.PAGE_UPDATED)
                .createdAt(page.getUpdatedAt().toInstant().atZone(ZoneId.of("UTC")))
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .actor(auditInfo.actor())
                .properties(Map.of(AuditProperties.PAGE, page.getId()))
                .oldValue(oldPage)
                .newValue(updatedPage)
                .build()
        );

        return updatedPage;
    }
}
