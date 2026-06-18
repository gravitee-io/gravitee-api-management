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
import io.gravitee.apim.core.portal.domain_service.navigation.plan.DeleteStrategy;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanner;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_documentation.domain_service.navigation.DocumentationNavigationIds;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationSyncDomainService {

    private static final PortalArea AREA = PortalArea.TOP_NAVBAR;

    private final PortalNavigationItemsQueryService queryService;
    private final AutomationManagedNavigationItemsQueryService automationManagedNavigationItemsQueryService;
    private final NavigationSyncPlanExecutor planExecutor;

    public void sync(AuditInfo auditInfo, PortalId portalId, List<NavigationPath> previouslyPersisted, List<NavigationPath> desired) {
        final var currentFolders = queryService.search(
            PortalNavigationItemQueryCriteria.builder()
                .environmentId(auditInfo.environmentId())
                .area(AREA)
                .type(PortalNavigationItemType.FOLDER)
                .build()
        );
        final var plan = NavigationSyncPlanner.plan(
            desired == null ? List.of() : desired,
            currentFolders,
            previouslyPersisted == null ? List.of() : previouslyPersisted
        );
        final var strategy = createDeleteStrategy(auditInfo, portalId);
        planExecutor.execute(
            plan,
            auditInfo,
            null,
            path -> DocumentationNavigationIds.folderId(auditInfo, portalId.toString(), path),
            strategy
        );
    }

    private DeleteStrategy createDeleteStrategy(AuditInfo auditInfo, PortalId portalId) {
        Set<PortalNavigationItemId> skipIds = Stream.concat(
            automationManagedNavigationItemsQueryService.activeListingApiRows(auditInfo, portalId).stream(),
            automationManagedNavigationItemsQueryService.automationManagedPortalDocPages(auditInfo, portalId).stream()
        ).collect(Collectors.toSet());
        return new DeleteStrategy(item -> skipIds.contains(item.getId()), true);
    }
}
