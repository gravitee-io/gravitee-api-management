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
import io.gravitee.apim.core.portal_documentation.domain_service.navigation.DocumentationNavigationPageMapper;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.Slug;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Materializes an API-attached Documentation into the navigation tree.
 *
 * <p>For every {@link PortalNavigationApi} row that exists for the API in the environment (created by a
 * {@code PortalListing}), creates/updates a {@link PortalNavigationPage}. When the doc's {@code location}
 * points to a folder materialized from {@code api.portalNavigation}, the page parents there; otherwise the
 * page parents directly under the nav-api row (orphan-tolerant: a phantom parent is set so the page
 * reconnects once the folder is materialized).
 *
 * <p>If no listing exists yet, the scan returns zero rows and the doc sits idle in {@code portal_page_contents}.
 * When a listing later creates a nav-api row, {@code PortalListingSyncDomainService} calls {@link #materialize}
 * again (Rule 2 backfill) so the new rows get populated regardless of apply order.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ApiDocumentationSyncDomainService {

    private static final int MAX_CASCADE_DEPTH = 50;

    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalPageContentQueryService portalPageContentQueryService;

    public void materialize(AuditInfo auditInfo, PortalPageContent<?> pageContent) {
        var meta = pageContent.getAutomationMetadata();
        var apiId = meta.referenceId();
        var contentId = pageContent.getId();
        for (var navApi : findNavApiRows(auditInfo.environmentId(), apiId)) {
            var pageId = PortalNavigationItemId.forApiDocumentation(auditInfo, navApi.getId(), contentId);
            var parent = resolveParent(auditInfo, navApi, meta.location().orElse(null));
            upsertNavPage(auditInfo, pageId, contentId, parent, meta);
        }
    }

    public void dematerialize(AuditInfo auditInfo, String apiId, PortalPageContentId contentId) {
        for (var navApi : findNavApiRows(auditInfo.environmentId(), apiId)) {
            var pageId = PortalNavigationItemId.forApiDocumentation(auditInfo, navApi.getId(), contentId);
            var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), pageId);
            if (existing != null) {
                navigationItemCrudService.delete(pageId);
            }
        }
    }

    /**
     * Cleans up navigation rows materialized by the API itself (api-folder subtree + phantom-parented doc pages).
     * Nav-api rows are owned by their {@code PortalListing} and {@code PortalPageContent} rows by their
     * {@code Documentation}, so neither is touched here.
     */
    public void cleanupForApi(AuditInfo auditInfo, String apiId) {
        for (var navApi : findNavApiRows(auditInfo.environmentId(), apiId)) {
            cascadeDeleteNavApiDescendants(auditInfo, navApi.getId(), apiId);
        }
    }

    /**
     * Cleans up a single {@link PortalNavigationApi} row and every descendant underneath it. Used by listing
     * sync when an entry is removed from a listing spec, and indirectly by {@link #cleanupForApi}.
     */
    public void cleanupNavApi(AuditInfo auditInfo, PortalNavigationItemId navApiId) {
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navApiId);
        if (!(existing instanceof PortalNavigationApi navApi)) {
            return;
        }
        cascadeDeleteNavApi(auditInfo, navApiId, navApi.getApiId());
    }

    private void cascadeDeleteNavApiDescendants(AuditInfo auditInfo, PortalNavigationItemId navApiId, String apiId) {
        portalPageContentQueryService
            .findByReference(auditInfo.environmentId(), AutomationMetadata.ReferenceType.API, apiId)
            .forEach(pc -> {
                var pageId = PortalNavigationItemId.forApiDocumentation(auditInfo, navApiId, pc.getId());
                if (navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), pageId) != null) {
                    navigationItemCrudService.delete(pageId);
                }
            });
        cascadeDeleteDescendants(auditInfo.environmentId(), navApiId, 0);
    }

    private void cascadeDeleteNavApi(AuditInfo auditInfo, PortalNavigationItemId navApiId, String apiId) {
        cascadeDeleteNavApiDescendants(auditInfo, navApiId, apiId);
        if (navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navApiId) != null) {
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

    private PortalNavigationItemContainer resolveParent(AuditInfo auditInfo, PortalNavigationApi navApi, String location) {
        if (location == null || location.isBlank() || "/".equals(location)) {
            return navApi;
        }
        var folderId = PortalNavigationItemId.forApiFolder(auditInfo, navApi.getId(), location);
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), folderId);
        if (existing instanceof PortalNavigationItemContainer container) {
            return container;
        }
        return DocumentationNavigationPageMapper.phantomParent(folderId);
    }

    private List<PortalNavigationApi> findNavApiRows(String environmentId, String apiId) {
        return navigationItemsQueryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(environmentId)
                    .type(PortalNavigationItemType.API)
                    .apiIds(Set.of(apiId))
                    .build()
            )
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .toList();
    }

    private void upsertNavPage(
        AuditInfo auditInfo,
        PortalNavigationItemId pageId,
        PortalPageContentId contentId,
        PortalNavigationItemContainer parent,
        AutomationMetadata meta
    ) {
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), pageId);
        var parentId = parent == null ? null : parent.getId();

        if (existing instanceof PortalNavigationPage page) {
            var segment = Slug.from(meta.name(), siblingSlugs(auditInfo.environmentId(), parentId, pageId));
            DocumentationNavigationPageMapper.apply(page, contentId, parent, meta, segment);
            navigationItemCrudService.update(page);
            return;
        }
        if (existing != null) {
            navigationItemCrudService.delete(pageId);
        }
        var segment = Slug.from(meta.name(), siblingSlugs(auditInfo.environmentId(), parentId, null));
        navigationItemCrudService.create(
            DocumentationNavigationPageMapper.build(
                pageId,
                contentId,
                parent,
                auditInfo.organizationId(),
                auditInfo.environmentId(),
                meta,
                segment
            )
        );
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
