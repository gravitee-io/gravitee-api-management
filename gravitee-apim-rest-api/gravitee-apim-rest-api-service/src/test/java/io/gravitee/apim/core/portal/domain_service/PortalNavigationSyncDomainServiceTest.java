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
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationSyncDomainServiceTest {

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
    }

    @Test
    void empty_input_on_empty_db_is_a_noop() {
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of());

        assertThat(crud.storage()).isEmpty();
    }

    @Test
    void single_root_path_creates_one_folder() {
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/a", null)));

        assertThat(crud.storage()).hasSize(1);
        var folder = (PortalNavigationFolder) crud.storage().get(0);
        assertThat(folder.getTitle()).isEqualTo("a");
        assertThat(folder.getOrder()).isZero();
        assertThat(folder.getParentId()).isNull();
        assertThat(folder.getRootId()).isEqualTo(folder.getId());
        assertThat(folder.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
        assertThat(folder.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(folder.getPublished()).isTrue();
        assertThat(folder.getType()).isEqualTo(PortalNavigationItemType.FOLDER);
    }

    @Test
    void display_name_when_provided_becomes_the_title_and_segment_holds_the_path_piece() {
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/a", "Alpha")));

        assertThat(crud.storage()).hasSize(1);
        var folder = crud.storage().get(0);
        assertThat(folder.getTitle()).isEqualTo("Alpha");
        assertThat(folder.getSegment()).isEqualTo("a");
    }

    @Test
    void title_falls_back_to_segment_when_no_display_name_provided() {
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/a", null)));

        assertThat(crud.storage()).hasSize(1);
        var folder = crud.storage().get(0);
        assertThat(folder.getTitle()).isEqualTo("a");
        assertThat(folder.getSegment()).isEqualTo("a");
    }

    @Test
    void implicit_ancestors_are_created_with_mkdir_p() {
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/a/b/c", null)));

        assertThat(crud.storage()).hasSize(3);
        var a = findByPath("/a").orElseThrow();
        var b = findByPath("/a/b").orElseThrow();
        var c = findByPath("/a/b/c").orElseThrow();
        assertThat(a.getTitle()).isEqualTo("a");
        assertThat(b.getTitle()).isEqualTo("b");
        assertThat(c.getTitle()).isEqualTo("c");
        assertThat(a.getParentId()).isNull();
        assertThat(b.getParentId()).isEqualTo(a.getId());
        assertThat(c.getParentId()).isEqualTo(b.getId());
        assertThat(a.getRootId()).isEqualTo(a.getId());
        assertThat(b.getRootId()).isEqualTo(a.getId());
        assertThat(c.getRootId()).isEqualTo(a.getId());
    }

    @Test
    void siblings_get_order_from_first_appearance_position() {
        syncService.sync(
            AUDIT_INFO,
            PORTAL_ID,
            List.of(),
            List.of(new NavigationPath("/a", null), new NavigationPath("/a/b", null), new NavigationPath("/a/c", null))
        );

        var a = findByPath("/a").orElseThrow();
        var b = findByPath("/a/b").orElseThrow();
        var c = findByPath("/a/c").orElseThrow();
        assertThat(a.getOrder()).isZero();
        assertThat(b.getOrder()).isZero();
        assertThat(c.getOrder()).isEqualTo(1);
    }

    @Test
    void re_sync_with_same_input_is_idempotent() {
        var input = List.of(new NavigationPath("/a", "Alpha"), new NavigationPath("/a/b", null), new NavigationPath("/c", null));

        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), input);
        var snapshot = new ArrayList<>(crud.storage());

        syncService.sync(AUDIT_INFO, PORTAL_ID, input, input);

        assertThat(crud.storage()).hasSize(snapshot.size());
        for (int i = 0; i < snapshot.size(); i++) {
            assertThat(crud.storage().get(i)).isSameAs(snapshot.get(i));
        }
    }

    @Test
    void folder_ids_are_deterministic_from_audit_portal_and_path() {
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/a/b", null)));

        var idsBefore = crud.storage().stream().map(PortalNavigationItem::getId).toList();

        crud.reset();
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/a/b", null)));

        var idsAfter = crud.storage().stream().map(PortalNavigationItem::getId).toList();
        assertThat(idsAfter).isEqualTo(idsBefore);
    }

    @Test
    void different_portals_produce_different_folder_ids_for_the_same_path() {
        var otherPortal = PortalId.of("22222222-2222-2222-2222-222222222222");

        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/a", null)));
        var idForPortalOne = findByPath("/a").orElseThrow().getId();

        crud.reset();
        syncService.sync(AUDIT_INFO, otherPortal, List.of(), List.of(new NavigationPath("/a", null)));
        var idForPortalTwo = findByPath("/a").orElseThrow().getId();

        assertThat(idForPortalTwo).isNotEqualTo(idForPortalOne);
    }

    @Test
    void reorder_updates_existing_folders() {
        var first = List.of(new NavigationPath("/a/b", null), new NavigationPath("/a/c", null));
        var reordered = List.of(new NavigationPath("/a/c", null), new NavigationPath("/a/b", null));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), first);

        syncService.sync(AUDIT_INFO, PORTAL_ID, first, reordered);

        var b = findByPath("/a/b").orElseThrow();
        var c = findByPath("/a/c").orElseThrow();
        assertThat(c.getOrder()).isZero();
        assertThat(b.getOrder()).isEqualTo(1);
        assertThat(crud.storage()).hasSize(3);
    }

    @Test
    void removed_path_is_deleted_when_previously_managed() {
        var firstInput = List.of(new NavigationPath("/a", null), new NavigationPath("/b", null));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), firstInput);
        assertThat(crud.storage()).hasSize(2);

        syncService.sync(AUDIT_INFO, PORTAL_ID, firstInput, List.of(new NavigationPath("/a", null)));

        assertThat(crud.storage()).hasSize(1);
        assertThat(findByPath("/a")).isPresent();
        assertThat(findByPath("/b")).isEmpty();
    }

    @Test
    void unmanaged_folder_is_not_deleted_when_no_longer_desired() {
        var unmanaged = folderRow("manual", null, 0);
        unmanaged.markAsRoot();
        crud.initWith(List.of(unmanaged));

        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of());

        assertThat(crud.storage()).contains(unmanaged);
    }

    @Test
    void unmanaged_folder_is_not_deleted_even_when_other_managed_paths_change() {
        var unmanaged = folderRow("manual", null, 0);
        unmanaged.markAsRoot();
        crud.initWith(List.of(unmanaged));

        var previously = List.of(new NavigationPath("/managed", null));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), previously);

        syncService.sync(AUDIT_INFO, PORTAL_ID, previously, List.of());

        assertThat(findByPath("/manual")).isPresent();
        assertThat(findByPath("/managed")).isEmpty();
    }

    @Test
    void rename_via_display_name_updates_title_without_changing_id() {
        var original = List.of(new NavigationPath("/a", "Original"));
        var renamed = List.of(new NavigationPath("/a", "Renamed"));
        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), original);
        var originalId = crud.storage().get(0).getId();

        syncService.sync(AUDIT_INFO, PORTAL_ID, original, renamed);

        assertThat(crud.storage()).hasSize(1);
        var folder = crud.storage().get(0);
        assertThat(folder.getId()).isEqualTo(originalId);
        assertThat(folder.getTitle()).isEqualTo("Renamed");
    }

    @Test
    void sync_does_not_touch_other_node_types_or_areas() {
        var pageInTopNavbar = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("untouched-page")
            .segment("untouched-page")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        pageInTopNavbar.markAsRoot();
        var folderInHomepage = PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("untouched-homepage-folder")
            .segment("untouched-homepage-folder")
            .area(PortalArea.HOMEPAGE)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        folderInHomepage.markAsRoot();
        crud.initWith(List.of(pageInTopNavbar, folderInHomepage));

        syncService.sync(AUDIT_INFO, PORTAL_ID, List.of(), List.of(new NavigationPath("/a", null)));

        assertThat(crud.storage()).hasSize(3);
        assertThat(crud.storage()).contains(pageInTopNavbar, folderInHomepage);
        assertThat(findByPath("/a")).isPresent();
    }

    private PortalNavigationFolder folderRow(String title, PortalNavigationItemId parentId, int order) {
        return PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(order)
            .parentId(parentId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private PortalNavigationPage pageRow(String title, PortalNavigationItemId parentId, int order, PortalPageContentId contentId) {
        return PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(order)
            .parentId(parentId)
            .portalPageContentId(contentId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private PortalNavigationLink linkRow(String title, PortalNavigationItemId parentId, int order) {
        return PortalNavigationLink.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(order)
            .parentId(parentId)
            .url("https://example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private PortalNavigationApi apiRow(String title, PortalNavigationItemId parentId, int order) {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(order)
            .parentId(parentId)
            .apiId("api-id")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private Optional<PortalNavigationItem> findByPath(String path) {
        var segments = new ArrayList<String>();
        for (String s : path.split("/")) if (!s.isEmpty()) segments.add(s);

        PortalNavigationItem current = null;
        for (String seg : segments) {
            final PortalNavigationItemId parentId = current == null ? null : current.getId();
            current = crud
                .storage()
                .stream()
                .filter(it -> seg.equals(it.getTitle()))
                .filter(it -> parentId == null ? it.getParentId() == null : parentId.equals(it.getParentId()))
                .findFirst()
                .orElse(null);
            if (current == null) return Optional.empty();
        }
        return Optional.ofNullable(current);
    }
}
