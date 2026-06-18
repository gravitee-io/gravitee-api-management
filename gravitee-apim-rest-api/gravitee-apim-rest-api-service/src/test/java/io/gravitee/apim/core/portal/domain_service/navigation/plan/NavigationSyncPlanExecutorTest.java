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
package io.gravitee.apim.core.portal.domain_service.navigation.plan;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.navigation.actions.FolderActions;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.Slug;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NavigationSyncPlanExecutorTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private static final PortalNavigationItemId FIXED_ID = PortalNavigationItemId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final PortalNavigationItemsCrudServiceInMemory crud = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory query = new PortalNavigationItemsQueryServiceInMemory(crud.storage());
    private final PortalPageContentCrudServiceInMemory pageContentCrud = new PortalPageContentCrudServiceInMemory();
    private final NavigationSyncPlanExecutor executor = new NavigationSyncPlanExecutor(crud, query, pageContentCrud);

    @BeforeEach
    void setUp() {
        crud.reset();
        pageContentCrud.reset();
    }

    @Test
    void create_folder_under_root_when_parent_path_is_null() {
        var plan = new NavigationSyncPlan(List.of(new FolderActions.CreateFolder(desired("/a", null, "a", 0))));

        executor.execute(plan, AUDIT_INFO, null, path -> PortalNavigationItemId.random(), new DeleteStrategy(item -> false, false));

        assertThat(crud.storage()).hasSize(1);
        var folder = (PortalNavigationFolder) crud.storage().get(0);
        assertThat(folder.getTitle()).isEqualTo("a");
        assertThat(folder.getSegment()).isEqualTo("a");
        assertThat(folder.getParentId()).isNull();
        assertThat(folder.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
        assertThat(folder.getType()).isEqualTo(PortalNavigationItemType.FOLDER);
    }

    @Test
    void create_folder_under_explicitly_named_parent() {
        var plan = new NavigationSyncPlan(
            List.of(
                new FolderActions.CreateFolder(desired("/a", null, "a", 0)),
                new FolderActions.CreateFolder(desired("/a/b", "/a", "b", 0))
            )
        );

        executor.execute(plan, AUDIT_INFO, null, path -> PortalNavigationItemId.random(), new DeleteStrategy(item -> false, false));

        assertThat(crud.storage()).hasSize(2);
        var a = (PortalNavigationFolder) crud.storage().get(0);
        var b = (PortalNavigationFolder) crud.storage().get(1);
        assertThat(b.getParentId()).isEqualTo(a.getId());
    }

    @Test
    void id_comes_from_id_factory() {
        var plan = new NavigationSyncPlan(List.of(new FolderActions.CreateFolder(desired("/a", null, "a", 0))));

        executor.execute(plan, AUDIT_INFO, null, path -> FIXED_ID, new DeleteStrategy(item -> false, false));

        assertThat(crud.storage()).hasSize(1);
        assertThat(crud.storage().get(0).getId()).isEqualTo(FIXED_ID);
    }

    @Test
    void update_folder_when_it_changed() {
        var existing = folderRow(PortalNavigationItemId.random(), "old-title", null, 0);
        existing.markAsRoot();
        crud.initWith(List.of(existing));
        var plan = new NavigationSyncPlan(List.of(new FolderActions.UpdateFolder(existing, desired("/a", null, "a", 0))));

        executor.execute(plan, AUDIT_INFO, null, path -> PortalNavigationItemId.random(), new DeleteStrategy(item -> false, false));

        assertThat(crud.storage()).hasSize(1);
        assertThat(crud.storage().get(0).getTitle()).isEqualTo("a");
    }

    @Test
    void update_is_noop_when_nothing_changed() {
        var existing = folderRow(PortalNavigationItemId.random(), "a", null, 0);
        existing.markAsRoot();
        crud.initWith(List.of(existing));
        var plan = new NavigationSyncPlan(List.of(new FolderActions.UpdateFolder(existing, desired("/a", null, "a", 0))));

        executor.execute(plan, AUDIT_INFO, null, path -> PortalNavigationItemId.random(), new DeleteStrategy(item -> false, false));

        assertThat(crud.storage()).hasSize(1);
        assertThat(crud.storage().get(0)).isSameAs(existing);
    }

    @Test
    void delete_removes_the_folder_and_its_children() {
        var parent = folderRow(PortalNavigationItemId.random(), "parent", null, 0);
        parent.markAsRoot();
        var child = folderRow(PortalNavigationItemId.random(), "child", parent.getId(), 0);
        crud.initWith(List.of(parent, child));
        var plan = new NavigationSyncPlan(List.of(new FolderActions.DeleteFolder(parent)));

        executor.execute(plan, AUDIT_INFO, null, path -> PortalNavigationItemId.random(), new DeleteStrategy(item -> false, false));

        assertThat(crud.storage()).isEmpty();
    }

    @Test
    void skip_filter_short_circuits_subtree() {
        var parent = folderRow(PortalNavigationItemId.random(), "parent", null, 0);
        parent.markAsRoot();
        var protectedItem = folderRow(PortalNavigationItemId.random(), "protected", parent.getId(), 0);
        var grandchild = folderRow(PortalNavigationItemId.random(), "grand", protectedItem.getId(), 0);
        crud.initWith(List.of(parent, protectedItem, grandchild));
        var plan = new NavigationSyncPlan(List.of(new FolderActions.DeleteFolder(parent)));

        var skipIds = Set.of(protectedItem.getId());
        executor.execute(
            plan,
            AUDIT_INFO,
            null,
            path -> PortalNavigationItemId.random(),
            new DeleteStrategy(item -> skipIds.contains(item.getId()), false)
        );

        assertThat(crud.storage())
            .extracting(PortalNavigationItem::getId)
            .containsExactlyInAnyOrder(protectedItem.getId(), grandchild.getId());
    }

    private FolderActions.DesiredFolder desired(String path, String parentPath, String segment, int order) {
        return new FolderActions.DesiredFolder(path, parentPath, Slug.from(segment), segment, order, PortalVisibility.PUBLIC, true);
    }

    private PortalNavigationFolder folderRow(PortalNavigationItemId id, String title, PortalNavigationItemId parentId, int order) {
        return PortalNavigationFolder.builder()
            .id(id)
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
}
