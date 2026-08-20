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
package io.gravitee.apim.core.portal.query_service;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AutomationManagedNavigationItemsQueryServiceTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private static final String API_ID = "00000000-0000-0000-0000-0000000000a1";
    private static final String PORTAL_HRID = "default-portal";
    private static final PortalId PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
    private static final PortalListingId LISTING_ID = PortalListingId.of(
        HRIDToUUID.portalListing().context(AUDIT_INFO).portal(PORTAL_HRID).hrid("default-listing").id()
    );

    private final PortalListingCrudServiceInMemory listingCrud = new PortalListingCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navigationItemsQuery = new PortalNavigationItemsQueryServiceInMemory();
    private final AutomationManagedNavigationItemsQueryService queryService = new AutomationManagedNavigationItemsQueryService(
        listingCrud,
        navigationItemsQuery
    );

    @BeforeEach
    void setUp() {
        listingCrud.reset();
        navigationItemsQuery.reset();
    }

    @Test
    void active_listing_api_rows_returns_deterministic_ids_for_every_entry_under_portal() {
        var petsApiId = HRIDToUUID.api().context(AUDIT_INFO).hrid("pets-api").id();
        var shopApiId = HRIDToUUID.api().context(AUDIT_INFO).hrid("shop-api").id();
        listingCrud.initWith(
            List.of(
                PortalListing.of(
                    LISTING_ID,
                    AUDIT_INFO.environmentId(),
                    AUDIT_INFO.organizationId(),
                    PORTAL_ID,
                    List.of(
                        new PortalListingApiEntry("pets-api", "/projects/alpha", 1),
                        new PortalListingApiEntry("shop-api", "/projects/beta", 2)
                    )
                )
            )
        );

        var result = queryService.activeListingApiRows(AUDIT_INFO, PORTAL_ID);

        assertThat(result).containsExactlyInAnyOrder(
            PortalNavigationItemId.forListingApi(AUDIT_INFO, PORTAL_ID.toString(), petsApiId),
            PortalNavigationItemId.forListingApi(AUDIT_INFO, PORTAL_ID.toString(), shopApiId)
        );
    }

    @Test
    void active_listing_api_rows_returns_empty_when_no_listings_exist() {
        assertThat(queryService.activeListingApiRows(AUDIT_INFO, PORTAL_ID)).isEmpty();
    }

    @Test
    void automation_managed_portal_doc_pages_returns_pages_with_matching_automation_metadata() {
        var managedPage = pageRow("about", automationMetadata(AutomationMetadata.ReferenceType.PORTAL, PORTAL_ID.toString()));
        var unmanagedPage = pageRow("manual", null);
        navigationItemsQuery.initWith(List.of(managedPage, unmanagedPage));

        var result = queryService.automationManagedPortalDocPages(AUDIT_INFO, PORTAL_ID);

        assertThat(result).containsExactly(managedPage.getId());
    }

    @Test
    void automation_managed_portal_doc_pages_excludes_links_with_the_same_automation_reference() {
        var meta = automationMetadata(AutomationMetadata.ReferenceType.PORTAL, PORTAL_ID.toString());
        var managedPage = pageRow("managed-page", meta);
        var managedLink = linkRow("managed-link", meta);
        navigationItemsQuery.initWith(List.of(managedPage, managedLink));

        var result = queryService.automationManagedPortalDocPages(AUDIT_INFO, PORTAL_ID);

        assertThat(result).containsExactly(managedPage.getId());
    }

    @Test
    void automation_managed_api_doc_pages_returns_pages_with_matching_automation_metadata() {
        var managedPage = pageRow("getting-started", automationMetadata(AutomationMetadata.ReferenceType.API, API_ID));
        var unmanagedPage = pageRow("manual", null);
        navigationItemsQuery.initWith(List.of(managedPage, unmanagedPage));

        var result = queryService.automationManagedApiDocPages(AUDIT_INFO, API_ID);

        assertThat(result).containsExactly(managedPage.getId());
    }

    @Test
    void automation_managed_api_doc_pages_excludes_links_with_the_same_automation_reference() {
        var meta = automationMetadata(AutomationMetadata.ReferenceType.API, API_ID);
        var managedPage = pageRow("managed-page", meta);
        var managedLink = linkRow("managed-link", meta);
        navigationItemsQuery.initWith(List.of(managedPage, managedLink));

        var result = queryService.automationManagedApiDocPages(AUDIT_INFO, API_ID);

        assertThat(result).containsExactly(managedPage.getId());
    }

    @Test
    void automation_managed_portal_links_returns_links_with_matching_automation_metadata() {
        var managedLink = linkRow("managed-link", automationMetadata(AutomationMetadata.ReferenceType.PORTAL, PORTAL_ID.toString()));
        var manualLink = linkRow("manual-link", null);
        navigationItemsQuery.initWith(List.of(managedLink, manualLink));

        var result = queryService.automationManagedPortalLinks(AUDIT_INFO, PORTAL_ID);

        assertThat(result).containsExactly(managedLink.getId());
    }

    @Test
    void automation_managed_portal_links_excludes_pages_with_the_same_automation_reference() {
        var meta = automationMetadata(AutomationMetadata.ReferenceType.PORTAL, PORTAL_ID.toString());
        var managedLink = linkRow("managed-link", meta);
        var managedPage = pageRow("managed-page", meta);
        navigationItemsQuery.initWith(List.of(managedLink, managedPage));

        var result = queryService.automationManagedPortalLinks(AUDIT_INFO, PORTAL_ID);

        assertThat(result).containsExactly(managedLink.getId());
    }

    @Test
    void automation_managed_portal_links_returns_empty_when_no_links_exist() {
        assertThat(queryService.automationManagedPortalLinks(AUDIT_INFO, PORTAL_ID)).isEmpty();
    }

    @Test
    void automationManagedApiLinks_returns_only_link_items_referencing_the_api() {
        var link = linkRow("external-docs", automationMetadata(AutomationMetadata.ReferenceType.API, API_ID));
        var page = pageRow("a-page", automationMetadata(AutomationMetadata.ReferenceType.API, API_ID));
        navigationItemsQuery.initWith(List.of(link, page));

        var result = queryService.automationManagedApiLinks(AUDIT_INFO, API_ID);

        assertThat(result).containsExactly(link.getId());
    }

    private static PortalNavigationLink linkRow(String title, AutomationMetadata metadata) {
        var link = PortalNavigationLink.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title(title)
            .segment(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .url("https://example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .automationMetadata(metadata)
            .build();
        link.markAsRoot();
        return link;
    }

    private static PortalNavigationPage pageRow(String title, AutomationMetadata metadata) {
        var page = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title(title)
            .segment(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .automationMetadata(metadata)
            .build();
        page.markAsRoot();
        return page;
    }

    private static AutomationMetadata automationMetadata(AutomationMetadata.ReferenceType type, String refId) {
        return new AutomationMetadata(type, refId, "name", Optional.of("/x"), Optional.of(0));
    }
}
