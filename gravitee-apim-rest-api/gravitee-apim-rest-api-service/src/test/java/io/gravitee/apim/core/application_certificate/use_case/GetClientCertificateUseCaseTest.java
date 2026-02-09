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

import inmemory.ClientCertificateCrudServiceInMemory;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetClientCertificateUseCaseTest {

    private final ClientCertificateCrudServiceInMemory clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();
    private GetClientCertificateUseCase getClientCertificateUseCase;

    @BeforeEach
    void setUp() {
        clientCertificateCrudService.reset();
        getClientCertificateUseCase = new GetClientCertificateUseCase(clientCertificateCrudService);
    }

    @Test
    void should_return_client_certificate_when_found() {
        var certId = "cert-id";
        var certificate = ClientCertificate.builder()
            .id(certId)
            .crossId("cross-id")
            .applicationId("app-id")
            .name("Test Certificate")
            .startsAt(new Date())
            .endsAt(new Date())
            .createdAt(new Date())
            .updatedAt(new Date())
            .certificate("PEM_CONTENT")
            .certificateExpiration(new Date())
            .subject("CN=Test")
            .issuer("CN=Issuer")
            .fingerprint("fingerprint")
            .environmentId("env-id")
            .status(ClientCertificateStatus.ACTIVE)
            .build();
        clientCertificateCrudService.initWith(List.of(certificate));

        var result = getClientCertificateUseCase.execute(new GetClientCertificateUseCase.Input(certId));

        assertThat(result.clientCertificate()).isNotNull();
        assertThat(result.clientCertificate().getId()).isEqualTo(certId);
        assertThat(result.clientCertificate().getName()).isEqualTo("Test Certificate");
    }

    @Test
    void should_throw_exception_when_certificate_not_found() {
        var input = new GetClientCertificateUseCase.Input("non-existent-id");

        assertThatThrownBy(() -> getClientCertificateUseCase.execute(input)).isInstanceOf(ClientCertificateNotFoundException.class);
    }
}
