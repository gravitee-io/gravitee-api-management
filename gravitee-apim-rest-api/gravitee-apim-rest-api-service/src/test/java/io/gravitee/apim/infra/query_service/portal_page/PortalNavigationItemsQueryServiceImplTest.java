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
package io.gravitee.apim.infra.query_service.portal_page;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalNavigationItemRepositoryInMemory;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalPageNavigationId;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsQueryServiceImplTest {

    PortalNavigationItemRepositoryInMemory repository;
    PortalNavigationItemsQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new PortalNavigationItemRepositoryInMemory();
        service = new PortalNavigationItemsQueryServiceImpl(repository);
        repository.reset();
    }

    @Nested
    class FindByIdAndEnvironmentId {

        @Test
        @SneakyThrows
        void should_return_item_when_found_and_environment_matches() {
            // Given
            var itemId = "00000000-0000-0000-0000-000000000001";
            var environmentId = "env-id";
            var repoItem = PortalNavigationItem.builder()
                .id(itemId)
                .environmentId(environmentId)
                .title("Test Item")
                .type(PortalNavigationItem.Type.FOLDER)
                .area(PortalNavigationItem.Area.TOP_NAVBAR)
                .build();
            repository.initWith(List.of(repoItem));

            // When
            var result = service.findByIdAndEnvironmentId(environmentId, PortalPageNavigationId.of(itemId));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Item");
            assertThat(result).isInstanceOf(PortalNavigationFolder.class);
        }

        @Test
        @SneakyThrows
        void should_return_null_when_item_not_found() {
            // Given
            var itemId = "00000000-0000-0000-0000-000000000001";
            var environmentId = "env-id";

            // When
            var result = service.findByIdAndEnvironmentId(environmentId, PortalPageNavigationId.of(itemId));

            // Then
            assertThat(result).isNull();
        }

        @Test
        @SneakyThrows
        void should_return_null_when_environment_does_not_match() {
            // Given
            var itemId = "00000000-0000-0000-0000-000000000001";
            var environmentId = "env-id";
            var repoItem = PortalNavigationItem.builder().id(itemId).environmentId("different-env").build();
            repository.initWith(List.of(repoItem));

            // When
            var result = service.findByIdAndEnvironmentId(environmentId, PortalPageNavigationId.of(itemId));

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    class FindByParentIdAndEnvironmentId {

        @Test
        @SneakyThrows
        void should_return_filtered_items_by_parent_id() {
            // Given
            var environmentId = "env-id";
            var parentId = "00000000-0000-0000-0000-000000000002";
            var repoItems = List.of(
                PortalNavigationItem.builder()
                    .id("00000000-0000-0000-0000-000000000003")
                    .environmentId(environmentId)
                    .parentId(parentId)
                    .title("Item 1")
                    .type(PortalNavigationItem.Type.PAGE)
                    .area(PortalNavigationItem.Area.TOP_NAVBAR)
                    .configuration("{ \"pageId\": \"00000000-0000-0000-0001-000000000004\" }")
                    .build(),
                PortalNavigationItem.builder()
                    .id("00000000-0000-0000-0000-000000000004")
                    .environmentId(environmentId)
                    .parentId("00000000-0000-0000-0000-000000000005")
                    .title("Item 2")
                    .build()
            );
            repository.initWith(repoItems);

            // When
            var result = service.findByParentIdAndEnvironmentId(environmentId, PortalPageNavigationId.of(parentId));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.iterator().next().getTitle()).isEqualTo("Item 1");
        }
    }

    @Nested
    class FindTopLevelItemsByEnvironmentId {

        @Test
        @SneakyThrows
        void should_return_top_level_items_filtered_by_area_and_no_parent() {
            // Given
            var environmentId = "env-id";
            var portalArea = PortalArea.TOP_NAVBAR;
            var repoArea = io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR;
            var repoItems = List.of(
                PortalNavigationItem.builder()
                    .id("00000000-0000-0000-0000-000000000006")
                    .environmentId(environmentId)
                    .parentId(null)
                    .title("Top Level")
                    .type(PortalNavigationItem.Type.FOLDER)
                    .area(repoArea)
                    .build(),
                PortalNavigationItem.builder()
                    .id("00000000-0000-0000-0000-000000000007")
                    .environmentId(environmentId)
                    .parentId("00000000-0000-0000-0000-000000000008")
                    .title("Child")
                    .area(repoArea)
                    .build()
            );
            repository.initWith(repoItems);

            // When
            var result = service.findTopLevelItemsByEnvironmentId(environmentId, portalArea);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.iterator().next().getTitle()).isEqualTo("Top Level");
        }
    }
}
