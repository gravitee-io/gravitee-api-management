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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.ApplicationClientCertificateMigrationUpgrader.METADATA_CLIENT_CERTIFICATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Application;
import io.gravitee.rest.api.service.exceptions.ClientCertificateInvalidException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApplicationClientCertificateMigrationUpgraderTest {

    private static final String VALID_PEM_CERTIFICATE = """
        -----BEGIN CERTIFICATE-----
        MIIBkTCB+wIJAKHBfpegPjMCMA0GCSqGSIb3DQEBCwUAMBExDzANBgNVBAMMBnVu
        dXNlZDAeFw0yMzAxMDEwMDAwMDBaFw0yNDAxMDEwMDAwMDBaMBExDzANBgNVBAMM
        BnVudXNlZDBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQC5CrQrqVk4reoLH5lD9Bnb
        qtiJoGBzkhHyUn8I8lTm8hRKmPT5Y5K5Kn5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K
        AgMBAAEwDQYJKoZIhvcNAQELBQADQQBKLXrQm8K5K5K5K5K5K5K5K5K5K5K5K5K5
        K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5K5
        -----END CERTIFICATE-----
        """;

    @InjectMocks
    private ApplicationClientCertificateMigrationUpgrader upgrader;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ClientCertificateCrudService clientCertificateCrudService;

    @BeforeEach
    void setUp() {
        // Reset any context that might be set
    }

    @Test
    void should_return_correct_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.APPLICATION_CLIENT_CERTIFICATE_MIGRATION_UPGRADER);
    }

    @Test
    void should_succeed_when_no_applications_exist() throws Exception {
        // Given
        Page<Application> emptyPage = new Page<>(List.of(), 1, 100, 0);
        when(applicationRepository.search(any(ApplicationCriteria.class), any(Pageable.class))).thenReturn(emptyPage);

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        verifyNoInteractions(clientCertificateCrudService);
    }

    @Test
    void should_succeed_when_no_applications_have_certificate() throws Exception {
        // Given
        Application appWithoutCert = Application.builder().id("app-1").name("App 1").environmentId("env-1").metadata(Map.of()).build();

        Application appWithNullMetadata = Application.builder().id("app-2").name("App 2").environmentId("env-1").metadata(null).build();

        Application appWithBlankCert = Application.builder()
            .id("app-3")
            .name("App 3")
            .environmentId("env-1")
            .metadata(Map.of(METADATA_CLIENT_CERTIFICATE, "   "))
            .build();

        Page<Application> page = new Page<>(List.of(appWithoutCert, appWithNullMetadata, appWithBlankCert), 1, 100, 3);
        when(applicationRepository.search(any(ApplicationCriteria.class), any(Pageable.class))).thenReturn(page);

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        verifyNoInteractions(clientCertificateCrudService);
    }

    @Test
    void should_migrate_one_application_with_certificate() throws Exception {
        // Given
        String base64Cert = Base64.getEncoder().encodeToString(VALID_PEM_CERTIFICATE.getBytes());
        Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_CLIENT_CERTIFICATE, base64Cert);

        Application appWithCert = Application.builder().id("app-1").name("App 1").environmentId("env-1").metadata(metadata).build();

        Page<Application> page = new Page<>(List.of(appWithCert), 1, 100, 1);
        when(applicationRepository.search(any(ApplicationCriteria.class), any(Pageable.class))).thenReturn(page);
        when(clientCertificateCrudService.create(eq("app-1"), any(ClientCertificate.class))).thenReturn(mock(ClientCertificate.class));

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        verify(clientCertificateCrudService).create(
            eq("app-1"),
            argThat(createCert -> createCert.name().equals("App 1") && createCert.certificate().equals(VALID_PEM_CERTIFICATE))
        );
        verify(applicationRepository).update(
            argThat(app -> app.getId().equals("app-1") && !app.getMetadata().containsKey(METADATA_CLIENT_CERTIFICATE))
        );
    }

    @Test
    void should_migrate_applications_across_two_pages() throws Exception {
        // Given
        String base64Cert = Base64.getEncoder().encodeToString(VALID_PEM_CERTIFICATE.getBytes());

        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put(METADATA_CLIENT_CERTIFICATE, base64Cert);
        Application app1 = Application.builder().id("app-1").name("App 1").environmentId("env-1").metadata(metadata1).build();

        Map<String, String> metadata2 = new HashMap<>();
        metadata2.put(METADATA_CLIENT_CERTIFICATE, base64Cert);
        Application app2 = Application.builder().id("app-2").name("App 2").environmentId("env-1").metadata(metadata2).build();

        Page<Application> page1 = new Page<>(List.of(app1), 1, 1, 2);
        Page<Application> page2 = new Page<>(List.of(app2), 2, 1, 2);

        when(applicationRepository.search(any(ApplicationCriteria.class), any(Pageable.class))).thenReturn(page1).thenReturn(page2);
        when(clientCertificateCrudService.create(any(), any(ClientCertificate.class))).thenReturn(mock(ClientCertificate.class));

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        verify(clientCertificateCrudService, times(2)).create(any(), any(ClientCertificate.class));
        verify(applicationRepository, times(2)).update(any(Application.class));
    }

    @Test
    void should_skip_certificate_creation_but_update_app_when_certificate_is_not_base64_encoded() throws Exception {
        // Given
        Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_CLIENT_CERTIFICATE, "not-valid-base64!!!");

        Application appWithInvalidCert = Application.builder().id("app-1").name("App 1").environmentId("env-1").metadata(metadata).build();

        Page<Application> page = new Page<>(List.of(appWithInvalidCert), 1, 100, 1);
        when(applicationRepository.search(any(ApplicationCriteria.class), any(Pageable.class))).thenReturn(page);

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        verify(clientCertificateCrudService, never()).create(any(), any(ClientCertificate.class));
        verify(applicationRepository).update(
            argThat(app -> app.getId().equals("app-1") && !app.getMetadata().containsKey(METADATA_CLIENT_CERTIFICATE))
        );
    }

    @Test
    void should_skip_certificate_creation_but_update_app_when_certificate_is_not_parsable_pem() throws Exception {
        // Given
        String invalidPem = "This is not a valid PEM certificate";
        String base64InvalidPem = Base64.getEncoder().encodeToString(invalidPem.getBytes());
        Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_CLIENT_CERTIFICATE, base64InvalidPem);

        Application appWithInvalidPem = Application.builder().id("app-1").name("App 1").environmentId("env-1").metadata(metadata).build();

        Page<Application> page = new Page<>(List.of(appWithInvalidPem), 1, 100, 1);
        when(applicationRepository.search(any(ApplicationCriteria.class), any(Pageable.class))).thenReturn(page);
        when(clientCertificateCrudService.create(eq("app-1"), any(ClientCertificate.class))).thenThrow(
            new ClientCertificateInvalidException()
        );

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        verify(clientCertificateCrudService).create(eq("app-1"), any(ClientCertificate.class));
        verify(applicationRepository).update(
            argThat(app -> app.getId().equals("app-1") && !app.getMetadata().containsKey(METADATA_CLIENT_CERTIFICATE))
        );
    }

    @Test
    void should_fail_completely_when_create_throws_technical_exception() throws Exception {
        // Given
        String base64Cert = Base64.getEncoder().encodeToString(VALID_PEM_CERTIFICATE.getBytes());
        Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_CLIENT_CERTIFICATE, base64Cert);

        Application appWithCert = Application.builder().id("app-1").name("App 1").environmentId("env-1").metadata(metadata).build();

        Page<Application> page = new Page<>(List.of(appWithCert), 1, 100, 1);
        when(applicationRepository.search(any(ApplicationCriteria.class), any(Pageable.class))).thenReturn(page);
        when(clientCertificateCrudService.create(eq("app-1"), any(ClientCertificate.class))).thenThrow(
            new RuntimeException("Database connection failed")
        );

        // When / Then
        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
        verify(applicationRepository, never()).update(any(Application.class));
    }

    @Test
    void should_fail_completely_when_application_update_throws_technical_exception() throws Exception {
        // Given
        String base64Cert = Base64.getEncoder().encodeToString(VALID_PEM_CERTIFICATE.getBytes());
        Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_CLIENT_CERTIFICATE, base64Cert);

        Application appWithCert = Application.builder().id("app-1").name("App 1").environmentId("env-1").metadata(metadata).build();

        Page<Application> page = new Page<>(List.of(appWithCert), 1, 100, 1);
        when(applicationRepository.search(any(ApplicationCriteria.class), any(Pageable.class))).thenReturn(page);
        when(clientCertificateCrudService.create(eq("app-1"), any(ClientCertificate.class))).thenReturn(mock(ClientCertificate.class));
        when(applicationRepository.update(any(Application.class))).thenThrow(new TechnicalException("Database update failed"));

        // When / Then
        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }
}
