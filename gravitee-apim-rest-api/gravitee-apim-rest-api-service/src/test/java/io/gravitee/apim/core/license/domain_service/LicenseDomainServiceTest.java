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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.LicenseCrudServiceInMemory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LicenseDomainServiceTest {

    LicenseCrudServiceInMemory licenseCrudServiceInMemory;
    LicenseDomainService service;

    @BeforeEach
    void setUp() {
        licenseCrudServiceInMemory = new LicenseCrudServiceInMemory();
        service = new LicenseDomainService(licenseCrudServiceInMemory);
    }

    @Test
    void should_get_organization_license() {
        givenOrganizationLicense("org-id", "licenseAsBase64");

        var result = service.getLicenseByOrganizationId("org-id");
        assertThat(result).isPresent();
        assertThat(result.get().license()).isEqualTo("licenseAsBase64");
    }

    @Test
    void should_create_organization_license() {
        assertThat(service.getLicenseByOrganizationId("new")).isEmpty();

        service.createOrUpdateOrganizationLicense("new", "newLicense");

        var result = service.getLicenseByOrganizationId("new");
        assertThat(result).isPresent();
        assertThat(result.get().license()).isEqualTo("newLicense");
    }

    @Test
    void should_update_organization_license() {
        givenOrganizationLicense("org-to-update", "initialLicense");
        service.createOrUpdateOrganizationLicense("org-to-update", "updatedLicense");

        var result = service.getLicenseByOrganizationId("org-to-update");
        assertThat(result).isPresent();
        assertThat(result.get().license()).isEqualTo("updatedLicense");
    }

    @SneakyThrows
    private void givenOrganizationLicense(String organizationId, String license) {
        licenseCrudServiceInMemory.createOrganizationLicense(organizationId, license);
    }
}
