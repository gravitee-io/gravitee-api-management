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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.license.crud_service.LicenseCrudService;
import io.gravitee.apim.core.license.model.License;
import io.gravitee.apim.infra.adapter.LicenseAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.LicenseRepository;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LicenseCrudServiceImplTest {

    LicenseRepository licenseRepository;

    LicenseCrudService service;

    @BeforeEach
    void setUp() {
        licenseRepository = mock(LicenseRepository.class);
        service = new LicenseCrudServiceImpl(licenseRepository);
    }

    @Test
    void should_find_license_by_organization_id() {
        License license = anOrganizationLicense();
        givenOrganizationLicense(license);

        var result = service.getOrganizationLicense(license.getReferenceId());

        assertThat(result).contains(license);
    }

    @Test
    void should_return_empty_if_no_license_is_found() {
        assertThat(service.getOrganizationLicense("unknown")).isEmpty();
    }

    @Test
    void should_create_license() throws TechnicalException {
        assertThat(service.getOrganizationLicense("new")).isEmpty();
        service.createOrganizationLicense("new", "new license");
        verify(licenseRepository)
            .create(
                io.gravitee.repository.management.model.License
                    .builder()
                    .referenceId("new")
                    .referenceType(io.gravitee.repository.management.model.License.ReferenceType.ORGANIZATION)
                    .license("new license")
                    .build()
            );
    }

    @Test
    void should_update_license() throws TechnicalException {
        License license = anOrganizationLicense();
        givenOrganizationLicense(license);

        service.updateOrganizationLicense(license.getReferenceId(), "updated license");

        verify(licenseRepository)
            .update(
                io.gravitee.repository.management.model.License
                    .builder()
                    .referenceId(license.getReferenceId())
                    .referenceType(io.gravitee.repository.management.model.License.ReferenceType.ORGANIZATION)
                    .license("updated license")
                    .build()
            );
    }

    @SneakyThrows
    private void givenOrganizationLicense(License license) {
        when(
            licenseRepository.findById(license.getReferenceId(), io.gravitee.repository.management.model.License.ReferenceType.ORGANIZATION)
        )
            .thenReturn(Optional.of(LicenseAdapter.INSTANCE.toRepository(license)));
    }

    private License anOrganizationLicense() {
        return License
            .builder()
            .referenceType(License.ReferenceType.ORGANIZATION)
            .referenceId("organization-id")
            .license("fakeLicenseFileAsBase64")
            .build();
    }
}
