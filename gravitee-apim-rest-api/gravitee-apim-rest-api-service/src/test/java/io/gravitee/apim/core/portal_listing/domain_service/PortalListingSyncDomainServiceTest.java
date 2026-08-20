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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.portal_page.domain_service.ApiDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
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
class PortalListingSyncDomainServiceTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String PORTAL_HRID = "default-portal";
    private static final String API_HRID = "pets-api";
    private static final PortalId PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
    private static final PortalListingId LISTING_ID = PortalListingId.of(
        HRIDToUUID.portalListing().context(AUDIT_INFO).portal(PORTAL_HRID).hrid("default-listing").id()
    );

    private final PortalNavigationItemsCrudServiceInMemory navItemCrud = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navItemQuery = new PortalNavigationItemsQueryServiceInMemory(
        navItemCrud.storage()
    );
    private final PortalPageContentQueryServiceInMemory pageContentQuery = new PortalPageContentQueryServiceInMemory();
    private final PortalPageContentCrudServiceInMemory pageContentCrud = new PortalPageContentCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrud = new ApiCrudServiceInMemory();

    private PortalListingSyncDomainService syncService;

    @BeforeEach
    void setUp() {
        navItemCrud.reset();
        pageContentQuery.reset();
        pageContentCrud.reset();
        apiCrud.reset();
        var portalListingCrud = new PortalListingCrudServiceInMemory();
        var apiDocSync = new ApiDocumentationSyncDomainService(navItemCrud, navItemQuery, mock(PortalNavigationItemValidatorService.class));
        var automationManaged = new AutomationManagedNavigationItemsQueryService(portalListingCrud, navItemQuery);
        syncService = new PortalListingSyncDomainService(
            pageContentQuery,
            apiDocSync,
            new NavigationItemEntryMaterializer(navItemCrud, navItemQuery, apiDocSync),
            new ApiFolderSubtreeReconciler(
                navItemQuery,
                apiCrud,
                new NavigationSyncPlanExecutor(navItemCrud, navItemQuery, pageContentCrud),
                automationManaged
            )
        );
    }

    @Test
    void should_create_nav_api_row_at_deterministic_id_under_portal_folder() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));

        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);

        var expectedNavApiId = PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(AUDIT_INFO).portal(PORTAL_ID.toString()).listingApi(apiId).id()
        );
        assertThat(navItemCrud.storage())
            .filteredOn(PortalNavigationApi.class::isInstance)
            .extracting(PortalNavigationItem::getId)
            .containsExactly(expectedNavApiId);
    }

    @Test
    void should_be_idempotent_when_syncing_twice() {
        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));

        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);
        var afterFirst = navItemCrud.storage().size();
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);

        assertThat(navItemCrud.storage()).hasSize(afterFirst);
    }

    @Test
    void should_backfill_api_docs_into_freshly_created_nav_api_rows() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var docContentId = PortalPageContentId.of(
            HRIDToUUID.apiDocumentation().context(AUDIT_INFO).api(API_HRID).hrid("getting-started").id()
        );
        pageContentQuery.initWith(List.of(anApiDocPageContent(docContentId, apiId)));

        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);

        var expectedPageId = PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, apiId, docContentId);
        assertThat(navItemCrud.storage())
            .filteredOn(PortalNavigationPage.class::isInstance)
            .extracting(PortalNavigationItem::getId)
            .containsExactly(expectedPageId);
    }

    @Test
    void should_handle_empty_apis_list_without_creating_rows() {
        var listing = aListing(List.of());

        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);

        assertThat(navItemCrud.storage()).isEmpty();
    }

    @Test
    void should_dematerialize_entries_removed_from_listing_update() {
        var keepHrid = "shop-api";
        var removeHrid = "pets-api";
        var removeApiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(removeHrid).id();
        var keepApiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(keepHrid).id();

        var initial = aListing(
            List.of(new PortalListingApiEntry(removeHrid, "/projects/alpha", 1), new PortalListingApiEntry(keepHrid, "/projects/beta", 2))
        );
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), initial);

        var updated = aListing(List.of(new PortalListingApiEntry(keepHrid, "/projects/beta", 2)));
        syncService.sync(AUDIT_INFO, PORTAL_ID, initial.getApis(), updated);

        var removeNavApiId = PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(AUDIT_INFO).portal(PORTAL_ID.toString()).listingApi(removeApiId).id()
        );
        var keepNavApiId = PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(AUDIT_INFO).portal(PORTAL_ID.toString()).listingApi(keepApiId).id()
        );
        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).contains(keepNavApiId).doesNotContain(removeNavApiId);
    }

    @Test
    void removing_the_only_listing_entry_keeps_the_apis_documentation_and_folders() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var docContentId = PortalPageContentId.of(
            HRIDToUUID.apiDocumentation().context(AUDIT_INFO).api(API_HRID).hrid("getting-started").id()
        );
        pageContentQuery.initWith(List.of(anApiDocPageContent(docContentId, apiId)));
        apiCrud.initWith(List.of(Api.builder().id(apiId).portalNavigation(List.of(new NavigationPath("/getting-started", null))).build()));

        var initial = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), initial);

        var navApiId = PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(AUDIT_INFO).portal(PORTAL_ID.toString()).listingApi(apiId).id()
        );
        var folderId = PortalNavigationItemId.forApiFolder(AUDIT_INFO, apiId, "/getting-started");
        var pageId = PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, apiId, docContentId);

        var empty = aListing(List.of());
        syncService.sync(AUDIT_INFO, PORTAL_ID, initial.getApis(), empty);

        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).doesNotContain(navApiId).contains(folderId, pageId);
    }

    @Test
    void dematerialize_removes_only_the_nav_api_row_and_keeps_documentation_and_folders() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var docContentId = PortalPageContentId.of(
            HRIDToUUID.apiDocumentation().context(AUDIT_INFO).api(API_HRID).hrid("getting-started").id()
        );
        pageContentQuery.initWith(List.of(anApiDocPageContent(docContentId, apiId)));
        apiCrud.initWith(List.of(Api.builder().id(apiId).portalNavigation(List.of(new NavigationPath("/getting-started", null))).build()));

        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);

        var navApiId = PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(AUDIT_INFO).portal(PORTAL_ID.toString()).listingApi(apiId).id()
        );
        var folderId = PortalNavigationItemId.forApiFolder(AUDIT_INFO, apiId, "/getting-started");
        var pageId = PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, apiId, docContentId);

        syncService.dematerialize(AUDIT_INFO, PORTAL_ID, listing);

        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).doesNotContain(navApiId).contains(folderId, pageId);
    }

    @Test
    void reconciles_the_api_folder_subtree_when_no_portal_lists_the_api() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var guidesPath = new NavigationPath("/guides", null);
        apiCrud.initWith(List.of(Api.builder().id(apiId).portalNavigation(List.of(guidesPath)).build()));

        syncService.syncApiFolders(AUDIT_INFO, apiId, List.of());

        assertThat(navItemCrud.storage())
            .singleElement()
            .satisfies(folder -> {
                assertThat(folder.getId()).isEqualTo(PortalNavigationItemId.forApiFolder(AUDIT_INFO, apiId, "/guides"));
                assertThat(folder.getReference()).isEqualTo(new NavigationItemReference.ApiReference(apiId));
                assertThat(folder.isRoot()).isTrue();
            });
    }

    @Test
    void validates_api_folder_conflicts_with_zero_listings() {
        // A conflict is a conflict whether or not a portal currently lists the API: the impostor's
        // segment resolves to the same "/guides" path automation would create, but its id isn't the
        // deterministic one, so it fails the id check even though nothing lists this API yet.
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var impostor = PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("guides")
            .segment("guides")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .reference(new NavigationItemReference.ApiReference(apiId))
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        navItemCrud.create(impostor);

        var throwable = catchThrowable(() ->
            syncService.validateApiFolderConflictsForApi(AUDIT_INFO, apiId, List.of(new NavigationPath("/guides", null)))
        );

        assertThat(throwable).isInstanceOf(PathConflictException.class);
    }

    @Test
    void an_automation_managed_api_link_survives_its_folder_being_dropped_from_the_api_navigation() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var guidesPath = new NavigationPath("/guides", null);
        apiCrud.initWith(List.of(Api.builder().id(apiId).portalNavigation(List.of(guidesPath)).build()));

        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);

        var folderId = PortalNavigationItemId.forApiFolder(AUDIT_INFO, apiId, "/guides");
        var link = PortalNavigationLink.builder()
            .id(PortalNavigationItemId.forApiLink(AUDIT_INFO, apiId, "external-docs"))
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("External Docs")
            .segment("external-docs")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .url("https://docs.example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(folderId)
            .automationMetadata(
                new AutomationMetadata(AutomationMetadata.ReferenceType.API, apiId, null, Optional.of("/guides"), Optional.empty())
            )
            .build();
        navItemCrud.create(link);

        apiCrud.initWith(List.of(Api.builder().id(apiId).portalNavigation(List.of()).build()));
        syncService.syncApiFolders(AUDIT_INFO, apiId, List.of(guidesPath));

        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).contains(link.getId()).doesNotContain(folderId);
    }

    @Test
    void sweeps_legacy_api_attached_rows_still_parented_under_the_nav_api_row() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var navApiId = PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(AUDIT_INFO).portal(PORTAL_ID.toString()).listingApi(apiId).id()
        );
        var legacyPage = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("Legacy Doc")
            .segment("legacy-doc")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(navApiId)
            .automationMetadata(
                new AutomationMetadata(AutomationMetadata.ReferenceType.API, apiId, null, Optional.empty(), Optional.empty())
            )
            .build();
        navItemCrud.create(legacyPage);

        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);

        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).doesNotContain(legacyPage.getId());
    }

    @Test
    void the_sweep_leaves_hand_created_rows_under_the_nav_api_row_alone() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var navApiId = PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(AUDIT_INFO).portal(PORTAL_ID.toString()).listingApi(apiId).id()
        );
        var handCreatedFolder = PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("My Notes")
            .segment("my-notes")
            .area(PortalArea.TOP_NAVBAR)
            .order(1)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(navApiId)
            .build();
        navItemCrud.create(handCreatedFolder);

        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);

        assertThat(navItemCrud.storage()).extracting(PortalNavigationItem::getId).contains(handCreatedFolder.getId());
    }

    private static PortalListing aListing(List<PortalListingApiEntry> apis) {
        return PortalListing.of(LISTING_ID, AUDIT_INFO.environmentId(), AUDIT_INFO.organizationId(), PORTAL_ID, apis);
    }

    private static GraviteeMarkdownPageContent anApiDocPageContent(PortalPageContentId id, String apiId) {
        var meta = new AutomationMetadata(
            AutomationMetadata.ReferenceType.API,
            apiId,
            "Getting Started",
            Optional.of("/getting-started"),
            Optional.of(1)
        );
        return new GraviteeMarkdownPageContent(
            id,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# Hello"),
            meta
        );
    }
}
