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
package io.gravitee.apim.core.portal_page.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemDomainServiceTest {

    private final PortalNavigationItemsCrudServiceInMemory portalNavigationItemsCrudService =
        new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService =
        new PortalNavigationItemsQueryServiceInMemory(portalNavigationItemsCrudService.storage());
    private final PortalPageContentCrudServiceInMemory portalPageContentCrudService = new PortalPageContentCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private PortalNavigationItemDomainService domainService;

    @BeforeEach
    void setUp() {
        portalNavigationItemsCrudService.reset();
        portalNavigationItemsQueryService.reset();
        portalPageContentCrudService.reset();
        apiCrudService.reset();

        domainService = new PortalNavigationItemDomainService(
            portalNavigationItemsCrudService,
            portalNavigationItemsQueryService,
            portalPageContentCrudService,
            apiCrudService
        );
    }

    @Nested
    class Create {

        @Test
        void should_set_root_id_to_self_for_homepage_area_item() {
            // HOMEPAGE items are always root elements (no parent allowed by the validator)
            // When
            var toCreate = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title("Homepage")
                .area(PortalArea.HOMEPAGE)
                .order(0)
                .build();

            var created = domainService.create(PortalNavigationItemFixtures.ORG_ID, PortalNavigationItemFixtures.ENV_ID, toCreate);

            // Then — homepage item is always a root: rootId must equal its own id
            assertThat(created.getRootId()).isEqualTo(created.getId());
        }

        @Test
        void should_set_parentId_and_rootId_correctly_when_parent_already_has_correct_root_id() {
            // Given — grandparent (root), parent (child of grandparent with rootId already = grandparent.id)
            var grandparent = PortalNavigationItemFixtures.aFolder("grandparent-folder");
            grandparent.markAsRoot();
            var parent = PortalNavigationItemFixtures.aFolder("parent-folder", grandparent.getId());
            parent.updateParent(grandparent); // parent.rootId = grandparent.id (correctly set)
            portalNavigationItemsCrudService.initWith(List.of(grandparent, parent));
            portalNavigationItemsQueryService.initWith(List.of(grandparent, parent));

            // When — add a child under parent
            var toCreate = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title("Child Folder")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build();
            toCreate.setParentId(parent.getId());

            var created = domainService.create(PortalNavigationItemFixtures.ORG_ID, PortalNavigationItemFixtures.ENV_ID, toCreate);

            // Then — child inherits parentId and rootId directly from parent (no tree walk needed)
            assertThat(created.getParentId()).isEqualTo(parent.getId());
            assertThat(created.getRootId()).isEqualTo(grandparent.getId());
        }

        @Test
        void should_set_root_id_equal_to_item_id_for_root_item() {
            // When
            var toCreate = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title("Root Folder")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build();

            var created = domainService.create(PortalNavigationItemFixtures.ORG_ID, PortalNavigationItemFixtures.ENV_ID, toCreate);

            // Then
            assertThat(created.getRootId()).isEqualTo(created.getId());
        }

        @Test
        void should_set_root_id_to_parent_id_for_direct_child_of_root() {
            // Given — a root parent with rootId set to itself
            var parent = PortalNavigationItemFixtures.aFolder("parent-folder");
            parent.markAsRoot();
            portalNavigationItemsCrudService.initWith(List.of(parent));
            portalNavigationItemsQueryService.initWith(List.of(parent));

            // When
            var toCreate = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title("Child Folder")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build();
            toCreate.setParentId(parent.getId());

            var created = domainService.create(PortalNavigationItemFixtures.ORG_ID, PortalNavigationItemFixtures.ENV_ID, toCreate);

            // Then
            assertThat(created.getRootId()).isEqualTo(parent.getId());
        }

        @Test
        void should_set_root_id_to_grandparent_id_for_nested_child() {
            // Given — root → child (no rootId yet) → grandchild
            var root = PortalNavigationItemFixtures.aFolder("root-folder");
            root.markAsRoot();
            var child = PortalNavigationItemFixtures.aFolder("child-folder", root.getId());
            // child has rootId = ZERO (simulates items loaded from DB before Stage 2)
            portalNavigationItemsCrudService.initWith(List.of(root, child));
            portalNavigationItemsQueryService.initWith(List.of(root, child));

            // When
            var toCreate = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title("Grandchild Folder")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build();
            toCreate.setParentId(child.getId());

            var created = domainService.create(PortalNavigationItemFixtures.ORG_ID, PortalNavigationItemFixtures.ENV_ID, toCreate);

            // Then: resolveRootId walked up to the root (which has rootId set) and returned root.getId()
            assertThat(created.getRootId()).isEqualTo(root.getId());
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete() {
            var toDelete = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null);
            portalNavigationItemsCrudService.initWith(List.of(toDelete));
            portalNavigationItemsQueryService.initWith(List.of(toDelete));

            domainService.delete(toDelete);

            assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
        }

        @Test
        void should_reorder_when_delete() {
            PortalNavigationPage page1 = PortalNavigationItemFixtures.aPage("p1", null).toBuilder().order(1).build();
            PortalNavigationPage page2 = PortalNavigationItemFixtures.aPage("p2", null).toBuilder().order(2).build();
            PortalNavigationPage page3 = PortalNavigationItemFixtures.aPage("p3", null).toBuilder().order(3).build();
            PortalNavigationPage page4 = PortalNavigationItemFixtures.aPage("p4", null).toBuilder().order(4).build();
            PortalNavigationPage page5 = PortalNavigationItemFixtures.aPage("p5", null).toBuilder().order(5).build();
            portalNavigationItemsCrudService.initWith(List.of(page1, page2, page3, page4, page5));
            portalNavigationItemsQueryService.initWith(List.copyOf(portalNavigationItemsCrudService.storage()));

            domainService.delete(page2);

            assertThat(portalNavigationItemsCrudService.storage()).hasSize(4);
            var expected = Map.of(page1.getId(), 1, page3.getId(), 2, page4.getId(), 3, page5.getId(), 4);
            assertThat(
                portalNavigationItemsCrudService
                    .storage()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getId,
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getOrder
                        )
                    )
            )
                .containsOnlyKeys(expected.keySet())
                .containsAllEntriesOf(expected);
        }

        @Test
        void should_delete_page_with_content() {
            var pageContent = portalPageContentCrudService.createDefault(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                PortalPageContentType.GRAVITEE_MARKDOWN
            );
            var toDelete = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null)
                .toBuilder()
                .portalPageContentId(pageContent.getId())
                .build();
            portalNavigationItemsCrudService.initWith(List.of(toDelete));
            portalNavigationItemsQueryService.initWith(List.of(toDelete));
            portalPageContentCrudService.initWith(List.of(pageContent));

            domainService.delete(toDelete);

            assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
            assertThat(portalPageContentCrudService.storage()).isEmpty();
        }
    }

    @Nested
    class Update {

        @Test
        void should_update() {
            var existing = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null);
            portalNavigationItemsCrudService.initWith(List.of(existing));
            portalNavigationItemsQueryService.initWith(List.of(existing));

            var toUpdate = UpdatePortalNavigationItem.builder()
                .title("updated-name")
                .visibility(existing.getVisibility())
                .type(existing.getType())
                .order(existing.getOrder())
                .parentId(existing.getParentId())
                .published(existing.getPublished())
                .build();

            var updated = domainService.update(toUpdate, existing);

            assertThat(updated.getTitle()).isEqualTo("updated-name");
            assertThat(portalNavigationItemsCrudService.storage()).hasSize(1);
            assertThat(portalNavigationItemsCrudService.storage().getFirst().getTitle()).isEqualTo("updated-name");
        }

        @Test
        void should_update_order_decrementing_order() {
            // Given
            PortalNavigationPage page1 = PortalNavigationItemFixtures.aPage("p1", null).toBuilder().order(0).build();
            PortalNavigationPage page2 = PortalNavigationItemFixtures.aPage("p2", null).toBuilder().order(1).build();
            PortalNavigationPage page3 = PortalNavigationItemFixtures.aPage("p3", null).toBuilder().order(2).build();
            portalNavigationItemsCrudService.initWith(List.of(page1, page2, page3));
            portalNavigationItemsQueryService.initWith(List.copyOf(portalNavigationItemsCrudService.storage()));

            var toUpdate = UpdatePortalNavigationItem.builder()
                .order(3)
                .title(page1.getTitle())
                .visibility(page1.getVisibility())
                .type(page1.getType())
                .parentId(page1.getParentId())
                .published(page1.getPublished())
                .build();

            // When
            var result = domainService.update(toUpdate, page1);

            // Then
            assertThat(result.getOrder()).isEqualTo(2);

            var expected = Map.of(page2.getId(), 0, page3.getId(), 1, page1.getId(), 2);
            assertThat(
                portalNavigationItemsCrudService
                    .storage()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getId,
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getOrder
                        )
                    )
            )
                .containsOnlyKeys(expected.keySet())
                .containsAllEntriesOf(expected);
        }

        @Test
        void should_update_order_incrementing_order() {
            // Given
            PortalNavigationPage page1 = PortalNavigationItemFixtures.aPage("p1", null).toBuilder().order(0).build();
            PortalNavigationPage page2 = PortalNavigationItemFixtures.aPage("p2", null).toBuilder().order(1).build();
            PortalNavigationPage page3 = PortalNavigationItemFixtures.aPage("p3", null).toBuilder().order(2).build();
            PortalNavigationPage page4 = PortalNavigationItemFixtures.aPage("p4", null).toBuilder().order(3).build();
            portalNavigationItemsCrudService.initWith(List.of(page1, page2, page3, page4));
            portalNavigationItemsQueryService.initWith(List.copyOf(portalNavigationItemsCrudService.storage()));

            var toUpdate = UpdatePortalNavigationItem.builder()
                .order(1)
                .title(page4.getTitle())
                .visibility(page4.getVisibility())
                .type(page4.getType())
                .parentId(page4.getParentId())
                .published(page4.getPublished())
                .build();

            // When
            var result = domainService.update(toUpdate, page4);

            // Then
            assertThat(result.getOrder()).isEqualTo(1);

            var expected = Map.of(page1.getId(), 0, page2.getId(), 2, page4.getId(), 1, page3.getId(), 3);
            assertThat(
                portalNavigationItemsCrudService
                    .storage()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getId,
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getOrder
                        )
                    )
            )
                .containsOnlyKeys(expected.keySet())
                .containsAllEntriesOf(expected);
        }

        @Test
        void should_update_parent_id_and_recalculate_orders() {
            // Given
            PortalNavigationFolder parent1 = PortalNavigationItemFixtures.aFolder("parent1").toBuilder().order(0).build();
            PortalNavigationFolder parent2 = PortalNavigationItemFixtures.aFolder("parent2").toBuilder().order(1).build();
            PortalNavigationPage child1 = PortalNavigationItemFixtures.aPage("child1", parent1.getId()).toBuilder().order(0).build();
            PortalNavigationPage child2 = PortalNavigationItemFixtures.aPage("child2", parent1.getId()).toBuilder().order(1).build();
            PortalNavigationPage child3 = PortalNavigationItemFixtures.aPage("child3", parent2.getId()).toBuilder().order(0).build();
            portalNavigationItemsCrudService.initWith(List.of(parent1, parent2, child1, child2, child3));
            portalNavigationItemsQueryService.initWith(List.copyOf(portalNavigationItemsCrudService.storage()));

            var toUpdate = UpdatePortalNavigationItem.builder()
                .order(child1.getOrder())
                .title(child1.getTitle())
                .visibility(child1.getVisibility())
                .type(child1.getType())
                .parentId(parent2.getId())
                .published(child1.getPublished())
                .build();

            // When
            var result = domainService.update(toUpdate, child1);

            // Then
            assertThat(result.getParentId()).isEqualTo(parent2.getId());
            assertThat(result.getOrder()).isEqualTo(0);

            // Check orders under parent1
            var expectedParent1 = Map.of(child2.getId(), 0);
            assertThat(
                portalNavigationItemsCrudService
                    .storage()
                    .stream()
                    .filter(item -> item.getParentId() != null && item.getParentId().equals(parent1.getId()))
                    .collect(
                        Collectors.toMap(
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getId,
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getOrder
                        )
                    )
            )
                .containsOnlyKeys(expectedParent1.keySet())
                .containsAllEntriesOf(expectedParent1);

            // Check orders under parent2
            var expectedParent2 = Map.of(child1.getId(), 0, child3.getId(), 1);
            assertThat(
                portalNavigationItemsCrudService
                    .storage()
                    .stream()
                    .filter(item -> item.getParentId() != null && item.getParentId().equals(parent2.getId()))
                    .collect(
                        Collectors.toMap(
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getId,
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getOrder
                        )
                    )
            )
                .containsOnlyKeys(expectedParent2.keySet())
                .containsAllEntriesOf(expectedParent2);
        }

        @Test
        void should_update_root_id_when_child_is_moved_to_root() {
            // Given — root → child (has rootId = root.getId())
            PortalNavigationFolder root = PortalNavigationItemFixtures.aFolder("root1");
            root.markAsRoot();
            PortalNavigationPage child = PortalNavigationItemFixtures.aPage("child1", root.getId());
            child.updateParent(root);
            portalNavigationItemsCrudService.initWith(List.of(root, child));
            portalNavigationItemsQueryService.initWith(List.of(root, child));

            // When — move child to root level (parentId = null)
            var toUpdate = UpdatePortalNavigationItem.builder()
                .title(child.getTitle())
                .visibility(child.getVisibility())
                .type(child.getType())
                .order(0)
                .parentId(null)
                .published(child.getPublished())
                .build();

            var updated = domainService.update(toUpdate, child);

            // Then — rootId changes to the item's own id (it is now a root item)
            assertThat(updated.getRootId()).isEqualTo(updated.getId());
        }

        @Test
        void should_update_root_id_and_propagate_when_root_item_moves_under_another_root() {
            // Given — two roots, root1 has a child with a grandchild
            PortalNavigationFolder root1 = PortalNavigationItemFixtures.aFolder("root1-folder");
            root1.markAsRoot();
            PortalNavigationFolder root2 = PortalNavigationItemFixtures.aFolder("root2-folder");
            root2.markAsRoot();
            PortalNavigationFolder child = PortalNavigationItemFixtures.aFolder("child-of-root1", root1.getId());
            child.updateParent(root1);
            PortalNavigationPage grandchild = PortalNavigationItemFixtures.aPage("grandchild", child.getId());
            grandchild.updateParent(child);
            portalNavigationItemsCrudService.initWith(List.of(root1, root2, child, grandchild));
            portalNavigationItemsQueryService.initWith(List.of(root1, root2, child, grandchild));

            // When — move child under root2
            var toUpdate = UpdatePortalNavigationItem.builder()
                .title(child.getTitle())
                .visibility(child.getVisibility())
                .type(child.getType())
                .order(0)
                .parentId(root2.getId())
                .published(child.getPublished())
                .build();

            var updated = domainService.update(toUpdate, child);

            // Then — child rootId = root2.getId()
            assertThat(updated.getRootId()).isEqualTo(root2.getId());

            // And grandchild rootId propagated to root2.getId()
            var grandchildInStorage = portalNavigationItemsCrudService
                .storage()
                .stream()
                .filter(item -> item.getId().equals(grandchild.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(grandchildInStorage.getRootId()).isEqualTo(root2.getId());
        }

        @Test
        void should_not_change_root_id_when_reordering_within_same_parent() {
            // Given
            PortalNavigationFolder root = PortalNavigationItemFixtures.aFolder("stable-root");
            root.markAsRoot();
            PortalNavigationPage child = PortalNavigationItemFixtures.aPage("stable-child", root.getId());
            child.updateParent(root);
            child.setOrder(0);
            PortalNavigationPage sibling = PortalNavigationItemFixtures.aPage("stable-sibling", root.getId());
            sibling.updateParent(root);
            sibling.setOrder(1);
            portalNavigationItemsCrudService.initWith(List.of(root, child, sibling));
            portalNavigationItemsQueryService.initWith(List.of(root, child, sibling));

            // When — change order within same parent
            var toUpdate = UpdatePortalNavigationItem.builder()
                .title(child.getTitle())
                .visibility(child.getVisibility())
                .type(child.getType())
                .order(1)
                .parentId(root.getId())
                .published(child.getPublished())
                .build();

            var updated = domainService.update(toUpdate, child);

            // Then — rootId unchanged
            assertThat(updated.getRootId()).isEqualTo(root.getId());
        }

        @Test
        void should_update_root_id_and_propagate_when_non_root_item_moves_under_intermediary_of_different_root() {
            // Given — root1 → intermediary1 → item → childOfItem  AND  root2 → intermediary2
            PortalNavigationFolder root1 = PortalNavigationItemFixtures.aFolder("root1");
            root1.markAsRoot();
            PortalNavigationFolder intermediary1 = PortalNavigationItemFixtures.aFolder("intermediary1", root1.getId());
            intermediary1.updateParent(root1);
            PortalNavigationFolder item = PortalNavigationItemFixtures.aFolder("item", intermediary1.getId());
            item.updateParent(intermediary1); // item.rootId = root1.id
            PortalNavigationPage childOfItem = PortalNavigationItemFixtures.aPage("child-of-item", item.getId());
            childOfItem.updateParent(item); // childOfItem.rootId = root1.id

            PortalNavigationFolder root2 = PortalNavigationItemFixtures.aFolder("root2");
            root2.markAsRoot();
            PortalNavigationFolder intermediary2 = PortalNavigationItemFixtures.aFolder("intermediary2", root2.getId());
            intermediary2.updateParent(root2); // intermediary2.rootId = root2.id

            portalNavigationItemsCrudService.initWith(List.of(root1, intermediary1, item, childOfItem, root2, intermediary2));
            portalNavigationItemsQueryService.initWith(List.of(root1, intermediary1, item, childOfItem, root2, intermediary2));

            // When — move item (non-root) under intermediary2 (non-root under a different root)
            var toUpdate = UpdatePortalNavigationItem.builder()
                .title(item.getTitle())
                .visibility(item.getVisibility())
                .type(item.getType())
                .order(0)
                .parentId(intermediary2.getId())
                .published(item.getPublished())
                .build();

            var updated = domainService.update(toUpdate, item);

            // Then — item.rootId updated to root2.id
            assertThat(updated.getRootId()).isEqualTo(root2.getId());

            // And childOfItem.rootId propagated to root2.id
            var childInStorage = portalNavigationItemsCrudService
                .storage()
                .stream()
                .filter(i -> i.getId().equals(childOfItem.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(childInStorage.getRootId()).isEqualTo(root2.getId());
        }

        @Test
        void should_update_root_id_and_propagate_to_children_when_non_root_item_moves_to_root() {
            // Given — root → child (folder) → grandchild (page)
            PortalNavigationFolder root = PortalNavigationItemFixtures.aFolder("stable-root2");
            root.markAsRoot();
            PortalNavigationFolder child = PortalNavigationItemFixtures.aFolder("moving-child", root.getId());
            child.updateParent(root); // child.rootId = root.id
            PortalNavigationPage grandchild = PortalNavigationItemFixtures.aPage("grandchild-page", child.getId());
            grandchild.updateParent(child); // grandchild.rootId = root.id

            portalNavigationItemsCrudService.initWith(List.of(root, child, grandchild));
            portalNavigationItemsQueryService.initWith(List.of(root, child, grandchild));

            // When — move child to root level (parentId = null)
            var toUpdate = UpdatePortalNavigationItem.builder()
                .title(child.getTitle())
                .visibility(child.getVisibility())
                .type(child.getType())
                .order(0)
                .parentId(null)
                .published(child.getPublished())
                .build();

            var updated = domainService.update(toUpdate, child);

            // Then — child.rootId = child.id (became a root)
            assertThat(updated.getRootId()).isEqualTo(updated.getId());

            // And grandchild.rootId propagated to child.id
            var grandchildInStorage = portalNavigationItemsCrudService
                .storage()
                .stream()
                .filter(i -> i.getId().equals(grandchild.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(grandchildInStorage.getRootId()).isEqualTo(updated.getId());
        }

        @Test
        void should_update_root_id_and_propagate_when_root_item_moves_to_non_root_parent() {
            // Given — root1 (with a child page) is itself a root; root2 has a non-root child folder
            PortalNavigationFolder root1 = PortalNavigationItemFixtures.aFolder("root1-being-moved");
            root1.markAsRoot(); // root1.rootId = root1.id
            PortalNavigationPage childOfRoot1 = PortalNavigationItemFixtures.aPage("child-of-root1-being-moved", root1.getId());
            childOfRoot1.updateParent(root1); // childOfRoot1.rootId = root1.id

            PortalNavigationFolder root2 = PortalNavigationItemFixtures.aFolder("root2-target");
            root2.markAsRoot();
            PortalNavigationFolder childOfRoot2 = PortalNavigationItemFixtures.aFolder("child-of-root2-target", root2.getId());
            childOfRoot2.updateParent(root2); // childOfRoot2.rootId = root2.id

            portalNavigationItemsCrudService.initWith(List.of(root1, childOfRoot1, root2, childOfRoot2));
            portalNavigationItemsQueryService.initWith(List.of(root1, childOfRoot1, root2, childOfRoot2));

            // When — move root1 under childOfRoot2 (root → non-root)
            var toUpdate = UpdatePortalNavigationItem.builder()
                .title(root1.getTitle())
                .visibility(root1.getVisibility())
                .type(root1.getType())
                .order(0)
                .parentId(childOfRoot2.getId())
                .published(root1.getPublished())
                .build();

            var updated = domainService.update(toUpdate, root1);

            // Then — root1 now has rootId = root2.id (inherited from childOfRoot2's rootId)
            assertThat(updated.getRootId()).isEqualTo(root2.getId());

            // And childOfRoot1.rootId also propagated to root2.id
            var childOfRoot1InStorage = portalNavigationItemsCrudService
                .storage()
                .stream()
                .filter(i -> i.getId().equals(childOfRoot1.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(childOfRoot1InStorage.getRootId()).isEqualTo(root2.getId());
        }

        @Test
        void should_update_subtree_when_folder_visibility_changes_from_public_to_private() {
            // Given
            PortalNavigationFolder parentFolder = PortalNavigationItemFixtures.aFolder("10000000-0000-4000-8000-000000000001", "Parent");
            PortalNavigationFolder childFolder = PortalNavigationItemFixtures.aFolder(
                "10000000-0000-4000-8000-000000000002",
                "Child",
                parentFolder.getId()
            );
            PortalNavigationPage grandChildPage = PortalNavigationItemFixtures.aPage(
                "10000000-0000-4000-8000-000000000003",
                "Grand Child",
                childFolder.getId()
            );
            portalNavigationItemsCrudService.initWith(List.of(parentFolder, childFolder, grandChildPage));
            portalNavigationItemsQueryService.initWith(List.copyOf(portalNavigationItemsCrudService.storage()));

            var toUpdate = UpdatePortalNavigationItem.builder()
                .order(parentFolder.getOrder())
                .title(parentFolder.getTitle())
                .visibility(PortalVisibility.PRIVATE)
                .type(parentFolder.getType())
                .parentId(parentFolder.getParentId())
                .published(parentFolder.getPublished())
                .build();

            // When
            var result = domainService.update(toUpdate, parentFolder);

            // Then
            assertThat(result.getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
            assertThat(
                portalNavigationItemsCrudService
                    .storage()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getId,
                            io.gravitee.apim.core.portal_page.model.PortalNavigationItem::getVisibility
                        )
                    )
            )
                .containsEntry(parentFolder.getId(), PortalVisibility.PRIVATE)
                .containsEntry(childFolder.getId(), PortalVisibility.PRIVATE)
                .containsEntry(grandChildPage.getId(), PortalVisibility.PRIVATE);
        }
    }
}
