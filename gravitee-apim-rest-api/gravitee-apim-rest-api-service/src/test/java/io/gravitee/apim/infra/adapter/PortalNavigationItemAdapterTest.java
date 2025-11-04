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
            var repositoryItem = PortalNavigationItem.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .organizationId("org-id")
                .environmentId("env-id")
                .title("My Folder")
                .type(PortalNavigationItem.Type.FOLDER)
                .area(PortalNavigationItem.Area.TOP_NAVBAR)
                .parentId("550e8400-e29b-41d4-a716-446655440001")
                .order(1)
                .configuration("{}")
                .build();

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity).isInstanceOf(PortalNavigationFolder.class);
            var folder = (PortalNavigationFolder) entity;
            assertThat(folder.getId()).isEqualTo(PortalPageNavigationId.of("550e8400-e29b-41d4-a716-446655440000"));
            assertThat(folder.getOrganizationId()).isEqualTo("org-id");
            assertThat(folder.getEnvironmentId()).isEqualTo("env-id");
            assertThat(folder.getTitle()).isEqualTo("My Folder");
            assertThat(folder.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(folder.getParentId()).isEqualTo(PortalPageNavigationId.of("550e8400-e29b-41d4-a716-446655440001"));
            assertThat(folder.getOrder()).isEqualTo(1);
        }

        @Test
        void should_map_page_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItem.builder()
                .id("550e8400-e29b-41d4-a716-446655440002")
                .organizationId("org-id")
                .environmentId("env-id")
                .title("My Page")
                .type(PortalNavigationItem.Type.PAGE)
                .area(PortalNavigationItem.Area.HOMEPAGE)
                .order(2)
                .configuration("{\"pageId\":\"550e8400-e29b-41d4-a716-446655440003\"}")
                .build();

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity).isInstanceOf(PortalNavigationPage.class);
            var page = (PortalNavigationPage) entity;
            assertThat(page.getId()).isEqualTo(PortalPageNavigationId.of("550e8400-e29b-41d4-a716-446655440002"));
            assertThat(page.getOrganizationId()).isEqualTo("org-id");
            assertThat(page.getEnvironmentId()).isEqualTo("env-id");
            assertThat(page.getTitle()).isEqualTo("My Page");
            assertThat(page.getArea()).isEqualTo(PortalArea.HOMEPAGE);
            assertThat(page.getContentId()).isEqualTo(PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440003"));
            assertThat(page.getOrder()).isEqualTo(2);
        }

        @Test
        void should_map_link_to_entity() {
            // Given
            var repositoryItem = PortalNavigationItem.builder()
                .id("550e8400-e29b-41d4-a716-446655440004")
                .organizationId("org-id")
                .environmentId("env-id")
                .title("My Link")
                .type(PortalNavigationItem.Type.LINK)
                .area(PortalNavigationItem.Area.TOP_NAVBAR)
                .order(3)
                .configuration("{\"href\":\"https://example.com\"}")
                .build();

            // When
            var entity = adapter.toEntity(repositoryItem);

            // Then
            assertThat(entity).isInstanceOf(PortalNavigationLink.class);
            var link = (PortalNavigationLink) entity;
            assertThat(link.getId()).isEqualTo(PortalPageNavigationId.of("550e8400-e29b-41d4-a716-446655440004"));
            assertThat(link.getOrganizationId()).isEqualTo("org-id");
            assertThat(link.getEnvironmentId()).isEqualTo("env-id");
            assertThat(link.getTitle()).isEqualTo("My Link");
            assertThat(link.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(link.getHref()).isEqualTo("https://example.com");
            assertThat(link.getOrder()).isEqualTo(3);
        }

        @Test
        void should_throw_when_page_configuration_is_missing() {
            // Given
            var repositoryItem = PortalNavigationItem.builder()
                .id("550e8400-e29b-41d4-a716-446655440005")
                .type(PortalNavigationItem.Type.PAGE)
                .area(PortalNavigationItem.Area.TOP_NAVBAR)
                .configuration(null)
                .build();

            // When & Then
            assertThatThrownBy(() -> adapter.toEntity(repositoryItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PortalNavigationItem configuration is missing for PAGE type");
        }

        @Test
        void should_throw_when_page_configuration_is_empty() {
            // Given
            var repositoryItem = PortalNavigationItem.builder()
                .id("550e8400-e29b-41d4-a716-446655440006")
                .type(PortalNavigationItem.Type.PAGE)
                .area(PortalNavigationItem.Area.TOP_NAVBAR)
                .configuration("")
                .build();

            // When & Then
            assertThatThrownBy(() -> adapter.toEntity(repositoryItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PortalNavigationItem configuration is missing for PAGE type");
        }

        @Test
        void should_throw_when_page_configuration_is_invalid_json() {
            // Given
            var repositoryItem = PortalNavigationItem.builder()
                .id("550e8400-e29b-41d4-a716-446655440007")
                .type(PortalNavigationItem.Type.PAGE)
                .area(PortalNavigationItem.Area.TOP_NAVBAR)
                .configuration("invalid json")
                .build();

            // When & Then
            assertThatThrownBy(() -> adapter.toEntity(repositoryItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid configuration for PortalNavigationItem PAGE type");
        }

        @Test
        void should_throw_when_link_configuration_is_missing() {
            // Given
            var repositoryItem = PortalNavigationItem.builder()
                .id("550e8400-e29b-41d4-a716-446655440008")
                .type(PortalNavigationItem.Type.LINK)
                .area(PortalNavigationItem.Area.TOP_NAVBAR)
                .configuration(null)
                .build();

            // When & Then
            assertThatThrownBy(() -> adapter.toEntity(repositoryItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PortalNavigationItem configuration is missing for LINK type");
        }

        @Test
        void should_throw_when_link_configuration_is_invalid_json() {
            // Given
            var repositoryItem = PortalNavigationItem.builder()
                .id("550e8400-e29b-41d4-a716-446655440009")
                .type(PortalNavigationItem.Type.LINK)
                .area(PortalNavigationItem.Area.TOP_NAVBAR)
                .configuration("invalid json")
                .build();

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
            var entity = new PortalNavigationFolder(
                PortalPageNavigationId.of("550e8400-e29b-41d4-a716-446655440010"),
                "org-id",
                "env-id",
                "My Folder",
                PortalArea.TOP_NAVBAR
            );
            entity.setParentId(PortalPageNavigationId.of("550e8400-e29b-41d4-a716-446655440011"));
            entity.setOrder(1);

            // When
            var repositoryItem = adapter.toRepository(entity);

            // Then
            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440010");
            assertThat(repositoryItem.getOrganizationId()).isEqualTo("org-id");
            assertThat(repositoryItem.getEnvironmentId()).isEqualTo("env-id");
            assertThat(repositoryItem.getTitle()).isEqualTo("My Folder");
            assertThat(repositoryItem.getType()).isEqualTo(PortalNavigationItem.Type.FOLDER);
            assertThat(repositoryItem.getArea()).isEqualTo(PortalNavigationItem.Area.TOP_NAVBAR);
            assertThat(repositoryItem.getParentId()).isEqualTo("550e8400-e29b-41d4-a716-446655440011");
            assertThat(repositoryItem.getOrder()).isEqualTo(1);
            assertThat(repositoryItem.getConfiguration()).isEqualTo("{}");
        }

        @Test
        void should_map_page_to_repository() {
            // Given
            var entity = new PortalNavigationPage(
                PortalPageNavigationId.of("550e8400-e29b-41d4-a716-446655440012"),
                "org-id",
                "env-id",
                "My Page",
                PortalArea.HOMEPAGE,
                PortalPageContentId.of("550e8400-e29b-41d4-a716-446655440013")
            );
            entity.setOrder(2);

            // When
            var repositoryItem = adapter.toRepository(entity);

            // Then
            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440012");
            assertThat(repositoryItem.getOrganizationId()).isEqualTo("org-id");
            assertThat(repositoryItem.getEnvironmentId()).isEqualTo("env-id");
            assertThat(repositoryItem.getTitle()).isEqualTo("My Page");
            assertThat(repositoryItem.getType()).isEqualTo(PortalNavigationItem.Type.PAGE);
            assertThat(repositoryItem.getArea()).isEqualTo(PortalNavigationItem.Area.HOMEPAGE);
            assertThat(repositoryItem.getOrder()).isEqualTo(2);
            assertThat(repositoryItem.getConfiguration()).isEqualTo("{\"pageId\":\"550e8400-e29b-41d4-a716-446655440013\"}");
        }

        @Test
        void should_map_link_to_repository() {
            // Given
            var entity = new PortalNavigationLink(
                PortalPageNavigationId.of("550e8400-e29b-41d4-a716-446655440014"),
                "org-id",
                "env-id",
                "My Link",
                PortalArea.TOP_NAVBAR,
                "https://example.com"
            );
            entity.setOrder(3);

            // When
            var repositoryItem = adapter.toRepository(entity);

            // Then
            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440014");
            assertThat(repositoryItem.getOrganizationId()).isEqualTo("org-id");
            assertThat(repositoryItem.getEnvironmentId()).isEqualTo("env-id");
            assertThat(repositoryItem.getTitle()).isEqualTo("My Link");
            assertThat(repositoryItem.getType()).isEqualTo(PortalNavigationItem.Type.LINK);
            assertThat(repositoryItem.getArea()).isEqualTo(PortalNavigationItem.Area.TOP_NAVBAR);
            assertThat(repositoryItem.getOrder()).isEqualTo(3);
            assertThat(repositoryItem.getConfiguration()).isEqualTo("{\"href\":\"https://example.com\"}");
        }

        @Test
        void should_handle_null_parent_id() {
            // Given
            var entity = new PortalNavigationFolder(
                PortalPageNavigationId.of("550e8400-e29b-41d4-a716-446655440015"),
                "org-id",
                "env-id",
                "My Folder",
                PortalArea.TOP_NAVBAR
            );

            // When
            var repositoryItem = adapter.toRepository(entity);

            // Then
            assertThat(repositoryItem.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440015");
            assertThat(repositoryItem.getParentId()).isNull();
        }
    }
}
