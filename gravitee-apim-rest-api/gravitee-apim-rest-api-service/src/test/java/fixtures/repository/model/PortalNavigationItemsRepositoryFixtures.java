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
        return aFolder(id, title, parentId, parentId == null ? id : parentId);
    }

    public static PortalNavigationItem aFolder(String id, String title, String parentId, String rootId) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .type(Type.FOLDER)
            .area(Area.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .rootId(rootId)
            .configuration("{}")
            .published(true)
            .visibility(Visibility.PUBLIC)
            .build();
    }

    public static PortalNavigationItem aPage(String id, String title, String portalPageContentId, String parentId) {
        return aPage(id, title, portalPageContentId, parentId, parentId == null ? id : parentId);
    }

    public static PortalNavigationItem aPage(String id, String title, String portalPageContentId, String parentId, String rootId) {
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
            .rootId(rootId)
            .configuration(configuration)
            .published(true)
            .visibility(Visibility.PUBLIC)
            .build();
    }

    public static PortalNavigationItem aLink(String id, String title, String url, String parentId) {
        return aLink(id, title, url, parentId, parentId == null ? id : parentId);
    }

    public static PortalNavigationItem aLink(String id, String title, String url, String parentId, String rootId) {
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
            .rootId(rootId)
            .configuration(configuration)
            .published(true)
            .visibility(Visibility.PUBLIC)
            .build();
    }

    public static PortalNavigationItem anApi(String id, String title, String apiId, String parentId) {
        return anApi(id, title, apiId, parentId, parentId == null ? id : parentId);
    }

    public static PortalNavigationItem anApi(String id, String title, String apiId, String parentId, String rootId) {
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
            .rootId(rootId)
            .configuration(configuration)
            .published(true)
            .visibility(Visibility.PUBLIC)
            .apiId(apiId)
            .build();
    }

    /** RootId value when domain model uses PortalNavigationItemId.zero(). */
    public static final String ROOT_ID_ZERO = "00000000-0000-0000-0000-000000000000";

    /**
     * Expected repository item for a folder as produced by the adapter when mapping from domain to repository (create/update).
     */
    public static PortalNavigationItem expectedFolderFromCreate(
        String id,
        String title,
        String organizationId,
        String environmentId,
        int order,
        String rootId
    ) {
        return PortalNavigationItem.builder()
            .id(id)
            .title(title)
            .organizationId(organizationId)
            .environmentId(environmentId)
            .type(Type.FOLDER)
            .area(Area.TOP_NAVBAR)
            .order(order)
            .parentId(null)
            .rootId(rootId)
            .configuration("{}")
            .published(true)
            .visibility(Visibility.PUBLIC)
            .build();
    }

    /**
     * Expected repository item for a page as produced by the adapter when mapping from domain to repository (create/update).
     */
    public static PortalNavigationItem expectedPageFromCreate(
        String id,
        String title,
        String organizationId,
        String environmentId,
        int order,
        String rootId,
        String portalPageContentId
    ) {
        return PortalNavigationItem.builder()
            .id(id)
            .title(title)
            .organizationId(organizationId)
            .environmentId(environmentId)
            .type(Type.PAGE)
            .area(Area.TOP_NAVBAR)
            .order(order)
            .parentId(null)
            .rootId(rootId)
            .configuration("{\"portalPageContentId\":\"" + portalPageContentId + "\"}")
            .published(true)
            .visibility(Visibility.PUBLIC)
            .build();
    }

    /**
     * Expected repository item for a link as produced by the adapter when mapping from domain to repository (create/update).
     */
    public static PortalNavigationItem expectedLinkFromCreate(
        String id,
        String title,
        String organizationId,
        String environmentId,
        int order,
        String rootId,
        String url
    ) {
        return PortalNavigationItem.builder()
            .id(id)
            .title(title)
            .organizationId(organizationId)
            .environmentId(environmentId)
            .type(Type.LINK)
            .area(Area.TOP_NAVBAR)
            .order(order)
            .parentId(null)
            .rootId(rootId)
            .configuration("{\"url\":\"" + url + "\"}")
            .published(true)
            .visibility(Visibility.PUBLIC)
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
