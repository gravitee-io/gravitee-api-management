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
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.List;

public class PortalNavigationItemFixtures {

    public static final String ORG_ID = "org-id";
    public static final String ENV_ID = "env-id";

    public static final String APIS_ID = "00000000-0000-0000-0000-000000000001";
    private static final String GUIDES_ID = "00000000-0000-0000-0000-000000000002";
    public static final String SUPPORT_ID = "00000000-0000-0000-0000-000000000003";
    private static final String OVERVIEW_ID = "00000000-0000-0000-0000-000000000004";
    private static final String GETTING_STARTED_ID = "00000000-0000-0000-0000-000000000005";
    private static final String CATEGORY1_ID = "00000000-0000-0000-0000-000000000006";
    public static final String PAGE11_ID = "00000000-0000-0000-0000-000000000007";
    public static final String PAGE12_ID = "00000000-0000-0000-0000-000000000008";
    public static final String LINK1_ID = "00000000-0000-0000-0000-000000000009";
    public static final String API1_ID = "00000000-0000-0000-0000-000000000010";
    public static final String API1_FOLDER_ID = "00000000-0000-0000-0000-000000000011";
    public static final String API2_ID = "00000000-0000-0000-0000-000000000012";
    private static final String API2_FOLDER_ID = "00000000-0000-0000-0000-000000000013";

    public static PortalNavigationFolder aFolder(String id, String title) {
        return aFolder(id, title, null);
    }

    public static PortalNavigationFolder aFolder(String id, String title, PortalNavigationItemId parentId) {
        var folder = new PortalNavigationFolder(
            PortalNavigationItemId.of(id),
            ORG_ID,
            ENV_ID,
            title,
            PortalArea.TOP_NAVBAR,
            0,
            true,
            PortalVisibility.PUBLIC
        );
        folder.setParentId(parentId);
        return folder;
    }

    public static PortalNavigationFolder aFolder(String title) {
        return new PortalNavigationFolder(
            PortalNavigationItemId.random(),
            ORG_ID,
            ENV_ID,
            title,
            PortalArea.TOP_NAVBAR,
            0,
            true,
            PortalVisibility.PUBLIC
        );
    }

    public static PortalNavigationFolder aFolder(String title, PortalNavigationItemId parentId) {
        var folder = new PortalNavigationFolder(
            PortalNavigationItemId.random(),
            ORG_ID,
            ENV_ID,
            title,
            PortalArea.TOP_NAVBAR,
            0,
            true,
            PortalVisibility.PUBLIC
        );
        folder.setParentId(parentId);
        return folder;
    }

    public static PortalNavigationPage aPage(String id, String title, PortalNavigationItemId parentId) {
        var page = new PortalNavigationPage(
            PortalNavigationItemId.of(id),
            ORG_ID,
            ENV_ID,
            title,
            PortalArea.TOP_NAVBAR,
            0,
            PortalPageContentId.random(),
            true,
            PortalVisibility.PUBLIC
        );
        page.setParentId(parentId);
        return page;
    }

    public static PortalNavigationPage aPage(String title, PortalNavigationItemId parentId) {
        var page = new PortalNavigationPage(
            PortalNavigationItemId.random(),
            ORG_ID,
            ENV_ID,
            title,
            PortalArea.TOP_NAVBAR,
            0,
            PortalPageContentId.random(),
            true,
            PortalVisibility.PUBLIC
        );
        page.setParentId(parentId);
        return page;
    }

    public static PortalNavigationLink aLink(String id, String title, PortalNavigationItemId parentId) {
        var link = new PortalNavigationLink(
            PortalNavigationItemId.of(id),
            ORG_ID,
            ENV_ID,
            title,
            PortalArea.TOP_NAVBAR,
            0,
            "http://example.com",
            true,
            PortalVisibility.PUBLIC
        );
        link.setParentId(parentId);
        return link;
    }

    public static PortalNavigationApi anApi(String id, String title, PortalNavigationItemId parentId, String apiId) {
        var api = new PortalNavigationApi(
            PortalNavigationItemId.of(id),
            ORG_ID,
            ENV_ID,
            title,
            PortalArea.TOP_NAVBAR,
            0,
            apiId,
            true,
            PortalVisibility.PUBLIC
        );
        api.setParentId(parentId);
        return api;
    }

    public static List<PortalNavigationItem> sampleNavigationItems() {
        var apis = aFolder(APIS_ID, "APIs");
        apis.setOrder(0);
        var guides = aFolder(GUIDES_ID, "Guides");
        guides.setOrder(1);
        var support = aPage(SUPPORT_ID, "Support", null);
        support.setOrder(2);
        var link1 = aLink(LINK1_ID, "Example Link", null);
        support.setOrder(3);

        var overview = aPage(OVERVIEW_ID, "Overview", apis.getId());
        overview.setOrder(0);
        var gettingStarted = aPage(GETTING_STARTED_ID, "Getting Started", apis.getId());
        gettingStarted.setOrder(1);
        var category1 = aFolder(CATEGORY1_ID, "Category1", apis.getId());
        category1.setOrder(2);
        var api1 = anApi(API1_ID, "API 1", apis.getId(), "api-1");
        api1.setOrder(3);
        var api1Folder = aFolder(API1_FOLDER_ID, "API 1 Folder", api1.getId());
        api1Folder.setOrder(0);

        var api2Folder = aFolder(API2_FOLDER_ID, "API 2 Folder", category1.getId());
        api2Folder.setOrder(0);
        var api2 = anApi(API2_ID, "API 2", api2Folder.getId(), "api-2-id");
        api2.setOrder(0);

        var page11 = aPage(PAGE11_ID, "page11", category1.getId());
        page11.setOrder(0);
        page11.setPublished(false);
        var page12 = aPage(PAGE12_ID, "page12", category1.getId());
        page12.setOrder(1);
        page12.setPublished(true);
        page12.setVisibility(PortalVisibility.PRIVATE);

        return List.of(
            apis,
            guides,
            support,
            overview,
            gettingStarted,
            category1,
            api1,
            api2,
            api1Folder,
            api2Folder,
            page11,
            page12,
            link1
        );
    }
}
