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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LicenseDomainServiceTest {

    LicenseCrudServiceInMemory licenseCrudService = new LicenseCrudServiceInMemory();
    LicenseDomainService service;

    @BeforeEach
    void setUp() {
        service = new LicenseDomainService(licenseCrudService, null);
    }

    @AfterEach
    void tearDown() {
        licenseCrudService.reset();
    }

    @Test
    void should_create_organization_license() {
        assertThat(licenseCrudService.getOrganizationLicense("new")).isEmpty();

        service.createOrUpdateOrganizationLicense("new", "newLicense");

        var result = licenseCrudService.getOrganizationLicense("new");
        assertThat(result).isPresent();
        assertThat(result.get().getLicense()).isEqualTo("newLicense");
    }

    @Test
    void should_not_create_organization_license_if_null_license() {
        assertThat(licenseCrudService.getOrganizationLicense("new")).isEmpty();

        service.createOrUpdateOrganizationLicense("new", null);

        var result = licenseCrudService.getOrganizationLicense("new");
        assertThat(result).isEmpty();
    }

    @Test
    void should_update_organization_license() {
        givenOrganizationLicense("org-to-update", "initialLicense");
        service.createOrUpdateOrganizationLicense("org-to-update", "updatedLicense");

        var result = licenseCrudService.getOrganizationLicense("org-to-update");
        assertThat(result).isPresent();
        assertThat(result.get().getLicense()).isEqualTo("updatedLicense");
    }

    @SneakyThrows
    private void givenOrganizationLicense(String organizationId, String license) {
        licenseCrudService.createOrganizationLicense(organizationId, license);
    }
}
