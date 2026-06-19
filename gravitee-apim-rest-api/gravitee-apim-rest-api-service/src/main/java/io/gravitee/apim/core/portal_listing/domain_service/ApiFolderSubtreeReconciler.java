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
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationTreeWalker;
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationVisitor;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationOwnership;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanner;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

/** Reconciles the api-folder subtree under a {@link PortalNavigationApi} row against the api's {@code portalNavigation}. */
@DomainService
@RequiredArgsConstructor
class ApiFolderSubtreeReconciler {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final ApiCrudService apiCrudService;
    private final NavigationSyncPlanExecutor planExecutor;
    private final AutomationManagedNavigationItemsQueryService automationManagedNavigationItemsQueryService;

    void sync(
        AuditInfo auditInfo,
        PortalNavigationApi navApi,
        String apiId,
        List<NavigationPath> previousPaths,
        List<PortalNavigationItem> currentFolders
    ) {
        var desired = desiredPaths(apiId);
        var ownership = ownership(auditInfo, navApi, apiId, desired);
        var plan = NavigationSyncPlanner.plan(desired, currentFolders, previousPaths, ownership);
        Function<String, PortalNavigationItemId> idMapper = path -> apiFolderId(auditInfo, navApi.getId(), path);
        var deleteStrategy = ownership.asDeleteStrategy();
        planExecutor.execute(plan, auditInfo, navApi, idMapper, deleteStrategy);
    }

    void validateConflicts(
        AuditInfo auditInfo,
        PortalNavigationApi navApi,
        String apiId,
        List<NavigationPath> desired,
        List<PortalNavigationItem> currentFolders
    ) {
        var safeDesired = desired == null ? List.<NavigationPath>of() : desired;
        var ownership = ownership(auditInfo, navApi, apiId, safeDesired);
        NavigationSyncPlanner.plan(safeDesired, currentFolders, safeDesired, ownership);
    }

    List<PortalNavigationItem> loadAllFoldersInEnv(String environmentId) {
        return navigationItemsQueryService.search(
            PortalNavigationItemQueryCriteria.builder().environmentId(environmentId).type(PortalNavigationItemType.FOLDER).build()
        );
    }

    List<PortalNavigationItem> collectFolderDescendantsFrom(List<PortalNavigationItem> envFolders, PortalNavigationItemId rootId) {
        var collector = new FolderCollector();
        PortalNavigationTreeWalker.walkFrom(envFolders, rootId, collector);
        return collector.collected();
    }

    List<PortalNavigationApi> navApiRowsFor(AuditInfo auditInfo, String apiId) {
        return navigationItemsQueryService
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
    }

    List<NavigationPath> desiredPaths(String apiId) {
        return apiCrudService
            .findById(apiId)
            .map(api -> api.getPortalNavigation() == null ? List.<NavigationPath>of() : api.getPortalNavigation())
            .orElseGet(List::of);
    }

    private NavigationOwnership ownership(AuditInfo auditInfo, PortalNavigationApi navApi, String apiId, List<NavigationPath> paths) {
        return new NavigationOwnership(
            NavigationSyncPlanner.expandToFullPaths(paths),
            path -> apiFolderId(auditInfo, navApi.getId(), path),
            automationManagedNavigationItemsQueryService.automationManagedApiDocPages(auditInfo, navApi, apiId),
            Set.of()
        );
    }

    private PortalNavigationItemId apiFolderId(AuditInfo auditInfo, PortalNavigationItemId navApiId, String path) {
        return PortalNavigationItemId.forApiFolder(auditInfo, navApiId, path);
    }

    private static final class FolderCollector implements PortalNavigationVisitor {

        private final List<PortalNavigationItem> collected = new ArrayList<>();

        @Override
        public void visitFolder(PortalNavigationFolder folder, NavigationPath parentPath) {
            collected.add(folder);
        }

        List<PortalNavigationItem> collected() {
            return collected;
        }
    }
}
