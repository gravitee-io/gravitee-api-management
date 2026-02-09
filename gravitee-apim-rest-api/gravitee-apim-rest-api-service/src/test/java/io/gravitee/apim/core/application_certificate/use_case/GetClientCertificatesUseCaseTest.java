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

import inmemory.ClientCertificateCrudServiceInMemory;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetClientCertificatesUseCaseTest {

    private final ClientCertificateCrudServiceInMemory clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();
    private GetClientCertificatesUseCase getClientCertificatesUseCase;

    @BeforeEach
    void setUp() {
        clientCertificateCrudService.reset();
        getClientCertificatesUseCase = new GetClientCertificatesUseCase(clientCertificateCrudService);
    }

    @Test
    void should_return_certificates_for_application() {
        var appId = "app-id";
        var certificate1 = ClientCertificate.builder()
            .id("cert-1")
            .crossId("cross-id-1")
            .applicationId(appId)
            .name("Certificate 1")
            .startsAt(new Date())
            .endsAt(new Date())
            .createdAt(new Date())
            .updatedAt(new Date())
            .certificate("PEM_CONTENT_1")
            .certificateExpiration(new Date())
            .subject("CN=Test1")
            .issuer("CN=Issuer")
            .fingerprint("fingerprint1")
            .environmentId("env-id")
            .status(ClientCertificateStatus.ACTIVE)
            .build();
        var certificate2 = ClientCertificate.builder()
            .id("cert-2")
            .crossId("cross-id-2")
            .applicationId(appId)
            .name("Certificate 2")
            .startsAt(new Date())
            .endsAt(new Date())
            .createdAt(new Date())
            .updatedAt(new Date())
            .certificate("PEM_CONTENT_2")
            .certificateExpiration(new Date())
            .subject("CN=Test2")
            .issuer("CN=Issuer")
            .fingerprint("fingerprint2")
            .environmentId("env-id")
            .status(ClientCertificateStatus.ACTIVE)
            .build();
        clientCertificateCrudService.initWith(List.of(certificate1, certificate2));

        var result = getClientCertificatesUseCase.execute(new GetClientCertificatesUseCase.Input(appId, new PageableImpl(1, 10)));

        assertThat(result.clientCertificates()).isNotNull();
        assertThat(result.clientCertificates().getContent()).hasSize(2);
        assertThat(result.clientCertificates().getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_return_empty_page_when_no_certificates() {
        var result = getClientCertificatesUseCase.execute(
            new GetClientCertificatesUseCase.Input("app-without-certs", new PageableImpl(1, 10))
        );

        assertThat(result.clientCertificates()).isNotNull();
        assertThat(result.clientCertificates().getContent()).isEmpty();
        assertThat(result.clientCertificates().getTotalElements()).isZero();
    }
}
