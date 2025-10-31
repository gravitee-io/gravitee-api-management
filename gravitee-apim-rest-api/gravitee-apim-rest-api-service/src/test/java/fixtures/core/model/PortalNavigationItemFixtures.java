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
package fixtures.core.model;

import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageNavigationId;
import java.util.List;

public class PortalNavigationItemFixtures {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    private static final String APIS_ID = "00000000-0000-0000-0000-000000000001";
    private static final String GUIDES_ID = "00000000-0000-0000-0000-000000000002";
    private static final String SUPPORT_ID = "00000000-0000-0000-0000-000000000003";
    private static final String OVERVIEW_ID = "00000000-0000-0000-0000-000000000004";
    private static final String GETTING_STARTED_ID = "00000000-0000-0000-0000-000000000005";
    private static final String CATEGORY1_ID = "00000000-0000-0000-0000-000000000006";
    private static final String PAGE11_ID = "00000000-0000-0000-0000-000000000007";
    private static final String PAGE12_ID = "00000000-0000-0000-0000-000000000008";

    public static PortalNavigationFolder aFolder(String id, String title) {
        return aFolder(id, title, null);
    }

    public static PortalNavigationFolder aFolder(String id, String title, PortalPageNavigationId parentId) {
        var folder = new PortalNavigationFolder(PortalPageNavigationId.of(id), ORG_ID, ENV_ID, title, PortalArea.TOP_NAVBAR);
        folder.setParentId(parentId);
        return folder;
    }

    public static PortalNavigationPage aPage(String id, String title, PortalPageNavigationId parentId) {
        var page = new PortalNavigationPage(
            PortalPageNavigationId.of(id),
            ORG_ID,
            ENV_ID,
            title,
            PortalArea.TOP_NAVBAR,
            PortalPageContentId.random()
        );
        page.setParentId(parentId);
        return page;
    }

    public static List<PortalNavigationItem> sampleNavigationItems() {
        var apis = aFolder(APIS_ID, "APIs");
        var guides = aFolder(GUIDES_ID, "Guides");
        var support = aPage(SUPPORT_ID, "Support", null);

        var overview = aPage(OVERVIEW_ID, "Overview", apis.getId());
        var gettingStarted = aPage(GETTING_STARTED_ID, "Getting Started", apis.getId());
        var category1 = aFolder(CATEGORY1_ID, "Category1", apis.getId());

        var page11 = aPage(PAGE11_ID, "page11", category1.getId());
        var page12 = aPage(PAGE12_ID, "page12", category1.getId());

        return List.of(apis, guides, support, overview, gettingStarted, category1, page11, page12);
    }
}
