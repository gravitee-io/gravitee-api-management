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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.repository.model.PortalNavigationItemsRepositoryFixtures;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_page.model.*;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemAdapterTest {

    private final PortalNavigationItemAdapter adapter = PortalNavigationItemAdapter.INSTANCE;

    @Nested
    class ToEntity {

        @Test
        void should_map_folder_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aFolder(
                "550e8400-e29b-41d4-a716-446655440000",
                "My Folder",
                "550e8400-e29b-41d4-a716-446655440001"
            );

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity).isInstanceOf(PortalNavigationFolder.class);
            var folder = (PortalNavigationFolder) entity;
            assertThat(folder.getId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440000"));
            assertThat(folder.getOrganizationId()).isEqualTo("org-id");
            assertThat(folder.getEnvironmentId()).isEqualTo("env-id");
            assertThat(folder.getTitle()).isEqualTo("My Folder");
            assertThat(folder.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(folder.getParentId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440001"));
            assertThat(folder.getOrder()).isEqualTo(0);
            assertThat(folder.getRootId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440001"));
        }

        @Test
        void should_map_page_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aPage(
                "550e8400-e29b-41d4-a716-446655440002",
                "My Page",
                "550e8400-e29b-41d4-a716-446655440003",
                null
            );

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity).isInstanceOf(PortalNavigationPage.class);
            var page = (PortalNavigationPage) entity;
            assertThat(page.getId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440002"));
            assertThat(page.getOrganizationId()).isEqualTo("org-id");
            assertThat(page.getEnvironmentId()).isEqualTo("env-id");
            assertThat(page.getTitle()).isEqualTo("My Page");
            assertThat(page.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(page.getPortalPageContentId()).isEqualTo(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440003"));
            assertThat(page.getOrder()).isEqualTo(0);
            assertThat(page.getRootId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440002"));
        }

        @Test
        void should_map_link_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aLink(
                "550e8400-e29b-41d4-a716-446655440004",
                "My Link",
                "https://example.com",
                null
            );

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity).isInstanceOf(PortalNavigationLink.class);
            var link = (PortalNavigationLink) entity;
            assertThat(link.getId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440004"));
            assertThat(link.getOrganizationId()).isEqualTo("org-id");
            assertThat(link.getEnvironmentId()).isEqualTo("env-id");
            assertThat(link.getTitle()).isEqualTo("My Link");
            assertThat(link.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(link.getUrl()).isEqualTo("https://example.com");
            assertThat(link.getOrder()).isEqualTo(0);
            assertThat(link.getRootId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440004"));
        }

        @Test
        void should_map_api_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.anApi(
                "550e8400-e29b-41d4-a716-446655440004",
                "My Link",
                "testApi",
                null
            );

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity).isInstanceOf(PortalNavigationApi.class);
            var api = (PortalNavigationApi) entity;
            assertThat(api.getId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440004"));
            assertThat(api.getOrganizationId()).isEqualTo("org-id");
            assertThat(api.getEnvironmentId()).isEqualTo("env-id");
            assertThat(api.getTitle()).isEqualTo("My Link");
            assertThat(api.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(api.getApiId()).isEqualTo("testApi");
            assertThat(api.getOrder()).isEqualTo(0);
            assertThat(api.getRootId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440004"));
        }

        @Test
        void should_map_api_product_to_entity() {
            var categoryId = PortalCategoryId.random();
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.anApiProduct(
                "550e8400-e29b-41d4-a716-446655440020",
                "My API Product",
                "550e8400-e29b-41d4-a716-446655440021",
                null
            );
            repositoryItem.setCategoryIds(List.of(categoryId.toString()));

            var entity = adapter.toEntity(repositoryItem);

            assertThat(entity).isInstanceOf(PortalNavigationApiProduct.class);
            var apiProduct = (PortalNavigationApiProduct) entity;
            assertThat(apiProduct.getId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440020"));
            assertThat(apiProduct.getApiProductId()).isEqualTo("550e8400-e29b-41d4-a716-446655440021");
            assertThat(apiProduct.getCategoryIds()).containsExactly(categoryId);
            assertThat(apiProduct.getRootId()).isEqualTo(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440020"));
        }

        @Test
        void should_map_blank_or_empty_rootId_to_zero() {
            // Given - repository item with empty rootId (rootId is non-nullable; empty/blank still mapped defensively)
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aFolder(
                "550e8400-e29b-41d4-a716-446655440000",
                "My Folder",
                "550e8400-e29b-41d4-a716-446655440001",
                ""
            );

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity).isInstanceOf(PortalNavigationFolder.class);
            assertThat(((PortalNavigationFolder) entity).getRootId()).isEqualTo(PortalNavigationItemId.zero());

            // Given - repository item with blank rootId
            repositoryItem.setRootId("   ");

            // When
            entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(((PortalNavigationFolder) entity).getRootId()).isEqualTo(PortalNavigationItemId.zero());
        }

        /**
         * Items persisted before visibility was mandatory (e.g. MongoDB, which has no NOT NULL
         * constraint) can still have a null visibility. Defaulting to PUBLIC preserves the pre-existing
         * behaviour where all items were visible.
         */
        @Test
        void should_default_null_visibility_to_public_for_every_type() {
            // Given
            var folder = PortalNavigationItemsRepositoryFixtures.aFolder("550e8400-e29b-41d4-a716-446655440040", "My Folder", null);
            folder.setVisibility(null);
            var page = PortalNavigationItemsRepositoryFixtures.aPage(
                "550e8400-e29b-41d4-a716-446655440041",
                "My Page",
                PortalPageContentId.random().toString(),
                null
            );
            page.setVisibility(null);
            var link = PortalNavigationItemsRepositoryFixtures.aLink("550e8400-e29b-41d4-a716-446655440042", "My Link", null, null);
            link.setVisibility(null);
            var api = PortalNavigationItemsRepositoryFixtures.anApi("550e8400-e29b-41d4-a716-446655440043", "My Api", "apiId", null);
            api.setVisibility(null);
            var apiProduct = PortalNavigationItemsRepositoryFixtures.anApiProduct(
                "550e8400-e29b-41d4-a716-446655440044",
                "My API Product",
                "apiProductId",
                null
            );
            apiProduct.setVisibility(null);

            // Then
            assertThat(adapter.toEntity(folder).getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(adapter.toEntity(page).getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(adapter.toEntity(link).getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(adapter.toEntity(api).getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(adapter.toEntity(apiProduct).getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        }

        /**
         * The hand-written {@code Visibility} switch in {@code repositoryVisibilityToDomain} must map
         * PRIVATE as faithfully as PUBLIC; a mis-mapped PRIVATE would leak private items to anonymous
         * portal viewers without failing any null-visibility test.
         */
        @Test
        void should_map_private_visibility_to_private_for_every_type() {
            // Given
            var folder = PortalNavigationItemsRepositoryFixtures.aFolder("550e8400-e29b-41d4-a716-446655440050", "My Folder", null);
            folder.setVisibility(PortalNavigationItem.Visibility.PRIVATE);
            var page = PortalNavigationItemsRepositoryFixtures.aPage(
                "550e8400-e29b-41d4-a716-446655440051",
                "My Page",
                PortalPageContentId.random().toString(),
                null
            );
            page.setVisibility(PortalNavigationItem.Visibility.PRIVATE);
            var link = PortalNavigationItemsRepositoryFixtures.aLink("550e8400-e29b-41d4-a716-446655440052", "My Link", null, null);
            link.setVisibility(PortalNavigationItem.Visibility.PRIVATE);
            var api = PortalNavigationItemsRepositoryFixtures.anApi("550e8400-e29b-41d4-a716-446655440053", "My Api", "apiId", null);
            api.setVisibility(PortalNavigationItem.Visibility.PRIVATE);
            var apiProduct = PortalNavigationItemsRepositoryFixtures.anApiProduct(
                "550e8400-e29b-41d4-a716-446655440054",
                "My API Product",
                "apiProductId",
                null
            );
            apiProduct.setVisibility(PortalNavigationItem.Visibility.PRIVATE);

            // Then
            assertThat(adapter.toEntity(folder).getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
            assertThat(adapter.toEntity(page).getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
            assertThat(adapter.toEntity(link).getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
            assertThat(adapter.toEntity(api).getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
            assertThat(adapter.toEntity(apiProduct).getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
        }

        @Test
        void should_throw_when_page_configuration_is_missing() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aPage(
                "550e8400-e29b-41d4-a716-446655440005",
                "page",
                PortalPageContentId.random().toString(),
                null
            );
            repositoryItem.setConfiguration(null);

            // When & Then
            assertThatThrownBy(() -> adapter.toEntity(repositoryItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PortalNavigationItem configuration is missing for PAGE type");
        }

        @Test
        void should_throw_when_page_configuration_is_empty() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aPage(
                "550e8400-e29b-41d4-a716-446655440006",
                "page",
                PortalPageContentId.random().toString(),
                null
            );
            repositoryItem.setConfiguration("");

            // When & Then
            assertThatThrownBy(() -> adapter.toEntity(repositoryItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PortalNavigationItem configuration is missing for PAGE type");
        }

        @Test
        void should_throw_when_page_configuration_is_invalid_json() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aPage(
                "550e8400-e29b-41d4-a716-446655440007",
                "page",
                PortalPageContentId.random().toString(),
                null
            );
            repositoryItem.setConfiguration("invalid json");

            // When & Then
            assertThatThrownBy(() -> adapter.toEntity(repositoryItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid configuration for PortalNavigationItem PAGE type");
        }

        @Test
        void should_map_api_category_ids_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.anApi(
                "550e8400-e29b-41d4-a716-446655440026",
                "My Api",
                "apiId",
                null
            );
            var categoryId = PortalCategoryId.random();
            repositoryItem.setCategoryIds(List.of(categoryId.toString()));

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity).isInstanceOf(PortalNavigationApi.class);
            assertThat(((PortalNavigationApi) entity).getCategoryIds()).containsExactly(categoryId);
        }

        @Test
        void should_throw_when_link_configuration_is_missing() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aLink("550e8400-e29b-41d4-a716-446655440008", "link", null, null);
            repositoryItem.setConfiguration(null);

            // When & Then
            assertThatThrownBy(() -> adapter.toEntity(repositoryItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PortalNavigationItem configuration is missing for LINK type");
        }

        @Test
        void should_throw_when_link_configuration_is_invalid_json() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aLink("550e8400-e29b-41d4-a716-446655440009", "link", null, null);
            repositoryItem.setConfiguration("invalid json");

            // When & Then
            assertThatThrownBy(() -> adapter.toEntity(repositoryItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid configuration for PortalNavigationItem LINK type");
        }
    }

    @Nested
    class ToRepository {

        @Test
        void should_map_folder_to_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.aFolder(
                "550e8400-e29b-41d4-a716-446655440010",
                "My Folder",
                PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440011")
            );

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then
            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440010");
            assertThat(repositoryItem.getOrganizationId()).isEqualTo("org-id");
            assertThat(repositoryItem.getEnvironmentId()).isEqualTo("env-id");
            assertThat(repositoryItem.getTitle()).isEqualTo("My Folder");
            assertThat(repositoryItem.getType()).isEqualTo(PortalNavigationItem.Type.FOLDER);
            assertThat(repositoryItem.getArea()).isEqualTo(PortalNavigationItem.Area.TOP_NAVBAR);
            assertThat(repositoryItem.getParentId()).isEqualTo("550e8400-e29b-41d4-a716-446655440011");
            assertThat(repositoryItem.getOrder()).isEqualTo(0);
            assertThat(repositoryItem.getRootId()).isEqualTo("00000000-0000-0000-0000-000000000000");
            assertThat(repositoryItem.getConfiguration()).isEqualTo("{}");
            assertThat(repositoryItem.isPublished()).isTrue();
            assertThat(repositoryItem.getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PUBLIC);
        }

        @Test
        void should_map_page_to_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.aPage("550e8400-e29b-41d4-a716-446655440012", "My Page", null)
                .toBuilder()
                .portalPageContentId(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440013"))
                .build();

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then
            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440012");
            assertThat(repositoryItem.getOrganizationId()).isEqualTo("org-id");
            assertThat(repositoryItem.getEnvironmentId()).isEqualTo("env-id");
            assertThat(repositoryItem.getTitle()).isEqualTo("My Page");
            assertThat(repositoryItem.getType()).isEqualTo(PortalNavigationItem.Type.PAGE);
            assertThat(repositoryItem.getArea()).isEqualTo(PortalNavigationItem.Area.TOP_NAVBAR);
            assertThat(repositoryItem.getOrder()).isEqualTo(0);
            assertThat(repositoryItem.getRootId()).isEqualTo("00000000-0000-0000-0000-000000000000");
            assertThat(repositoryItem.getConfiguration()).isEqualTo("{\"portalPageContentId\":\"550e8400-e29b-41d4-a716-446655440013\"}");
            assertThat(repositoryItem.isPublished()).isTrue();
            assertThat(repositoryItem.getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PUBLIC);
        }

        @Test
        void should_map_link_to_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.aLink("550e8400-e29b-41d4-a716-446655440014", "My Link", null);

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then
            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440014");
            assertThat(repositoryItem.getOrganizationId()).isEqualTo("org-id");
            assertThat(repositoryItem.getEnvironmentId()).isEqualTo("env-id");
            assertThat(repositoryItem.getTitle()).isEqualTo("My Link");
            assertThat(repositoryItem.getType()).isEqualTo(PortalNavigationItem.Type.LINK);
            assertThat(repositoryItem.getArea()).isEqualTo(PortalNavigationItem.Area.TOP_NAVBAR);
            assertThat(repositoryItem.getOrder()).isEqualTo(0);
            assertThat(repositoryItem.getRootId()).isEqualTo("00000000-0000-0000-0000-000000000000");
            assertThat(repositoryItem.getConfiguration()).isEqualTo("{\"url\":\"http://example.com\"}");
            assertThat(repositoryItem.isPublished()).isTrue();
            assertThat(repositoryItem.getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PUBLIC);
        }

        @Test
        void should_map_api_to_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.anApi("550e8400-e29b-41d4-a716-446655440014", "My Link", null, "apiId");

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then
            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440014");
            assertThat(repositoryItem.getOrganizationId()).isEqualTo("org-id");
            assertThat(repositoryItem.getEnvironmentId()).isEqualTo("env-id");
            assertThat(repositoryItem.getTitle()).isEqualTo("My Link");
            assertThat(repositoryItem.getType()).isEqualTo(PortalNavigationItem.Type.API);
            assertThat(repositoryItem.getArea()).isEqualTo(PortalNavigationItem.Area.TOP_NAVBAR);
            assertThat(repositoryItem.getOrder()).isEqualTo(0);
            assertThat(repositoryItem.getRootId()).isEqualTo("00000000-0000-0000-0000-000000000000");
            assertThat(repositoryItem.getApiId()).isEqualTo("apiId");
            assertThat(repositoryItem.isPublished()).isTrue();
            assertThat(repositoryItem.getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PUBLIC);
        }

        @Test
        void should_map_api_product_to_repository() {
            var categoryId = PortalCategoryId.random();
            var entity = PortalNavigationItemFixtures.anApiProduct(
                "550e8400-e29b-41d4-a716-446655440022",
                "My API Product",
                null,
                "550e8400-e29b-41d4-a716-446655440023"
            )
                .toBuilder()
                .categoryIds(List.of(categoryId))
                .build();

            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440022");
            assertThat(repositoryItem.getType()).isEqualTo(PortalNavigationItem.Type.API_PRODUCT);
            assertThat(repositoryItem.getApiProductId()).isEqualTo("550e8400-e29b-41d4-a716-446655440023");
            assertThat(repositoryItem.getCategoryIds()).containsExactly(categoryId.toString());
            assertThat(repositoryItem.getConfiguration()).isEqualTo("{}");
            assertThat(repositoryItem.getRootId()).isEqualTo("00000000-0000-0000-0000-000000000000");
        }

        @Test
        void should_map_api_category_ids_to_repository() {
            // Given
            var categoryId = PortalCategoryId.random();
            var entity = PortalNavigationItemFixtures.anApi("550e8400-e29b-41d4-a716-446655440027", "My Api", null, "apiId");
            entity.update(PortalNavigationItemFixtures.anUpdatePortalNavigationApi(List.of(categoryId)));

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then
            assertThat(repositoryItem.getCategoryIds()).containsExactly(categoryId.toString());
        }

        @Test
        void should_handle_null_parent_id() {
            // Given
            var entity = PortalNavigationItemFixtures.aFolder("550e8400-e29b-41d4-a716-446655440015", "My Folder");

            // When
            var repositoryItem = adapter.toRepository(entity);

            // Then
            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440015");
            assertThat(repositoryItem.getParentId()).isNull();
        }
    }

    @Nested
    class SourceMapping {

        private static final Instant LAST_FETCHED_AT = Instant.parse("2026-07-17T10:00:00Z");
        // Deliberately after the last success: the last attempt failed
        private static final Instant LAST_FETCH_ATTEMPT_AT = Instant.parse("2026-07-17T11:00:00Z");

        private PortalNavigationItemSource aSource() {
            return PortalNavigationItemSource.builder()
                .sourceType("github-fetcher")
                .sourceConfiguration("{\"repository\":\"docs\"}")
                .useAutoFetch(true)
                .fetchCron("0 */10 * * * *")
                .lastFetchedAt(LAST_FETCHED_AT)
                .lastFetchAttemptAt(LAST_FETCH_ATTEMPT_AT)
                .lastFetchError("boom")
                .subtreeImport(true)
                .build();
        }

        @Test
        void should_map_page_source_to_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.aPage("550e8400-e29b-41d4-a716-446655440020", "My Page", null)
                .toBuilder()
                .portalPageContentId(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440013"))
                .source(aSource())
                .build();

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then: everything but the auto-fetch flag lives in the configuration JSON
            assertThat(repositoryItem.isUseAutoFetch()).isTrue();
            var source = configurationSourceOf(repositoryItem);
            assertThat(source.get("type").asText()).isEqualTo("github-fetcher");
            assertThat(source.get("configuration").asText()).isEqualTo("{\"repository\":\"docs\"}");
            assertThat(source.get("fetchCron").asText()).isEqualTo("0 */10 * * * *");
            assertThat(source.get("lastFetchedAt").asText()).isEqualTo(LAST_FETCHED_AT.toString());
            assertThat(source.get("lastFetchAttemptAt").asText()).isEqualTo(LAST_FETCH_ATTEMPT_AT.toString());
            assertThat(source.get("lastFetchError").asText()).isEqualTo("boom");
            assertThat(source.get("subtreeImport").asBoolean()).isTrue();
        }

        @Test
        void should_map_folder_source_to_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.aFolder("550e8400-e29b-41d4-a716-446655440021", "My Folder")
                .toBuilder()
                .source(aSource())
                .build();

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then
            assertThat(repositoryItem.isUseAutoFetch()).isTrue();
            assertThat(configurationSourceOf(repositoryItem).get("type").asText()).isEqualTo("github-fetcher");
        }

        @Test
        void should_map_null_source_to_no_source_in_configuration() {
            // Given
            var entity = PortalNavigationItemFixtures.aPage("550e8400-e29b-41d4-a716-446655440022", "My Page", null)
                .toBuilder()
                .portalPageContentId(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440013"))
                .build();

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then
            assertThat(repositoryItem.isUseAutoFetch()).isFalse();
            assertThat(configurationSourceOf(repositoryItem)).isNull();
        }

        @Test
        void should_map_configuration_source_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aPage(
                "550e8400-e29b-41d4-a716-446655440023",
                "My Page",
                "550e8400-e29b-41d4-a716-446655440013",
                null
            );
            repositoryItem.setConfiguration(
                """
                {
                  "portalPageContentId": "550e8400-e29b-41d4-a716-446655440013",
                  "source": {
                    "type": "github-fetcher",
                    "configuration": "{\\"repository\\":\\"docs\\"}",
                    "fetchCron": "0 */10 * * * *",
                    "lastFetchedAt": "2026-07-17T10:00:00Z",
                    "lastFetchAttemptAt": "2026-07-17T11:00:00Z",
                    "lastFetchError": "boom",
                    "subtreeImport": true
                  }
                }
                """
            );
            repositoryItem.setUseAutoFetch(true);

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity.getSource()).isNotNull();
            assertThat(entity.getSource().getSourceType()).isEqualTo("github-fetcher");
            assertThat(entity.getSource().getSourceConfiguration()).isEqualTo("{\"repository\":\"docs\"}");
            assertThat(entity.getSource().isUseAutoFetch()).isTrue();
            assertThat(entity.getSource().getFetchCron()).isEqualTo("0 */10 * * * *");
            assertThat(entity.getSource().getLastFetchedAt()).isEqualTo(LAST_FETCHED_AT);
            assertThat(entity.getSource().getLastFetchAttemptAt()).isEqualTo(LAST_FETCH_ATTEMPT_AT);
            assertThat(entity.getSource().getLastFetchError()).isEqualTo("boom");
            assertThat(entity.getSource().isSubtreeImport()).isTrue();
        }

        /** Items stored before the key existed: no migration, the field simply reads as absent. */
        @Test
        void should_map_configuration_without_last_fetch_attempt_at_to_null() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aPage(
                "550e8400-e29b-41d4-a716-446655440026",
                "My Page",
                "550e8400-e29b-41d4-a716-446655440013",
                null
            );
            repositoryItem.setConfiguration(
                """
                {
                  "portalPageContentId": "550e8400-e29b-41d4-a716-446655440013",
                  "source": {
                    "type": "github-fetcher",
                    "configuration": "{\\"repository\\":\\"docs\\"}",
                    "fetchCron": "0 */10 * * * *",
                    "lastFetchedAt": "2026-07-17T10:00:00Z"
                  }
                }
                """
            );
            repositoryItem.setUseAutoFetch(true);

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity.getSource().getLastFetchAttemptAt()).isNull();
            assertThat(entity.getSource().getLastFetchedAt()).isEqualTo(LAST_FETCHED_AT);
            // A pre-import row must never read as an import target
            assertThat(entity.getSource().isSubtreeImport()).isFalse();
        }

        @Test
        void should_map_configuration_without_source_to_null_source() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aPage(
                "550e8400-e29b-41d4-a716-446655440024",
                "My Page",
                "550e8400-e29b-41d4-a716-446655440013",
                null
            );

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity.getSource()).isNull();
        }

        private com.fasterxml.jackson.databind.JsonNode configurationSourceOf(
            io.gravitee.repository.management.model.PortalNavigationItem repositoryItem
        ) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readTree(repositoryItem.getConfiguration()).get("source");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Test
        void should_round_trip_source_through_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.aPage("550e8400-e29b-41d4-a716-446655440025", "My Page", null)
                .toBuilder()
                .portalPageContentId(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440013"))
                .source(aSource())
                .build();

            // When
            var roundTripped = adapter.toEntity(
                adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity)
            );

            // Then
            assertThat(roundTripped.getSource()).usingRecursiveComparison().isEqualTo(entity.getSource());
        }
    }

    @Nested
    class AutomationMetadataMapping {

        @Test
        void should_map_automation_metadata_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aLink(
                "550e8400-e29b-41d4-a716-446655440030",
                "My Link",
                null,
                null
            );
            repositoryItem.setAutomationMetadata(
                io.gravitee.repository.management.model.AutomationMetadata.builder()
                    .referenceType(io.gravitee.repository.management.model.AutomationTargetReferenceType.PORTAL)
                    .referenceId("portal-id")
                    .name("ignored-on-read")
                    .location("/projects/alpha")
                    .order(7)
                    .build()
            );

            // When
            var entity = (PortalNavigationLink) adapter.toEntity(repositoryItem);

            // Then: name/order are dropped on the core-facing copy (already live natively on the nav item)
            assertThat(entity.getAutomationMetadata()).isNotNull();
            assertThat(entity.getAutomationMetadata().referenceType()).isEqualTo(AutomationMetadata.ReferenceType.PORTAL);
            assertThat(entity.getAutomationMetadata().referenceId()).isEqualTo("portal-id");
            assertThat(entity.getAutomationMetadata().name()).isNull();
            assertThat(entity.getAutomationMetadata().location()).contains("/projects/alpha");
            assertThat(entity.getAutomationMetadata().order()).isEmpty();
        }

        @Test
        void should_map_null_automation_metadata_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItemsRepositoryFixtures.aLink(
                "550e8400-e29b-41d4-a716-446655440031",
                "My Link",
                null,
                null
            );

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity.getAutomationMetadata()).isNull();
        }

        @Test
        void should_map_automation_metadata_to_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.aLink(
                "550e8400-e29b-41d4-a716-446655440032",
                "My Link",
                null,
                new AutomationMetadata(
                    AutomationMetadata.ReferenceType.PORTAL,
                    "portal-id",
                    "ignored-on-write",
                    Optional.of("/x"),
                    Optional.of(3)
                )
            );

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then: name/order aren't part of the trimmed core-facing metadata, so they're absent on write too
            assertThat(repositoryItem.getAutomationMetadata()).isNotNull();
            assertThat(repositoryItem.getAutomationMetadata().getReferenceType()).isEqualTo(
                io.gravitee.repository.management.model.AutomationTargetReferenceType.PORTAL
            );
            assertThat(repositoryItem.getAutomationMetadata().getReferenceId()).isEqualTo("portal-id");
            assertThat(repositoryItem.getAutomationMetadata().getLocation()).isEqualTo("/x");
        }

        @Test
        void should_map_null_automation_metadata_to_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.aLink("550e8400-e29b-41d4-a716-446655440033", "My Link", null);

            // When
            var repositoryItem = adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity);

            // Then
            assertThat(repositoryItem.getAutomationMetadata()).isNull();
        }

        @Test
        void should_round_trip_automation_metadata_through_repository() {
            // Given
            var entity = PortalNavigationItemFixtures.aLink(
                "550e8400-e29b-41d4-a716-446655440034",
                "My Link",
                null,
                new AutomationMetadata(AutomationMetadata.ReferenceType.PORTAL, "portal-id", null, Optional.of("/x"), Optional.empty())
            );

            // When
            var roundTripped = adapter.toEntity(
                adapter.toRepository((io.gravitee.apim.core.portal_page.model.PortalNavigationItem) entity)
            );

            // Then
            assertThat(roundTripped.getAutomationMetadata()).isEqualTo(entity.getAutomationMetadata());
        }
    }
}
