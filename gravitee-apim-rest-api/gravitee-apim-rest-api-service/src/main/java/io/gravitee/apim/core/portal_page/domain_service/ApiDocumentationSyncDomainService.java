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
import java.util.HashSet;
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
        var resolved = resolvePlacement(auditInfo, pageContent, callerVisibility);
        apply(resolved.plan(), auditInfo, resolved.pageId(), resolved.parent(), resolved.automationMetadata(), resolved.existing());
    }

    public void validatePlacement(AuditInfo auditInfo, PortalPageContent<?> pageContent, PortalVisibility callerVisibility) {
        resolvePlacement(auditInfo, pageContent, callerVisibility);
    }

    private ResolvedPlacement resolvePlacement(AuditInfo auditInfo, PortalPageContent<?> pageContent, PortalVisibility callerVisibility) {
        var automationMetadata = pageContent.getAutomationMetadata();
        var apiId = automationMetadata.referenceId();
        var contentId = pageContent.getId();
        var pageId = PortalNavigationItemId.forApiDocumentation(auditInfo, apiId, contentId);
        var parent = resolveParent(auditInfo, apiId, automationMetadata.location().orElse(null));
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), pageId);
        var plan = plan(auditInfo, pageId, contentId, parent, automationMetadata, callerVisibility, existing);
        validate(plan, auditInfo.environmentId(), pageId, contentId, existing);
        return new ResolvedPlacement(plan, pageId, parent, automationMetadata, existing);
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

    private sealed interface NavPagePlan {}

    private record UpdateInPlace(PortalNavigationPage page, UpdatePortalNavigationItem update) implements NavPagePlan {}

    private record CreateNew(CreatePortalNavigationItem create) implements NavPagePlan {}

    private record ResolvedPlacement(
        NavPagePlan plan,
        PortalNavigationItemId pageId,
        PortalNavigationItemContainer parent,
        AutomationMetadata automationMetadata,
        PortalNavigationItem existing
    ) {}

    private NavPagePlan plan(
        AuditInfo auditInfo,
        PortalNavigationItemId pageId,
        PortalPageContentId contentId,
        PortalNavigationItemContainer parent,
        AutomationMetadata automationMetadata,
        PortalVisibility callerVisibility,
        PortalNavigationItem existing
    ) {
        final var envId = auditInfo.environmentId();
        var parentId = parent == null ? null : parent.getId();
        var fallbackVisibility = Optional.ofNullable(existing)
            .map(PortalNavigationItem::getVisibility)
            .or(() -> Optional.ofNullable(parent).map(PortalNavigationItemContainer::getVisibility))
            .orElse(null);
        var visibility = PortalVisibility.resolve(callerVisibility, fallbackVisibility);

        if (existing instanceof PortalNavigationPage page && page.getArea() == API_DOCUMENTATION_AREA) {
            var segment = Slug.from(automationMetadata.name(), siblingSlugs(envId, parentId, pageId));
            var update = UpdatePortalNavigationItem.builder()
                .title(automationMetadata.name())
                .segment(segment.value())
                .type(TYPE)
                .order(automationMetadata.order().orElse(DEFAULT_ORDER))
                .parentId(parentId)
                .visibility(visibility)
                .published(DEFAULT_PUBLISHED)
                .build();
            return new UpdateInPlace(page, update);
        }

        var segment = Slug.from(automationMetadata.name(), siblingSlugs(envId, parentId, null));
        var create = CreatePortalNavigationItem.builder()
            .id(pageId)
            .title(automationMetadata.name())
            .segment(segment.value())
            .area(API_DOCUMENTATION_AREA)
            .type(TYPE)
            .order(automationMetadata.order().orElse(DEFAULT_ORDER))
            .portalPageContentId(contentId)
            .parentId(parentId)
            .reference(automationMetadata.reference())
            .visibility(visibility)
            .published(DEFAULT_PUBLISHED)
            .automationMetadata(automationMetadata.trimmedForNavItem())
            .build();
        return new CreateNew(create);
    }

    private void validate(
        NavPagePlan plan,
        String environmentId,
        PortalNavigationItemId pageId,
        PortalPageContentId contentId,
        PortalNavigationItem existing
    ) {
        switch (plan) {
            case UpdateInPlace(var page, var update) -> validatorService.validateToUpdate(update, page);
            case CreateNew(var create) -> {
                var itemIdsBeingReplaced = new HashSet<PortalNavigationItemId>();
                if (existing != null) {
                    itemIdsBeingReplaced.add(pageId);
                }
                validatorService.validateOne(create, environmentId, Set.of(contentId), itemIdsBeingReplaced);
            }
        }
    }

    private void apply(
        NavPagePlan plan,
        AuditInfo auditInfo,
        PortalNavigationItemId pageId,
        PortalNavigationItemContainer parent,
        AutomationMetadata automationMetadata,
        PortalNavigationItem existing
    ) {
        switch (plan) {
            case UpdateInPlace(var page, var update) -> {
                page.update(update, automationMetadata.trimmedForNavItem());
                page.attachTo(parent);
                navigationItemCrudService.update(page);
            }
            case CreateNew(var create) -> {
                if (existing != null) {
                    navigationItemCrudService.delete(pageId);
                }
                navigationItemCrudService.create(
                    PortalNavigationItem.from(create, auditInfo.organizationId(), auditInfo.environmentId(), parent)
                );
            }
        }
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
