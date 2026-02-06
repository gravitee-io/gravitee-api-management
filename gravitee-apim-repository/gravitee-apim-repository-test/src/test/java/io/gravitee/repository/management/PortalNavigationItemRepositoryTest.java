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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.api.search.PortalNavigationItemCriteria;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portalnavigationitem-tests/";
    }

    @Test
    public void should_find_all_navigation_items_for_organization_and_environment() throws Exception {
        List<PortalNavigationItem> items = portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("org-1", "env-1");

        assertThat(items).isNotNull();
        assertThat(items).hasSize(6);
        assertThat(items).anyMatch(i -> "2d7b9f6c-1a2b-4c3d-8e9f-0a1b2c3d4e5f".equals(i.getId()));
        assertThat(items).anyMatch(i -> "3e8c0d7f-2b3c-4d5e-9f0a-1b2c3d4e5f6a".equals(i.getId()));
        assertThat(items).anyMatch(i -> "5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c".equals(i.getId()));
        assertThat(items).anyMatch(i -> "6b1c2d3e-4e5f-6a7b-8c9d-0e1f2a3b4c5d".equals(i.getId()));
        assertThat(items).anyMatch(i -> "7c2d3e4f-5f6a-7b8c-9d0e-1f2a3b4c5d6e".equals(i.getId()));
        assertThat(items).anyMatch(i -> "8d3e4f5a-6a7b-8c9d-0e1f-2a3b4c5d6e7f".equals(i.getId()));
    }

    @Test
    public void should_find_all_navigation_items_for_area_and_env() throws Exception {
        List<PortalNavigationItem> items = portalNavigationItemRepository.findAllByAreaAndEnvironmentId(
            PortalNavigationItem.Area.TOP_NAVBAR,
            "env-1"
        );

        assertThat(items).isNotNull();
        assertThat(items).hasSize(5);
        assertThat(items).anyMatch(i -> "3e8c0d7f-2b3c-4d5e-9f0a-1b2c3d4e5f6a".equals(i.getId()));
    }

    @Test
    public void should_find_all_top_level_navigation_items_for_area_and_env() throws Exception {
        List<PortalNavigationItem> items = portalNavigationItemRepository.findAllByAreaAndEnvironmentIdAndParentIdIsNull(
            PortalNavigationItem.Area.HOMEPAGE,
            "env-1"
        );

        assertThat(items).isNotNull();
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getId()).isEqualTo("2d7b9f6c-1a2b-4c3d-8e9f-0a1b2c3d4e5f");
    }

    @Test
    public void should_delete_all_navigation_items_for_organization() throws Exception {
        portalNavigationItemRepository.deleteByOrganizationId("org-1");

        Set<PortalNavigationItem> remaining = portalNavigationItemRepository.findAll();
        assertThat(remaining).isNotNull();
        assertThat(remaining).noneMatch(i -> "org-1".equals(i.getOrganizationId()));
    }

    @Test
    public void should_delete_all_navigation_items_for_environment() throws Exception {
        portalNavigationItemRepository.deleteByEnvironmentId("env-1");

        Set<PortalNavigationItem> remaining = portalNavigationItemRepository.findAll();
        assertThat(remaining).isNotNull();
        assertThat(remaining).noneMatch(i -> "env-1".equals(i.getEnvironmentId()));
    }

    @Test
    public void should_create_and_delete_navigation_item() throws Exception {
        PortalNavigationItem item = PortalNavigationItem.builder()
            .id("new-nav-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("Support")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(4)
            .published(true)
            .configuration("{ \"url\": \"https://support.example.com\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();

        PortalNavigationItem created = portalNavigationItemRepository.create(item);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(item.getId());

        portalNavigationItemRepository.delete(item.getId());
        var maybeFound = portalNavigationItemRepository.findById(item.getId());
        assertThat(maybeFound).isEmpty();
    }

    @Test
    public void should_create_and_delete_api_navigation_item() throws Exception {
        PortalNavigationItem item = PortalNavigationItem.builder()
            .id("new-nav-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("Support")
            .type(PortalNavigationItem.Type.API)
            .apiId("testApiId")
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(4)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();

        PortalNavigationItem created = portalNavigationItemRepository.create(item);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(item.getId());

        portalNavigationItemRepository.delete(item.getId());
        var maybeFound = portalNavigationItemRepository.findById(item.getId());
        assertThat(maybeFound).isEmpty();
    }

    @Test
    public void should_find_all_navigation_items_for_parent_id_and_environment() throws Exception {
        List<PortalNavigationItem> items = portalNavigationItemRepository.findAllByParentIdAndEnvironmentId(
            "5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c",
            "env-1"
        );

        assertThat(items).isNotNull();
        assertThat(items).hasSize(3);
        assertThat(items)
            .extracting("id")
            .contains(
                "6b1c2d3e-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
                "7c2d3e4f-5f6a-7b8c-9d0e-1f2a3b4c5d6e",
                "8d3e4f5a-6a7b-8c9d-0e1f-2a3b4c5d6e7f"
            );
    }

    @Test
    public void should_update_navigation_item() throws Exception {
        PortalNavigationItem item = PortalNavigationItem.builder()
            .id("update-nav-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("Original Title")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(1)
            .published(true)
            .configuration("{ \"url\": \"https://original.com\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();

        PortalNavigationItem created = portalNavigationItemRepository.create(item);
        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Original Title");

        PortalNavigationItem updatedItem = PortalNavigationItem.builder()
            .id("update-nav-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("Updated Title")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(2)
            .published(false)
            .configuration("{ \"url\": \"https://updated.com\" }")
            .visibility(PortalNavigationItem.Visibility.PRIVATE)
            .build();

        PortalNavigationItem updated = portalNavigationItemRepository.update(updatedItem);
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo("update-nav-item");
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getOrder()).isEqualTo(2);
        assertThat(updated.getConfiguration()).isEqualTo("{ \"url\": \"https://updated.com\" }");
        assertThat(updated.isPublished()).isFalse();
        assertThat(updated.getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PRIVATE);

        portalNavigationItemRepository.delete("update-nav-item");
    }

    //////////////////////////////////////
    ////   SEARCH BY CRITERIA TESTS
    //////////////////////////////////////

    @Test
    public void should_search_by_environment_id() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder().environmentId("env-1").build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isNotNull();
        assertThat(items).hasSize(6);
        assertThat(items).extracting("environmentId").containsOnly("env-1");
    }

    @Test
    public void should_search_by_environment_id_and_parent_id() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .parentId("5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c")
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isNotNull();
        assertThat(items).hasSize(3);
        assertThat(items)
            .extracting("id")
            .containsExactlyInAnyOrder(
                "6b1c2d3e-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
                "7c2d3e4f-5f6a-7b8c-9d0e-1f2a3b4c5d6e",
                "8d3e4f5a-6a7b-8c9d-0e1f-2a3b4c5d6e7f"
            );
    }

    @Test
    public void should_search_by_environment_id_and_root_and_null_parent_id() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .root(true)
            .parentId(null)
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isNotNull();
        assertThat(items).hasSize(3);
        assertThat(items)
            .extracting("id")
            .containsExactlyInAnyOrder(
                "2d7b9f6c-1a2b-4c3d-8e9f-0a1b2c3d4e5f",
                "3e8c0d7f-2b3c-4d5e-9f0a-1b2c3d4e5f6a",
                "5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c"
            );
    }

    @Test
    public void should_search_by_environment_id_and_portal_area() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .portalArea("TOP_NAVBAR")
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isNotNull();
        assertThat(items).hasSize(5);
        assertThat(items).extracting("area").containsOnly(PortalNavigationItem.Area.TOP_NAVBAR);
    }

    @Test
    public void should_search_by_environment_id_and_published() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder().environmentId("env-1").published(true).build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isNotNull();
        assertThat(items).hasSize(6);
        assertThat(items).extracting("published").containsOnly(true);
    }

    @Test
    public void should_search_by_environment_id_and_published_false() throws Exception {
        // Create an unpublished item for testing
        PortalNavigationItem unpublishedItem = PortalNavigationItem.builder()
            .id("unpublished-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("Unpublished")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(10)
            .published(false)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();

        portalNavigationItemRepository.create(unpublishedItem);

        try {
            PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder().environmentId("env-1").published(false).build();

            List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

            assertThat(items).isNotNull();
            assertThat(items).hasSize(1);
            assertThat(items.getFirst().getId()).isEqualTo("unpublished-item");
            assertThat(items.getFirst().isPublished()).isFalse();
        } finally {
            portalNavigationItemRepository.delete("unpublished-item");
        }
    }

    @Test
    public void should_search_with_all_criteria() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .parentId("5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c")
            .portalArea("TOP_NAVBAR")
            .published(true)
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isNotNull();
        assertThat(items).hasSize(3);
        assertThat(items)
            .extracting("id")
            .containsExactlyInAnyOrder(
                "6b1c2d3e-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
                "7c2d3e4f-5f6a-7b8c-9d0e-1f2a3b4c5d6e",
                "8d3e4f5a-6a7b-8c9d-0e1f-2a3b4c5d6e7f"
            );
        assertThat(items).extracting("area").containsOnly(PortalNavigationItem.Area.TOP_NAVBAR);
        assertThat(items).extracting("published").containsOnly(true);
    }

    @Test
    public void should_search_without_published_filter_when_published_is_null() throws Exception {
        // Create both published and unpublished items
        PortalNavigationItem unpublishedItem = PortalNavigationItem.builder()
            .id("unpublished-item-2")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("Unpublished 2")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(11)
            .published(false)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();

        portalNavigationItemRepository.create(unpublishedItem);

        try {
            PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder().environmentId("env-1").published(null).build();

            List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

            assertThat(items).isNotNull();
            assertThat(items).hasSize(7); // 6 original + 1 unpublished
            assertThat(items).extracting("id").contains("unpublished-item-2");
        } finally {
            portalNavigationItemRepository.delete("unpublished-item-2");
        }
    }

    @Test
    public void should_search_public_items() throws Exception {
        // Create a private item for testing
        PortalNavigationItem privateItem = PortalNavigationItem.builder()
            .id("private-item")
            .organizationId("org-1")
            .environmentId("public-private-env")
            .title("Private Item")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(12)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PRIVATE)
            .build();

        PortalNavigationItem publicItem = PortalNavigationItem.builder()
            .id("public-item")
            .organizationId("org-1")
            .environmentId("public-private-env")
            .title("Public Item")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(13)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();

        portalNavigationItemRepository.create(privateItem);
        portalNavigationItemRepository.create(publicItem);

        try {
            PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
                .environmentId("public-private-env")
                .visibility("PUBLIC")
                .build();

            List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

            assertThat(items).isNotNull();
            assertThat(items).hasSize(1);
            assertThat(items.getFirst().getId()).isEqualTo("public-item");
            assertThat(items.getFirst().getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PUBLIC);
        } finally {
            portalNavigationItemRepository.delete("public-item");
        }
    }
}
