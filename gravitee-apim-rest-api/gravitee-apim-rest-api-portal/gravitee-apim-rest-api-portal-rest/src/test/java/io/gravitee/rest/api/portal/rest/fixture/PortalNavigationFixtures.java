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
package io.gravitee.rest.api.portal.rest.fixture;

import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import java.util.List;

public final class PortalNavigationFixtures {

    private PortalNavigationFixtures() {}

    public static PortalNavigationItemId randomNavigationId() {
        return PortalNavigationItemId.random();
    }

    public static PortalPageContentId randomPageId() {
        return PortalPageContentId.random();
    }

    public static final PortalNavigationItemId GRANDPARENT_ID = randomNavigationId();
    public static final PortalNavigationItemId PARENT_ID = randomNavigationId();
    public static final PortalNavigationItemId SIBLING_ID = randomNavigationId();
    public static final PortalNavigationItemId CHILD1_ID = randomNavigationId();
    public static final PortalNavigationItemId CHILD2_ID = randomNavigationId();

    public static PortalNavigationFolder folder(PortalNavigationItemId id, String title, PortalArea area) {
        return PortalNavigationFolder.builder()
            .id(id)
            .organizationId("org")
            .environmentId("env")
            .title(title)
            .area(area)
            .order(1)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    public static PortalNavigationLink link(PortalNavigationItemId id, String title, PortalArea area, String url) {
        return PortalNavigationLink.builder()
            .id(id)
            .organizationId("org")
            .environmentId("env")
            .title(title)
            .area(area)
            .order(1)
            .url(url)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    public static PortalNavigationPage page(PortalNavigationItemId id, String title, PortalArea area, PortalPageContentId pageId) {
        return PortalNavigationPage.builder()
            .id(id)
            .organizationId("org")
            .environmentId("env")
            .title(title)
            .area(area)
            .order(1)
            .portalPageContentId(pageId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    public static List<PortalNavigationItem> sampleList(PortalArea area) {
        List<PortalNavigationItem> items = new ArrayList<>();
        var id1 = randomNavigationId();
        var id2 = randomNavigationId();
        var id3 = randomNavigationId();

        items.add(folder(id1, "Folder 1", area));
        items.add(link(id2, "Link 1", area, "https://example.org"));
        items.add(page(id3, "Page 1", area, randomPageId()));
        return items;
    }

    // New: helper to build the hierarchy used by should_not_show_children_of_unpublished_parent test
    public static List<PortalNavigationItem> unpublishedParentHierarchy(PortalArea area, String environmentId) {
        var grandparent = folder(GRANDPARENT_ID, "Grandparent (Pub)", area);
        grandparent.setEnvironmentId(environmentId);
        grandparent.setPublished(true);

        var parent = folder(PARENT_ID, "Parent (Unpub)", area);
        parent.setEnvironmentId(environmentId);
        parent.updateParent(grandparent);
        parent.setPublished(false);

        var sibling = link(SIBLING_ID, "Sibling (Pub)", area, "https://example.com");
        sibling.setEnvironmentId(environmentId);
        sibling.updateParent(grandparent);
        sibling.setPublished(true);

        var child1 = page(CHILD1_ID, "Child 1", area, randomPageId());
        child1.setEnvironmentId(environmentId);
        child1.updateParent(parent);
        child1.setPublished(true);

        var child2 = page(CHILD2_ID, "Child 2", area, randomPageId());
        child2.setEnvironmentId(environmentId);
        child2.updateParent(parent);
        child2.setPublished(true);

        return List.of(grandparent, parent, sibling, child1, child2);
    }
}
