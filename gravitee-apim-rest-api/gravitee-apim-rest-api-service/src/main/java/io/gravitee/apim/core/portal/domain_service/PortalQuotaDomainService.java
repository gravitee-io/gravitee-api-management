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
import java.util.OptionalInt;
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
        var value = license.getAttributes().get(PORTALS_MAX_ATTRIBUTE);
        if (value == null) {
            return DEFAULT_LIMIT;
        }
        return parseCount(value).orElseGet(() -> {
            log.warn(
                "Ignoring the license attribute {} of organization {}: [{}] is not a portal count, allowing {} portal",
                PORTALS_MAX_ATTRIBUTE,
                organizationId,
                value,
                DEFAULT_LIMIT
            );
            return DEFAULT_LIMIT;
        });
    }

    private static OptionalInt parseCount(Object value) {
        try {
            int count = value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString().trim());
            return count >= 0 ? OptionalInt.of(count) : OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
