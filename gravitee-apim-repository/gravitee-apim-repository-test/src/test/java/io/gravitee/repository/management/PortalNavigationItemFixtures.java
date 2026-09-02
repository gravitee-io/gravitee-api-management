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
package io.gravitee.repository.management;

import io.gravitee.repository.management.model.PortalNavigationItem;

public final class PortalNavigationItemFixtures {

    public static final String ORGANIZATION_ID = "org-1";
    public static final String ENVIRONMENT_ID = "env-1";

    private PortalNavigationItemFixtures() {}

    public static PortalNavigationItem.PortalNavigationItemBuilder anItem(String id, PortalNavigationItem.Type type) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .title(id)
            .segment(id)
            .type(type)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(4)
            .published(true)
            .configuration("{}")
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .rootId(id);
    }

    public static PortalNavigationItem.PortalNavigationItemBuilder aLink(String id) {
        return anItem(id, PortalNavigationItem.Type.LINK).configuration("{ \"url\": \"https://support.example.com\" }");
    }

    public static PortalNavigationItem.PortalNavigationItemBuilder anApi(String id, String apiId) {
        return anItem(id, PortalNavigationItem.Type.API).apiId(apiId);
    }

    public static PortalNavigationItem.PortalNavigationItemBuilder anApiProduct(String id, String apiProductId) {
        return anItem(id, PortalNavigationItem.Type.API_PRODUCT).apiProductId(apiProductId).published(false);
    }

    public static PortalNavigationItem.PortalNavigationItemBuilder anAgent(String id, String agentId) {
        return anItem(id, PortalNavigationItem.Type.AGENT).agentId(agentId);
    }

    public static PortalNavigationItem.PortalNavigationItemBuilder aPage(String id, String portalPageContentId) {
        return anItem(id, PortalNavigationItem.Type.PAGE).configuration("{ \"portalPageContentId\": \"" + portalPageContentId + "\" }");
    }

    /** A PAGE whose configuration also carries an external source, with auto-fetch enabled. */
    public static PortalNavigationItem.PortalNavigationItemBuilder aSourcedPage(String id, String portalPageContentId) {
        return anItem(id, PortalNavigationItem.Type.PAGE)
            .configuration(
                """
                { "portalPageContentId": "%s",
                  "source": {
                    "type": "github-fetcher",
                    "configuration": "{\\"repository\\":\\"docs\\"}",
                    "fetchCron": "0 */10 * * * *",
                    "lastFetchedAt": "2024-07-17T12:00:00Z",
                    "lastFetchError": "previous fetch failed"
                  }
                }""".formatted(portalPageContentId)
            )
            .useAutoFetch(true);
    }
}
