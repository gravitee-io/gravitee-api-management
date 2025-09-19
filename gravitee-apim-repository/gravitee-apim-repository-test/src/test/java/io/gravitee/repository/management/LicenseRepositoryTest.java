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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.api.search.LicenseCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.License;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class LicenseRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/license-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        var date = new Date();
        final License license = License.builder()
            .referenceId("my-org")
            .referenceType(License.ReferenceType.ORGANIZATION)
            .license("a-great-license")
            .createdAt(date)
            .updatedAt(date)
            .build();

        final License createdOrg = licenseRepository.create(license);

        assertThat(createdOrg.getReferenceId()).isEqualTo(license.getReferenceId());
        assertThat(createdOrg.getReferenceType()).isEqualTo(license.getReferenceType());
        assertThat(createdOrg.getLicense()).isEqualTo(license.getLicense());
        assertThat(createdOrg.getCreatedAt()).isEqualTo(license.getCreatedAt());
        assertThat(createdOrg.getUpdatedAt()).isEqualTo(license.getUpdatedAt());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<License> optional = licenseRepository.findById("cockpitId-org-update", License.ReferenceType.ORGANIZATION);
        assertThat(optional).as("License to update not found").isPresent();

        final License orgLicenseBeforeUpdate = optional.get();

        final License license = License.builder()
            .referenceId(orgLicenseBeforeUpdate.getReferenceId())
            .referenceType(orgLicenseBeforeUpdate.getReferenceType())
            .license("new license")
            .createdAt(orgLicenseBeforeUpdate.getCreatedAt())
            .updatedAt(new Date())
            .build();

        final License fetchedOrganization = licenseRepository.update(license);

        assertThat(fetchedOrganization.getReferenceId()).isEqualTo(license.getReferenceId());
        assertThat(fetchedOrganization.getReferenceType()).isEqualTo(license.getReferenceType());
        assertThat(fetchedOrganization.getLicense()).isEqualTo(license.getLicense());
        assertThat(fetchedOrganization.getCreatedAt()).isEqualTo(license.getCreatedAt());
        assertThat(fetchedOrganization.getUpdatedAt()).isEqualTo(license.getUpdatedAt());

        optional = licenseRepository.findById("cockpitId-org-update", License.ReferenceType.ORGANIZATION);
        assertThat(optional).as("License to update not found").isPresent();
    }

    @Test
    public void shouldFindById() throws Exception {
        Optional<License> optional = licenseRepository.findById("cockpitId-org-find-by-id", License.ReferenceType.ORGANIZATION);
        assertThat(optional).as("License to find not found").isPresent();
    }

    @Test
    public void shouldNotFindByIdIfMissing() throws Exception {
        Optional<License> optional = licenseRepository.findById("id-does-not-exist", License.ReferenceType.ORGANIZATION);
        assertThat(optional.isEmpty()).as("License is found").isTrue();
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<License> optional = licenseRepository.findById("cockpitId-org-delete", License.ReferenceType.ORGANIZATION);
        assertThat(optional).as("License to delete not found").isPresent();
        licenseRepository.delete("cockpitId-org-delete", License.ReferenceType.ORGANIZATION);
        optional = licenseRepository.findById("cockpitId-org-delete", License.ReferenceType.ORGANIZATION);
        assertThat(optional).as("License to delete has not been deleted").isNotPresent();
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Collection<License> organizations = licenseRepository.findAll();
        assertThat(organizations.size()).as("License count should be 4").isEqualTo(4L);
    }

    @Test
    public void shouldFindByReferenceType() throws Exception {
        final Collection<License> licenses = licenseRepository
            .findByCriteria(
                LicenseCriteria.builder().referenceType(License.ReferenceType.ORGANIZATION).build(),
                new PageableBuilder().pageSize(50).pageNumber(0).build()
            )
            .getContent();
        assertThat(licenses.size()).as("License count should be 3").isEqualTo(3L);
    }

    @Test
    public void shouldFindByReferenceTypeAndId() throws Exception {
        final Collection<License> licenses = licenseRepository
            .findByCriteria(
                LicenseCriteria.builder()
                    .referenceType(License.ReferenceType.ORGANIZATION)
                    .referenceIds(Set.of("cockpitId-org-find-by-id"))
                    .build(),
                new PageableBuilder().pageSize(50).pageNumber(0).build()
            )
            .getContent();
        assertThat(licenses.size()).as("License count should be 1").isEqualTo(1L);
    }

    @Test
    public void shouldFindByTimeInterval() throws Exception {
        final Collection<License> licenses = licenseRepository
            .findByCriteria(
                LicenseCriteria.builder().from(1539022010000L).to(1639022020000L).build(),
                new PageableBuilder().pageSize(50).pageNumber(0).build()
            )
            .getContent();
        assertThat(licenses.size()).as("License count should be 2").isEqualTo(2L);
    }
}
