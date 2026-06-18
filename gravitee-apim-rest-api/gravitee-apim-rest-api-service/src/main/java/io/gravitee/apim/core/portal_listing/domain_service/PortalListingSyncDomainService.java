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
package io.gravitee.apim.core.portal_listing.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.DeleteStrategy;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanner;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_documentation.domain_service.navigation.DocumentationNavigationIds;
import io.gravitee.apim.core.portal_documentation.domain_service.navigation.DocumentationNavigationPageMapper;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.domain_service.ApiDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.navigation.ApiDocumentationNavigationIds;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.Slug;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

/**
 * Materializes a {@link PortalListing} into the navigation tree.
 *
 * <p>For each api entry, upserts one {@link PortalNavigationApi} row parented under the deterministic folder
 * for {@code entry.location()}. Folder existence is not checked: the parent id is computed as the same hash
 * the portal folder sync uses, so the row appears live once the matching folder exists and stays orphan
 * (invisible) otherwise.
 *
 * <p>After upserting each nav-api row, if the api carries a {@code portalNavigation} field, every path is
 * "mkdir -p"-materialized as a folder hierarchy under the nav-api row. Then Rule 2 backfill runs: every
 * existing API-attached {@link io.gravitee.apim.core.portal_page.model.PortalPageContent} is re-materialized
 * so doc pages appear under the matching api-folder (or directly under the nav-api row when {@code location}
 * doesn't match any folder).
 *
 * <p>On listing update, entries that disappeared from the new spec are dematerialized: the corresponding
 * {@link PortalNavigationApi} row and every descendant (api folders + doc pages) are cascade-removed. On
 * listing delete the full set is dematerialized via {@link #dematerialize}. {@code PortalPageContent} rows
 * are never touched — the orphan-tolerant design lets the next reconciliation re-materialize them.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class PortalListingSyncDomainService {

    private static final PortalArea AREA = PortalArea.TOP_NAVBAR;

    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalPageContentQueryService portalPageContentQueryService;
    private final ApiDocumentationSyncDomainService apiDocumentationSyncDomainService;
    private final ApiCrudService apiCrudService;
    private final NavigationSyncPlanExecutor planExecutor;
    private final AutomationManagedNavigationItemsQueryService automationManagedNavigationItemsQueryService;

    public void sync(AuditInfo auditInfo, PortalId portalId, List<PortalListingApiEntry> previousEntries, PortalListing listing) {
        Set<String> newHrids = listing.getApis().stream().map(PortalListingApiEntry::apiHrid).collect(Collectors.toSet());
        previousEntries
            .stream()
            .filter(entry -> !newHrids.contains(entry.apiHrid()))
            .forEach(entry -> dematerializeNavApi(auditInfo, portalId, entry.apiId(auditInfo)));

        for (var entry : listing.getApis()) {
            var apiId = entry.apiId(auditInfo);
            var navApi = upsertNavApi(auditInfo, portalId, apiId, entry);
            syncApiNavFolders(auditInfo, navApi, apiId, List.of());

            portalPageContentQueryService
                .findByReference(auditInfo.environmentId(), AutomationMetadata.ReferenceType.API, apiId)
                .forEach(pc -> apiDocumentationSyncDomainService.materialize(auditInfo, pc));
        }
    }

    /**
     * Syncs the api-folder subtree under every nav-api row for this api against the api's current
     * {@code portalNavigation}. Folders present in {@code previousPaths} but absent from the new navigation
     * are removed; folders not managed by automation (not in {@code previousPaths}) are left untouched.
     * Then re-materializes all api-attached documentation rows.
     */
    public void syncApiFolders(AuditInfo auditInfo, String apiId, List<NavigationPath> previousPaths) {
        var navApiRows = navigationItemsQueryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(auditInfo.environmentId())
                    .type(PortalNavigationItemType.API)
                    .apiIds(Set.of(apiId))
                    .build()
            )
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .toList();

        navApiRows.forEach(navApi -> syncApiNavFolders(auditInfo, navApi, apiId, previousPaths));

        portalPageContentQueryService
            .findByReference(auditInfo.environmentId(), AutomationMetadata.ReferenceType.API, apiId)
            .forEach(pc -> apiDocumentationSyncDomainService.materialize(auditInfo, pc));
    }

    public void dematerialize(AuditInfo auditInfo, PortalId portalId, PortalListing listing) {
        for (var entry : listing.getApis()) {
            dematerializeNavApi(auditInfo, portalId, entry.apiId(auditInfo));
        }
    }

    private void dematerializeNavApi(AuditInfo auditInfo, PortalId portalId, String apiId) {
        var navApiId = DocumentationNavigationIds.navigationApiId(auditInfo, portalId.toString(), apiId);
        apiDocumentationSyncDomainService.cleanupNavApi(auditInfo, navApiId);
    }

    private void syncApiNavFolders(AuditInfo auditInfo, PortalNavigationApi navApi, String apiId, List<NavigationPath> previousPaths) {
        var maybeApi = apiCrudService.findById(apiId);
        List<NavigationPath> desired = maybeApi.isEmpty() || maybeApi.get().getPortalNavigation() == null
            ? List.of()
            : maybeApi.get().getPortalNavigation();
        var currentFolders = collectFolderDescendants(auditInfo.environmentId(), navApi.getId());
        var plan = NavigationSyncPlanner.plan(desired, currentFolders, previousPaths);
        var deleteStrategy = createDeleteStrategy(auditInfo, navApi, apiId);
        planExecutor.execute(plan, auditInfo, navApi, path -> apiFolderId(auditInfo, navApi.getId(), path), deleteStrategy);
    }

    private DeleteStrategy createDeleteStrategy(AuditInfo auditInfo, PortalNavigationApi navApi, String apiId) {
        var skipIds = automationManagedNavigationItemsQueryService.automationManagedApiDocPages(auditInfo, navApi, apiId);
        return new DeleteStrategy(item -> skipIds.contains(item.getId()), true);
    }

    private PortalNavigationItemId apiFolderId(AuditInfo auditInfo, PortalNavigationItemId navApiId, String path) {
        return ApiDocumentationNavigationIds.folderUnder(auditInfo, navApiId, path);
    }

    private List<PortalNavigationItem> collectFolderDescendants(String environmentId, PortalNavigationItemId parentId) {
        List<PortalNavigationItem> result = new ArrayList<>();
        for (var child : navigationItemsQueryService.findByParentIdAndEnvironmentId(environmentId, parentId)) {
            if (child instanceof PortalNavigationFolder) {
                result.add(child);
                result.addAll(collectFolderDescendants(environmentId, child.getId()));
            }
        }
        return result;
    }

    private PortalNavigationApi upsertNavApi(AuditInfo auditInfo, PortalId portalId, String apiId, PortalListingApiEntry entry) {
        var navApiId = DocumentationNavigationIds.navigationApiId(auditInfo, portalId.toString(), apiId);
        var parent = resolveParent(auditInfo, portalId.toString(), entry.location());

        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), navApiId);
        if (existing instanceof PortalNavigationApi navApi) {
            applyNavApiUpdate(navApi, apiId, entry, parent);
            return (PortalNavigationApi) navigationItemCrudService.update(navApi);
        }
        if (existing != null) {
            navigationItemCrudService.delete(navApiId);
        }
        var create = CreatePortalNavigationItem.builder()
            .id(navApiId)
            .title(entry.apiHrid())
            .segment(Slug.from(entry.apiHrid()).value())
            .area(AREA)
            .type(PortalNavigationItemType.API)
            .order(entry.order() != null ? entry.order() : 0)
            .apiId(apiId)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();
        return (PortalNavigationApi) navigationItemCrudService.create(
            PortalNavigationItem.from(create, auditInfo.organizationId(), auditInfo.environmentId(), parent)
        );
    }

    private static void applyNavApiUpdate(
        PortalNavigationApi navApi,
        String apiId,
        PortalListingApiEntry entry,
        PortalNavigationItemContainer parent
    ) {
        navApi.setApiId(apiId);
        navApi.setOrder(entry.order() != null ? entry.order() : 0);
        navApi.setTitle(entry.apiHrid());
        navApi.setSegment(Slug.from(entry.apiHrid()).value());
        navApi.setVisibility(PortalVisibility.PUBLIC);
        navApi.setPublished(true);
        if (parent == null) {
            navApi.markAsRoot();
        } else {
            navApi.updateParent(parent);
        }
    }

    private PortalNavigationItemContainer resolveParent(AuditInfo auditInfo, String portalId, String location) {
        var folderId = DocumentationNavigationIds.folderId(auditInfo, portalId, location);
        if (folderId == null) {
            return null;
        }
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(auditInfo.environmentId(), folderId);
        if (existing instanceof PortalNavigationItemContainer container) {
            return container;
        }
        return DocumentationNavigationPageMapper.phantomParent(folderId);
    }
}
