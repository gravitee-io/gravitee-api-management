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
package io.gravitee.apim.core.portal.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationOwnership;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlan;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanner;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationSyncDomainService {

    private static final PortalArea AREA = PortalArea.TOP_NAVBAR;

    private final PortalNavigationItemsQueryService queryService;
    private final AutomationManagedNavigationItemsQueryService automationManagedNavigationItemsQueryService;
    private final NavigationSyncPlanExecutor planExecutor;

    public void sync(AuditInfo auditInfo, PortalId portalId, List<NavigationPath> previouslyPersisted, List<NavigationPath> desired) {
        var ctx = buildSyncContext(auditInfo, portalId, previouslyPersisted, desired);
        executeSyncPlan(auditInfo, portalId, buildSyncPlan(ctx, desired), ctx);
    }

    public void validateForConflicts(
        AuditInfo auditInfo,
        PortalId portalId,
        List<NavigationPath> previouslyPersisted,
        List<NavigationPath> desired
    ) {
        buildSyncPlan(buildSyncContext(auditInfo, portalId, previouslyPersisted, desired), desired);
    }

    private NavigationSyncPlan buildSyncPlan(SyncContext ctx, List<NavigationPath> desired) {
        return NavigationSyncPlanner.plan(
            desired == null ? List.of() : desired,
            ctx.currentFolders,
            ctx.previouslyPersisted,
            ctx.ownership
        );
    }

    private void executeSyncPlan(AuditInfo auditInfo, PortalId portalId, NavigationSyncPlan plan, SyncContext ctx) {
        planExecutor.execute(
            plan,
            auditInfo,
            null,
            path -> PortalNavigationItemId.forPortalFolder(auditInfo, portalId.toString(), path),
            ctx.ownership.asDeleteStrategy()
        );
    }

    private SyncContext buildSyncContext(
        AuditInfo auditInfo,
        PortalId portalId,
        List<NavigationPath> previouslyPersisted,
        List<NavigationPath> desired
    ) {
        var currentFolders = queryService.search(
            PortalNavigationItemQueryCriteria.builder()
                .environmentId(auditInfo.environmentId())
                .area(AREA)
                .type(PortalNavigationItemType.FOLDER)
                .build()
        );
        var safePrevious = previouslyPersisted == null ? List.<NavigationPath>of() : previouslyPersisted;
        var safeDesired = desired == null ? List.<NavigationPath>of() : desired;
        var ownership = new NavigationOwnership(
            NavigationSyncPlanner.expandToFullPaths(safeDesired),
            path -> PortalNavigationItemId.forPortalFolder(auditInfo, portalId.toString(), path),
            automationManagedNavigationItemsQueryService.automationManagedPortalDocPages(auditInfo, portalId),
            automationManagedNavigationItemsQueryService.activeListingApiRows(auditInfo, portalId)
        );
        return new SyncContext(currentFolders, safePrevious, ownership);
    }

    private record SyncContext(
        List<PortalNavigationItem> currentFolders,
        List<NavigationPath> previouslyPersisted,
        NavigationOwnership ownership
    ) {}
}
