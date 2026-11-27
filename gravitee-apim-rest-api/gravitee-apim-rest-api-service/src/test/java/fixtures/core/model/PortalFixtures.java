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

import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;

public class PortalFixtures {

    private PortalFixtures() {}

    public static final PortalId PORTAL_ID = PortalId.of("00000000-0000-0000-0000-0000000000a1");
    public static final String PORTAL_ACCESS_POINT_ID = "portal-access-point";

    public static Portal aPortal() {
        return aPortal(PORTAL_ID, "environment-id", "organization-id", "Default Portal");
    }

    public static Portal aPortal(PortalId id, String environmentId, String organizationId, String name) {
        return aPortal(id, environmentId, organizationId, name, PortalNavigationStructure.empty());
    }

    public static Portal aPortal(
        PortalId id,
        String environmentId,
        String organizationId,
        String name,
        PortalNavigationStructure navigationStructure
    ) {
        return Portal.of(id, environmentId, organizationId, name, navigationStructure, null, false, PORTAL_ACCESS_POINT_ID);
    }
}
