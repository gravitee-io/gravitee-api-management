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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateDomainService;
import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateValidationDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.rest.api.service.exceptions.ClientCertificateDateBoundsInvalidException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateClientCertificateUseCaseTest {

    @Mock
    private ClientCertificateDomainService clientCertificateDomainService;

    @Mock
    private ClientCertificateValidationDomainService clientCertificateValidationDomainService;

    private UpdateClientCertificateUseCase updateClientCertificateUseCase;

    @BeforeEach
    void setUp() {
        updateClientCertificateUseCase = new UpdateClientCertificateUseCase(
            clientCertificateDomainService,
            clientCertificateValidationDomainService
        );
    }

    @Test
    void should_update_client_certificate() {
        var certId = "cert-id";
        var appId = "app-id";
        var updatedCertificate = new ClientCertificate(
            certId,
            "cross-id",
            appId,
            "Updated Name",
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
            ClientCertificateStatus.ACTIVE
        );
        var updateRequest = new ClientCertificate("Updated Name", new Date(), new Date());
        when(clientCertificateDomainService.update(isNull(), eq(certId), any(ClientCertificate.class))).thenReturn(updatedCertificate);

        var result = updateClientCertificateUseCase.execute(new UpdateClientCertificateUseCase.Input(certId, updateRequest));

        assertThat(result.clientCertificate()).isNotNull();
        assertThat(result.clientCertificate().id()).isEqualTo(certId);
        assertThat(result.clientCertificate().name()).isEqualTo("Updated Name");

        InOrder inOrder = inOrder(clientCertificateValidationDomainService, clientCertificateDomainService);
        inOrder.verify(clientCertificateValidationDomainService).validateForUpdate(any(ClientCertificate.class));
        inOrder.verify(clientCertificateDomainService).update(isNull(), eq(certId), any(ClientCertificate.class));
    }

    @Test
    void should_throw_exception_when_certificate_not_found() {
        var updateRequest = new ClientCertificate("Updated Name", new Date(), new Date());
        when(clientCertificateDomainService.update(isNull(), eq("non-existent-id"), any(ClientCertificate.class))).thenThrow(
            new ClientCertificateNotFoundException("non-existent-id")
        );

        var input = new UpdateClientCertificateUseCase.Input("non-existent-id", updateRequest);

        assertThatThrownBy(() -> updateClientCertificateUseCase.execute(input)).isInstanceOf(ClientCertificateNotFoundException.class);
    }

    @Test
    void should_throw_exception_when_applicationId_does_not_match() {
        var certId = "cert-id";
        var updateRequest = new ClientCertificate("Updated Name", new Date(), new Date());
        when(clientCertificateDomainService.update(eq("other-app-id"), eq(certId), any(ClientCertificate.class))).thenThrow(
            new ClientCertificateNotFoundException(certId)
        );

        var input = new UpdateClientCertificateUseCase.Input(Optional.of("other-app-id"), certId, updateRequest);

        assertThatThrownBy(() -> updateClientCertificateUseCase.execute(input)).isInstanceOf(ClientCertificateNotFoundException.class);
    }

    @Test
    void should_throw_exception_when_date_bounds_are_invalid() {
        var startsAt = Date.from(Instant.now().plus(2, ChronoUnit.DAYS));
        var endsAt = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        var updateRequest = new ClientCertificate("Bad Dates", startsAt, endsAt);
        doThrow(new ClientCertificateDateBoundsInvalidException())
            .when(clientCertificateValidationDomainService)
            .validateForUpdate(any(ClientCertificate.class));

        var input = new UpdateClientCertificateUseCase.Input("cert-id", updateRequest);

        assertThatThrownBy(() -> updateClientCertificateUseCase.execute(input)).isInstanceOf(
            ClientCertificateDateBoundsInvalidException.class
        );
    }
}
