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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.domain_service.reconciliation.HomepageReconciler;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.slug.model.Slug;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalDocumentationSyncDomainService {

    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final HomepageReconciler homepageReconciler;

    public void materialize(AuditInfo auditInfo, PortalPageContent<?> pageContent, PortalArea targetArea) {
        var navigationItemId = PortalNavigationItemId.forPortalDocumentationContent(auditInfo, pageContent);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navigationItemId);
        upsertNavigationPage(auditInfo, pageContent, navigationItemId, existing, targetArea);
    }

    public void materialize(AuditInfo auditInfo, PortalPageContent<?> pageContent) {
        var navigationItemId = PortalNavigationItemId.forPortalDocumentationContent(auditInfo, pageContent);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navigationItemId);
        var targetArea = existing instanceof PortalNavigationPage page ? page.getArea() : PortalArea.TOP_NAVBAR;
        upsertNavigationPage(auditInfo, pageContent, navigationItemId, existing, targetArea);
    }

    public void dematerialize(AuditInfo auditInfo, String portalId, PortalPageContentId pageContentId) {
        final var navigationItemId = PortalNavigationItemId.forPortalDocumentation(auditInfo, portalId, pageContentId);
        final var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navigationItemId);
        if (existing != null) {
            navigationItemCrudService.delete(navigationItemId);
        }
    }

    private void upsertNavigationPage(
        AuditInfo auditInfo,
        PortalPageContent<?> pageContent,
        PortalNavigationItemId navigationItemId,
        PortalNavigationItem existing,
        PortalArea targetArea
    ) {
        final var meta = pageContent.getAutomationMetadata();
        final var parent = resolveParent(auditInfo, meta.location().orElse(null), meta.referenceId());
        final var parentId = parent == null ? null : parent.getId();

        if (isUpdatableInPlace(existing, targetArea)) {
            var page = (PortalNavigationPage) existing;
            final var segment = Slug.from(meta.name(), siblingsSlugs(auditInfo.environmentId(), parentId, navigationItemId));
            page.update(meta, parent, segment);
            navigationItemCrudService.update(page);
            return;
        }
        if (existing != null) {
            navigationItemCrudService.delete(navigationItemId);
        }
        if (targetArea == PortalArea.HOMEPAGE) {
            homepageReconciler.dropStaleHomepages(auditInfo.environmentId(), meta.referenceId(), navigationItemId);
        }
        final var segment = Slug.from(meta.name(), siblingsSlugs(auditInfo.environmentId(), parentId, null));
        var create = CreatePortalNavigationItem.builder()
            .id(navigationItemId)
            .title(meta.name())
            .segment(segment.value())
            .area(targetArea)
            .type(PortalNavigationItemType.PAGE)
            .order(meta.order().orElse(0))
            .portalPageContentId(pageContent.getId())
            .reference(meta.reference())
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();
        navigationItemCrudService.create(PortalNavigationItem.from(create, auditInfo.organizationId(), auditInfo.environmentId(), parent));
    }

    private static boolean isUpdatableInPlace(PortalNavigationItem existing, PortalArea targetArea) {
        return existing instanceof PortalNavigationPage page && page.getArea() == targetArea;
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
        var folderId = PortalNavigationItemId.forPortalFolder(auditInfo, portalId, location);
        if (folderId == null) return null;
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), folderId);
        if (existing instanceof PortalNavigationItemContainer container) {
            return container;
        }
        return PortalNavigationItemContainer.phantom(folderId);
    }
}
