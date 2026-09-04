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
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.slug.model.Slug;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Materializes an API-attached Documentation into the navigation tree, keyed on the API itself.
 *
 * <p>The nav page is upserted unconditionally — no portal or listing needs to exist. When the doc's
 * {@code location} points to a folder materialized from {@code api.portalNavigation}, the page parents
 * there (orphan-tolerant: a phantom parent is set so the page reconnects once the folder is
 * materialized); an absent, blank, or {@code "/"} location makes the page a root of the API's own
 * subtree instead.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ApiDocumentationSyncDomainService {

    private static final int MAX_CASCADE_DEPTH = 50;
    private static final PortalArea API_DOCUMENTATION_AREA = PortalArea.TOP_NAVBAR;
    private static final PortalNavigationItemType TYPE = PortalNavigationItemType.PAGE;
    private static final int DEFAULT_ORDER = 0;
    private static final boolean DEFAULT_PUBLISHED = true;

    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalNavigationItemValidatorService validatorService;

    public void materialize(AuditInfo auditInfo, PortalPageContent<?> pageContent) {
        materialize(auditInfo, pageContent, null);
    }

    public void materialize(AuditInfo auditInfo, PortalPageContent<?> pageContent, PortalVisibility callerVisibility) {
        var meta = pageContent.getAutomationMetadata();
        var apiId = meta.referenceId();
        var contentId = pageContent.getId();
        var pageId = PortalNavigationItemId.forApiDocumentation(auditInfo, apiId, contentId);
        var parent = resolveParent(auditInfo, apiId, meta.location().orElse(null));
        upsertNavPage(auditInfo, pageId, contentId, parent, meta, callerVisibility);
    }

    public void dematerialize(AuditInfo auditInfo, String apiId, PortalPageContentId contentId) {
        var pageId = PortalNavigationItemId.forApiDocumentation(auditInfo, apiId, contentId);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), pageId);
        if (existing != null) {
            navigationItemCrudService.delete(pageId);
        }
    }

    /**
     * Cleans up the API's own subtree — its folder subtree, its documentation pages, and its links, wherever
     * they are rooted — enumerated by reference rather than by iterating nav-api rows: the subtree belongs to
     * the API, not to any listing. Nav-api rows are owned by their {@code PortalListing}, so none are touched here.
     */
    public void cleanupForApi(AuditInfo auditInfo, String apiId) {
        var reference = new NavigationItemReference.ApiReference(apiId);
        var envFolders = navigationItemsQueryService.search(
            PortalNavigationItemQueryCriteria.builder()
                .environmentId(auditInfo.environmentId())
                .type(PortalNavigationItemType.FOLDER)
                .build()
        );
        envFolders
            .stream()
            .filter(folder -> reference.equals(folder.getReference()))
            .filter(PortalNavigationItem::isRoot)
            .forEach(root -> {
                cascadeDeleteDescendants(auditInfo.environmentId(), root.getId(), 0);
                navigationItemCrudService.delete(root.getId());
            });
        navigationItemsQueryService
            .findByAutomationReference(auditInfo.environmentId(), AutomationMetadata.ReferenceType.API, apiId)
            .stream()
            .filter(item -> item.getType() == PortalNavigationItemType.PAGE || item.getType() == PortalNavigationItemType.LINK)
            .forEach(item -> navigationItemCrudService.delete(item.getId()));
    }

    /**
     * Cleans up a single {@link PortalNavigationApi} row. The API's own subtree is not a descendant of this
     * row — it belongs to the API (see {@link #cleanupForApi}) — so removing a listing entry never takes it.
     */
    public void cleanupNavApi(AuditInfo auditInfo, PortalNavigationItemId navApiId) {
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navApiId);
        if (existing instanceof PortalNavigationApi) {
            navigationItemCrudService.delete(navApiId);
        }
    }

    private void cascadeDeleteDescendants(String environmentId, PortalNavigationItemId parentId, int depth) {
        if (depth > MAX_CASCADE_DEPTH) throw new IllegalStateException(
            "Maximum portal navigation nesting level of %d exceeded".formatted(MAX_CASCADE_DEPTH)
        );
        for (var child : navigationItemsQueryService.findByParentIdAndEnvironmentId(environmentId, parentId)) {
            cascadeDeleteDescendants(environmentId, child.getId(), depth + 1);
            navigationItemCrudService.delete(child.getId());
        }
    }

    private PortalNavigationItemContainer resolveParent(AuditInfo auditInfo, String apiId, String location) {
        if (location == null || location.isBlank() || "/".equals(location)) {
            return null;
        }
        var folderId = PortalNavigationItemId.forApiFolder(auditInfo, apiId, location);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), folderId);
        if (existing instanceof PortalNavigationItemContainer container) {
            return container;
        }
        return PortalNavigationItemContainer.phantom(folderId);
    }

    private void upsertNavPage(
        AuditInfo auditInfo,
        PortalNavigationItemId pageId,
        PortalPageContentId contentId,
        PortalNavigationItemContainer parent,
        AutomationMetadata meta,
        PortalVisibility callerVisibility
    ) {
        final var envId = auditInfo.environmentId();
        final var orgId = auditInfo.organizationId();
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(envId, pageId);
        var parentId = parent == null ? null : parent.getId();
        var fallbackVisibility = Optional.ofNullable(existing)
            .map(PortalNavigationItem::getVisibility)
            .or(() -> Optional.ofNullable(parent).map(PortalNavigationItemContainer::getVisibility))
            .orElse(null);
        var visibility = PortalVisibility.resolve(callerVisibility, fallbackVisibility);

        if (existing instanceof PortalNavigationPage page && page.getArea() == API_DOCUMENTATION_AREA) {
            var segment = Slug.from(meta.name(), siblingSlugs(envId, parentId, pageId));
            var update = UpdatePortalNavigationItem.builder()
                .title(meta.name())
                .segment(segment.value())
                .type(TYPE)
                .order(meta.order().orElse(DEFAULT_ORDER))
                .parentId(parentId)
                .visibility(visibility)
                .published(DEFAULT_PUBLISHED)
                .build();
            validatorService.validateToUpdate(update, page);
            page.update(update, meta.trimmedForNavItem());
            page.attachTo(parent);
            navigationItemCrudService.update(page);
            return;
        }
        if (existing != null) {
            navigationItemCrudService.delete(pageId);
        }
        var segment = Slug.from(meta.name(), siblingSlugs(envId, parentId, null));
        var create = CreatePortalNavigationItem.builder()
            .id(pageId)
            .title(meta.name())
            .segment(segment.value())
            .area(API_DOCUMENTATION_AREA)
            .type(TYPE)
            .order(meta.order().orElse(DEFAULT_ORDER))
            .portalPageContentId(contentId)
            .parentId(parentId)
            .reference(meta.reference())
            .visibility(visibility)
            .published(DEFAULT_PUBLISHED)
            .automationMetadata(meta.trimmedForNavItem())
            .build();
        validatorService.validateOne(create, envId);
        navigationItemCrudService.create(PortalNavigationItem.from(create, orgId, envId, parent));
    }

    private Set<Slug> siblingSlugs(String environmentId, PortalNavigationItemId parentId, PortalNavigationItemId excludeId) {
        if (parentId == null) {
            return Set.of();
        }
        return navigationItemsQueryService
            .findByParentIdAndEnvironmentId(environmentId, parentId)
            .stream()
            .filter(item -> !item.getId().equals(excludeId))
            .map(PortalNavigationItem::getSegment)
            .map(Slug::new)
            .collect(Collectors.toSet());
    }
}
