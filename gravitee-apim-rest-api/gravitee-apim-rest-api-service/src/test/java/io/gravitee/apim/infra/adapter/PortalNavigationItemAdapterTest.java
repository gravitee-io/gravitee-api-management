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
import io.gravitee.apim.core.portal_page.model.*;
import io.gravitee.repository.management.model.PortalNavigationItem;
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
            var entity = PortalNavigationItemFixtures.aFolder("550e8400-e29b-41d4-a716-446655440010", "My Folder");
            entity.setParentId(PortalNavigationItemId.of("550e8400-e29b-41d4-a716-446655440011"));

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
            assertThat(repositoryItem.getApiId()).isEqualTo("apiId");
            assertThat(repositoryItem.isPublished()).isTrue();
            assertThat(repositoryItem.getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PUBLIC);
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
}
