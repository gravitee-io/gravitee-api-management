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
package io.gravitee.apim.infra.crud_service.license;

import io.gravitee.apim.core.license.crud_service.LicenseCrudService;
import io.gravitee.apim.core.license.exception.LicenseNotFound;
import io.gravitee.apim.core.license.model.License;
import io.gravitee.apim.infra.adapter.LicenseAdapter;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LicenseCrudServiceImpl implements LicenseCrudService {

    private final LicenseRepository licenseRepository;

    public LicenseCrudServiceImpl(@Lazy LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
    }

    @Override
    public Optional<License> getOrganizationLicense(String organizationId) {
        try {
            log.debug("Find license by organization id: {}", organizationId);

            return this.licenseRepository.findById(
                organizationId,
                io.gravitee.repository.management.model.License.ReferenceType.ORGANIZATION
            ).map(LicenseAdapter.INSTANCE::toModel);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    @Override
    public License createOrganizationLicense(String organizationId, String license) {
        try {
            log.debug("Create license for organization id: {}", organizationId);

            var currentTime = TimeProvider.now();

            var organizationLicense = License.builder()
                .referenceType(License.ReferenceType.ORGANIZATION)
                .referenceId(organizationId)
                .license(license)
                .createdAt(currentTime)
                .updatedAt(currentTime)
                .build();
            return LicenseAdapter.INSTANCE.toModel(
                this.licenseRepository.create(LicenseAdapter.INSTANCE.toRepository(organizationLicense))
            );
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    @Override
    public License updateOrganizationLicense(String organizationId, String license) {
        try {
            log.debug("Update license for organization id: {}", organizationId);

            var organizationLicense = this.licenseRepository.findById(
                organizationId,
                io.gravitee.repository.management.model.License.ReferenceType.ORGANIZATION
            );
            if (organizationLicense.isPresent()) {
                return LicenseAdapter.INSTANCE.toModel(
                    this.licenseRepository.update(
                        organizationLicense.get().toBuilder().license(license).updatedAt(Date.from(TimeProvider.instantNow())).build()
                    )
                );
            } else {
                throw new LicenseNotFound(License.ReferenceType.ORGANIZATION.name(), organizationId);
            }
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }
}
