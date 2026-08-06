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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SourcedPageDescendantsFinderTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    private ArrayList<PortalNavigationItem> storage;
    private SourcedPageDescendantsFinder cut;

    @BeforeEach
    void setUp() {
        storage = new ArrayList<>();
        cut = new SourcedPageDescendantsFinder(new PortalNavigationItemsQueryServiceInMemory(storage));
    }

    @Test
    void should_collect_sourced_pages_across_nested_folders() {
        var root = aFolder("Guides", null);
        var nested = aFolder("Advanced", root.getId());
        var sourced = aPage("Sourced", root.getId(), aSource());
        var nestedSourced = aPage("Nested Sourced", nested.getId(), aSource());
        aPage("Inline", root.getId(), null);

        assertThat(cut.findSourcedPageDescendants(ENV_ID, root.getId()))
            .extracting(PortalNavigationPage::getId)
            .containsExactlyInAnyOrder(sourced.getId(), nestedSourced.getId());
    }

    @Test
    void should_terminate_and_collect_each_page_once_when_the_tree_contains_a_cycle() {
        // Corrupted tree: the walk would loop between the two folders forever
        var firstId = PortalNavigationItemId.random();
        var secondId = PortalNavigationItemId.random();
        aFolder(firstId, "First", secondId);
        aFolder(secondId, "Second", firstId);
        var sourced = aPage("Sourced", secondId, aSource());

        var descendants = cut.findSourcedPageDescendants(ENV_ID, firstId);

        assertThat(descendants).extracting(PortalNavigationPage::getId).containsExactly(sourced.getId());
    }

    @Test
    void should_return_empty_when_no_descendant_carries_a_source() {
        var root = aFolder("Guides", null);
        aPage("Inline", root.getId(), null);

        assertThat(cut.findSourcedPageDescendants(ENV_ID, root.getId())).isEmpty();
    }

    private PortalNavigationFolder aFolder(String title, PortalNavigationItemId parentId) {
        return aFolder(PortalNavigationItemId.random(), title, parentId);
    }

    private PortalNavigationFolder aFolder(PortalNavigationItemId id, String title, PortalNavigationItemId parentId) {
        var folder = PortalNavigationFolder.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .build();
        storage.add(folder);
        return folder;
    }

    private PortalNavigationPage aPage(String title, PortalNavigationItemId parentId, PortalNavigationItemSource source) {
        var page = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .source(source)
            .build();
        storage.add(page);
        return page;
    }

    private PortalNavigationItemSource aSource() {
        return PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{\"url\":\"https://example.com/doc.md\"}")
            .build();
    }
}
