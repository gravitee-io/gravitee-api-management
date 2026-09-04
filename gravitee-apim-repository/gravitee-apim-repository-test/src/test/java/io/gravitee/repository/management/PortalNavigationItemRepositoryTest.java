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
import io.gravitee.repository.management.model.AutomationMetadata;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

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
        assertThat(items).hasSize(8);
        assertThat(items).anyMatch(i -> "2d7b9f6c-1a2b-4c3d-8e9f-0a1b2c3d4e5f".equals(i.getId()));
        assertThat(items).anyMatch(i -> "3e8c0d7f-2b3c-4d5e-9f0a-1b2c3d4e5f6a".equals(i.getId()));
        assertThat(items).anyMatch(i -> "5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c".equals(i.getId()));
        assertThat(items).anyMatch(i -> "6b1c2d3e-4e5f-6a7b-8c9d-0e1f2a3b4c5d".equals(i.getId()));
        assertThat(items).anyMatch(i -> "7c2d3e4f-5f6a-7b8c-9d0e-1f2a3b4c5d6e".equals(i.getId()));
        assertThat(items).anyMatch(i -> "8d3e4f5a-6a7b-8c9d-0e1f-2a3b4c5d6e7f".equals(i.getId()));
        assertThat(items).anyMatch(i -> "9e4f5a6b-7b8c-9d0e-1f2a-3b4c5d6e7f8a".equals(i.getId()));
        assertThat(items).anyMatch(i -> "af5a6b7c-8c9d-0e1f-2a3b-4c5d6e7f8a9b".equals(i.getId()));
    }

    @Test
    public void should_find_all_navigation_items_for_area_and_env() throws Exception {
        List<PortalNavigationItem> items = portalNavigationItemRepository.findAllByAreaAndEnvironmentId(
            PortalNavigationItem.Area.TOP_NAVBAR,
            "env-1"
        );

        assertThat(items).isNotNull();
        assertThat(items).hasSize(7);
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
            .segment("support")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(4)
            .published(true)
            .configuration("{ \"url\": \"https://support.example.com\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("new-nav-item")
            .build();

        PortalNavigationItem created = portalNavigationItemRepository.create(item);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(item.getId());
        assertThat(created.getRootId()).isEqualTo("new-nav-item");

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
            .segment("support")
            .type(PortalNavigationItem.Type.API)
            .apiId("testApiId")
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(4)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("new-nav-item")
            .build();

        PortalNavigationItem created = portalNavigationItemRepository.create(item);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(item.getId());
        assertThat(created.getRootId()).isEqualTo("new-nav-item");

        portalNavigationItemRepository.delete(item.getId());
        var maybeFound = portalNavigationItemRepository.findById(item.getId());
        assertThat(maybeFound).isEmpty();
    }

    @Test
    public void should_create_update_and_delete_api_product_navigation_item() throws Exception {
        PortalNavigationItem item = PortalNavigationItem.builder()
            .id("new-api-product-nav-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("API Product")
            .segment("api-product")
            .type(PortalNavigationItem.Type.API_PRODUCT)
            .apiProductId("00000000-0000-0000-0000-000000000020")
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(4)
            .published(false)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("new-api-product-nav-item")
            .build();

        try {
            PortalNavigationItem created = portalNavigationItemRepository.create(item);
            assertThat(created.getType()).isEqualTo(PortalNavigationItem.Type.API_PRODUCT);
            assertThat(created.getApiProductId()).isEqualTo(item.getApiProductId());
            assertThat(created.isPublished()).isFalse();

            created.setTitle("Updated API Product");
            PortalNavigationItem updated = portalNavigationItemRepository.update(created);
            assertThat(updated.getTitle()).isEqualTo("Updated API Product");
            assertThat(updated.getApiProductId()).isEqualTo(item.getApiProductId());

            var found = portalNavigationItemRepository.findById(item.getId());
            assertThat(found).isPresent();
            assertThat(found.orElseThrow().getApiProductId()).isEqualTo(item.getApiProductId());
        } finally {
            portalNavigationItemRepository.delete(item.getId());
        }
    }

    @Test
    public void should_create_and_read_navigation_item_with_source() throws Exception {
        PortalNavigationItem item = PortalNavigationItemFixtures.aSourcedPage(
            "new-sourced-nav-item",
            "00f8c9e7-78fc-4907-b8c9-e778fc790750"
        ).build();

        portalNavigationItemRepository.create(item);

        var found = portalNavigationItemRepository.findById(item.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isUseAutoFetch()).isTrue();
        assertThat(found.get().getConfiguration()).isEqualTo(item.getConfiguration());

        portalNavigationItemRepository.delete(item.getId());
    }

    @Test
    public void should_read_navigation_item_without_source() throws Exception {
        var found = portalNavigationItemRepository.findById("2d7b9f6c-1a2b-4c3d-8e9f-0a1b2c3d4e5f");

        assertThat(found).isPresent();
        assertThat(found.get().isUseAutoFetch()).isFalse();
        assertThat(found.get().getConfiguration()).doesNotContain("source");
    }

    @Test
    public void should_create_read_update_and_delete_navigation_item_with_reference() throws Exception {
        PortalNavigationItem item = PortalNavigationItem.builder()
            .id("portal-attached-nav-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .referenceType(PortalNavigationReferenceType.PORTAL)
            .referenceId("portal-42")
            .title("Home")
            .segment("home")
            .type(PortalNavigationItem.Type.PAGE)
            .area(PortalNavigationItem.Area.HOMEPAGE)
            .order(1)
            .published(true)
            .configuration("{ \"portalPageContentId\": \"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("portal-attached-nav-item")
            .build();

        try {
            PortalNavigationItem created = portalNavigationItemRepository.create(item);
            assertThat(created.getReferenceType()).isEqualTo(PortalNavigationReferenceType.PORTAL);
            assertThat(created.getReferenceId()).isEqualTo("portal-42");

            var found = portalNavigationItemRepository.findById(item.getId());
            assertThat(found).isPresent();
            assertThat(found.orElseThrow().getReferenceId()).isEqualTo("portal-42");

            created.setReferenceId("portal-42-renamed");
            PortalNavigationItem updated = portalNavigationItemRepository.update(created);
            assertThat(updated.getReferenceId()).isEqualTo("portal-42-renamed");
        } finally {
            portalNavigationItemRepository.delete(item.getId());
        }
    }

    @Test
    public void should_default_reference_to_unattached_sentinel_when_missing_from_seed_data() throws Exception {
        var found = portalNavigationItemRepository.findById("2d7b9f6c-1a2b-4c3d-8e9f-0a1b2c3d4e5f");

        assertThat(found).isPresent();
        assertThat(found.get().getReferenceType()).isEqualTo(PortalNavigationReferenceType.PORTAL);
        assertThat(found.get().getReferenceId()).isEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    public void should_find_top_level_homepage_with_unattached_sentinel_reference() throws Exception {
        var homepageForPortalA = PortalNavigationItem.builder()
            .id("homepage-portal-a")
            .organizationId("org-1")
            .environmentId("env-portal-scoped-homepage")
            .referenceType(PortalNavigationReferenceType.PORTAL)
            .referenceId("portal-a")
            .title("Home A")
            .segment("home-a")
            .type(PortalNavigationItem.Type.PAGE)
            .area(PortalNavigationItem.Area.HOMEPAGE)
            .order(1)
            .published(true)
            .configuration("{ \"portalPageContentId\": \"11111111-1111-1111-1111-111111111111\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("homepage-portal-a")
            .build();
        var seededHomepage = PortalNavigationItem.builder()
            .id("homepage-seeded")
            .organizationId("org-1")
            .environmentId("env-portal-scoped-homepage")
            .title("Home Seeded")
            .segment("home-seeded")
            .type(PortalNavigationItem.Type.PAGE)
            .area(PortalNavigationItem.Area.HOMEPAGE)
            .order(1)
            .published(true)
            .configuration("{ \"portalPageContentId\": \"22222222-2222-2222-2222-222222222222\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("homepage-seeded")
            .build();

        try {
            portalNavigationItemRepository.create(homepageForPortalA);
            portalNavigationItemRepository.create(seededHomepage);

            var unattachedResults = portalNavigationItemRepository.findAllTopLevelByAreaAndEnvironmentAndReference(
                PortalNavigationItem.Area.HOMEPAGE,
                "env-portal-scoped-homepage",
                PortalNavigationReferenceType.PORTAL,
                "00000000-0000-0000-0000-000000000000"
            );

            assertThat(unattachedResults).hasSize(1);
            assertThat(unattachedResults.getFirst().getId()).isEqualTo("homepage-seeded");
            assertThat(unattachedResults.getFirst().getReferenceId()).isEqualTo("00000000-0000-0000-0000-000000000000");
        } finally {
            portalNavigationItemRepository.delete("homepage-portal-a");
            portalNavigationItemRepository.delete("homepage-seeded");
        }
    }

    @Test
    public void should_find_top_level_homepage_by_reference() throws Exception {
        var homepageForPortalA = PortalNavigationItem.builder()
            .id("homepage-portal-a-scoped")
            .organizationId("org-1")
            .environmentId("env-portal-scoped-lookup")
            .referenceType(PortalNavigationReferenceType.PORTAL)
            .referenceId("portal-a")
            .title("Home A")
            .segment("home-a")
            .type(PortalNavigationItem.Type.PAGE)
            .area(PortalNavigationItem.Area.HOMEPAGE)
            .order(1)
            .published(true)
            .configuration("{ \"portalPageContentId\": \"33333333-3333-3333-3333-333333333333\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("homepage-portal-a-scoped")
            .build();
        var homepageForPortalB = PortalNavigationItem.builder()
            .id("homepage-portal-b-scoped")
            .organizationId("org-1")
            .environmentId("env-portal-scoped-lookup")
            .referenceType(PortalNavigationReferenceType.PORTAL)
            .referenceId("portal-b")
            .title("Home B")
            .segment("home-b")
            .type(PortalNavigationItem.Type.PAGE)
            .area(PortalNavigationItem.Area.HOMEPAGE)
            .order(1)
            .published(true)
            .configuration("{ \"portalPageContentId\": \"44444444-4444-4444-4444-444444444444\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("homepage-portal-b-scoped")
            .build();

        try {
            portalNavigationItemRepository.create(homepageForPortalA);
            portalNavigationItemRepository.create(homepageForPortalB);

            var forPortalA = portalNavigationItemRepository.findAllTopLevelByAreaAndEnvironmentAndReference(
                PortalNavigationItem.Area.HOMEPAGE,
                "env-portal-scoped-lookup",
                PortalNavigationReferenceType.PORTAL,
                "portal-a"
            );

            assertThat(forPortalA).hasSize(1);
            assertThat(forPortalA.getFirst().getId()).isEqualTo("homepage-portal-a-scoped");
            assertThat(forPortalA.getFirst().getReferenceId()).isEqualTo("portal-a");
        } finally {
            portalNavigationItemRepository.delete("homepage-portal-a-scoped");
            portalNavigationItemRepository.delete("homepage-portal-b-scoped");
        }
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
            .segment("original-title")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(1)
            .published(true)
            .configuration("{ \"url\": \"https://original.com\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("update-nav-item")
            .build();

        PortalNavigationItem created = portalNavigationItemRepository.create(item);
        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Original Title");
        assertThat(created.getRootId()).isEqualTo("update-nav-item");

        PortalNavigationItem updatedItem = PortalNavigationItem.builder()
            .id("update-nav-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("Updated Title")
            .segment("updated-title")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(2)
            .published(false)
            .configuration("{ \"url\": \"https://updated.com\" }")
            .visibility(PortalNavigationItem.Visibility.PRIVATE)
            .rootId("update-nav-item")
            .build();

        PortalNavigationItem updated = portalNavigationItemRepository.update(updatedItem);
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo("update-nav-item");
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getOrder()).isEqualTo(2);
        assertThat(updated.getConfiguration()).isEqualTo("{ \"url\": \"https://updated.com\" }");
        assertThat(updated.isPublished()).isFalse();
        assertThat(updated.getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PRIVATE);
        assertThat(updated.getRootId()).isEqualTo("update-nav-item");

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
        assertThat(items).hasSize(8);
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
        assertThat(items).hasSize(5);
        assertThat(items)
            .extracting("id")
            .containsExactlyInAnyOrder(
                "2d7b9f6c-1a2b-4c3d-8e9f-0a1b2c3d4e5f",
                "3e8c0d7f-2b3c-4d5e-9f0a-1b2c3d4e5f6a",
                "5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c",
                "9e4f5a6b-7b8c-9d0e-1f2a-3b4c5d6e7f8a",
                "af5a6b7c-8c9d-0e1f-2a3b-4c5d6e7f8a9b"
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
        assertThat(items).hasSize(7);
        assertThat(items).extracting("area").containsOnly(PortalNavigationItem.Area.TOP_NAVBAR);
    }

    @Test
    public void should_search_by_environment_id_and_published() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder().environmentId("env-1").published(true).build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isNotNull();
        assertThat(items).hasSize(8);
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
            .segment("unpublished")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(10)
            .published(false)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("unpublished-item")
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
            .segment("unpublished-2")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(11)
            .published(false)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("unpublished-item-2")
            .build();

        portalNavigationItemRepository.create(unpublishedItem);

        try {
            PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder().environmentId("env-1").published(null).build();

            List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

            assertThat(items).isNotNull();
            assertThat(items).hasSize(9); // 8 original + 1 unpublished
            assertThat(items).extracting("id").contains("unpublished-item-2");
        } finally {
            portalNavigationItemRepository.delete("unpublished-item-2");
        }
    }

    //////////////////////////////////////
    ////   SEARCH BY TYPE TESTS
    //////////////////////////////////////

    @Test
    public void should_search_by_type_page() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .type(PortalNavigationItem.Type.PAGE.name())
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).hasSize(4);
        assertThat(items).extracting("type").containsOnly(PortalNavigationItem.Type.PAGE);
        assertThat(items)
            .extracting("id")
            .containsExactlyInAnyOrder(
                "2d7b9f6c-1a2b-4c3d-8e9f-0a1b2c3d4e5f",
                "6b1c2d3e-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
                "7c2d3e4f-5f6a-7b8c-9d0e-1f2a3b4c5d6e",
                "8d3e4f5a-6a7b-8c9d-0e1f-2a3b4c5d6e7f"
            );
    }

    @Test
    public void should_search_by_type_link() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .type(PortalNavigationItem.Type.LINK.name())
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getId()).isEqualTo("3e8c0d7f-2b3c-4d5e-9f0a-1b2c3d4e5f6a");
        assertThat(items.getFirst().getType()).isEqualTo(PortalNavigationItem.Type.LINK);
    }

    @Test
    public void should_search_by_type_folder() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .type(PortalNavigationItem.Type.FOLDER.name())
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getId()).isEqualTo("5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c");
        assertThat(items.getFirst().getType()).isEqualTo(PortalNavigationItem.Type.FOLDER);
    }

    @Test
    public void should_search_by_type_api() throws Exception {
        PortalNavigationItem apiItem = PortalNavigationItem.builder()
            .id("type-api-item")
            .organizationId("org-1")
            .environmentId("env-type-api-test")
            .title("My API")
            .segment("my-api")
            .type(PortalNavigationItem.Type.API)
            .apiId("some-api-id")
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(99)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("type-api-item")
            .build();

        portalNavigationItemRepository.create(apiItem);

        try {
            PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
                .environmentId("env-type-api-test")
                .type(PortalNavigationItem.Type.API.name())
                .build();

            List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

            assertThat(items).hasSize(1);
            assertThat(items.getFirst().getId()).isEqualTo("type-api-item");
            assertThat(items.getFirst().getType()).isEqualTo(PortalNavigationItem.Type.API);
            assertThat(items.getFirst().getApiId()).isEqualTo("some-api-id");
        } finally {
            portalNavigationItemRepository.delete("type-api-item");
        }
    }

    @Test
    public void should_search_by_type_returns_empty_when_no_match() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-no-api-items")
            .type(PortalNavigationItem.Type.API.name())
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isEmpty();
    }

    @Test
    public void should_search_public_items() throws Exception {
        // Create a private item for testing
        PortalNavigationItem privateItem = PortalNavigationItem.builder()
            .id("private-item")
            .organizationId("org-1")
            .environmentId("public-private-env")
            .title("Private Item")
            .segment("private-item")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(12)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PRIVATE)
            .rootId("private-item")
            .build();

        PortalNavigationItem publicItem = PortalNavigationItem.builder()
            .id("public-item")
            .organizationId("org-1")
            .environmentId("public-private-env")
            .title("Public Item")
            .segment("public-item")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(13)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("public-item")
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

    //////////////////////////////////////
    ////   SEARCH BY TYPE / API IDS
    //////////////////////////////////////

    @Test
    public void should_search_by_type() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder().environmentId("env-1").type("API").build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).hasSize(2);
        assertThat(items).extracting("type").containsOnly(PortalNavigationItem.Type.API);
        assertThat(items).extracting("apiId").containsExactlyInAnyOrder("api-public-1", "api-private-1");
    }

    @Test
    public void should_search_by_api_ids() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .apiIds(Set.of("api-public-1"))
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getApiId()).isEqualTo("api-public-1");
    }

    @Test
    public void should_search_by_api_ids_multiple() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .apiIds(Set.of("api-public-1", "api-private-1"))
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).hasSize(2);
        assertThat(items).extracting("apiId").containsExactlyInAnyOrder("api-public-1", "api-private-1");
    }

    @Test
    public void should_search_by_type_and_api_ids() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .type("API")
            .apiIds(Set.of("api-public-1"))
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getType()).isEqualTo(PortalNavigationItem.Type.API);
        assertThat(items.getFirst().getApiId()).isEqualTo("api-public-1");
    }

    @Test
    public void should_return_empty_when_api_ids_do_not_match() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .apiIds(Set.of("non-existent-api"))
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isEmpty();
    }

    @Test
    public void should_search_by_type_and_api_product_ids() throws Exception {
        PortalNavigationItem first = PortalNavigationItem.builder()
            .id("api-product-search-1")
            .organizationId("org-1")
            .environmentId("env-api-product-search")
            .title("First API Product")
            .segment("first-api-product")
            .type(PortalNavigationItem.Type.API_PRODUCT)
            .apiProductId("00000000-0000-0000-0000-000000000021")
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(0)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("api-product-search-1")
            .build();
        PortalNavigationItem second = PortalNavigationItem.builder()
            .id("api-product-search-2")
            .organizationId("org-1")
            .environmentId("env-api-product-search")
            .title("Second API Product")
            .segment("second-api-product")
            .type(PortalNavigationItem.Type.API_PRODUCT)
            .apiProductId("00000000-0000-0000-0000-000000000022")
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(1)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("api-product-search-2")
            .build();

        portalNavigationItemRepository.create(first);
        portalNavigationItemRepository.create(second);
        try {
            PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
                .environmentId("env-api-product-search")
                .type(PortalNavigationItem.Type.API_PRODUCT.name())
                .apiProductIds(Set.of(first.getApiProductId()))
                .build();

            List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

            assertThat(items)
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getType()).isEqualTo(PortalNavigationItem.Type.API_PRODUCT);
                    assertThat(item.getApiProductId()).isEqualTo(first.getApiProductId());
                });
        } finally {
            portalNavigationItemRepository.delete(first.getId());
            portalNavigationItemRepository.delete(second.getId());
        }
    }

    @Test
    public void should_allow_duplicate_page_titles_for_same_organization_and_environment() throws Exception {
        String sharedTitle = "Shared Page Title";

        PortalNavigationItem firstPage = PortalNavigationItem.builder()
            .id("duplicate-title-page-1")
            .organizationId("org-1")
            .environmentId("env-duplicate-title")
            .title(sharedTitle)
            .segment("shared-page-title")
            .type(PortalNavigationItem.Type.PAGE)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(1)
            .published(true)
            .configuration("{ \"portalPageContentId\": \"880e8400-e29b-41d4-a716-446655440003\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("duplicate-title-page-1")
            .build();

        PortalNavigationItem secondPage = PortalNavigationItem.builder()
            .id("duplicate-title-page-2")
            .organizationId("org-1")
            .environmentId("env-duplicate-title")
            .title(sharedTitle)
            .segment("shared-page-title-2")
            .type(PortalNavigationItem.Type.PAGE)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(2)
            .published(true)
            .configuration("{ \"portalPageContentId\": \"990e8400-e29b-41d4-a716-446655440004\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("duplicate-title-page-2")
            .build();

        try {
            portalNavigationItemRepository.create(firstPage);
            portalNavigationItemRepository.create(secondPage);

            var firstFound = portalNavigationItemRepository.findById("duplicate-title-page-1");
            var secondFound = portalNavigationItemRepository.findById("duplicate-title-page-2");

            assertThat(firstFound).isPresent();
            assertThat(secondFound).isPresent();
            assertThat(firstFound.get().getTitle()).isEqualTo(sharedTitle);
            assertThat(secondFound.get().getTitle()).isEqualTo(sharedTitle);
        } finally {
            portalNavigationItemRepository.delete("duplicate-title-page-1");
            portalNavigationItemRepository.delete("duplicate-title-page-2");
        }
    }

    @Test
    public void should_allow_duplicate_api_titles_for_same_organization_and_environment() throws Exception {
        String sharedTitle = "Shared API Name";

        PortalNavigationItem firstApi = PortalNavigationItem.builder()
            .id("duplicate-title-api-1")
            .organizationId("org-1")
            .environmentId("env-duplicate-api-title")
            .title(sharedTitle)
            .segment("shared-api-name")
            .type(PortalNavigationItem.Type.API)
            .apiId("api-duplicate-title-1")
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(1)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("duplicate-title-api-1")
            .build();

        PortalNavigationItem secondApi = PortalNavigationItem.builder()
            .id("duplicate-title-api-2")
            .organizationId("org-1")
            .environmentId("env-duplicate-api-title")
            .title(sharedTitle)
            .segment("shared-api-name-2")
            .type(PortalNavigationItem.Type.API)
            .apiId("api-duplicate-title-2")
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(2)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("duplicate-title-api-2")
            .build();

        try {
            portalNavigationItemRepository.create(firstApi);
            portalNavigationItemRepository.create(secondApi);

            var firstFound = portalNavigationItemRepository.findById("duplicate-title-api-1");
            var secondFound = portalNavigationItemRepository.findById("duplicate-title-api-2");

            assertThat(firstFound).isPresent();
            assertThat(secondFound).isPresent();
            assertThat(firstFound.get().getTitle()).isEqualTo(sharedTitle);
            assertThat(secondFound.get().getTitle()).isEqualTo(sharedTitle);
            assertThat(firstFound.get().getApiId()).isEqualTo("api-duplicate-title-1");
            assertThat(secondFound.get().getApiId()).isEqualTo("api-duplicate-title-2");
        } finally {
            portalNavigationItemRepository.delete("duplicate-title-api-1");
            portalNavigationItemRepository.delete("duplicate-title-api-2");
        }
    }

    //////////////////////////////////////
    ////   SEARCH BY AUTO FETCH
    //////////////////////////////////////

    @Test
    public void should_search_items_with_auto_fetch_enabled_across_environments() throws Exception {
        PortalNavigationItem autoFetched = PortalNavigationItemFixtures.aSourcedPage(
            "auto-fetch-search-1",
            "00f8c9e7-78fc-4907-b8c9-e778fc790750"
        )
            .environmentId("env-auto-fetch-search")
            .build();
        PortalNavigationItem notAutoFetched = PortalNavigationItemFixtures.aSourcedPage(
            "auto-fetch-search-2",
            "00f8c9e7-78fc-4907-b8c9-e778fc790751"
        )
            .environmentId("env-auto-fetch-search")
            .useAutoFetch(false)
            .build();

        try {
            portalNavigationItemRepository.create(autoFetched);
            portalNavigationItemRepository.create(notAutoFetched);

            PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder().useAutoFetch(true).build();

            List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

            assertThat(items).extracting("id").contains("auto-fetch-search-1").doesNotContain("auto-fetch-search-2");
            assertThat(items).allMatch(PortalNavigationItem::isUseAutoFetch);
        } finally {
            portalNavigationItemRepository.delete("auto-fetch-search-1");
            portalNavigationItemRepository.delete("auto-fetch-search-2");
        }
    }

    @Test
    public void should_search_items_with_auto_fetch_disabled() throws Exception {
        PortalNavigationItem autoFetched = PortalNavigationItemFixtures.aSourcedPage(
            "auto-fetch-search-3",
            "00f8c9e7-78fc-4907-b8c9-e778fc790752"
        )
            .environmentId("env-auto-fetch-search")
            .build();

        try {
            portalNavigationItemRepository.create(autoFetched);

            PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
                .environmentId("env-auto-fetch-search")
                .useAutoFetch(false)
                .build();

            List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

            assertThat(items).isEmpty();
        } finally {
            portalNavigationItemRepository.delete("auto-fetch-search-3");
        }
    }

    @Test
    public void should_find_all_navigation_items_by_root_id() throws Exception {
        // "Resources" folder and its 3 children all share rootId 5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c
        List<PortalNavigationItem> items = portalNavigationItemRepository.findAllByRootId("5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c", "env-1");

        assertThat(items).isNotNull();
        assertThat(items).hasSize(4);
        assertThat(items)
            .extracting("id")
            .containsExactlyInAnyOrder(
                "5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c",
                "6b1c2d3e-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
                "7c2d3e4f-5f6a-7b8c-9d0e-1f2a3b4c5d6e",
                "8d3e4f5a-6a7b-8c9d-0e1f-2a3b4c5d6e7f"
            );
    }

    //////////////////////////////////////
    ////   SEARCH BY CATEGORY ID TESTS
    //////////////////////////////////////

    @Test
    public void should_search_by_category_id() throws Exception {
        PortalNavigationItem itemInCategory = PortalNavigationItem.builder()
            .id("category-search-1")
            .organizationId("org-1")
            .environmentId("env-category-search")
            .title("In Category")
            .segment("in-category")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(1)
            .published(true)
            .configuration("{ \"url\": \"https://in-category.example.com\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("category-search-1")
            .categoryIds(List.of("category-1", "category-2"))
            .build();
        PortalNavigationItem itemOutsideCategory = PortalNavigationItem.builder()
            .id("category-search-2")
            .organizationId("org-1")
            .environmentId("env-category-search")
            .title("Outside Category")
            .segment("outside-category")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(2)
            .published(true)
            .configuration("{ \"url\": \"https://outside-category.example.com\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("category-search-2")
            .build();

        portalNavigationItemRepository.create(itemInCategory);
        portalNavigationItemRepository.create(itemOutsideCategory);

        try {
            PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
                .environmentId("env-category-search")
                .categoryId("category-1")
                .build();

            List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

            assertThat(items).hasSize(1);
            assertThat(items.getFirst().getId()).isEqualTo("category-search-1");
            assertThat(items.getFirst().getCategoryIds()).containsExactlyInAnyOrder("category-1", "category-2");
        } finally {
            portalNavigationItemRepository.delete("category-search-1");
            portalNavigationItemRepository.delete("category-search-2");
        }
    }

    @Test
    public void should_return_empty_when_category_id_does_not_match() throws Exception {
        PortalNavigationItemCriteria criteria = PortalNavigationItemCriteria.builder()
            .environmentId("env-1")
            .categoryId("unknown-category")
            .build();

        List<PortalNavigationItem> items = portalNavigationItemRepository.searchByCriteria(criteria);

        assertThat(items).isEmpty();
    }

    @Test
    public void should_delete_navigation_items_by_ids() throws Exception {
        List<String> idsToDelete = List.of("6b1c2d3e-4e5f-6a7b-8c9d-0e1f2a3b4c5d", "7c2d3e4f-5f6a-7b8c-9d0e-1f2a3b4c5d6e");

        portalNavigationItemRepository.deleteByIds(idsToDelete);

        assertThat(portalNavigationItemRepository.findById("6b1c2d3e-4e5f-6a7b-8c9d-0e1f2a3b4c5d")).isEmpty();
        assertThat(portalNavigationItemRepository.findById("7c2d3e4f-5f6a-7b8c-9d0e-1f2a3b4c5d6e")).isEmpty();
        // sibling and root remain
        assertThat(portalNavigationItemRepository.findById("5a0b1c2d-3d4e-5f6a-7b8c-9d0e1f2a3b4c")).isPresent();
        assertThat(portalNavigationItemRepository.findById("8d3e4f5a-6a7b-8c9d-0e1f-2a3b4c5d6e7f")).isPresent();
    }

    //////////////////////////////////////
    ////   AGENT TYPE TESTS
    //////////////////////////////////////

    @Test
    public void should_create_update_and_delete_agent_navigation_item() throws Exception {
        PortalNavigationItem item = PortalNavigationItem.builder()
            .id("new-agent-nav-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("My Agent")
            .segment("my-agent")
            .type(PortalNavigationItem.Type.AGENT)
            .agentId("a2a-proxy-api-id-1")
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(4)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("new-agent-nav-item")
            .build();

        try {
            PortalNavigationItem created = portalNavigationItemRepository.create(item);
            assertThat(created.getType()).isEqualTo(PortalNavigationItem.Type.AGENT);
            assertThat(created.getAgentId()).isEqualTo("a2a-proxy-api-id-1");
            assertThat(created.isPublished()).isTrue();

            created.setTitle("Updated Agent");
            created.setConfiguration(
                "{\"termsAndConditionsEnabled\":true,\"termsAndConditionsPageContentId\":\"550e8400-e29b-41d4-a716-446655440099\"}"
            );
            PortalNavigationItem updated = portalNavigationItemRepository.update(created);
            assertThat(updated.getTitle()).isEqualTo("Updated Agent");
            assertThat(updated.getAgentId()).isEqualTo("a2a-proxy-api-id-1");
            assertThat(updated.getConfiguration()).isEqualTo(
                "{\"termsAndConditionsEnabled\":true,\"termsAndConditionsPageContentId\":\"550e8400-e29b-41d4-a716-446655440099\"}"
            );

            var found = portalNavigationItemRepository.findById(item.getId());
            assertThat(found).isPresent();
            assertThat(found.orElseThrow().getAgentId()).isEqualTo("a2a-proxy-api-id-1");
        } finally {
            portalNavigationItemRepository.delete(item.getId());
        }
    }

    //////////////////////////////////////
    ////   AUTOMATION METADATA TESTS
    //////////////////////////////////////

    @Test
    public void should_create_and_read_navigation_item_with_automation_metadata() throws Exception {
        AutomationMetadata metadata = AutomationMetadata.builder()
            .referenceType(AutomationTargetReferenceType.API)
            .referenceId("automation-api-1")
            .location("/some/portal/location")
            .build();

        PortalNavigationItem item = PortalNavigationItem.builder()
            .id("automation-metadata-item")
            .organizationId("org-1")
            .environmentId("env-automation-metadata")
            .title("Automation Managed Link")
            .segment("automation-managed-link")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(1)
            .published(true)
            .configuration("{ \"url\": \"https://automation.example.com\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("automation-metadata-item")
            .automationMetadata(metadata)
            .build();

        try {
            portalNavigationItemRepository.create(item);

            var found = portalNavigationItemRepository.findById("automation-metadata-item");
            assertThat(found).isPresent();
            AutomationMetadata foundMetadata = found.get().getAutomationMetadata();
            assertThat(foundMetadata).isNotNull();
            assertThat(foundMetadata.getReferenceType()).isEqualTo(AutomationTargetReferenceType.API);
            assertThat(foundMetadata.getReferenceId()).isEqualTo("automation-api-1");
            assertThat(foundMetadata.getLocation()).isEqualTo("/some/portal/location");
        } finally {
            portalNavigationItemRepository.delete("automation-metadata-item");
        }
    }

    @Test
    public void should_find_by_automation_reference() throws Exception {
        String environmentId = "env-automation-reference";

        PortalNavigationItem matching = PortalNavigationItem.builder()
            .id("automation-reference-matching")
            .organizationId("org-1")
            .environmentId(environmentId)
            .title("Matching Link")
            .segment("matching-link")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(1)
            .published(true)
            .configuration("{ \"url\": \"https://automation.example.com/matching\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("automation-reference-matching")
            .automationMetadata(
                AutomationMetadata.builder().referenceType(AutomationTargetReferenceType.API).referenceId("automation-api-2").build()
            )
            .build();

        PortalNavigationItem differentReference = PortalNavigationItem.builder()
            .id("automation-reference-different")
            .organizationId("org-1")
            .environmentId(environmentId)
            .title("Different Reference Link")
            .segment("different-reference-link")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(2)
            .published(true)
            .configuration("{ \"url\": \"https://automation.example.com/different\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("automation-reference-different")
            .automationMetadata(
                AutomationMetadata.builder().referenceType(AutomationTargetReferenceType.API).referenceId("automation-api-3").build()
            )
            .build();

        PortalNavigationItem noAutomationMetadata = PortalNavigationItem.builder()
            .id("automation-reference-none")
            .organizationId("org-1")
            .environmentId(environmentId)
            .title("Plain Link")
            .segment("plain-link")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(3)
            .published(true)
            .configuration("{ \"url\": \"https://plain.example.com\" }")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId("automation-reference-none")
            .build();

        try {
            portalNavigationItemRepository.create(matching);
            portalNavigationItemRepository.create(differentReference);
            portalNavigationItemRepository.create(noAutomationMetadata);

            List<PortalNavigationItem> found = portalNavigationItemRepository.findByAutomationReference(
                environmentId,
                AutomationTargetReferenceType.API,
                "automation-api-2"
            );

            assertThat(found).extracting(PortalNavigationItem::getId).containsExactly("automation-reference-matching");
        } finally {
            portalNavigationItemRepository.delete("automation-reference-matching");
            portalNavigationItemRepository.delete("automation-reference-different");
            portalNavigationItemRepository.delete("automation-reference-none");
        }
    }
}
