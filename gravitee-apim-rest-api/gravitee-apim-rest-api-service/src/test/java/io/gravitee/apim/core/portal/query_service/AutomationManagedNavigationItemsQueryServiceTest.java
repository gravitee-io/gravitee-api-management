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
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_documentation.domain_service.navigation.DocumentationNavigationIds;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.portal_page.domain_service.navigation.ApiDocumentationNavigationIds;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
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

    private static final String PORTAL_HRID = "default-portal";
    private static final PortalId PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
    private static final PortalListingId LISTING_ID = PortalListingId.of(
        HRIDToUUID.portalListing().context(AUDIT_INFO).portal(PORTAL_HRID).hrid("default-listing").id()
    );

    private final PortalListingCrudServiceInMemory listingCrud = new PortalListingCrudServiceInMemory();
    private final PortalPageContentQueryServiceInMemory pageContentQuery = new PortalPageContentQueryServiceInMemory();
    private final AutomationManagedNavigationItemsQueryService queryService = new AutomationManagedNavigationItemsQueryService(
        listingCrud,
        pageContentQuery
    );

    @BeforeEach
    void setUp() {
        listingCrud.reset();
        pageContentQuery.reset();
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
            DocumentationNavigationIds.navigationApiId(AUDIT_INFO, PORTAL_ID.toString(), petsApiId),
            DocumentationNavigationIds.navigationApiId(AUDIT_INFO, PORTAL_ID.toString(), shopApiId)
        );
    }

    @Test
    void active_listing_api_rows_returns_empty_when_no_listings_exist() {
        assertThat(queryService.activeListingApiRows(AUDIT_INFO, PORTAL_ID)).isEmpty();
    }

    @Test
    void automation_managed_portal_doc_pages_returns_only_pages_with_automation_metadata() {
        var managedId = PortalPageContentId.of(HRIDToUUID.portalDocumentation().context(AUDIT_INFO).portal(PORTAL_HRID).hrid("about").id());
        var unmanagedId = PortalPageContentId.of(
            HRIDToUUID.portalDocumentation().context(AUDIT_INFO).portal(PORTAL_HRID).hrid("manual").id()
        );
        pageContentQuery.initWith(
            List.of(
                portalDoc(managedId, automationMetadata(AutomationMetadata.ReferenceType.PORTAL, PORTAL_ID.toString())),
                portalDoc(unmanagedId, null)
            )
        );

        var result = queryService.automationManagedPortalDocPages(AUDIT_INFO, PORTAL_ID);

        assertThat(result).containsExactly(DocumentationNavigationIds.navigationItemId(AUDIT_INFO, PORTAL_ID.toString(), managedId));
    }

    @Test
    void automation_managed_api_doc_pages_returns_page_ids_under_the_specific_nav_api_row() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid("pets-api").id();
        var contentId = PortalPageContentId.of(
            HRIDToUUID.apiDocumentation().context(AUDIT_INFO).api("pets-api").hrid("getting-started").id()
        );
        pageContentQuery.initWith(List.of(apiDoc(contentId, apiId)));
        var navApi = navApiRow(DocumentationNavigationIds.navigationApiId(AUDIT_INFO, PORTAL_ID.toString(), apiId), apiId);

        var result = queryService.automationManagedApiDocPages(AUDIT_INFO, navApi, apiId);

        assertThat(result).containsExactly(ApiDocumentationNavigationIds.pageIdUnder(AUDIT_INFO, navApi.getId(), contentId));
    }

    private static GraviteeMarkdownPageContent portalDoc(PortalPageContentId id, AutomationMetadata metadata) {
        return new GraviteeMarkdownPageContent(
            id,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# x"),
            metadata
        );
    }

    private static GraviteeMarkdownPageContent apiDoc(PortalPageContentId id, String apiId) {
        return new GraviteeMarkdownPageContent(
            id,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# x"),
            automationMetadata(AutomationMetadata.ReferenceType.API, apiId)
        );
    }

    private static AutomationMetadata automationMetadata(AutomationMetadata.ReferenceType type, String refId) {
        return new AutomationMetadata(type, refId, "name", Optional.of("/x"), Optional.of(0));
    }

    private static PortalNavigationApi navApiRow(PortalNavigationItemId id, String apiId) {
        return PortalNavigationApi.builder()
            .id(id)
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("api")
            .segment("api")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId(apiId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }
}
