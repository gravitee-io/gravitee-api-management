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
package io.gravitee.apim.core.license.domain_service;

import io.gravitee.apim.core.license.crud_service.LicenseCrudService;
import io.gravitee.apim.core.license.model.License;
import java.util.Optional;

public class LicenseDomainService {

    private final LicenseCrudService licenseCrudService;

    public LicenseDomainService(LicenseCrudService licenseCrudService) {
        this.licenseCrudService = licenseCrudService;
    }

    public Optional<License> getLicenseByOrganizationId(String organizationId) {
        return this.licenseCrudService.getOrganizationLicense(organizationId);
    }

    public License createOrUpdateOrganizationLicense(String organizationId, String license) {
        return this.licenseCrudService.getOrganizationLicense(organizationId)
            .map(organizationLicense -> this.licenseCrudService.updateOrganizationLicense(organizationId, license))
            .orElse(this.licenseCrudService.createOrganizationLicense(organizationId, license));
    }
}
