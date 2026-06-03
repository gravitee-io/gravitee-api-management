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

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.NavigationPath;
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

    private final PortalNavigationItemsCrudServiceInMemory crud = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory query = new PortalNavigationItemsQueryServiceInMemory(crud.storage());
    private final PortalPageContentCrudServiceInMemory pageContentCrud = new PortalPageContentCrudServiceInMemory();
    private PortalNavigationSyncDomainService syncService;

    @BeforeEach
    void setUp() {
        crud.reset();
        pageContentCrud.reset();
        syncService = new PortalNavigationSyncDomainService(crud, query, pageContentCrud);
    }

    @Test
    void empty_input_on_empty_db_is_a_noop() {
        syncService.sync(AUDIT_INFO, List.of());

        assertThat(crud.storage()).isEmpty();
    }

    @Test
    void single_root_path_creates_one_folder() {
        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a", null)));

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
        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a", "Alpha")));

        assertThat(crud.storage()).hasSize(1);
        var folder = crud.storage().get(0);
        assertThat(folder.getTitle()).isEqualTo("Alpha");
        assertThat(folder.getSegment()).isEqualTo("a");
    }

    @Test
    void title_falls_back_to_segment_when_no_display_name_provided() {
        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a", null)));

        assertThat(crud.storage()).hasSize(1);
        var folder = crud.storage().get(0);
        assertThat(folder.getTitle()).isEqualTo("a");
        assertThat(folder.getSegment()).isEqualTo("a");
    }

    @Test
    void implicit_ancestors_are_created_with_mkdir_p() {
        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a/b/c", null)));

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

        syncService.sync(AUDIT_INFO, input);
        var snapshot = new ArrayList<>(crud.storage());

        syncService.sync(AUDIT_INFO, input);

        assertThat(crud.storage()).hasSize(snapshot.size());
        for (int i = 0; i < snapshot.size(); i++) {
            assertThat(crud.storage().get(i)).isSameAs(snapshot.get(i));
        }
    }

    @Test
    void reorder_updates_existing_folders() {
        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a/b", null), new NavigationPath("/a/c", null)));

        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a/c", null), new NavigationPath("/a/b", null)));

        var b = findByPath("/a/b").orElseThrow();
        var c = findByPath("/a/c").orElseThrow();
        assertThat(c.getOrder()).isZero();
        assertThat(b.getOrder()).isEqualTo(1);
        assertThat(crud.storage()).hasSize(3);
    }

    @Test
    void removed_path_is_deleted() {
        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a", null), new NavigationPath("/b", null)));
        assertThat(crud.storage()).hasSize(2);

        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a", null)));

        assertThat(crud.storage()).hasSize(1);
        assertThat(findByPath("/a")).isPresent();
        assertThat(findByPath("/b")).isEmpty();
    }

    @Test
    void rename_via_display_name_updates_title_without_changing_id() {
        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a", "Original")));
        var originalId = crud.storage().get(0).getId();

        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a", "Renamed")));

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
            .area(PortalArea.HOMEPAGE)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        folderInHomepage.markAsRoot();
        crud.initWith(List.of(pageInTopNavbar, folderInHomepage));

        syncService.sync(AUDIT_INFO, List.of(new NavigationPath("/a", null)));

        assertThat(crud.storage()).hasSize(3);
        assertThat(crud.storage()).contains(pageInTopNavbar, folderInHomepage);
        assertThat(findByPath("/a")).isPresent();
    }

    @Test
    void cascade_deletes_non_folder_descendants_of_removed_folder() {
        var folder = folderRow("x", null, 0);
        folder.markAsRoot();
        var pageChild = pageRow("page-child", folder.getId(), 0, PortalPageContentId.random());
        var linkChild = linkRow("link-child", folder.getId(), 1);
        var apiChild = apiRow("api-child", folder.getId(), 2);
        crud.initWith(List.of(folder, pageChild, linkChild, apiChild));

        syncService.sync(AUDIT_INFO, List.of());

        assertThat(crud.storage()).isEmpty();
    }

    @Test
    void cascade_deletes_associated_page_content_for_page_descendants() {
        var contentId = PortalPageContentId.random();
        var content = new GraviteeMarkdownPageContent(
            contentId,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("hello")
        );
        pageContentCrud.initWith(List.of(content));
        var folder = folderRow("x", null, 0);
        folder.markAsRoot();
        var pageChild = pageRow("page-child", folder.getId(), 0, contentId);
        crud.initWith(List.of(folder, pageChild));

        syncService.sync(AUDIT_INFO, List.of());

        assertThat(crud.storage()).isEmpty();
        assertThat(pageContentCrud.storage()).isEmpty();
    }

    @Test
    void cascade_deletes_nested_folder_chain() {
        var a = folderRow("a", null, 0);
        a.markAsRoot();
        var b = folderRow("b", a.getId(), 0);
        var c = folderRow("c", b.getId(), 0);
        var leafPage = pageRow("leaf", c.getId(), 0, PortalPageContentId.random());
        crud.initWith(List.of(a, b, c, leafPage));

        syncService.sync(AUDIT_INFO, List.of());

        assertThat(crud.storage()).isEmpty();
    }

    private PortalNavigationFolder folderRow(String title, PortalNavigationItemId parentId, int order) {
        return PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title(title)
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
