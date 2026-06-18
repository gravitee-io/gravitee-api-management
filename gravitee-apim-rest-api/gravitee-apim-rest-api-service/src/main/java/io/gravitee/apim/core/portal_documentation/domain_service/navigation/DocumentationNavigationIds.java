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
package io.gravitee.apim.core.portal_documentation.domain_service.navigation;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.service.common.HRIDToUUID;

public final class DocumentationNavigationIds {

    private DocumentationNavigationIds() {}

    public static PortalNavigationItemId navigationItemId(AuditInfo auditInfo, String portalId, PortalPageContentId pageContentId) {
        return PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(auditInfo).portal(portalId).documentation(pageContentId.toString()).id()
        );
    }

    public static PortalNavigationItemId folderId(AuditInfo auditInfo, String portalId, String location) {
        if (location == null || location.isBlank() || "/".equals(location)) {
            return null;
        }
        return PortalNavigationItemId.of(HRIDToUUID.navigation().context(auditInfo).portal(portalId).folder(location).id());
    }

    public static PortalNavigationItemId navigationApiId(AuditInfo auditInfo, String portalId, String apiId) {
        return PortalNavigationItemId.of(HRIDToUUID.navigation().context(auditInfo).portal(portalId).listingApi(apiId).id());
    }
}
