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
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.model.IndexablePage;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ClearIngestedApiDocumentationDomainService {

    private final PageCrudService pageCrudService;
    private final AuditDomainService auditDomainService;
    private final Indexer indexer;
    private final PageQueryService pageQueryService;

    public void clearIngestedPagesOf(String apiId, Collection<String> keepPages, AuditInfo auditInfo) {
        pageQueryService
            .searchByApiId(apiId)
            .stream()
            .filter(Page::isIngested)
            .filter(p -> !keepPages.contains(p.getName()))
            .forEach(page -> {
                if (page.isPublished()) {
                    indexer.delete(
                        new Indexer.IndexationContext(auditInfo.organizationId(), auditInfo.environmentId()),
                        new IndexablePage(page)
                    );
                }
                pageCrudService.delete(page.getId());
                auditDomainService.createApiAuditLog(
                    ApiAuditLogEntity.builder()
                        .apiId(page.getReferenceId())
                        .event(PageAuditEvent.PAGE_DELETED)
                        .createdAt(page.getUpdatedAt().toInstant().atZone(ZoneId.of("UTC")))
                        .organizationId(auditInfo.organizationId())
                        .environmentId(auditInfo.environmentId())
                        .actor(auditInfo.actor())
                        .properties(Map.of(AuditProperties.PAGE, page.getId()))
                        .oldValue(page)
                        .newValue(null)
                        .build()
                );
            });
    }
}
