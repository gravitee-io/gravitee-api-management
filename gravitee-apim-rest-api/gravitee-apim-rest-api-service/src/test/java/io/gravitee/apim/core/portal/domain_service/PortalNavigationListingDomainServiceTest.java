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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationListingDomainServiceTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private static final PortalId PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");

    private final PortalNavigationItemsCrudServiceInMemory crud = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory query = new PortalNavigationItemsQueryServiceInMemory(crud.storage());
    private final PortalPageContentCrudServiceInMemory pageContentCrud = new PortalPageContentCrudServiceInMemory();
    private final PortalPageContentQueryServiceInMemory pageContentQuery = new PortalPageContentQueryServiceInMemory();
    private final PortalListingCrudServiceInMemory portalListingCrud = new PortalListingCrudServiceInMemory();
    private PortalNavigationSyncDomainService syncService;
    private PortalNavigationListingDomainService listingService;

    @BeforeEach
    void setUp() {
        crud.reset();
        pageContentCrud.reset();
        pageContentQuery.reset();
        portalListingCrud.reset();
        syncService = new PortalNavigationSyncDomainService(
            query,
            new AutomationManagedNavigationItemsQueryService(portalListingCrud, pageContentQuery),
            new NavigationSyncPlanExecutor(crud, query, pageContentCrud)
        );
        listingService = new PortalNavigationListingDomainService(query);
    }

    @Test
    void list_as_navigation_paths_round_trips_input_order() {
        syncService.sync(
            AUDIT_INFO,
            PORTAL_ID,
            List.of(),
            List.of(
                new NavigationPath("/a", null),
                new NavigationPath("/a/b", null),
                new NavigationPath("/c", null),
                new NavigationPath("/c/d", null)
            )
        );

        var result = listingService.listAsNavigationPaths(AUDIT_INFO.environmentId());

        assertThat(result).extracting(NavigationPath::path).containsExactly("/a", "/a/b", "/c", "/c/d");
    }

    @Test
    void path_uses_segment_not_title_when_display_name_provided() {
        syncService.sync(
            AUDIT_INFO,
            PORTAL_ID,
            List.of(),
            List.of(new NavigationPath("/projects/alpha", "Alpha"), new NavigationPath("/projects/alpha/docs", null))
        );

        var result = listingService.listAsNavigationPaths(AUDIT_INFO.environmentId());

        assertThat(result).extracting(NavigationPath::path).containsExactly("/projects", "/projects/alpha", "/projects/alpha/docs");
    }

    @Test
    void display_name_is_surfaced_when_title_differs_from_segment() {
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/projects/alpha", "Alpha")));

        var result = listingService.listAsNavigationPaths(AUDIT_INFO.environmentId());

        assertThat(result)
            .filteredOn(p -> "/projects/alpha".equals(p.path()))
            .singleElement()
            .extracting(NavigationPath::displayName)
            .isEqualTo("Alpha");
    }

    @Test
    void display_name_is_null_when_title_equals_segment() {
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/a", null)));

        var result = listingService.listAsNavigationPaths(AUDIT_INFO.environmentId());

        assertThat(result).singleElement().extracting(NavigationPath::displayName).isNull();
    }
}
