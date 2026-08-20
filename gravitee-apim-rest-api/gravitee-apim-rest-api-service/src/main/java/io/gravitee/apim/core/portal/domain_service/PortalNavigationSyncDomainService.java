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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationSyncDomainService {

    private final PortalNavigationItemsQueryService queryService;
    private final AutomationManagedNavigationItemsQueryService automationManagedNavigationItemsQueryService;
    private final NavigationSyncPlanExecutor planExecutor;

    public void sync(
        AuditInfo auditInfo,
        PortalId portalId,
        PortalNavigationStructure previouslyPersisted,
        PortalNavigationStructure desired
    ) {
        for (var area : allAreas(previouslyPersisted, desired)) {
            syncArea(auditInfo, portalId, area, previouslyPersisted.forArea(area), desired.forArea(area));
        }
    }

    public void validateForConflicts(
        AuditInfo auditInfo,
        PortalId portalId,
        PortalNavigationStructure previouslyPersisted,
        PortalNavigationStructure desired
    ) {
        for (var area : allAreas(previouslyPersisted, desired)) {
            planForArea(auditInfo, portalId, area, previouslyPersisted.forArea(area), desired.forArea(area));
        }
    }

    private void syncArea(
        AuditInfo auditInfo,
        PortalId portalId,
        PortalArea area,
        List<NavigationPath> previousPaths,
        List<NavigationPath> desiredPaths
    ) {
        rejectUnsupportedArea(area);
        var ctx = buildSyncContext(auditInfo, portalId, area, previousPaths, desiredPaths);
        planExecutor.execute(
            buildSyncPlan(ctx, desiredPaths),
            auditInfo,
            area,
            null,
            NavigationItemReference.DEFAULT,
            path -> PortalNavigationItemId.forPortalFolder(auditInfo, portalId.toString(), path),
            ctx.ownership().asDeleteStrategy()
        );
    }

    private void planForArea(
        AuditInfo auditInfo,
        PortalId portalId,
        PortalArea area,
        List<NavigationPath> previousPaths,
        List<NavigationPath> desiredPaths
    ) {
        rejectUnsupportedArea(area);
        buildSyncPlan(buildSyncContext(auditInfo, portalId, area, previousPaths, desiredPaths), desiredPaths);
    }

    private static void rejectUnsupportedArea(PortalArea area) {
        if (area != PortalArea.TOP_NAVBAR) {
            throw new IllegalArgumentException("Setting navigation for " + area + " area is not allowed.");
        }
    }

    private static Set<PortalArea> allAreas(PortalNavigationStructure previouslyPersisted, PortalNavigationStructure desired) {
        Set<PortalArea> areas = new HashSet<>();
        areas.addAll(previouslyPersisted.areas().keySet());
        areas.addAll(desired.areas().keySet());
        return areas;
    }

    private NavigationSyncPlan buildSyncPlan(SyncContext ctx, List<NavigationPath> desired) {
        return NavigationSyncPlanner.plan(
            desired == null ? List.of() : desired,
            ctx.currentFolders,
            ctx.previouslyPersisted,
            ctx.ownership
        );
    }

    private SyncContext buildSyncContext(
        AuditInfo auditInfo,
        PortalId portalId,
        PortalArea area,
        List<NavigationPath> previouslyPersisted,
        List<NavigationPath> desired
    ) {
        var currentFolders = queryService.search(
            PortalNavigationItemQueryCriteria.builder()
                .environmentId(auditInfo.environmentId())
                .area(area)
                .type(PortalNavigationItemType.FOLDER)
                .build()
        );
        var safePrevious = previouslyPersisted == null ? List.<NavigationPath>of() : previouslyPersisted;
        var safeDesired = desired == null ? List.<NavigationPath>of() : desired;
        var ownership = new NavigationOwnership(
            NavigationSyncPlanner.expandToFullPaths(safeDesired),
            path -> PortalNavigationItemId.forPortalFolder(auditInfo, portalId.toString(), path),
            automationManagedNavigationItemsQueryService.automationManagedPortalDocPages(auditInfo, portalId),
            automationManagedNavigationItemsQueryService.activeListingApiRows(auditInfo, portalId),
            automationManagedNavigationItemsQueryService.automationManagedPortalLinks(auditInfo, portalId)
        );
        return new SyncContext(currentFolders, safePrevious, ownership);
    }

    private record SyncContext(
        List<PortalNavigationItem> currentFolders,
        List<NavigationPath> previouslyPersisted,
        NavigationOwnership ownership
    ) {}
}
