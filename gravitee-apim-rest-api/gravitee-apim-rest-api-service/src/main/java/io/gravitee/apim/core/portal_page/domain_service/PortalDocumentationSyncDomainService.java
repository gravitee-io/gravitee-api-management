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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_documentation.domain_service.navigation.DocumentationNavigationIds;
import io.gravitee.apim.core.portal_documentation.domain_service.navigation.DocumentationNavigationPageMapper;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.Slug;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalDocumentationSyncDomainService {

    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;

    public void materialize(AuditInfo auditInfo, PortalPageContent<?> pageContent) {
        final var meta = pageContent.getAutomationMetadata();
        final var portalId = meta.referenceId();
        final var contentId = pageContent.getId();
        final var navigationItemId = DocumentationNavigationIds.navigationItemId(auditInfo, portalId, contentId);
        final var parent = resolveParent(auditInfo, meta.location().orElse(null), portalId);
        upsertNavigationPage(auditInfo, navigationItemId, contentId, parent, meta);
    }

    public void dematerialize(AuditInfo auditInfo, String portalId, PortalPageContentId pageContentId) {
        final var navigationItemId = DocumentationNavigationIds.navigationItemId(auditInfo, portalId, pageContentId);
        final var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navigationItemId);
        if (existing != null) {
            navigationItemCrudService.delete(navigationItemId);
        }
    }

    private void upsertNavigationPage(
        AuditInfo auditInfo,
        PortalNavigationItemId navigationItemId,
        PortalPageContentId contentId,
        PortalNavigationItemContainer parent,
        AutomationMetadata meta
    ) {
        final var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navigationItemId);
        final var parentId = parent == null ? null : parent.getId();

        if (existing instanceof PortalNavigationPage page) {
            final var segment = Slug.from(meta.name(), siblingsSlugs(auditInfo.environmentId(), parentId, navigationItemId));
            DocumentationNavigationPageMapper.apply(page, contentId, parent, meta, segment);
            navigationItemCrudService.update(page);
            return;
        }
        if (existing != null) {
            navigationItemCrudService.delete(navigationItemId);
        }
        final var segment = Slug.from(meta.name(), siblingsSlugs(auditInfo.environmentId(), parentId, null));
        navigationItemCrudService.create(
            DocumentationNavigationPageMapper.build(
                navigationItemId,
                contentId,
                parent,
                auditInfo.organizationId(),
                auditInfo.environmentId(),
                meta,
                segment
            )
        );
    }

    private Set<Slug> siblingsSlugs(String environmentId, PortalNavigationItemId parentId, PortalNavigationItemId excludeId) {
        return navigationItemsQueryService
            .findByParentIdAndEnvironmentId(environmentId, parentId)
            .stream()
            .filter(item -> !item.getId().equals(excludeId))
            .map(PortalNavigationItem::getSegment)
            .map(Slug::new)
            .collect(Collectors.toSet());
    }

    private PortalNavigationItemContainer resolveParent(AuditInfo auditInfo, String location, String portalId) {
        var folderId = DocumentationNavigationIds.folderId(auditInfo, portalId, location);
        if (folderId == null) return null;
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), folderId);
        if (existing instanceof PortalNavigationItemContainer container) {
            return container;
        }
        return DocumentationNavigationPageMapper.phantomParent(folderId);
    }
}
