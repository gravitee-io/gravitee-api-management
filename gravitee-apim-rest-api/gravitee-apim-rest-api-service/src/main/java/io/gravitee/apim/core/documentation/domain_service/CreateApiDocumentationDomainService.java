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
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
public class CreateApiDocumentationDomainService {

    private final PageCrudService pageCrudService;
    private final PageRevisionCrudService pageRevisionCrudService;
    private final AuditDomainService auditDomainService;

    private final Indexer indexer;

    public CreateApiDocumentationDomainService(
        PageCrudService pageCrudService,
        PageRevisionCrudService pageRevisionCrudService,
        AuditDomainService auditDomainService,
        Indexer indexer
    ) {
        this.pageCrudService = pageCrudService;
        this.pageRevisionCrudService = pageRevisionCrudService;
        this.auditDomainService = auditDomainService;
        this.indexer = indexer;
    }

    public Page createPage(Page page, AuditInfo auditInfo) {
        var createdPage = pageCrudService.createDocumentationPage(page);

        if (page.isSwaggerOrMarkdown()) {
            pageRevisionCrudService.create(createdPage);
            indexer.index(new IndexationContext(auditInfo.organizationId(), auditInfo.environmentId()), new IndexablePage(createdPage));
        }

        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .apiId(page.getReferenceId())
                .event(PageAuditEvent.PAGE_CREATED)
                .createdAt(page.getCreatedAt().toInstant().atZone(ZoneId.of("UTC")))
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .actor(auditInfo.actor())
                .properties(Map.of(AuditProperties.PAGE, page.getId()))
                .oldValue(null)
                .newValue(page)
                .build()
        );

        return createdPage;
    }
}
