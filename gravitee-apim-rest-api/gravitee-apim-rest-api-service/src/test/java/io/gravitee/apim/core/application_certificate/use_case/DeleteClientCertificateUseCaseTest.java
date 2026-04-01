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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateDomainService;
import io.gravitee.rest.api.service.exceptions.ClientCertificateLastRemovalException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteClientCertificateUseCaseTest {

    @Mock
    private ClientCertificateDomainService clientCertificateDomainService;

    private DeleteClientCertificateUseCase deleteClientCertificateUseCase;

    @BeforeEach
    void setUp() {
        deleteClientCertificateUseCase = new DeleteClientCertificateUseCase(clientCertificateDomainService);
    }

    @Test
    void should_delete_client_certificate() {
        var certId = "cert-id";

        deleteClientCertificateUseCase.execute(new DeleteClientCertificateUseCase.Input(certId));

        verify(clientCertificateDomainService).delete(null, certId);
    }

    @Test
    void should_delete_client_certificate_with_application_id() {
        var certId = "cert-id";
        var appId = "app-id";

        deleteClientCertificateUseCase.execute(new DeleteClientCertificateUseCase.Input(Optional.of(appId), certId));

        verify(clientCertificateDomainService).delete(appId, certId);
    }

    @Test
    void should_not_delete_when_last_active_certificate_with_mtls_subscriptions() {
        var certId = "cert-id";
        doThrow(new ClientCertificateLastRemovalException("app-id")).when(clientCertificateDomainService).delete(null, certId);

        var input = new DeleteClientCertificateUseCase.Input(certId);

        assertThatThrownBy(() -> deleteClientCertificateUseCase.execute(input)).isInstanceOf(ClientCertificateLastRemovalException.class);
    }

    @Test
    void should_throw_exception_when_certificate_not_found() {
        doThrow(new ClientCertificateNotFoundException("non-existent-id"))
            .when(clientCertificateDomainService)
            .delete(null, "non-existent-id");

        var input = new DeleteClientCertificateUseCase.Input("non-existent-id");

        assertThatThrownBy(() -> deleteClientCertificateUseCase.execute(input)).isInstanceOf(ClientCertificateNotFoundException.class);
    }

    @Test
    void should_throw_exception_when_applicationId_does_not_match() {
        doThrow(new ClientCertificateNotFoundException("cert-id")).when(clientCertificateDomainService).delete("other-app-id", "cert-id");

        var input = new DeleteClientCertificateUseCase.Input(Optional.of("other-app-id"), "cert-id");

        assertThatThrownBy(() -> deleteClientCertificateUseCase.execute(input)).isInstanceOf(ClientCertificateNotFoundException.class);
    }
}
