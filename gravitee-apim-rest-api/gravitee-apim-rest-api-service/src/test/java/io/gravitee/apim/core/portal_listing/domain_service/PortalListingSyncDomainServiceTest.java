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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationValidator;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
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
import io.gravitee.apim.core.slug.model.Slug;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
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

    private PortalNavigationValidator validator;
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
        validator = mock(PortalNavigationValidator.class);
        syncService = new PortalListingSyncDomainService(
            pageContentQuery,
            apiDocSync,
            new NavigationItemEntryMaterializer(navItemCrud, navItemQuery, apiDocSync, apiCrud),
            new ApiFolderSubtreeReconciler(
                navItemQuery,
                apiCrud,
                new NavigationSyncPlanExecutor(navItemCrud, navItemQuery, pageContentCrud),
                automationManaged
            ),
            validator
        );
    }

    @Test
    void should_create_nav_api_row_at_deterministic_id_under_portal_folder() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        apiCrud.initWith(List.of(anApi(API_HRID)));
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
    void should_use_api_name_as_title_when_api_exists() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        apiCrud.initWith(List.of(Api.builder().id(apiId).name("Echo API Declarative").environmentId(AUDIT_INFO.environmentId()).build()));
        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));

        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);

        assertThat(navItemCrud.storage())
            .filteredOn(PortalNavigationApi.class::isInstance)
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly("Echo API Declarative");
    }

    @Test
    void should_throw_api_not_found_when_api_does_not_exist() {
        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));

        assertThatThrownBy(() -> syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing)).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_be_idempotent_when_syncing_twice() {
        apiCrud.initWith(List.of(anApi(API_HRID)));
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
        apiCrud.initWith(List.of(anApi(API_HRID)));

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
        apiCrud.initWith(List.of(anApi(removeHrid), anApi(keepHrid)));

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
    void an_api_folder_may_take_a_root_segment_already_owned_by_a_portal_root() {
        // Every environment is seeded with a TOP_NAVBAR root folder titled "Guides" (segment "guides"),
        // and "/guides" is the ordinary thing to put in an API's portalNavigation. The API's folder is a
        // root of the API's own subtree, rendered under the nav-api row, so it never becomes a sibling of
        // the portal's own root and the two may share a segment.
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var portalRoot = portalRootFolder("Guides", "guides");
        navItemCrud.create(portalRoot);
        apiCrud.initWith(List.of(Api.builder().id(apiId).portalNavigation(List.of(new NavigationPath("/guides", null))).build()));

        syncService.syncApiFolders(AUDIT_INFO, apiId, List.of());

        assertThat(navItemCrud.storage())
            .extracting(PortalNavigationItem::getId)
            .contains(PortalNavigationItemId.forApiFolder(AUDIT_INFO, apiId, "/guides"), portalRoot.getId());
    }

    @Test
    void two_apis_may_each_own_a_root_folder_with_the_same_segment() {
        var firstApiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var secondApiId = HRIDToUUID.api().context(AUDIT_INFO).hrid("other-api").id();
        var guides = List.of(new NavigationPath("/guides", null));
        apiCrud.initWith(
            List.of(
                Api.builder().id(firstApiId).portalNavigation(guides).build(),
                Api.builder().id(secondApiId).portalNavigation(guides).build()
            )
        );

        syncService.syncApiFolders(AUDIT_INFO, firstApiId, List.of());
        syncService.syncApiFolders(AUDIT_INFO, secondApiId, List.of());

        assertThat(navItemCrud.storage())
            .extracting(PortalNavigationItem::getId)
            .contains(
                PortalNavigationItemId.forApiFolder(AUDIT_INFO, firstApiId, "/guides"),
                PortalNavigationItemId.forApiFolder(AUDIT_INFO, secondApiId, "/guides")
            );
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

    private static PortalNavigationFolder portalRootFolder(String title, String segment) {
        return PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title(title)
            .segment(segment)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static Api anApi(String hrid) {
        return Api.builder()
            .id(HRIDToUUID.api().context(AUDIT_INFO).hrid(hrid).id())
            .name(hrid)
            .environmentId(AUDIT_INFO.environmentId())
            .build();
    }

    private static PortalListing aListing(List<PortalListingApiEntry> apis) {
        return PortalListing.of(LISTING_ID, AUDIT_INFO.environmentId(), AUDIT_INFO.organizationId(), PORTAL_ID, apis);
    }

    @Test
    void validate_for_conflicts_produces_create_for_new_nav_api_and_no_updates() {
        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));

        syncService.validateForConflicts(AUDIT_INFO, PORTAL_ID, listing);

        var createsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        var updatesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(validator).validate(
            createsCaptor.capture(),
            updatesCaptor.capture(),
            org.mockito.ArgumentMatchers.anyString()
        );
        assertThat(createsCaptor.getValue()).hasSize(1);
        assertThat(updatesCaptor.getValue()).isEmpty();
    }

    @Test
    void validate_for_conflicts_is_a_noop_on_empty_listing() {
        syncService.validateForConflicts(AUDIT_INFO, PORTAL_ID, aListing(List.of()));

        org.mockito.Mockito.verifyNoInteractions(validator);
    }

    @Test
    void validate_api_folder_conflicts_routes_desired_paths_through_the_shared_validator() {
        apiCrud.initWith(List.of(anApi(API_HRID)));
        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), listing);
        org.mockito.Mockito.reset(validator);

        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        var desired = List.of(new NavigationPath("/guides", "Guides"), new NavigationPath("/reference", "Reference"));

        syncService.validateApiFolderConflictsForApi(AUDIT_INFO, apiId, desired);

        var createsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        var updatesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(validator).validate(
            createsCaptor.capture(),
            updatesCaptor.capture(),
            org.mockito.ArgumentMatchers.anyString()
        );
        assertThat(createsCaptor.getValue()).hasSize(2);
        assertThat(updatesCaptor.getValue()).isEmpty();
    }

    @Test
    void validate_for_conflicts_includes_folder_subtree_creates_for_new_nav_api_rows() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        apiCrud.initWith(
            List.of(
                io.gravitee.apim.core.api.model.Api.builder()
                    .id(apiId)
                    .name(API_HRID)
                    .environmentId(AUDIT_INFO.environmentId())
                    .portalNavigation(List.of(new NavigationPath("/reports", "Reports")))
                    .build()
            )
        );
        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));

        syncService.validateForConflicts(AUDIT_INFO, PORTAL_ID, listing);

        var createsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        var updatesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(validator).validate(
            createsCaptor.capture(),
            updatesCaptor.capture(),
            org.mockito.ArgumentMatchers.anyString()
        );
        assertThat(createsCaptor.getValue())
            .extracting("type", "parentId", "segment")
            .contains(
                org.assertj.core.groups.Tuple.tuple(
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.FOLDER,
                    null,
                    "reports"
                )
            );
        assertThat(updatesCaptor.getValue()).isEmpty();
    }

    @Test
    void validate_for_conflicts_propagates_api_nav_visibility_to_folder_validation_items() {
        var apiId = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
        apiCrud.initWith(
            List.of(
                io.gravitee.apim.core.api.model.Api.builder()
                    .id(apiId)
                    .name(API_HRID)
                    .environmentId(AUDIT_INFO.environmentId())
                    .portalNavigation(
                        List.of(
                            new NavigationPath("/internal", "Internal", null, PortalVisibility.PRIVATE),
                            new NavigationPath("/internal/deep", "Deep", null, PortalVisibility.PUBLIC)
                        )
                    )
                    .build()
            )
        );
        var listing = aListing(List.of(new PortalListingApiEntry(API_HRID, "/projects/alpha", 1)));

        syncService.validateForConflicts(AUDIT_INFO, PORTAL_ID, listing);

        var createsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(validator).validate(
            createsCaptor.capture(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString()
        );
        assertThat(createsCaptor.getValue())
            .filteredOn(
                item ->
                    item instanceof io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem c &&
                    c.getType() == io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.FOLDER
            )
            .extracting("segment", "visibility")
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("internal", PortalVisibility.PRIVATE),
                org.assertj.core.groups.Tuple.tuple("deep", PortalVisibility.PUBLIC)
            );
    }

    @Nested
    class VacateAndFillSameSlotInOneBatch {

        @BeforeEach
        void wireInRealValidator() {
            var realValidator = new PortalNavigationItemValidatorService(
                navItemQuery,
                pageContentQuery,
                new ApiProductQueryServiceInMemory(),
                new PortalNavigationItemSourceDomainServiceInMemory()
            );
            var portalListingCrud = new PortalListingCrudServiceInMemory();
            var apiDocSync = new ApiDocumentationSyncDomainService(
                navItemCrud,
                navItemQuery,
                mock(PortalNavigationItemValidatorService.class)
            );
            var automationManaged = new AutomationManagedNavigationItemsQueryService(portalListingCrud, navItemQuery);
            syncService = new PortalListingSyncDomainService(
                pageContentQuery,
                apiDocSync,
                new NavigationItemEntryMaterializer(navItemCrud, navItemQuery, apiDocSync, apiCrud),
                new ApiFolderSubtreeReconciler(
                    navItemQuery,
                    apiCrud,
                    new NavigationSyncPlanExecutor(navItemCrud, navItemQuery, pageContentCrud),
                    automationManaged
                ),
                realValidator
            );
        }

        @Test
        void accepts_a_new_entry_that_takes_the_slot_a_relocated_entry_is_vacating() {
            // Two APIs whose hrids slugify to the same segment, so the incoming listing simultaneously
            // vacates and fills (parentP, "pets-api") in one payload — the scenario Marek called out on
            // the listing path in #19381 (SegmentConflictRule.java:102).
            var apiHridA = "pets-api";
            var apiHridB = "Pets-Api";
            assertThat(Slug.from(apiHridA).value()).isEqualTo(Slug.from(apiHridB).value());
            var apiIdA = HRIDToUUID.api().context(AUDIT_INFO).hrid(apiHridA).id();
            var parentP = PortalNavigationItemId.forPortalFolder(AUDIT_INFO, PORTAL_ID.toString(), "/projects/alpha");

            var existingA = PortalNavigationApi.builder()
                .id(PortalNavigationItemId.forListingApi(AUDIT_INFO, PORTAL_ID.toString(), apiIdA))
                .organizationId(AUDIT_INFO.organizationId())
                .environmentId(AUDIT_INFO.environmentId())
                .title(apiHridA)
                .segment(Slug.from(apiHridA).value())
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .apiId(apiIdA)
                .parentId(parentP)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .automationMetadata(
                    new AutomationMetadata(
                        AutomationMetadata.ReferenceType.PORTAL,
                        PORTAL_ID.toString(),
                        null,
                        Optional.of("/projects/alpha"),
                        Optional.empty()
                    )
                )
                .build();
            navItemCrud.storage().add(existingA);

            var listing = aListing(
                List.of(new PortalListingApiEntry(apiHridA, "/projects/beta", 1), new PortalListingApiEntry(apiHridB, "/projects/alpha", 2))
            );

            assertThatCode(() -> syncService.validateForConflicts(AUDIT_INFO, PORTAL_ID, listing)).doesNotThrowAnyException();
        }
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
