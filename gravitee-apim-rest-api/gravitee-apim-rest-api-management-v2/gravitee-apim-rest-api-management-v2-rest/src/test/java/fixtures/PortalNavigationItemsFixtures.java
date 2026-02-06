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
package fixtures;

import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType;
import io.gravitee.rest.api.management.v2.rest.model.PortalVisibility;
import java.util.UUID;

public class PortalNavigationItemsFixtures {

    private PortalNavigationItemsFixtures() {}

    public static BaseCreatePortalNavigationItem aCreatePortalNavigationPage() {
        var title = "My Page";
        var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
        var contentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003");
        return new CreatePortalNavigationPage()
            .portalPageContentId(contentId)
            .type(PortalNavigationItemType.PAGE)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(1)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC);
    }

    public static BaseCreatePortalNavigationItem aCreatePortalNavigationFolder() {
        var title = "My Folder";
        var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        return new CreatePortalNavigationFolder()
            .type(PortalNavigationItemType.FOLDER)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(2)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC);
    }

    public static BaseCreatePortalNavigationItem aCreatePortalNavigationLink() {
        var title = "My Link";
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var url = "http://example.com";
        return new CreatePortalNavigationLink()
            .url(url)
            .type(PortalNavigationItemType.LINK)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(3)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC);
    }

    public static BaseCreatePortalNavigationItem aCreatePortalNavigationApi() {
        var title = "My Link";
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var url = "apiId";
        return new CreatePortalNavigationApi()
            .apiId(url)
            .type(PortalNavigationItemType.API)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(3)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC);
    }

    public static BaseCreatePortalNavigationItem aPrivateCreatePortalNavigationPage() {
        var title = "My Page";
        var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        var parentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
        var contentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003");
        return new CreatePortalNavigationPage()
            .portalPageContentId(contentId)
            .type(PortalNavigationItemType.PAGE)
            .id(id)
            .title(title)
            .area(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .order(1)
            .parentId(parentId)
            .visibility(PortalVisibility.PRIVATE);
    }
}
