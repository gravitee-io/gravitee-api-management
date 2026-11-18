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
import io.gravitee.apim.core.portal.model.PortalQuota;
import io.gravitee.node.api.license.LicenseManager;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalQuotaDomainService {

    private static final String PORTALS_MAX_ATTRIBUTE = "apim-portals-max";
    // Acts as the tier limit for tiers that declare none: like node's tier limits, a license can only raise it.
    private static final int DEFAULT_LIMIT = 1;

    private final LicenseManager licenseManager;

    /** @param used the number of portals the environment holds */
    public PortalQuota quota(String organizationId, int used) {
        return new PortalQuota(used, limit(organizationId));
    }

    private int limit(String organizationId) {
        var license = licenseManager.getOrganizationLicenseOrPlatform(organizationId);
        if (license == null) {
            return DEFAULT_LIMIT;
        }
        var value = license.getEffectiveAttributes().get(PORTALS_MAX_ATTRIBUTE);
        if (value == null) {
            return DEFAULT_LIMIT;
        }
        // The license model declares the attribute as an integer limit, and gravitee-node and gravitee-license keep it to that type.
        return Math.clamp(((Number) value).longValue(), DEFAULT_LIMIT, Integer.MAX_VALUE);
    }
}
