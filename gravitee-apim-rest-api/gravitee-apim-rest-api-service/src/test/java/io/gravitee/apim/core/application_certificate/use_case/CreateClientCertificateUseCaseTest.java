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
import static org.mockito.Mockito.verify;

import inmemory.ClientCertificateCrudServiceInMemory;
import io.gravitee.apim.core.application_certificate.domain_service.ApplicationCertificatesUpdateDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateClientCertificateUseCaseTest {

    private final ClientCertificateCrudServiceInMemory clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();

    @Mock
    private ApplicationCertificatesUpdateDomainService applicationCertificatesUpdateDomainService;

    private CreateClientCertificateUseCase createClientCertificateUseCase;

    @BeforeEach
    void setUp() {
        clientCertificateCrudService.reset();
        createClientCertificateUseCase = new CreateClientCertificateUseCase(
            clientCertificateCrudService,
            applicationCertificatesUpdateDomainService
        );
    }

    @Test
    void should_create_client_certificate() {
        var appId = "app-id";

        var result = createClientCertificateUseCase.execute(
            new CreateClientCertificateUseCase.Input(
                appId,
                ClientCertificate.builder()
                    .name("Test Certificate")
                    .startsAt(new Date())
                    .endsAt(new Date())
                    .certificate("PEM_CONTENT")
                    .build()
            )
        );

        assertThat(result.clientCertificate()).isNotNull();
        assertThat(result.clientCertificate().getId()).isNotNull();
        assertThat(result.clientCertificate().getApplicationId()).isEqualTo(appId);
        assertThat(result.clientCertificate().getName()).isEqualTo("Test Certificate");
        assertThat(clientCertificateCrudService.storage()).hasSize(1);
        verify(applicationCertificatesUpdateDomainService).updateActiveMTLSSubscriptions(appId);
    }
}
