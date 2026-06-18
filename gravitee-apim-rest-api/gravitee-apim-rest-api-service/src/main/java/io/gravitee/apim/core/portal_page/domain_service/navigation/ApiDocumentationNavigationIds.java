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
package io.gravitee.apim.core.portal_page.domain_service.navigation;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.annotation.Nullable;

/**
 * Deterministic id helpers for API-attached documentation navigation rows.
 *
 * <p>Page and api-folder ids are scoped by the {@link io.gravitee.apim.core.portal_page.model.PortalNavigationApi}
 * row created by a {@code PortalListing}, so the same documentation materialized N times (one per listing) gets
 * N distinct navigation rows that share a single underlying
 * {@link io.gravitee.apim.core.portal_page.model.PortalPageContent}. Same for folders derived from
 * {@code api.portalNavigation} — they are materialized once per listing.
 *
 * @author GraviteeSource Team
 */
public final class ApiDocumentationNavigationIds {

    private ApiDocumentationNavigationIds() {}

    public static PortalNavigationItemId pageIdUnder(
        AuditInfo auditInfo,
        PortalNavigationItemId navApiRowId,
        PortalPageContentId contentId
    ) {
        return PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(auditInfo).api(navApiRowId.toString()).documentation(contentId.toString()).id()
        );
    }

    public static PortalNavigationItemId folderUnder(AuditInfo auditInfo, PortalNavigationItemId navApiRowId, @Nullable String location) {
        return PortalNavigationItemId.of(
            HRIDToUUID.navigation().context(auditInfo).api(navApiRowId.toString()).folder(normalizeLocation(location)).id()
        );
    }

    private static String normalizeLocation(@Nullable String location) {
        if (location == null) return "";
        return location.endsWith("/") && location.length() > 1 ? location.substring(0, location.length() - 1) : location;
    }
}
