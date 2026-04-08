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
package io.gravitee.apim.core.application_certificate.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateDomainService;
import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateValidationDomainService;
import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateValidationDomainService.CertificateInfo;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.rest.api.service.exceptions.ClientCertificateEmptyException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateClientCertificateUseCaseTest {

    @Mock
    private ClientCertificateDomainService clientCertificateDomainService;

    @Mock
    private ClientCertificateValidationDomainService clientCertificateValidationDomainService;

    private CreateClientCertificateUseCase createClientCertificateUseCase;

    @BeforeEach
    void setUp() {
        createClientCertificateUseCase = new CreateClientCertificateUseCase(
            clientCertificateDomainService,
            clientCertificateValidationDomainService
        );
    }

    @Test
    void should_create_client_certificate() {
        var appId = "app-id";
        var expiration = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));
        var certInfo = new CertificateInfo(expiration, "CN=unit-tests", "CN=unit-tests-issuer", "sha256-fingerprint");

        when(clientCertificateValidationDomainService.validateForCreation(any(ClientCertificate.class), any())).thenReturn(certInfo);

        var createdCertificate = new ClientCertificate(
            "generated-id",
            "cross-id",
            appId,
            "Test Certificate",
            new Date(),
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
            new Date(),
            new Date(),
            "pem-content",
            expiration,
            "CN=unit-tests",
            "CN=unit-tests-issuer",
            "sha256-fingerprint",
            "env-id",
            ClientCertificateStatus.ACTIVE
        );
        when(clientCertificateDomainService.create(eq(appId), any(ClientCertificate.class))).thenReturn(createdCertificate);

        var result = createClientCertificateUseCase.execute(
            new CreateClientCertificateUseCase.Input(
                appId,
                new ClientCertificate("Test Certificate", "pem-content", new Date(), Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            )
        );

        assertThat(result.clientCertificate()).isNotNull();
        assertThat(result.clientCertificate().id()).isEqualTo("generated-id");
        assertThat(result.clientCertificate().applicationId()).isEqualTo(appId);
        assertThat(result.clientCertificate().name()).isEqualTo("Test Certificate");
        assertThat(result.clientCertificate().fingerprint()).isEqualTo("sha256-fingerprint");
        assertThat(result.clientCertificate().subject()).isEqualTo("CN=unit-tests");
        assertThat(result.clientCertificate().issuer()).isEqualTo("CN=unit-tests-issuer");
        assertThat(result.clientCertificate().certificateExpiration()).isEqualTo(expiration);
        verify(clientCertificateDomainService).create(eq(appId), any(ClientCertificate.class));
    }

    @Test
    void should_throw_when_certificate_is_invalid() {
        when(clientCertificateValidationDomainService.validateForCreation(any(ClientCertificate.class), any())).thenThrow(
            new ClientCertificateEmptyException()
        );

        assertThatThrownBy(() ->
            createClientCertificateUseCase.execute(
                new CreateClientCertificateUseCase.Input("app-id", new ClientCertificate("Bad Cert", "not-a-pem", null, null))
            )
        ).isInstanceOf(ClientCertificateEmptyException.class);
    }
}
