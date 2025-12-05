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

    public static PortalNavigationFolder folder(PortalNavigationItemId id, String title, PortalArea area) {
        return new PortalNavigationFolder(id, "org", "env", title, area, 1, true, PortalVisibility.PUBLIC);
    }

    public static PortalNavigationLink link(PortalNavigationItemId id, String title, PortalArea area, String url) {
        return new PortalNavigationLink(id, "org", "env", title, area, 1, url, true, PortalVisibility.PUBLIC);
    }

    public static PortalNavigationPage page(PortalNavigationItemId id, String title, PortalArea area, PortalPageContentId pageId) {
        return new PortalNavigationPage(id, "org", "env", title, area, 1, pageId, true, PortalVisibility.PUBLIC);
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
}
