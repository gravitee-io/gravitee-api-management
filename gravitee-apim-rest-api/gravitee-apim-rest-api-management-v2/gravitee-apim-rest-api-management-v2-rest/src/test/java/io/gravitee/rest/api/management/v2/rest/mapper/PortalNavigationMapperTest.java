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

import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationMapperTest {

    private PortalNavigationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = PortalNavigationMapper.INSTANCE;
    }

    @Test
    void should_map_portal_navigation_page() {
        String pageId = "12345678-1234-1234-1234-123456789abc";
        var page = PortalNavigationItemFixtures.aPage(pageId, "My Page", null);
        page.setOrder(1);

        var result = mapper.map(page);

        assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage.class);
        assertThat(result.getId()).isEqualTo(pageId);
        assertThat(result.getOrganizationId()).isEqualTo("org-id");
        assertThat(result.getEnvironmentId()).isEqualTo("env-id");
        assertThat(result.getTitle()).isEqualTo("My Page");
        assertThat(result.getType()).isEqualTo(BasePortalNavigationItem.TypeEnum.PAGE);
        assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
        assertThat(result.getOrder()).isEqualTo(1);
        assertThat(result.getParentId()).isNull();
        assertThat(result.getConfiguration()).isNotNull();
        assertThat(result.getConfiguration().getPortalPageContentId()).isEqualTo(page.getPortalPageContentId().toString());
    }

    @Test
    void should_map_portal_navigation_folder() {
        String folderId = "87654321-4321-4321-4321-cba987654321";
        var folder = PortalNavigationItemFixtures.aFolder(folderId, "My Folder");
        folder.setOrder(2);

        var result = mapper.map(folder);

        assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder.class);
        assertThat(result.getId()).isEqualTo(folderId);
        assertThat(result.getOrganizationId()).isEqualTo("org-id");
        assertThat(result.getEnvironmentId()).isEqualTo("env-id");
        assertThat(result.getTitle()).isEqualTo("My Folder");
        assertThat(result.getType()).isEqualTo(BasePortalNavigationItem.TypeEnum.FOLDER);
        assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
        assertThat(result.getOrder()).isEqualTo(2);
        assertThat(result.getParentId()).isNull();
    }

    @Test
    void should_map_portal_navigation_link() {
        String linkId = "abcd1234-5678-9012-3456-789012345678";
        var link = new PortalNavigationLink(
            PortalNavigationItemId.of(linkId),
            "org-id",
            "env-id",
            "My Link",
            PortalArea.TOP_NAVBAR,
            "https://example.com"
        );
        link.setOrder(3);

        var result = mapper.map(link);

        assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink.class);
        assertThat(result.getId()).isEqualTo(linkId);
        assertThat(result.getOrganizationId()).isEqualTo("org-id");
        assertThat(result.getEnvironmentId()).isEqualTo("env-id");
        assertThat(result.getTitle()).isEqualTo("My Link");
        assertThat(result.getType()).isEqualTo(BasePortalNavigationItem.TypeEnum.LINK);
        assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
        assertThat(result.getOrder()).isEqualTo(3);
        assertThat(result.getParentId()).isNull();
        assertThat(result.getConfiguration()).isNotNull();
        assertThat(result.getConfiguration().getUrl()).isEqualTo("https://example.com");
    }

    @Test
    void should_map_list_of_portal_navigation_items() {
        var items = PortalNavigationItemFixtures.sampleNavigationItems();

        var result = mapper.map(items);

        assertThat(result).hasSize(8);
        // Check that all items are mapped correctly
        assertThat(
            result
                .stream()
                .map(i -> (BasePortalNavigationItem) i.getActualInstance())
                .map(BasePortalNavigationItem::getId)
        ).containsExactlyInAnyOrder(
            "00000000-0000-0000-0000-000000000001",
            "00000000-0000-0000-0000-000000000002",
            "00000000-0000-0000-0000-000000000003",
            "00000000-0000-0000-0000-000000000004",
            "00000000-0000-0000-0000-000000000005",
            "00000000-0000-0000-0000-000000000006",
            "00000000-0000-0000-0000-000000000007",
            "00000000-0000-0000-0000-000000000008"
        );
    }
}
