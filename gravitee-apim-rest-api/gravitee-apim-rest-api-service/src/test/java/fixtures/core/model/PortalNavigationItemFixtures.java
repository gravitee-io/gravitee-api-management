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

    public static final String PAGE_ID = "00000000-0000-0000-0000-000000000014";
    public static final String FOLDER_ID = "00000000-0000-0000-0000-000000000015";
    public static final String LINK_ID = "00000000-0000-0000-0000-000000000016";
    public static final String API_ID = "00000000-0000-0000-0000-000000000017";

    public static final String APIS_ID = "00000000-0000-0000-0000-000000000001";
    private static final String GUIDES_ID = "00000000-0000-0000-0000-000000000002";
    public static final String SUPPORT_ID = "00000000-0000-0000-0000-000000000003";
    private static final String OVERVIEW_ID = "00000000-0000-0000-0000-000000000004";
    private static final String GETTING_STARTED_ID = "00000000-0000-0000-0000-000000000005";
    public static final String CATEGORY1_ID = "00000000-0000-0000-0000-000000000006";
    public static final String PAGE11_ID = "00000000-0000-0000-0000-000000000007";
    public static final String PAGE12_ID = "00000000-0000-0000-0000-000000000008";
    public static final String LINK1_ID = "00000000-0000-0000-0000-000000000009";
    public static final String API1_ID = "00000000-0000-0000-0000-000000000010";
    public static final String API1_FOLDER_ID = "00000000-0000-0000-0000-000000000011";
    public static final String API2_ID = "00000000-0000-0000-0000-000000000012";
    private static final String API2_FOLDER_ID = "00000000-0000-0000-0000-000000000013";
    public static final String SUPPORT_CONTENT_ID = "00000000-0000-0000-0000-000000000010";
    public static final String PAGE11_CONTENT_ID = "00000000-0000-0000-0000-000000000011";
    public static final String UNPUBLISHED_ID = "00000000-0000-0000-0000-000000000012";
    public static final String PRIVATE_ID = "00000000-0000-0000-0000-000000000013";

    public static final List<String> SAMPLE_NAVIGATION_ITEMS_IDS = List.of(
        APIS_ID,
        GUIDES_ID,
        SUPPORT_ID,
        OVERVIEW_ID,
        GETTING_STARTED_ID,
        CATEGORY1_ID,
        API1_ID,
        API2_ID,
        API1_FOLDER_ID,
        API2_FOLDER_ID,
        PAGE11_ID,
        PAGE12_ID,
        LINK1_ID
    );

    public static PortalNavigationFolder aFolder(String id, String title) {
        return aFolder(id, title, null);
    }

    public static PortalNavigationFolder aFolder(String id, String title, PortalNavigationItemId parentId) {
        return PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.of(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .build();
    }

    public static PortalNavigationFolder aFolder(String title) {
        return aFolder(title, (PortalNavigationItemId) null);
    }

    public static PortalNavigationFolder aFolder(String title, PortalNavigationItemId parentId) {
        return PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .build();
    }

    public static PortalNavigationPage aPage(String id, String title, PortalNavigationItemId parentId) {
        return PortalNavigationPage.builder()
            .id(PortalNavigationItemId.of(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .build();
    }

    public static PortalNavigationPage aPage(String title, PortalNavigationItemId parentId) {
        return PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .build();
    }

    public static PortalNavigationPage aPage(String id, String title, PortalNavigationItemId parentId, PortalPageContentId contentId) {
        return PortalNavigationPage.builder()
            .id(PortalNavigationItemId.of(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(contentId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .build();
    }

    public static List<PortalNavigationItem> navigationItemsForContentTest() {
        var apisFolder = aFolder(APIS_ID, "APIs");
        apisFolder.markAsRoot();

        var category1Folder = aFolder(CATEGORY1_ID, "Category1", PortalNavigationItemId.of(APIS_ID));
        category1Folder.updateParent(apisFolder);

        var supportPage = aPage(SUPPORT_ID, "Support", null, PortalPageContentId.of(SUPPORT_CONTENT_ID));
        supportPage.markAsRoot();

        var page11 = aPage(PAGE11_ID, "page11", category1Folder.getId(), PortalPageContentId.of(PAGE11_CONTENT_ID));
        page11.updateParent(category1Folder);

        var unpublishedPage = aPage(UNPUBLISHED_ID, "Unpublished Page", null, PortalPageContentId.random());
        unpublishedPage.markAsRoot();
        unpublishedPage.setPublished(false);

        var privatePage = aPage(PRIVATE_ID, "Private Page", null, PortalPageContentId.random());
        privatePage.markAsRoot();
        privatePage.setVisibility(PortalVisibility.PRIVATE);

        return List.of(apisFolder, category1Folder, supportPage, page11, unpublishedPage, privatePage);
    }

    public static PortalNavigationLink aLink(String id, String title, PortalNavigationItemId parentId) {
        return PortalNavigationLink.builder()
            .id(PortalNavigationItemId.of(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .url("http://example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .build();
    }

    public static PortalNavigationLink aLink() {
        return PortalNavigationLink.builder()
            .id(PortalNavigationItemId.of(LINK_ID))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("My Link")
            .area(PortalArea.TOP_NAVBAR)
            .order(3)
            .url("https://example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    public static PortalNavigationApi anApi(String id, String title, PortalNavigationItemId parentId, String apiId) {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.of(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId(apiId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .build();
    }

    public static PortalNavigationApi anApi() {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.of(API_ID))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("My Api")
            .area(PortalArea.TOP_NAVBAR)
            .order(3)
            .apiId("apiId")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    public static List<PortalNavigationItem> sampleNavigationItems() {
        var apis = aFolder(APIS_ID, "APIs");
        apis.setOrder(0);
        apis.markAsRoot();

        var guides = aFolder(GUIDES_ID, "Guides");
        guides.setOrder(1);
        guides.markAsRoot();

        var support = aPage(SUPPORT_ID, "Support", null);
        support.setOrder(2);
        support.markAsRoot();

        var link1 = aLink(LINK1_ID, "Example Link", null);
        support.setOrder(3);
        link1.markAsRoot();

        var overview = aPage(OVERVIEW_ID, "Overview", apis.getId());
        overview.setOrder(0);
        overview.updateParent(apis);

        var gettingStarted = aPage(GETTING_STARTED_ID, "Getting Started", apis.getId());
        gettingStarted.setOrder(1);
        gettingStarted.updateParent(apis);

        var category1 = aFolder(CATEGORY1_ID, "Category1", apis.getId());
        category1.setOrder(2);
        category1.updateParent(apis);

        var api1 = anApi(API1_ID, "API 1", apis.getId(), "api-1");
        api1.setOrder(3);
        api1.updateParent(apis);

        var api1Folder = aFolder(API1_FOLDER_ID, "API 1 Folder", api1.getId());
        api1Folder.setOrder(0);
        api1Folder.updateParent(api1);

        var api2Folder = aFolder(API2_FOLDER_ID, "API 2 Folder", category1.getId());
        api2Folder.setOrder(0);
        api2Folder.updateParent(category1);

        var api2 = anApi(API2_ID, "API 2", api2Folder.getId(), "api-2-id");
        api2.setOrder(0);
        api2.updateParent(api2Folder);

        var page11 = aPage(PAGE11_ID, "page11", category1.getId());
        page11.setOrder(0);
        page11.setPublished(false);
        page11.updateParent(category1);

        var page12 = aPage(PAGE12_ID, "page12", category1.getId());
        page12.setOrder(1);
        page12.setPublished(true);
        page12.setVisibility(PortalVisibility.PRIVATE);
        page12.updateParent(category1);

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
