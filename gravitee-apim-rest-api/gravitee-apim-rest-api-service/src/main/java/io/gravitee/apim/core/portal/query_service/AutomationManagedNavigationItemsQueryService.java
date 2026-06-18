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
package io.gravitee.apim.core.portal.query_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_documentation.domain_service.navigation.DocumentationNavigationIds;
import io.gravitee.apim.core.portal_listing.crud_service.PortalListingCrudService;
import io.gravitee.apim.core.portal_page.domain_service.navigation.ApiDocumentationNavigationIds;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Queries navigation items, scoped per portal and environment, that are managed by the automation API.
 * These items have dedicated materialize/dematerialize flows and should be left alone by cascade deletes
 * driven by other syncs.
 */
@DomainService
@RequiredArgsConstructor
public class AutomationManagedNavigationItemsQueryService {

    private final PortalListingCrudService portalListingCrudService;
    private final PortalPageContentQueryService portalPageContentQueryService;

    public Set<PortalNavigationItemId> activeListingApiRows(AuditInfo auditInfo, PortalId portalId) {
        return portalListingCrudService
            .findAllByPortalIdAndEnvironmentId(portalId, auditInfo.environmentId())
            .stream()
            .flatMap(listing -> listing.getApis().stream())
            .map(entry -> DocumentationNavigationIds.navigationApiId(auditInfo, portalId.toString(), entry.apiId(auditInfo)))
            .collect(Collectors.toSet());
    }

    public Set<PortalNavigationItemId> automationManagedPortalDocPages(AuditInfo auditInfo, PortalId portalId) {
        return portalPageContentQueryService
            .findByReference(auditInfo.environmentId(), AutomationMetadata.ReferenceType.PORTAL, portalId.toString())
            .stream()
            .filter(PortalPageContent::isAutomationManaged)
            .map(pc -> DocumentationNavigationIds.navigationItemId(auditInfo, portalId.toString(), pc.getId()))
            .collect(Collectors.toSet());
    }

    public Set<PortalNavigationItemId> automationManagedApiDocPages(AuditInfo auditInfo, PortalNavigationApi navApi, String apiId) {
        return portalPageContentQueryService
            .findByReference(auditInfo.environmentId(), AutomationMetadata.ReferenceType.API, apiId)
            .stream()
            .filter(PortalPageContent::isAutomationManaged)
            .map(pc -> ApiDocumentationNavigationIds.pageIdUnder(auditInfo, navApi.getId(), pc.getId()))
            .collect(Collectors.toSet());
    }
}
