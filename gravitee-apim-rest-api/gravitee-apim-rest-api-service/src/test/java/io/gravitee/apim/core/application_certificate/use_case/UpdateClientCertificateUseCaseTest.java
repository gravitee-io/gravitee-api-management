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
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificateStatus;
import io.gravitee.rest.api.model.clientcertificate.UpdateClientCertificate;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateClientCertificateUseCaseTest {

    private final ClientCertificateCrudServiceInMemory clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();
    private UpdateClientCertificateUseCase updateClientCertificateUseCase;

    @BeforeEach
    void setUp() {
        clientCertificateCrudService.reset();
        updateClientCertificateUseCase = new UpdateClientCertificateUseCase(clientCertificateCrudService);
    }

    @Test
    void should_update_client_certificate() {
        var certId = "cert-id";
        var certificate = new ClientCertificate(
            certId,
            "cross-id",
            "app-id",
            "Original Name",
            new Date(),
            new Date(),
            new Date(),
            new Date(),
            "PEM_CONTENT",
            new Date(),
            "CN=Test",
            "CN=Issuer",
            "fingerprint",
            "env-id",
            "org-id",
            ClientCertificateStatus.ACTIVE
        );
        clientCertificateCrudService.initWith(List.of(certificate));

        var updateRequest = new UpdateClientCertificate("Updated Name", new Date(), new Date());

        var result = updateClientCertificateUseCase.execute(new UpdateClientCertificateUseCase.Input(certId, updateRequest));

        assertThat(result.clientCertificate()).isNotNull();
        assertThat(result.clientCertificate().id()).isEqualTo(certId);
        assertThat(result.clientCertificate().name()).isEqualTo("Updated Name");
    }

    @Test
    void should_throw_exception_when_certificate_not_found() {
        var updateRequest = new UpdateClientCertificate("Updated Name", new Date(), new Date());
        var input = new UpdateClientCertificateUseCase.Input("non-existent-id", updateRequest);

        assertThatThrownBy(() -> updateClientCertificateUseCase.execute(input)).isInstanceOf(ClientCertificateNotFoundException.class);
    }
}
