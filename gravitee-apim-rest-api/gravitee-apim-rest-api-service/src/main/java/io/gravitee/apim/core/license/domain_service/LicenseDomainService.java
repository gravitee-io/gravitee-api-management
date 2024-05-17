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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.license.crud_service.LicenseCrudService;
import io.gravitee.apim.core.license.model.License;
import java.util.Objects;
import java.util.Optional;

@DomainService
public class LicenseDomainService {

    private final LicenseCrudService licenseCrudService;

    public LicenseDomainService(LicenseCrudService licenseCrudService) {
        this.licenseCrudService = licenseCrudService;
    }

    /**
     * Create or update license by organization ID.
     * If on create and license is null, no license is saved in the database.
     * If on update and license is the same, no license is updated.
     * @param organizationId -- organization identifier
     * @param license -- license content to be saved
     */
    public void createOrUpdateOrganizationLicense(String organizationId, String license) {
        this.licenseCrudService.getOrganizationLicense(organizationId)
            .ifPresentOrElse(
                l -> {
                    if (!Objects.equals(l.getLicense(), license)) {
                        this.licenseCrudService.updateOrganizationLicense(organizationId, license);
                    }
                },
                () -> {
                    if (Objects.nonNull(license)) {
                        this.licenseCrudService.createOrganizationLicense(organizationId, license);
                    }
                }
            );
    }
}
