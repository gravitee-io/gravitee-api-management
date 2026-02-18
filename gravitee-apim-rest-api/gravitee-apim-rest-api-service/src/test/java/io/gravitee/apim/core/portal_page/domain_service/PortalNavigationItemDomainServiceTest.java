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
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
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
                PortalNavigationItemFixtures.ENV_ID
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
            PortalNavigationPage parent1 = PortalNavigationItemFixtures.aPage("parent1", null).toBuilder().order(0).build();
            PortalNavigationPage parent2 = PortalNavigationItemFixtures.aPage("parent2", null).toBuilder().order(1).build();
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
