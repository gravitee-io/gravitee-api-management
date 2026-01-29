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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientCertificateRepository;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificateStatus;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.UpdateClientCertificate;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.ClientCertificateService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAlreadyUsedException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientCertificateServiceTest {

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

    @InjectMocks
    private ClientCertificateService clientCertificateService = new ClientCertificateServiceImpl();

    @Mock
    private ClientCertificateRepository clientCertificateRepository;

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_find_by_id() throws TechnicalException {
        var repoEntity = buildRepoClientCertificate();
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(repoEntity));

        ClientCertificate result = clientCertificateService.findById(CERTIFICATE_ID);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(CERTIFICATE_ID);
        assertThat(result.name()).isEqualTo("Test Certificate");
        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void should_throw_not_found_when_certificate_does_not_exist() throws TechnicalException {
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientCertificateService.findById(CERTIFICATE_ID))
            .isInstanceOf(ClientCertificateNotFoundException.class)
            .hasMessageContaining(CERTIFICATE_ID);
    }

    @Test
    void should_throw_technical_exception_on_find_by_id_error() throws TechnicalException {
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenThrow(new TechnicalException("DB error"));

        assertThatThrownBy(() -> clientCertificateService.findById(CERTIFICATE_ID)).isInstanceOf(TechnicalManagementException.class);
    }

    @Test
    void should_create_certificate_with_active_status_when_no_dates() throws TechnicalException {
        CreateClientCertificate createDto = new CreateClientCertificate("New Certificate", null, null, PEM_CERTIFICATE);

        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(false);
        when(clientCertificateRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientCertificate created = clientCertificateService.create(APPLICATION_ID, createDto);

        assertThat(created.id()).isNotNull();
        assertThat(created.crossId()).isNotNull();
        assertThat(created.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(created.name()).isEqualTo("New Certificate");
        assertThat(created.organizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(created.environmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.status()).isEqualTo(ClientCertificateStatus.ACTIVE);
        assertThat(created.certificateExpiration()).isNotNull();
        assertThat(created.subject()).isNotNull();
        assertThat(created.issuer()).isNotNull();
        assertThat(created.fingerprint()).isNotNull();
    }

    @Test
    void should_throw_exception_when_certificate_already_used_by_active_application() throws TechnicalException {
        CreateClientCertificate createDto = new CreateClientCertificate("New Certificate", null, null, PEM_CERTIFICATE);

        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(true);

        assertThatThrownBy(() -> clientCertificateService.create(APPLICATION_ID, createDto)).isInstanceOf(
            ClientCertificateAlreadyUsedException.class
        );
    }

    @Test
    void should_create_certificate_with_scheduled_status_when_starts_at_in_future() throws TechnicalException {
        CreateClientCertificate createDto = new CreateClientCertificate(
            "Scheduled Certificate",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
            null,
            PEM_CERTIFICATE
        );

        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(false);
        when(clientCertificateRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = clientCertificateService.create(APPLICATION_ID, createDto);

        assertThat(created.status()).isEqualTo(ClientCertificateStatus.SCHEDULED);
    }

    @Test
    void should_create_certificate_with_active_with_end_status_when_ends_at_in_future() throws TechnicalException {
        CreateClientCertificate createDto = new CreateClientCertificate(
            "Certificate with end date",
            null,
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
            PEM_CERTIFICATE
        );

        when(clientCertificateRepository.existsByFingerprintAndActiveApplication(any(), eq(ENVIRONMENT_ID))).thenReturn(false);
        when(clientCertificateRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = clientCertificateService.create(APPLICATION_ID, createDto);

        assertThat(created.status()).isEqualTo(ClientCertificateStatus.ACTIVE_WITH_END);
    }

    @Test
    void should_create_certificate_with_revoked_status_when_ends_at_in_past() throws TechnicalException {
        CreateClientCertificate createDto = new CreateClientCertificate(
            "Revoked Certificate",
            null,
            Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), // yesterday
            PEM_CERTIFICATE
        );

        when(clientCertificateRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = clientCertificateService.create(APPLICATION_ID, createDto);

        assertThat(created.status()).isEqualTo(ClientCertificateStatus.REVOKED);
    }

    @Test
    void should_update_certificate() throws TechnicalException {
        var existingCert = buildRepoClientCertificate();
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));
        when(clientCertificateRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateClientCertificate updateDto = new UpdateClientCertificate("Updated Name", null, null);

        ClientCertificate updated = clientCertificateService.update(CERTIFICATE_ID, updateDto);

        assertThat(updated.name()).isEqualTo("Updated Name");
        assertThat(updated.updatedAt()).isNotNull();
        assertThat(updated.status()).isEqualTo(ClientCertificateStatus.ACTIVE);
    }

    @Test
    void should_update_certificate_status_to_scheduled_when_starts_at_in_future() throws TechnicalException {
        var existingCert = buildRepoClientCertificate();
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));
        when(clientCertificateRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateClientCertificate updateDto = new UpdateClientCertificate(
            "Updated Name",
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
            null
        );

        var updated = clientCertificateService.update(CERTIFICATE_ID, updateDto);

        assertThat(updated.status()).isEqualTo(ClientCertificateStatus.SCHEDULED);
    }

    @Test
    void should_update_certificate_status_to_active_with_end_when_ends_at_in_future() throws TechnicalException {
        var existingCert = buildRepoClientCertificate();
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));
        when(clientCertificateRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateClientCertificate updateDto = new UpdateClientCertificate(
            "Updated Name",
            null,
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS))
        );

        var updated = clientCertificateService.update(CERTIFICATE_ID, updateDto);

        assertThat(updated.status()).isEqualTo(ClientCertificateStatus.ACTIVE_WITH_END);
    }

    @Test
    void should_update_certificate_status_to_revoked_when_ends_at_in_past() throws TechnicalException {
        var existingCert = buildRepoClientCertificate();
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));
        when(clientCertificateRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateClientCertificate updateDto = new UpdateClientCertificate(
            "Updated Name",
            null,
            Date.from(Instant.now().minus(1, ChronoUnit.DAYS)) // yesterday
        );

        var updated = clientCertificateService.update(CERTIFICATE_ID, updateDto);

        assertThat(updated.status()).isEqualTo(ClientCertificateStatus.REVOKED);
    }

    @Test
    void should_throw_not_found_when_updating_non_existing_certificate() throws TechnicalException {
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.empty());

        UpdateClientCertificate updateDto = new UpdateClientCertificate("Updated Name", null, null);

        assertThatThrownBy(() -> clientCertificateService.update(CERTIFICATE_ID, updateDto)).isInstanceOf(
            ClientCertificateNotFoundException.class
        );
    }

    @Test
    void should_delete_certificate() throws TechnicalException {
        var existingCert = buildRepoClientCertificate();
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.of(existingCert));

        clientCertificateService.delete(CERTIFICATE_ID);

        verify(clientCertificateRepository).delete(CERTIFICATE_ID);
    }

    @Test
    void should_throw_not_found_when_deleting_non_existing_certificate() throws TechnicalException {
        when(clientCertificateRepository.findById(CERTIFICATE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientCertificateService.delete(CERTIFICATE_ID)).isInstanceOf(ClientCertificateNotFoundException.class);
    }

    @Test
    void should_find_by_application_id() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate repoEntity = buildRepoClientCertificate();
        Page<io.gravitee.repository.management.model.ClientCertificate> repoPage = new Page<>(List.of(repoEntity), 0, 1, 1);

        when(clientCertificateRepository.findByApplicationId(eq(APPLICATION_ID), any())).thenReturn(repoPage);

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

        Page<ClientCertificate> result = clientCertificateService.findByApplicationId(APPLICATION_ID, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1).extracting("id").containsExactly(CERTIFICATE_ID);
    }

    @Test
    void should_find_by_application_id_and_statuses() throws TechnicalException {
        io.gravitee.repository.management.model.ClientCertificate repoEntity = buildRepoClientCertificate();
        when(clientCertificateRepository.findByApplicationIdAndStatuses(eq(APPLICATION_ID), any())).thenReturn(Set.of(repoEntity));

        Set<ClientCertificate> result = clientCertificateService.findByApplicationIdAndStatuses(
            APPLICATION_ID,
            List.of(io.gravitee.rest.api.model.clientcertificate.ClientCertificateStatus.ACTIVE)
        );

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().id()).isEqualTo(CERTIFICATE_ID);
    }

    private io.gravitee.repository.management.model.ClientCertificate buildRepoClientCertificate() {
        var cert = new io.gravitee.repository.management.model.ClientCertificate();
        cert.setId(CERTIFICATE_ID);
        cert.setCrossId("cross-id");
        cert.setApplicationId(APPLICATION_ID);
        cert.setName("Test Certificate");
        cert.setCertificate(PEM_CERTIFICATE);
        cert.setOrganizationId(ORGANIZATION_ID);
        cert.setEnvironmentId(ENVIRONMENT_ID);
        cert.setCreatedAt(new Date());
        cert.setStatus(io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE);
        cert.setSubject("CN=localhost");
        cert.setIssuer("CN=localhost");
        cert.setFingerprint("abc123");
        cert.setCertificateExpiration(Date.from(Instant.now().plus(365, ChronoUnit.DAYS)));
        return cert;
    }
}
