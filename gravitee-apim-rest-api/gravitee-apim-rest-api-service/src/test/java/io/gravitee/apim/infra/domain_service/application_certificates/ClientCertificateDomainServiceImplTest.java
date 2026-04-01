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
package io.gravitee.apim.infra.domain_service.application_certificates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.ClientCertificateCrudServiceInMemory;
import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.domain_service.MtlsSubscriptionSyncDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.rest.api.service.exceptions.ClientCertificateLastRemovalException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientCertificateDomainServiceImplTest {

    private static final String APPLICATION_ID = "app-id";
    private static final String ENVIRONMENT_ID = "env-id";

    private final ClientCertificateCrudServiceInMemory clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();

    @Mock
    private MtlsSubscriptionSyncDomainService mtlsSubscriptionSyncDomainService;

    private ClientCertificateDomainServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientCertificateDomainServiceImpl(clientCertificateCrudService, mtlsSubscriptionSyncDomainService);
    }

    @AfterEach
    void tearDown() {
        clientCertificateCrudService.reset();
    }

    @Test
    void should_create_certificate_and_sync_subscriptions() {
        var toCreate = new ClientCertificate(
            null,
            null,
            null,
            "Test Certificate",
            new Date(),
            Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
            null,
            null,
            "PEM_CONTENT",
            Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
            "CN=Test",
            "CN=Issuer",
            "fingerprint",
            ENVIRONMENT_ID,
            null
        );

        var result = service.create(APPLICATION_ID, toCreate);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.name()).isEqualTo("Test Certificate");
        assertThat(clientCertificateCrudService.storage()).hasSize(1);
        verify(mtlsSubscriptionSyncDomainService).updateActiveMTLSSubscriptions(APPLICATION_ID);
    }

    @Test
    void should_update_certificate_and_sync_subscriptions() {
        var certId = "cert-id";
        var existing = buildClientCertificate(certId, "Original Name", ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(existing));

        var updateRequest = new ClientCertificate("Updated Name", new Date(), Date.from(Instant.now().plus(365, ChronoUnit.DAYS)));

        var result = service.update(certId, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(certId);
        assertThat(result.name()).isEqualTo("Updated Name");
        verify(mtlsSubscriptionSyncDomainService).updateActiveMTLSSubscriptions(APPLICATION_ID);
    }

    @Test
    void should_delete_certificate_and_sync_subscriptions() {
        var certId = "cert-id";
        var existing = buildClientCertificate(certId, "Test Certificate", ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(existing));

        service.delete(APPLICATION_ID, certId);

        assertThat(clientCertificateCrudService.storage()).isEmpty();
        verify(mtlsSubscriptionSyncDomainService).validateCertificateRemoval(APPLICATION_ID, certId);
        verify(mtlsSubscriptionSyncDomainService).updateActiveMTLSSubscriptions(APPLICATION_ID);
    }

    @Test
    void should_not_delete_when_validation_fails() {
        var certId = "cert-id";
        var existing = buildClientCertificate(certId, "Test Certificate", ClientCertificateStatus.ACTIVE);
        clientCertificateCrudService.initWith(List.of(existing));

        doThrow(new ClientCertificateLastRemovalException(APPLICATION_ID))
            .when(mtlsSubscriptionSyncDomainService)
            .validateCertificateRemoval(APPLICATION_ID, certId);

        assertThatThrownBy(() -> service.delete(APPLICATION_ID, certId)).isInstanceOf(ClientCertificateLastRemovalException.class);

        assertThat(clientCertificateCrudService.storage()).hasSize(1);
        verify(mtlsSubscriptionSyncDomainService, never()).updateActiveMTLSSubscriptions(anyString());
    }

    @Test
    void should_not_sync_subscriptions_when_create_fails() {
        var crudService = mock(ClientCertificateCrudService.class);
        when(crudService.create(anyString(), any())).thenThrow(new RuntimeException("DB error"));
        var failingService = new ClientCertificateDomainServiceImpl(crudService, mtlsSubscriptionSyncDomainService);

        var toCreate = buildClientCertificate(null, "Test Certificate", null);
        assertThatThrownBy(() -> failingService.create(APPLICATION_ID, toCreate)).isInstanceOf(RuntimeException.class);

        verify(mtlsSubscriptionSyncDomainService, never()).updateActiveMTLSSubscriptions(anyString());
    }

    @Test
    void should_not_sync_subscriptions_when_update_fails() {
        var crudService = mock(ClientCertificateCrudService.class);
        when(crudService.update(anyString(), any())).thenThrow(new RuntimeException("DB error"));
        var failingService = new ClientCertificateDomainServiceImpl(crudService, mtlsSubscriptionSyncDomainService);

        var updateRequest = new ClientCertificate("Updated Name", new Date(), Date.from(Instant.now().plus(365, ChronoUnit.DAYS)));
        assertThatThrownBy(() -> failingService.update("cert-id", updateRequest)).isInstanceOf(RuntimeException.class);

        verify(mtlsSubscriptionSyncDomainService, never()).updateActiveMTLSSubscriptions(anyString());
    }

    @Test
    void should_not_sync_subscriptions_when_crud_delete_fails() {
        var crudService = mock(ClientCertificateCrudService.class);
        doThrow(new RuntimeException("DB error")).when(crudService).delete(anyString());
        var failingService = new ClientCertificateDomainServiceImpl(crudService, mtlsSubscriptionSyncDomainService);

        assertThatThrownBy(() -> failingService.delete(APPLICATION_ID, "cert-id")).isInstanceOf(RuntimeException.class);

        verify(mtlsSubscriptionSyncDomainService).validateCertificateRemoval(APPLICATION_ID, "cert-id");
        verify(mtlsSubscriptionSyncDomainService, never()).updateActiveMTLSSubscriptions(anyString());
    }

    private ClientCertificate buildClientCertificate(String id, String name, ClientCertificateStatus status) {
        return new ClientCertificate(
            id,
            "cross-id-" + id,
            APPLICATION_ID,
            name,
            new Date(),
            Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
            new Date(),
            new Date(),
            "PEM_CONTENT",
            Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
            "CN=Test",
            "CN=Issuer",
            "fingerprint-" + id,
            ENVIRONMENT_ID,
            status
        );
    }
}
