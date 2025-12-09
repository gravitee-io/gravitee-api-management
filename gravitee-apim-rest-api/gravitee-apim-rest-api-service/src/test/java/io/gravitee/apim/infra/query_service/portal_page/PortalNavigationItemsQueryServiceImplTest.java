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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.infra.adapter.PortalNavigationItemAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.api.search.PortalNavigationItemCriteria;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsQueryServiceImplTest {

    @Mock
    PortalNavigationItemRepository repository;

    PortalNavigationItemsQueryServiceImpl service;

    private static final PortalNavigationItemAdapter adapter = PortalNavigationItemAdapter.INSTANCE;

    @BeforeEach
    void setUp() {
        service = new PortalNavigationItemsQueryServiceImpl(repository);
    }

    @Nested
    class FindByIdAndEnvironmentId {

        @Test
        void should_return_item_when_found_and_environment_matches() throws TechnicalException {
            // Given
            var itemId = "00000000-0000-0000-0000-000000000001";
            var environmentId = "env-id";
            var repoItem = new io.gravitee.repository.management.model.PortalNavigationItem();
            repoItem.setId(itemId);
            repoItem.setEnvironmentId(environmentId);
            repoItem.setTitle("Test Item");
            repoItem.setType(io.gravitee.repository.management.model.PortalNavigationItem.Type.FOLDER);
            repoItem.setArea(io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR);
            when(repository.findById(itemId)).thenReturn(Optional.of(repoItem));

            // When
            var result = service.findByIdAndEnvironmentId(environmentId, PortalNavigationItemId.of(itemId));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Item");
            assertThat(result).isInstanceOf(PortalNavigationItem.class);
        }

        @Test
        void should_return_null_when_item_not_found() throws TechnicalException {
            // Given
            var itemId = "00000000-0000-0000-0000-000000000001";
            var environmentId = "env-id";
            when(repository.findById(itemId)).thenReturn(Optional.empty());

            // When
            var result = service.findByIdAndEnvironmentId(environmentId, PortalNavigationItemId.of(itemId));

            // Then
            assertThat(result).isNull();
        }

        @Test
        void should_return_null_when_environment_does_not_match() throws TechnicalException {
            // Given
            var itemId = "00000000-0000-0000-0000-000000000001";
            var environmentId = "env-id";
            var repoItem = new io.gravitee.repository.management.model.PortalNavigationItem();
            repoItem.setId(itemId);
            repoItem.setEnvironmentId("different-env");
            when(repository.findById(itemId)).thenReturn(Optional.of(repoItem));

            // When
            var result = service.findByIdAndEnvironmentId(environmentId, PortalNavigationItemId.of(itemId));

            // Then
            assertThat(result).isNull();
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            // Given
            var itemId = "00000000-0000-0000-0000-000000000001";
            var environmentId = "env-id";
            when(repository.findById(itemId)).thenThrow(new TechnicalException("Database error"));

            // When & Then
            assertThatThrownBy(() -> service.findByIdAndEnvironmentId(environmentId, PortalNavigationItemId.of(itemId)))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while finding portal navigation item by id 00000000-0000-0000-0000-000000000001 and environmentId env-id"
                )
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }

    @Nested
    class FindByParentIdAndEnvironmentId {

        @Test
        void should_return_filtered_items_by_parent_id() throws TechnicalException {
            // Given
            var environmentId = "env-id";
            var parentId = "00000000-0000-0000-0000-000000000002";
            var item1 = new io.gravitee.repository.management.model.PortalNavigationItem();
            item1.setId("00000000-0000-0000-0000-000000000003");
            item1.setEnvironmentId(environmentId);
            item1.setParentId(parentId);
            item1.setTitle("Item 1");
            item1.setType(io.gravitee.repository.management.model.PortalNavigationItem.Type.PAGE);
            item1.setArea(io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR);
            item1.setConfiguration("{ \"portalPageContentId\": \"00000000-0000-0000-0001-000000000004\" }");

            var repoItems = List.of(item1);
            when(repository.findAllByParentIdAndEnvironmentId(parentId, environmentId)).thenReturn(repoItems);

            // When
            var result = service.findByParentIdAndEnvironmentId(environmentId, PortalNavigationItemId.of(parentId));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getTitle()).isEqualTo("Item 1");
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception_for_find_by_parent_id()
            throws TechnicalException {
            // Given
            var environmentId = "env-id";
            var parentId = "00000000-0000-0000-0000-000000000002";
            when(repository.findAllByParentIdAndEnvironmentId(parentId, environmentId)).thenThrow(new TechnicalException("Database error"));

            // When & Then
            assertThatThrownBy(() -> service.findByParentIdAndEnvironmentId(environmentId, PortalNavigationItemId.of(parentId)))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while finding portal navigation items by parentId 00000000-0000-0000-0000-000000000002 and environmentId env-id"
                )
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }

    @Nested
    class FindTopLevelItemsByEnvironmentIdAndPortalArea {

        @Test
        void should_return_top_level_items_filtered_by_area_and_no_parent() throws TechnicalException {
            // Given
            var environmentId = "env-id";
            var portalArea = PortalArea.TOP_NAVBAR;
            var repoArea = io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR;

            var item1 = new io.gravitee.repository.management.model.PortalNavigationItem();
            item1.setId("00000000-0000-0000-0000-000000000006");
            item1.setEnvironmentId(environmentId);
            item1.setParentId(null);
            item1.setTitle("Top Level");
            item1.setType(io.gravitee.repository.management.model.PortalNavigationItem.Type.FOLDER);
            item1.setArea(repoArea);

            var repoItems = List.of(item1);
            when(repository.findAllByAreaAndEnvironmentIdAndParentIdIsNull(repoArea, environmentId)).thenReturn(repoItems);

            // When
            var result = service.findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, portalArea);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getTitle()).isEqualTo("Top Level");
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception_for_find_top_level_items()
            throws TechnicalException {
            // Given
            var environmentId = "env-id";
            var portalArea = PortalArea.TOP_NAVBAR;
            var repoArea = io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR;
            when(repository.findAllByAreaAndEnvironmentIdAndParentIdIsNull(repoArea, environmentId)).thenThrow(
                new TechnicalException("Database error")
            );

            // When & Then
            assertThatThrownBy(() -> service.findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, portalArea))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while finding top level portal navigation items by environmentId env-id and area TOP_NAVBAR")
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }

    @Nested
    class Search {

        @Test
        void should_map_all_criteria_fields_correctly() throws TechnicalException {
            // Given
            var environmentId = "env-id";
            var parentId = PortalNavigationItemId.random();
            var criteria = PortalNavigationItemQueryCriteria.builder()
                .environmentId(environmentId)
                .parentId(parentId)
                .area(PortalArea.TOP_NAVBAR)
                .published(true)
                .root(true)
                .visibility(PortalVisibility.PRIVATE)
                .build();

            var domainItem = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().json(), "Test Item", parentId);
            domainItem.setEnvironmentId(environmentId);
            domainItem.setPublished(true);
            domainItem.setVisibility(PortalVisibility.PRIVATE);
            var repoItem = adapter.toRepository(domainItem);

            var repoItems = List.of(repoItem);
            ArgumentCaptor<PortalNavigationItemCriteria> criteriaCaptor = ArgumentCaptor.forClass(PortalNavigationItemCriteria.class);
            when(repository.searchByCriteria(criteriaCaptor.capture())).thenReturn(repoItems);

            // When
            var result = service.search(criteria);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getTitle()).isEqualTo("Test Item");
            var capturedCriteria = criteriaCaptor.getValue();
            assertThat(capturedCriteria.getEnvironmentId()).isEqualTo(environmentId);
            assertThat(capturedCriteria.getParentId()).isEqualTo(parentId.json());
            assertThat(capturedCriteria.getPortalArea()).isEqualTo("TOP_NAVBAR");
            assertThat(capturedCriteria.getPublished()).isTrue();
            assertThat(capturedCriteria.getRoot()).isTrue();
            assertThat(capturedCriteria.getVisibility()).isEqualTo("PRIVATE");
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            // Given
            var environmentId = "env-id";
            var criteria = PortalNavigationItemQueryCriteria.builder().environmentId(environmentId).build();
            ArgumentCaptor<PortalNavigationItemCriteria> criteriaCaptor = ArgumentCaptor.forClass(PortalNavigationItemCriteria.class);
            when(repository.searchByCriteria(criteriaCaptor.capture())).thenThrow(new TechnicalException("Database error"));

            // When & Then
            assertThatThrownBy(() -> service.search(criteria))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessageContaining("An error occurred while searching portal navigation items by criteria")
                .hasCauseInstanceOf(TechnicalException.class);
            var capturedCriteria = criteriaCaptor.getValue();
            assertThat(capturedCriteria.getEnvironmentId()).isEqualTo(environmentId);
        }
    }
}
