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
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.domain_service.MtlsSubscriptionSyncDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.exceptions.ClientCertificateLastRemovalException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
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
    private static final String PLAN_ID = "plan-id";
    private static final String API_ID = "api-id";

    private final ClientCertificateCrudServiceInMemory clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();
    private final SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory(
        subscriptionCrudService,
        planCrudService
    );

    @Mock
    private MtlsSubscriptionSyncDomainService mtlsSubscriptionSyncDomainService;

    private ClientCertificateDomainServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientCertificateDomainServiceImpl(
            clientCertificateCrudService,
            mtlsSubscriptionSyncDomainService,
            subscriptionQueryService
        );
    }

    @AfterEach
    void tearDown() {
        clientCertificateCrudService.reset();
        subscriptionCrudService.reset();
        planCrudService.reset();
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

    @Nested
    class Update {

        @Test
        void should_update_certificate_and_sync_subscriptions() {
            var certId = "cert-id";
            var existing = buildClientCertificate(certId, "Original Name", ClientCertificateStatus.ACTIVE);
            clientCertificateCrudService.initWith(List.of(existing));

            var updateRequest = new ClientCertificate("Updated Name", new Date(), Date.from(Instant.now().plus(365, ChronoUnit.DAYS)));

            var result = service.update(null, certId, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(certId);
            assertThat(result.name()).isEqualTo("Updated Name");
            verify(mtlsSubscriptionSyncDomainService).updateActiveMTLSSubscriptions(APPLICATION_ID);
        }

        @Test
        void should_update_certificate_when_applicationId_matches() {
            var certId = "cert-id";
            var existing = buildClientCertificate(certId, "Original Name", ClientCertificateStatus.ACTIVE);
            clientCertificateCrudService.initWith(List.of(existing));

            var updateRequest = new ClientCertificate("Updated Name", new Date(), Date.from(Instant.now().plus(365, ChronoUnit.DAYS)));

            var result = service.update(APPLICATION_ID, certId, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Updated Name");
        }

        @Test
        void should_throw_when_applicationId_does_not_match() {
            var certId = "cert-id";
            var existing = buildClientCertificate(certId, "Original Name", ClientCertificateStatus.ACTIVE);
            clientCertificateCrudService.initWith(List.of(existing));

            var updateRequest = new ClientCertificate("Updated Name", new Date(), Date.from(Instant.now().plus(365, ChronoUnit.DAYS)));

            assertThatThrownBy(() -> service.update("other-app-id", certId, updateRequest)).isInstanceOf(
                ClientCertificateNotFoundException.class
            );
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_certificate_and_sync_subscriptions() {
            var certId = "cert-id";
            var existing = buildClientCertificate(certId, "Test Certificate", ClientCertificateStatus.ACTIVE);
            clientCertificateCrudService.initWith(List.of(existing));

            service.delete(APPLICATION_ID, certId);

            assertThat(clientCertificateCrudService.storage()).isEmpty();
            verify(mtlsSubscriptionSyncDomainService).updateActiveMTLSSubscriptions(APPLICATION_ID);
        }

        @Test
        void should_delete_when_no_mtls_subscriptions() {
            var certId = "cert-id";
            var existing = buildClientCertificate(certId, "Test Certificate", ClientCertificateStatus.ACTIVE);
            clientCertificateCrudService.initWith(List.of(existing));

            // No subscriptions at all
            service.delete(APPLICATION_ID, certId);

            assertThat(clientCertificateCrudService.storage()).isEmpty();
        }

        @Test
        void should_delete_when_other_active_certs_remain() {
            var certId = "cert-1";
            var cert1 = buildClientCertificate(certId, "Cert 1", ClientCertificateStatus.ACTIVE);
            var cert2 = buildClientCertificate("cert-2", "Cert 2", ClientCertificateStatus.ACTIVE);
            clientCertificateCrudService.initWith(List.of(cert1, cert2));

            var mtlsPlan = buildPlan(PlanSecurityType.MTLS.name());
            planCrudService.initWith(List.of(mtlsPlan));
            var subscription = buildSubscription("sub-1", APPLICATION_ID, PLAN_ID);
            subscriptionCrudService.initWith(List.of(subscription));

            service.delete(APPLICATION_ID, certId);

            assertThat(clientCertificateCrudService.storage()).hasSize(1);
            assertThat(clientCertificateCrudService.storage().getFirst().id()).isEqualTo("cert-2");
        }

        @Test
        void should_reject_deletion_when_last_active_cert_and_mtls_subscriptions_exist() {
            var certId = "cert-id";
            var existing = buildClientCertificate(certId, "Test Certificate", ClientCertificateStatus.ACTIVE);
            clientCertificateCrudService.initWith(List.of(existing));

            var mtlsPlan = buildPlan(PlanSecurityType.MTLS.name());
            planCrudService.initWith(List.of(mtlsPlan));
            var subscription = buildSubscription("sub-1", APPLICATION_ID, PLAN_ID);
            subscriptionCrudService.initWith(List.of(subscription));

            assertThatThrownBy(() -> service.delete(APPLICATION_ID, certId)).isInstanceOf(ClientCertificateLastRemovalException.class);

            assertThat(clientCertificateCrudService.storage()).hasSize(1);
        }

        @Test
        void should_delete_when_remaining_cert_is_active_with_end() {
            var certId = "cert-1";
            var cert1 = buildClientCertificate(certId, "Cert 1", ClientCertificateStatus.ACTIVE);
            var cert2 = buildClientCertificate("cert-2", "Cert 2", ClientCertificateStatus.ACTIVE_WITH_END);
            clientCertificateCrudService.initWith(List.of(cert1, cert2));

            var mtlsPlan = buildPlan(PlanSecurityType.MTLS.name());
            planCrudService.initWith(List.of(mtlsPlan));
            var subscription = buildSubscription("sub-1", APPLICATION_ID, PLAN_ID);
            subscriptionCrudService.initWith(List.of(subscription));

            service.delete(APPLICATION_ID, certId);

            assertThat(clientCertificateCrudService.storage()).hasSize(1);
            assertThat(clientCertificateCrudService.storage().getFirst().id()).isEqualTo("cert-2");
        }

        @Test
        void should_reject_deletion_when_only_revoked_certs_remain() {
            var certId = "cert-1";
            var cert1 = buildClientCertificate(certId, "Cert 1", ClientCertificateStatus.ACTIVE);
            var cert2 = buildClientCertificate("cert-2", "Cert 2", ClientCertificateStatus.REVOKED);
            clientCertificateCrudService.initWith(List.of(cert1, cert2));

            var mtlsPlan = buildPlan(PlanSecurityType.MTLS.name());
            planCrudService.initWith(List.of(mtlsPlan));
            var subscription = buildSubscription("sub-1", APPLICATION_ID, PLAN_ID);
            subscriptionCrudService.initWith(List.of(subscription));

            assertThatThrownBy(() -> service.delete(APPLICATION_ID, certId)).isInstanceOf(ClientCertificateLastRemovalException.class);

            assertThat(clientCertificateCrudService.storage()).hasSize(2);
        }

        @Test
        void should_throw_when_applicationId_does_not_match() {
            var certId = "cert-id";
            var existing = buildClientCertificate(certId, "Test Certificate", ClientCertificateStatus.ACTIVE);
            clientCertificateCrudService.initWith(List.of(existing));

            assertThatThrownBy(() -> service.delete("other-app-id", certId)).isInstanceOf(ClientCertificateNotFoundException.class);

            assertThat(clientCertificateCrudService.storage()).hasSize(1);
        }

        @Test
        void should_resolve_applicationId_from_certificate_when_null() {
            var certId = "cert-id";
            var existing = buildClientCertificate(certId, "Test Certificate", ClientCertificateStatus.ACTIVE);
            clientCertificateCrudService.initWith(List.of(existing));

            service.delete(null, certId);

            assertThat(clientCertificateCrudService.storage()).isEmpty();
            verify(mtlsSubscriptionSyncDomainService).updateActiveMTLSSubscriptions(APPLICATION_ID);
        }
    }

    @Test
    void should_not_sync_subscriptions_when_create_fails() {
        var crudService = mock(ClientCertificateCrudService.class);
        when(crudService.create(anyString(), any())).thenThrow(new RuntimeException("DB error"));
        var failingService = new ClientCertificateDomainServiceImpl(
            crudService,
            mtlsSubscriptionSyncDomainService,
            subscriptionQueryService
        );

        var toCreate = buildClientCertificate(null, "Test Certificate", null);
        assertThatThrownBy(() -> failingService.create(APPLICATION_ID, toCreate)).isInstanceOf(RuntimeException.class);

        verify(mtlsSubscriptionSyncDomainService, never()).updateActiveMTLSSubscriptions(anyString());
    }

    @Test
    void should_not_sync_subscriptions_when_update_fails() {
        var crudService = mock(ClientCertificateCrudService.class);
        when(crudService.update(anyString(), any())).thenThrow(new RuntimeException("DB error"));
        var failingService = new ClientCertificateDomainServiceImpl(
            crudService,
            mtlsSubscriptionSyncDomainService,
            subscriptionQueryService
        );

        var updateRequest = new ClientCertificate("Updated Name", new Date(), Date.from(Instant.now().plus(365, ChronoUnit.DAYS)));
        assertThatThrownBy(() -> failingService.update(null, "cert-id", updateRequest)).isInstanceOf(RuntimeException.class);

        verify(mtlsSubscriptionSyncDomainService, never()).updateActiveMTLSSubscriptions(anyString());
    }

    @Test
    void should_not_sync_subscriptions_when_crud_delete_fails() {
        var crudService = mock(ClientCertificateCrudService.class);
        var cert = buildClientCertificate("cert-id", "Test Certificate", ClientCertificateStatus.ACTIVE);
        when(crudService.findById("cert-id")).thenReturn(cert);
        doThrow(new RuntimeException("DB error")).when(crudService).delete(anyString());
        var failingService = new ClientCertificateDomainServiceImpl(
            crudService,
            mtlsSubscriptionSyncDomainService,
            subscriptionQueryService
        );

        assertThatThrownBy(() -> failingService.delete(APPLICATION_ID, "cert-id")).isInstanceOf(RuntimeException.class);

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

    private Plan buildPlan(String securityType) {
        var planDefinition = io.gravitee.definition.model.v4.plan.Plan.builder()
            .id(PLAN_ID)
            .security(PlanSecurity.builder().type(securityType).build())
            .build();

        return Plan.builder()
            .id(PLAN_ID)
            .apiId(API_ID)
            .definitionVersion(DefinitionVersion.V4)
            .planDefinitionHttpV4(planDefinition)
            .build();
    }

    private SubscriptionEntity buildSubscription(String subscriptionId, String applicationId, String planId) {
        var now = ZonedDateTime.now();
        return SubscriptionEntity.builder()
            .id(subscriptionId)
            .applicationId(applicationId)
            .planId(planId)
            .apiId(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .status(SubscriptionEntity.Status.ACCEPTED)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
