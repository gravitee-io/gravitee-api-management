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
package io.gravitee.apim.infra.crud_service.application_certificates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ClientCertificateCrudServiceInMemory;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificateStatus;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.UpdateClientCertificate;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientCertificateCrudServiceTest {

    private static final String CERTIFICATE_ID = "cert-id";
    private static final String APPLICATION_ID = "app-id";
    private static final String ORGANIZATION_ID = "org-id";
    private static final String ENVIRONMENT_ID = "env-id";

    private static final String PEM_CERTIFICATE = """
        -----BEGIN CERTIFICATE-----
        MIIDCTCCAfGgAwIBAgIUdh1NFpteTomLlWoO7O5HKI7fg10wDQYJKoZIhvcNAQEL
        BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTI2MDEyOTE0MTYyMVoXDTI3MDEy
        OTE0MTYyMVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF
        AAOCAQ8AMIIBCgKCAQEAwquFLM7wi+EBt5JL1Q6c/qzqickBKhlymOf1a18jYsWO
        UrRRqANMvEub5zks44qdYbdm7Kj9EruDD3hfrS6YemQhIMeT+SgY3MU5cY12yB1e
        fn+0bkEg6CJdIfgvcuccqY9pu0hgFdlgK9YXEXYZzb+ai7b4qPHN2BR6toBdjf56
        ReNygcrT0igPQC9P/MsmpFuJzD69i5Z8fJcLU2V7RwXW9MKyej8CN/zrcqftG+ck
        egt5cpB2OyIt6ajQBeXarGYvOlm975RKWOD5mHpt87GgcEvMEnhFTSs47iA7X91o
        01+rhFDfPxgvhur2AG6oiB7/zS4lqHWQHRTBwurVJQIDAQABo1MwUTAdBgNVHQ4E
        FgQUHP3c1CNhl6RZHn3g/HkbSssfPhMwHwYDVR0jBBgwFoAUHP3c1CNhl6RZHn3g
        /HkbSssfPhMwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAgTa/
        lgY6fEbt88yQcLxQDseam2lox0sK6sYOwpIQV7/RiXbeM6KrXmCy59HBrJMjSpZY
        LEp9RPDf8Awg50iv6oFXXn+ZJ5Cmq2WXMpCvKxAjQWnmNs99SGfXyQsiLMxe3HlL
        CKqM8O7LZrVdOxWbNW/0ZMJl4d4vCf0LhVrbfMGLeQfqtKVmygjJM1rycKiFazM4
        cTHphvWA9/5XRFC+yD3V3ZTFE9LDeMoSF0soigR0NnCqFc5E7S9OQvuB5h5eBu9s
        T1dLBVOY8zcIYu6LDjeMr6MpiZyk+/O7ewue2kQURmDBuVrwiSSQF4AhsmbMDUuB
        iyr26LfMYtituDZy7w==
        -----END CERTIFICATE-----
        """;

    private ClientCertificateCrudServiceInMemory clientCertificateCrudService;

    @BeforeEach
    void setUp() {
        clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();
    }

    @AfterEach
    void tearDown() {
        clientCertificateCrudService.reset();
    }

    @Test
    void should_find_by_id() {
        ClientCertificate certificate = buildClientCertificate(CERTIFICATE_ID, APPLICATION_ID, ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(certificate));

        ClientCertificate result = clientCertificateCrudService.findById(CERTIFICATE_ID);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(CERTIFICATE_ID);
        assertThat(result.name()).isEqualTo("Test Certificate");
        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void should_throw_not_found_when_certificate_does_not_exist() {
        assertThatThrownBy(() -> clientCertificateCrudService.findById(CERTIFICATE_ID))
            .isInstanceOf(ClientCertificateNotFoundException.class)
            .hasMessageContaining(CERTIFICATE_ID);
    }

    @Test
    void should_create_certificate_with_active_status_when_no_dates() {
        CreateClientCertificate createDto = new CreateClientCertificate("New Certificate", null, null, PEM_CERTIFICATE);

        ClientCertificate created = clientCertificateCrudService.create(APPLICATION_ID, createDto);

        assertThat(created.id()).isNotNull();
        assertThat(created.crossId()).isNotNull();
        assertThat(created.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(created.name()).isEqualTo("New Certificate");
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.status()).isEqualTo(ClientCertificateStatus.ACTIVE);
    }

    @Test
    void should_create_certificate_with_scheduled_status_when_starts_at_in_future() {
        CreateClientCertificate createDto = new CreateClientCertificate(
            "Scheduled Certificate",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
            null,
            PEM_CERTIFICATE
        );

        ClientCertificate created = clientCertificateCrudService.create(APPLICATION_ID, createDto);

        assertThat(created.status()).isEqualTo(ClientCertificateStatus.SCHEDULED);
    }

    @Test
    void should_create_certificate_with_active_with_end_status_when_ends_at_in_future() {
        CreateClientCertificate createDto = new CreateClientCertificate(
            "Certificate with end date",
            null,
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
            PEM_CERTIFICATE
        );

        ClientCertificate created = clientCertificateCrudService.create(APPLICATION_ID, createDto);

        assertThat(created.status()).isEqualTo(ClientCertificateStatus.ACTIVE_WITH_END);
    }

    @Test
    void should_create_certificate_with_revoked_status_when_ends_at_in_past() {
        CreateClientCertificate createDto = new CreateClientCertificate(
            "Revoked Certificate",
            null,
            Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
            PEM_CERTIFICATE
        );

        ClientCertificate created = clientCertificateCrudService.create(APPLICATION_ID, createDto);

        assertThat(created.status()).isEqualTo(ClientCertificateStatus.REVOKED);
    }

    @Test
    void should_update_certificate() {
        ClientCertificate existingCert = buildClientCertificate(CERTIFICATE_ID, APPLICATION_ID, ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(existingCert));

        UpdateClientCertificate updateDto = new UpdateClientCertificate("Updated Name", null, null);

        ClientCertificate updated = clientCertificateCrudService.update(CERTIFICATE_ID, updateDto);

        assertThat(updated.name()).isEqualTo("Updated Name");
        assertThat(updated.updatedAt()).isNotNull();
        assertThat(updated.status()).isEqualTo(ClientCertificateStatus.ACTIVE);
    }

    @Test
    void should_update_certificate_status_to_scheduled_when_starts_at_in_future() {
        ClientCertificate existingCert = buildClientCertificate(CERTIFICATE_ID, APPLICATION_ID, ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(existingCert));

        UpdateClientCertificate updateDto = new UpdateClientCertificate(
            "Updated Name",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
            null
        );

        ClientCertificate updated = clientCertificateCrudService.update(CERTIFICATE_ID, updateDto);

        assertThat(updated.status()).isEqualTo(ClientCertificateStatus.SCHEDULED);
    }

    @Test
    void should_update_certificate_status_to_active_with_end_when_ends_at_in_future() {
        ClientCertificate existingCert = buildClientCertificate(CERTIFICATE_ID, APPLICATION_ID, ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(existingCert));

        UpdateClientCertificate updateDto = new UpdateClientCertificate(
            "Updated Name",
            null,
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS))
        );

        ClientCertificate updated = clientCertificateCrudService.update(CERTIFICATE_ID, updateDto);

        assertThat(updated.status()).isEqualTo(ClientCertificateStatus.ACTIVE_WITH_END);
    }

    @Test
    void should_update_certificate_status_to_revoked_when_ends_at_in_past() {
        ClientCertificate existingCert = buildClientCertificate(CERTIFICATE_ID, APPLICATION_ID, ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(existingCert));

        UpdateClientCertificate updateDto = new UpdateClientCertificate(
            "Updated Name",
            null,
            Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        );

        ClientCertificate updated = clientCertificateCrudService.update(CERTIFICATE_ID, updateDto);

        assertThat(updated.status()).isEqualTo(ClientCertificateStatus.REVOKED);
    }

    @Test
    void should_throw_not_found_when_updating_non_existing_certificate() {
        UpdateClientCertificate updateDto = new UpdateClientCertificate("Updated Name", null, null);

        assertThatThrownBy(() -> clientCertificateCrudService.update(CERTIFICATE_ID, updateDto)).isInstanceOf(
            ClientCertificateNotFoundException.class
        );
    }

    @Test
    void should_delete_certificate() {
        ClientCertificate existingCert = buildClientCertificate(CERTIFICATE_ID, APPLICATION_ID, ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(existingCert));

        clientCertificateCrudService.delete(CERTIFICATE_ID);

        assertThat(clientCertificateCrudService.storage()).isEmpty();
    }

    @Test
    void should_throw_not_found_when_deleting_non_existing_certificate() {
        assertThatThrownBy(() -> clientCertificateCrudService.delete(CERTIFICATE_ID)).isInstanceOf(
            ClientCertificateNotFoundException.class
        );
    }

    @Test
    void should_find_by_application_id() {
        ClientCertificate certificate = buildClientCertificate(CERTIFICATE_ID, APPLICATION_ID, ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(certificate));

        Pageable pageable = new Pageable() {
            @Override
            public int getPageNumber() {
                return 0;
            }

            @Override
            public int getPageSize() {
                return 10;
            }
        };

        Page<ClientCertificate> result = clientCertificateCrudService.findByApplicationId(APPLICATION_ID, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1).extracting("id").containsExactly(CERTIFICATE_ID);
    }

    @Test
    void should_find_by_application_id_and_statuses() {
        ClientCertificate certificate = buildClientCertificate(CERTIFICATE_ID, APPLICATION_ID, ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(certificate));

        Set<ClientCertificate> result = clientCertificateCrudService.findByApplicationIdAndStatuses(
            APPLICATION_ID,
            ClientCertificateStatus.ACTIVE
        );

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().id()).isEqualTo(CERTIFICATE_ID);
    }

    private ClientCertificate buildClientCertificate(String id, String applicationId, ClientCertificateStatus status) {
        return new ClientCertificate(
            id,
            "cross-id",
            applicationId,
            "Test Certificate",
            null,
            null,
            new Date(),
            new Date(),
            PEM_CERTIFICATE,
            Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
            "CN=localhost",
            "CN=localhost",
            "abc123",
            ENVIRONMENT_ID,
            ORGANIZATION_ID,
            status
        );
    }
}
