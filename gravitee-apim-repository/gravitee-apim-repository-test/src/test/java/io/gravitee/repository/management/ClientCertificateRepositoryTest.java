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

import static io.gravitee.repository.utils.DateUtils.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.ClientCertificate;
import io.gravitee.repository.management.model.ClientCertificateStatus;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ClientCertificateRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/clientcertificate-tests/";
    }

    @Test
    public void should_find_all() throws Exception {
        Set<ClientCertificate> clientCertificates = clientCertificateRepository.findAll();

        assertThat(clientCertificates).isNotNull().hasSize(5);
    }

    @Test
    public void should_find_by_id() throws Exception {
        Optional<ClientCertificate> optionalCertificate = clientCertificateRepository.findById("cert-1");

        assertThat(optionalCertificate).isPresent();
        ClientCertificate certificate = optionalCertificate.get();
        assertThat(certificate.getId()).isEqualTo("cert-1");
        assertThat(certificate.getCrossId()).isEqualTo("cross-1");
        assertThat(certificate.getApplicationId()).isEqualTo("app-1");
        assertThat(certificate.getName()).isEqualTo("Certificate 1");
        assertThat(certificate.getSubject()).isEqualTo("CN=Test Subject");
        assertThat(certificate.getIssuer()).isEqualTo("CN=Test Issuer");
        assertThat(certificate.getFingerprint()).isEqualTo("ABC123");
        assertThat(certificate.getEnvironmentId()).isEqualTo("env-1");
        assertThat(certificate.getOrganizationId()).isEqualTo("org-1");
        assertThat(certificate.getStatus()).isEqualTo(ClientCertificateStatus.ACTIVE);
    }

    @Test
    public void should_not_find_by_unknown_id() throws Exception {
        Optional<ClientCertificate> optionalCertificate = clientCertificateRepository.findById("unknown-id");

        assertThat(optionalCertificate).isEmpty();
    }

    @Test
    public void should_create() throws Exception {
        ClientCertificate certificate = new ClientCertificate();
        certificate.setId("new-cert");
        certificate.setCrossId("new-cross");
        certificate.setApplicationId("app-new");
        certificate.setName("New Certificate");
        certificate.setStartsAt(parse("01/01/2025"));
        certificate.setEndsAt(parse("01/01/2026"));
        certificate.setCreatedAt(parse("01/01/2025"));
        certificate.setUpdatedAt(parse("01/01/2025"));
        certificate.setCertificate("-----BEGIN CERTIFICATE-----\nNEW CERT\n-----END CERTIFICATE-----");
        certificate.setCertificateExpiration(parse("01/01/2026"));
        certificate.setSubject("CN=New Subject");
        certificate.setIssuer("CN=New Issuer");
        certificate.setFingerprint("NEW123");
        certificate.setEnvironmentId("env-new");
        certificate.setOrganizationId("org-new");
        certificate.setStatus(ClientCertificateStatus.SCHEDULED);

        ClientCertificate created = clientCertificateRepository.create(certificate);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo("new-cert");

        Optional<ClientCertificate> found = clientCertificateRepository.findById("new-cert");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("New Certificate");
        assertThat(found.get().getStatus()).isEqualTo(ClientCertificateStatus.SCHEDULED);
    }

    @Test
    public void should_update() throws Exception {
        Optional<ClientCertificate> optionalCertificate = clientCertificateRepository.findById("cert-to-update");
        assertThat(optionalCertificate).isPresent();

        ClientCertificate certificate = optionalCertificate.get();
        certificate.setName("Updated Certificate Name");
        certificate.setStatus(ClientCertificateStatus.REVOKED);
        certificate.setUpdatedAt(new Date());

        ClientCertificate updated = clientCertificateRepository.update(certificate);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Updated Certificate Name");
        assertThat(updated.getStatus()).isEqualTo(ClientCertificateStatus.REVOKED);

        Optional<ClientCertificate> found = clientCertificateRepository.findById("cert-to-update");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Updated Certificate Name");
        assertThat(found.get().getStatus()).isEqualTo(ClientCertificateStatus.REVOKED);
    }

    @Test
    public void should_delete() throws Exception {
        Optional<ClientCertificate> optionalCertificate = clientCertificateRepository.findById("cert-to-delete");
        assertThat(optionalCertificate).as("Certificate should exist before deletion").isPresent();

        clientCertificateRepository.delete("cert-to-delete");

        Optional<ClientCertificate> deleted = clientCertificateRepository.findById("cert-to-delete");
        assertThat(deleted).as("Certificate should not exist after deletion").isEmpty();
    }

    @Test
    public void should_find_by_application_id_with_pagination() throws Exception {
        Page<ClientCertificate> page = clientCertificateRepository.findByApplicationId(
            "app-1",
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertThat(page).isNotNull();
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(ClientCertificate::getId).containsExactlyInAnyOrder("cert-1", "cert-2");
    }

    @Test
    public void should_find_by_application_id_with_pagination_page_size() throws Exception {
        Page<ClientCertificate> page = clientCertificateRepository.findByApplicationId(
            "app-1",
            new PageableBuilder().pageNumber(0).pageSize(1).build()
        );

        assertThat(page).isNotNull();
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    public void should_find_by_application_id_and_statuses() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdAndStatuses(
            "app-1",
            ClientCertificateStatus.ACTIVE
        );

        assertThat(certificates).isNotNull().hasSize(1);
        assertThat(certificates).extracting(ClientCertificate::getId).containsExactly("cert-1");
    }

    @Test
    public void should_find_by_application_id_and_multiple_statuses() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdAndStatuses(
            "app-1",
            ClientCertificateStatus.ACTIVE,
            ClientCertificateStatus.SCHEDULED
        );

        assertThat(certificates).isNotNull().hasSize(2);
        assertThat(certificates).extracting(ClientCertificate::getId).containsExactlyInAnyOrder("cert-1", "cert-2");
    }

    @Test
    public void should_return_empty_when_no_matching_statuses() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdAndStatuses(
            "app-1",
            ClientCertificateStatus.REVOKED
        );

        assertThat(certificates).isNotNull().isEmpty();
    }

    @Test
    public void should_return_empty_when_statuses_is_empty() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdAndStatuses("app-1");

        assertThat(certificates).isNotNull().isEmpty();
    }

    @Test
    public void should_find_by_application_ids_and_statuses() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdsAndStatuses(
            List.of("app-1", "app-3"),
            ClientCertificateStatus.ACTIVE
        );

        assertThat(certificates).isNotNull().hasSize(2);
        assertThat(certificates).extracting(ClientCertificate::getId).containsExactlyInAnyOrder("cert-1", "cert-to-update");
    }

    @Test
    public void should_find_by_application_ids_and_multiple_statuses() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdsAndStatuses(
            List.of("app-1", "app-2"),
            ClientCertificateStatus.ACTIVE,
            ClientCertificateStatus.ACTIVE_WITH_END
        );

        assertThat(certificates).isNotNull().hasSize(2);
        assertThat(certificates).extracting(ClientCertificate::getId).containsExactlyInAnyOrder("cert-1", "cert-3");
    }

    @Test
    public void should_return_empty_when_application_ids_is_empty() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdsAndStatuses(
            List.of(),
            ClientCertificateStatus.ACTIVE
        );

        assertThat(certificates).isNotNull().isEmpty();
    }

    @Test
    public void should_return_empty_when_application_ids_is_null() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdsAndStatuses(
            null,
            ClientCertificateStatus.ACTIVE
        );

        assertThat(certificates).isNotNull().isEmpty();
    }

    @Test
    public void should_return_empty_when_statuses_is_empty_for_application_ids() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdsAndStatuses(List.of("app-1", "app-2"));

        assertThat(certificates).isNotNull().isEmpty();
    }

    @Test
    public void should_return_empty_when_statuses_is_null_for_application_ids() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdsAndStatuses(
            List.of("app-1", "app-2"),
            (ClientCertificateStatus[]) null
        );

        assertThat(certificates).isNotNull().isEmpty();
    }

    @Test
    public void should_return_empty_when_no_matching_application_ids() throws Exception {
        Set<ClientCertificate> certificates = clientCertificateRepository.findByApplicationIdsAndStatuses(
            List.of("unknown-app-1", "unknown-app-2"),
            ClientCertificateStatus.ACTIVE
        );

        assertThat(certificates).isNotNull().isEmpty();
    }

    @Test
    public void should_not_update_unknown_certificate() {
        ClientCertificate certificate = new ClientCertificate();
        certificate.setId("unknown-cert");
        certificate.setCrossId("unknown-cert");
        certificate.setApplicationId("unknown-app");
        certificate.setName("Unknown");
        certificate.setCreatedAt(parse("01/01/2025"));
        certificate.setUpdatedAt(parse("01/01/2025"));
        certificate.setCertificate("-----BEGIN CERTIFICATE-----\nNEW CERT\n-----END CERTIFICATE-----");
        certificate.setCertificateExpiration(parse("01/01/2026"));
        certificate.setSubject("CN=New Subject");
        certificate.setIssuer("CN=New Issuer");
        certificate.setFingerprint("NEW123");
        certificate.setEnvironmentId("env-new");
        certificate.setOrganizationId("org-new");
        certificate.setStatus(ClientCertificateStatus.SCHEDULED);
        assertThatThrownBy(() -> clientCertificateRepository.update(certificate)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_not_update_null_certificate() {
        assertThatThrownBy(() -> clientCertificateRepository.update(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_return_true_when_fingerprint_exists_for_active_application_with_non_revoked_certificate() throws Exception {
        // cert-1 has fingerprint ABC123, belongs to app-1 which is ACTIVE, and has status ACTIVE
        boolean exists = clientCertificateRepository.existsByFingerprintAndActiveApplication("ABC123", "env-1");

        assertThat(exists).isTrue();
    }

    @Test
    public void should_return_false_when_fingerprint_exists_for_archived_application() throws Exception {
        // cert-3 has fingerprint GHI789, belongs to app-2 which is ARCHIVED
        boolean exists = clientCertificateRepository.existsByFingerprintAndActiveApplication("GHI789", "env-2");

        assertThat(exists).isFalse();
    }

    @Test
    public void should_return_false_when_fingerprint_exists_but_certificate_is_revoked() throws Exception {
        // cert-to-delete has fingerprint DEL123, belongs to app-4 which is ACTIVE, but certificate status is REVOKED
        boolean exists = clientCertificateRepository.existsByFingerprintAndActiveApplication("DEL123", "env-1");

        assertThat(exists).isFalse();
    }

    @Test
    public void should_return_false_when_fingerprint_does_not_exist() throws Exception {
        boolean exists = clientCertificateRepository.existsByFingerprintAndActiveApplication("UNKNOWN_FINGERPRINT", "env-1");

        assertThat(exists).isFalse();
    }

    @Test
    public void should_return_false_when_fingerprint_exists_but_different_environment() throws Exception {
        // cert-1 has fingerprint ABC123 in env-1, but we search in env-2
        boolean exists = clientCertificateRepository.existsByFingerprintAndActiveApplication("ABC123", "env-2");

        assertThat(exists).isFalse();
    }

    @Test
    public void should_delete_by_application_id() throws Exception {
        // app-1 has 2 certificates: cert-1 and cert-2
        Page<ClientCertificate> beforeDelete = clientCertificateRepository.findByApplicationId(
            "app-1",
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );
        assertThat(beforeDelete.getTotalElements()).isEqualTo(2);

        clientCertificateRepository.deleteByApplicationId("app-1");

        Page<ClientCertificate> afterDelete = clientCertificateRepository.findByApplicationId(
            "app-1",
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );
        assertThat(afterDelete.getTotalElements()).isZero();
    }
}
