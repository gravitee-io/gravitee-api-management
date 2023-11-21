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

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.PageAuditEvent;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.exception.ApiFolderNotEmptyException;
import io.gravitee.apim.core.documentation.exception.ApiPageInvalidReferenceTypeException;
import io.gravitee.apim.core.documentation.exception.ApiPageUsedAsGeneralConditionException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

public class DeleteApiDocumentationDomainService {

    private final PageCrudService pageCrudService;
    private final PageQueryService pageQueryService;
    private final AuditDomainService auditDomainService;
    private final PlanQueryService planQueryService;

    public DeleteApiDocumentationDomainService(
        PageCrudService pageCrudService,
        PageQueryService pageQueryService,
        AuditDomainService auditDomainService,
        PlanQueryService planQueryService
    ) {
        this.pageCrudService = pageCrudService;
        this.pageQueryService = pageQueryService;
        this.auditDomainService = auditDomainService;
        this.planQueryService = planQueryService;
    }

    public void delete(Api api, String pageId, AuditInfo auditInfo) {
        final Page pageToDelete = this.pageCrudService.get(pageId);

        throwIfNotApiPage(pageToDelete);
        throwIfPageUsedAsGeneralCondition(api, pageToDelete);
        throwIfDeletingNonEmptyFolder(api, pageToDelete);

        this.pageCrudService.delete(pageId);

        // TODO: remove revisions ?
        // TODO: remove from search engine index
        // TODO: delete LINK and TRANSLATION associated pages

        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .apiId(pageToDelete.getReferenceId())
                .event(PageAuditEvent.PAGE_DELETED)
                .createdAt(Instant.now().atZone(ZoneId.of("UTC")))
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .actor(auditInfo.actor())
                .properties(Map.of(AuditProperties.PAGE, pageId))
                .oldValue(pageToDelete)
                .newValue(null)
                .build()
        );
    }

    private void throwIfDeletingNonEmptyFolder(Api api, Page pageToDelete) {
        if (pageToDelete.isFolder()) {
            pageQueryService
                .searchByApiIdAndParentId(api.getId(), pageToDelete.getId())
                .stream()
                .findAny()
                .ifPresent(childPage -> {
                    throw new ApiFolderNotEmptyException(pageToDelete.getId());
                });
        }
    }

    private void throwIfPageUsedAsGeneralCondition(Api api, Page pageToDelete) {
        if (!pageToDelete.isFolder()) {
            planQueryService
                .findAllByApiIdAndGeneralConditionsAndIsActive(api.getId(), api.getDefinitionVersion(), pageToDelete.getId())
                .stream()
                .findFirst()
                .ifPresent(plan -> {
                    throw new ApiPageUsedAsGeneralConditionException(pageToDelete.getId(), plan.getId());
                });
        }
    }

    private void throwIfNotApiPage(Page pageToDelete) {
        if (!pageToDelete.getReferenceType().equals(Page.ReferenceType.API)) {
            throw new ApiPageInvalidReferenceTypeException(pageToDelete.getId(), Page.ReferenceType.API.name());
        }
    }
}
