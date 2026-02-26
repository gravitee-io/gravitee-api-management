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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.PortalNavigationItemsFixtures;
import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsMapperTest {

    private PortalNavigationItemsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = PortalNavigationItemsMapper.INSTANCE;
    }

    @Nested
    class DomainToResource {

        @Test
        void should_map_portal_navigation_page() {
            var page = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE_ID, "My Page", null);
            page.setOrder(1);

            var result = mapper.map(page);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage.class);
            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.PAGE_ID));
            assertThat(result.getOrganizationId()).isEqualTo("org-id");
            assertThat(result.getEnvironmentId()).isEqualTo("env-id");
            assertThat(result.getTitle()).isEqualTo("My Page");
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.PAGE);
            assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(1);
            assertThat(result.getParentId()).isNull();
            assertThat(result.getPortalPageContentId()).isEqualTo(page.getPortalPageContentId().id());
        }

        @Test
        void should_map_portal_navigation_folder() {
            var folder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemFixtures.FOLDER_ID, "My Folder");
            folder.setOrder(2);

            var result = mapper.map(folder);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder.class);
            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.FOLDER_ID));
            assertThat(result.getOrganizationId()).isEqualTo("org-id");
            assertThat(result.getEnvironmentId()).isEqualTo("env-id");
            assertThat(result.getTitle()).isEqualTo("My Folder");
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.FOLDER);
            assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(2);
            assertThat(result.getParentId()).isNull();
        }

        @Test
        void should_map_portal_navigation_link() {
            var link = PortalNavigationItemFixtures.aLink();

            var result = mapper.map(link);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink.class);
            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.LINK_ID));
            assertThat(result.getOrganizationId()).isEqualTo("org-id");
            assertThat(result.getEnvironmentId()).isEqualTo("env-id");
            assertThat(result.getTitle()).isEqualTo("My Link");
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.LINK);
            assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(3);
            assertThat(result.getParentId()).isNull();
            assertThat(result.getUrl()).isEqualTo("https://example.com");
        }

        @Test
        void should_map_portal_navigation_api() {
            var api = PortalNavigationItemFixtures.anApi();

            var result = mapper.map(api);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationApi.class);
            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.API_ID));
            assertThat(result.getOrganizationId()).isEqualTo("org-id");
            assertThat(result.getEnvironmentId()).isEqualTo("env-id");
            assertThat(result.getTitle()).isEqualTo("My Api");
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.API);
            assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(3);
            assertThat(result.getParentId()).isNull();
            assertThat(result.getApiId()).isEqualTo("apiId");
        }

        @Test
        void should_map_list_of_portal_navigation_items() {
            var items = PortalNavigationItemFixtures.sampleNavigationItems();

            var result = mapper.map(items);

            assertThat(result).hasSize(13);
            // Check that all items are mapped correctly
            assertThat(
                result
                    .stream()
                    .map(i -> (BasePortalNavigationItem) i.getActualInstance())
                    .map(BasePortalNavigationItem::getId)
            ).containsExactlyInAnyOrder(
                PortalNavigationItemFixtures.SAMPLE_NAVIGATION_ITEMS_IDS.stream().map(UUID::fromString).toArray(UUID[]::new)
            );
        }
    }

    @Nested
    class ResourceToDomain {

        @Test
        void should_map_create_portal_navigation_page() {
            final var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage();

            var result = mapper.map(page);

            assertThat(result).isInstanceOf(CreatePortalNavigationItem.class);
            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.PAGE);
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo(page.getTitle());
            assertThat(result.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(1);
            assertThat(result.getParentId().id()).isEqualTo(page.getParentId());
            assertThat(result.getPortalPageContentId().id()).isEqualTo(((CreatePortalNavigationPage) page).getPortalPageContentId());
        }

        @Test
        void should_map_create_portal_navigation_folder() {
            final var folder = PortalNavigationItemsFixtures.aCreatePortalNavigationFolder();

            var result = mapper.map(folder);

            assertThat(result).isInstanceOf(CreatePortalNavigationItem.class);
            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.FOLDER);
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo(folder.getTitle());
            assertThat(result.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(2);
            assertThat(result.getParentId().id()).isEqualTo(folder.getParentId());
        }

        @Test
        void should_map_create_portal_navigation_link() {
            final var link = PortalNavigationItemsFixtures.aCreatePortalNavigationLink();

            var result = mapper.map(link);

            assertThat(result).isInstanceOf(CreatePortalNavigationItem.class);
            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.LINK);
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo(link.getTitle());
            assertThat(result.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(3);
            assertThat(result.getParentId().id()).isEqualTo(link.getParentId());
            assertThat(result.getUrl()).isEqualTo(((CreatePortalNavigationLink) link).getUrl());
        }

        @Test
        void should_map_bulk_create_portal_navigation_items() {
            final var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage();
            final var folder = PortalNavigationItemsFixtures.aCreatePortalNavigationFolder();
            final var link = PortalNavigationItemsFixtures.aCreatePortalNavigationLink();
            final var api = PortalNavigationItemsFixtures.aCreatePortalNavigationApi();

            final var requestItems = java.util.List.of(page, folder, link, api);

            final var result = mapper.mapCreatePortalNavigationItems(requestItems);

            assertThat(result).hasSize(4);
            assertThat(result)
                .extracting(io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem::getType)
                .containsExactly(
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.PAGE,
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.FOLDER,
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.LINK,
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.API
                );
        }
    }
}
