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
import java.util.OptionalLong;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@DomainService
@RequiredArgsConstructor
public class PortalQuotaDomainService {

    private static final String PORTALS_MAX_ATTRIBUTE = "apim-portals-max";
    private static final int DEFAULT_LIMIT = 1;

    private final LicenseManager licenseManager;
    private final PortalCrudService portalCrudService;

    public PortalQuota quota(String organizationId, String environmentId) {
        return new PortalQuota(portalCrudService.findByEnvironmentId(environmentId).size(), limit(organizationId));
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
        var limit = readLimit(value);
        if (limit.isEmpty()) {
            log.warn(
                "Ignoring the license attribute {} of organization {}: [{}] is not a number, allowing {} portal",
                PORTALS_MAX_ATTRIBUTE,
                organizationId,
                value,
                DEFAULT_LIMIT
            );
            return DEFAULT_LIMIT;
        }
        return Math.clamp(limit.getAsLong(), 0, Integer.MAX_VALUE);
    }

    // Reads a limit the way gravitee-node's DefaultLicenseFactory does when it picks the higher of the tier's and the
    // license's value, so APIM enforces the value node resolved.
    private static OptionalLong readLimit(Object value) {
        if (value instanceof Number number) {
            return OptionalLong.of(number.longValue());
        }
        try {
            return OptionalLong.of(Long.parseLong(value.toString().trim()));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }
}
