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
package fixtures.repository.model;

import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationItem.Area;
import io.gravitee.repository.management.model.PortalNavigationItem.Type;
import io.gravitee.repository.management.model.PortalNavigationItem.Visibility;
import java.util.List;

public class PortalNavigationItemsRepositoryFixtures {

    public static final String ORG_ID = "org-id";
    public static final String ENV_ID = "env-id";

    public static final String APIS_ID = "00000000-0000-0000-0000-000000000001";
    public static final String PAGE11_ID = "00000000-0000-0000-0000-000000000007";
    public static final String PAGE12_ID = "00000000-0000-0000-0000-000000000008";
    public static final String LINK1_ID = "00000000-0000-0000-0000-000000000009";

    public static PortalNavigationItem aFolder(String id, String title) {
        return aFolder(id, title, null);
    }

    public static PortalNavigationItem aFolder(String id, String title, String parentId) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .type(Type.FOLDER)
            .area(Area.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .configuration("{}")
            .published(true)
            .visibility(Visibility.PUBLIC)
            .build();
    }

    public static PortalNavigationItem aPage(String id, String title, String portalPageContentId, String parentId) {
        var configuration = String.format("{\"portalPageContentId\":\"%s\"}", portalPageContentId);
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .type(Type.PAGE)
            .area(Area.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .configuration(configuration)
            .published(true)
            .visibility(Visibility.PUBLIC)
            .build();
    }

    public static PortalNavigationItem aLink(String id, String title, String url, String parentId) {
        var configuration = String.format("{\"url\":\"%s\"}", url == null ? "http://example.com" : url);
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .type(Type.LINK)
            .area(Area.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .configuration(configuration)
            .published(true)
            .visibility(Visibility.PUBLIC)
            .build();
    }

    public static PortalNavigationItem anApi(String id, String title, String apiId, String parentId) {
        var configuration = "{}";
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .type(Type.API)
            .area(Area.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .configuration(configuration)
            .published(true)
            .visibility(Visibility.PUBLIC)
            .apiId(apiId)
            .build();
    }

    public static List<PortalNavigationItem> sampleRepositoryNavigationItems() {
        var apis = aFolder(APIS_ID, "APIs");
        apis.setOrder(0);
        var page11 = aPage(PAGE11_ID, "page11", PortalPageContentId.random().toString(), null);
        page11.setOrder(1);
        var page12 = aPage(PAGE12_ID, "page12", PortalPageContentId.random().toString(), null);
        page12.setOrder(2);
        var link1 = aLink(LINK1_ID, "Example Link", null, null);
        link1.setOrder(3);
        var aip1 = anApi(LINK1_ID, "Example Link", "apiId", null);
        aip1.setOrder(4);

        return List.of(apis, page11, page12, link1, aip1);
    }
}
