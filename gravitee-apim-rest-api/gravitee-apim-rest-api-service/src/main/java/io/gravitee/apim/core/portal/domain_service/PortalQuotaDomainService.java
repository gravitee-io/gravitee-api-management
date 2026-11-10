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
package io.gravitee.apim.core.portal.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.PortalQuota;
import io.gravitee.node.api.license.LicenseManager;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalQuotaDomainService {

    private static final String UNIVERSE_TIER = "universe";
    private static final int UNIVERSE_LIMIT = 10;
    // Trial licenses have no tier of their own, so they land here too, which is their intended limit.
    private static final int DEFAULT_LIMIT = 1;

    private final LicenseManager licenseManager;
    private final PortalCrudService portalCrudService;

    public PortalQuota quota(String organizationId, String environmentId) {
        return new PortalQuota(portalCrudService.findByEnvironmentId(environmentId).size(), limit(organizationId));
    }

    private int limit(String organizationId) {
        var license = licenseManager.getOrganizationLicenseOrPlatform(organizationId);
        return license != null && UNIVERSE_TIER.equals(license.getTier()) ? UNIVERSE_LIMIT : DEFAULT_LIMIT;
    }
}
