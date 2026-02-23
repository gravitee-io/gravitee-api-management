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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import inmemory.AuditCrudServiceInMemory;
import inmemory.ClientCertificateCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.application_certificate.domain_service.ApplicationCertificatesUpdateDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditEntity.AuditReferenceType;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProcessPendingCertificateTransitionsUseCaseTest {

    private static final String USER_ID = "system";
    private static final AuditActor AUDIT_ACTOR = AuditActor.builder().userId(USER_ID).build();

    private final ClientCertificateCrudServiceInMemory clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();
    private final EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    @Mock
    private ApplicationCertificatesUpdateDomainService applicationCertificatesUpdateDomainService;

    private ProcessPendingCertificateTransitionsUseCase useCase;

    @BeforeEach
    void setUp() {
        UuidString.overrideGenerator(() -> "audit-id");

        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());

        useCase = new ProcessPendingCertificateTransitionsUseCase(
            clientCertificateCrudService,
            environmentCrudService,
            auditDomainService,
            applicationCertificatesUpdateDomainService
        );

        environmentCrudService.initWith(
            List.of(
                Environment.builder().id("env1").organizationId("org1").build(),
                Environment.builder().id("env2").organizationId("org2").build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        clientCertificateCrudService.reset();
        environmentCrudService.reset();
        auditCrudService.reset();
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    private ClientCertificate cert(
        String id,
        String appId,
        String envId,
        String name,
        Date startsAt,
        Date endsAt,
        Date createdAt,
        Date updatedAt,
        ClientCertificateStatus status
    ) {
        return new ClientCertificate(
            id,
            null,
            appId,
            name,
            startsAt,
            endsAt,
            createdAt,
            updatedAt,
            null,
            null,
            null,
            null,
            null,
            envId,
            status
        );
    }

    @Test
    void should_revoke_expired_active_with_end_certificates() {
        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        var pastStartDate = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));

        clientCertificateCrudService.initWith(
            List.of(
                cert(
                    "cert1",
                    "app1",
                    "env1",
                    "Expired cert",
                    pastStartDate,
                    pastDate,
                    pastStartDate,
                    pastStartDate,
                    ClientCertificateStatus.ACTIVE_WITH_END
                )
            )
        );

        var result = useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        assertThat(result.transitionedCertificates()).hasSize(1);
        assertThat(result.transitionedCertificates().get(0).status()).isEqualTo(ClientCertificateStatus.REVOKED);
        assertThat(clientCertificateCrudService.storage())
            .filteredOn(c -> "cert1".equals(c.id()))
            .extracting(ClientCertificate::status)
            .containsExactly(ClientCertificateStatus.REVOKED);
    }

    @Test
    void should_activate_scheduled_certificates_whose_start_date_has_passed() {
        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        var futureDate = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));

        clientCertificateCrudService.initWith(
            List.of(
                cert(
                    "cert2",
                    "app1",
                    "env1",
                    "Scheduled cert with end date",
                    pastDate,
                    futureDate,
                    pastDate,
                    pastDate,
                    ClientCertificateStatus.SCHEDULED
                )
            )
        );

        var result = useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        assertThat(result.transitionedCertificates()).hasSize(1);
        assertThat(result.transitionedCertificates().get(0).status()).isEqualTo(ClientCertificateStatus.ACTIVE_WITH_END);
        assertThat(clientCertificateCrudService.storage())
            .filteredOn(c -> "cert2".equals(c.id()))
            .extracting(ClientCertificate::status)
            .containsExactly(ClientCertificateStatus.ACTIVE_WITH_END);
    }

    @Test
    void should_activate_scheduled_certificates_without_end_date() {
        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));

        clientCertificateCrudService.initWith(
            List.of(
                cert(
                    "cert3",
                    "app1",
                    "env1",
                    "Scheduled cert no end",
                    pastDate,
                    null,
                    pastDate,
                    pastDate,
                    ClientCertificateStatus.SCHEDULED
                )
            )
        );

        var result = useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        assertThat(result.transitionedCertificates()).hasSize(1);
        assertThat(result.transitionedCertificates().get(0).status()).isEqualTo(ClientCertificateStatus.ACTIVE);
        assertThat(clientCertificateCrudService.storage())
            .filteredOn(c -> "cert3".equals(c.id()))
            .extracting(ClientCertificate::status)
            .containsExactly(ClientCertificateStatus.ACTIVE);
    }

    @Test
    void should_not_transition_certificates_that_are_not_yet_due() {
        var futureStart = Date.from(Instant.now().plus(10, ChronoUnit.DAYS));
        var futureEnd = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));
        var pastStart = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));
        var now = new Date();

        clientCertificateCrudService.initWith(
            List.of(
                cert(
                    "cert-scheduled-future",
                    "app1",
                    "env1",
                    "Future scheduled cert",
                    futureStart,
                    null,
                    now,
                    now,
                    ClientCertificateStatus.SCHEDULED
                ),
                cert(
                    "cert-active-with-end-future",
                    "app1",
                    "env1",
                    "Active with future end",
                    pastStart,
                    futureEnd,
                    now,
                    now,
                    ClientCertificateStatus.ACTIVE_WITH_END
                )
            )
        );

        var result = useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        assertThat(result.transitionedCertificates()).isEmpty();
        verify(applicationCertificatesUpdateDomainService, never()).updateActiveMTLSSubscriptions(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void should_create_audit_logs_for_each_transition() {
        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        var pastStartDate = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));
        var futureDate = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));

        clientCertificateCrudService.initWith(
            List.of(
                cert(
                    "cert-revoked",
                    "app1",
                    "env1",
                    "To be revoked",
                    pastStartDate,
                    pastDate,
                    pastStartDate,
                    pastStartDate,
                    ClientCertificateStatus.ACTIVE_WITH_END
                ),
                cert(
                    "cert-activated",
                    "app2",
                    "env2",
                    "To be activated",
                    pastDate,
                    futureDate,
                    pastDate,
                    pastDate,
                    ClientCertificateStatus.SCHEDULED
                )
            )
        );

        useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        assertThat(auditCrudService.storage())
            .hasSize(2)
            .extracting(
                AuditEntity::getOrganizationId,
                AuditEntity::getEnvironmentId,
                AuditEntity::getReferenceType,
                AuditEntity::getReferenceId,
                AuditEntity::getEvent,
                AuditEntity::getUser,
                AuditEntity::getProperties
            )
            .containsExactlyInAnyOrder(
                tuple(
                    "org1",
                    "env1",
                    AuditReferenceType.APPLICATION,
                    "app1",
                    "CLIENT_CERTIFICATE_REVOKED",
                    USER_ID,
                    Map.of(AuditProperties.CLIENT_CERTIFICATE.name(), "cert-revoked")
                ),
                tuple(
                    "org2",
                    "env2",
                    AuditReferenceType.APPLICATION,
                    "app2",
                    "CLIENT_CERTIFICATE_ACTIVATED",
                    USER_ID,
                    Map.of(AuditProperties.CLIENT_CERTIFICATE.name(), "cert-activated")
                )
            );
    }

    @Test
    void should_call_update_mtls_subscriptions_once_per_affected_application() {
        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        var pastStartDate = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));

        clientCertificateCrudService.initWith(
            List.of(
                cert(
                    "cert1",
                    "app1",
                    "env1",
                    "Cert 1 for app1",
                    pastStartDate,
                    pastDate,
                    pastStartDate,
                    pastStartDate,
                    ClientCertificateStatus.ACTIVE_WITH_END
                ),
                cert("cert2", "app1", "env1", "Cert 2 for app1", pastDate, null, pastDate, pastDate, ClientCertificateStatus.SCHEDULED),
                cert("cert3", "app2", "env2", "Cert for app2", pastDate, null, pastDate, pastDate, ClientCertificateStatus.SCHEDULED)
            )
        );

        useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        verify(applicationCertificatesUpdateDomainService).updateActiveMTLSSubscriptions("app1");
        verify(applicationCertificatesUpdateDomainService).updateActiveMTLSSubscriptions("app2");
    }

    @Test
    void should_return_empty_when_no_certificates_need_processing() {
        clientCertificateCrudService.initWith(List.of());

        var result = useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        assertThat(result.transitionedCertificates()).isEmpty();
        verify(applicationCertificatesUpdateDomainService, never()).updateActiveMTLSSubscriptions(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void should_capture_old_status_in_audit_patch() {
        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        var pastStartDate = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));

        clientCertificateCrudService.initWith(
            List.of(
                cert(
                    "cert-to-revoke",
                    "app1",
                    "env1",
                    "Expiring cert",
                    pastStartDate,
                    pastDate,
                    pastStartDate,
                    pastStartDate,
                    ClientCertificateStatus.ACTIVE_WITH_END
                )
            )
        );

        useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        // The patch should record the status change (replace op with new value REVOKED).
        // An empty patch would mean old and new were identical — the bug this test guards against.
        assertThat(auditCrudService.storage())
            .hasSize(1)
            .first()
            .satisfies(audit -> {
                assertThat(audit.getPatch()).isNotBlank();
                assertThat(audit.getPatch()).contains("replace");
                assertThat(audit.getPatch()).contains("REVOKED");
            });
    }

    @Test
    void should_still_transition_certificate_when_audit_log_creation_fails() {
        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        var pastStartDate = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));

        // "env-missing" does not exist in environmentCrudService, causing audit to throw
        clientCertificateCrudService.initWith(
            List.of(
                cert(
                    "cert-audit-fails",
                    "app1",
                    "env-missing",
                    "Cert with bad env",
                    pastStartDate,
                    pastDate,
                    pastStartDate,
                    pastStartDate,
                    ClientCertificateStatus.ACTIVE_WITH_END
                )
            )
        );

        var result = useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        assertThat(result.transitionedCertificates()).hasSize(1);
        assertThat(result.transitionedCertificates().get(0).status()).isEqualTo(ClientCertificateStatus.REVOKED);
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @Test
    void should_continue_updating_other_applications_when_one_mtls_update_fails() {
        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));

        clientCertificateCrudService.initWith(
            List.of(
                cert("cert-app1", "app1", "env1", "Cert for app1", pastDate, null, pastDate, pastDate, ClientCertificateStatus.SCHEDULED),
                cert("cert-app2", "app2", "env2", "Cert for app2", pastDate, null, pastDate, pastDate, ClientCertificateStatus.SCHEDULED)
            )
        );

        doThrow(new RuntimeException("mTLS update failed"))
            .when(applicationCertificatesUpdateDomainService)
            .updateActiveMTLSSubscriptions("app1");

        var result = useCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        assertThat(result.transitionedCertificates()).hasSize(2);
        verify(applicationCertificatesUpdateDomainService).updateActiveMTLSSubscriptions("app1");
        verify(applicationCertificatesUpdateDomainService).updateActiveMTLSSubscriptions("app2");
    }

    @Test
    void should_not_call_mtls_update_for_application_whose_certificate_update_fails() {
        var spiedService = spy(clientCertificateCrudService);
        var spiedUseCase = new ProcessPendingCertificateTransitionsUseCase(
            spiedService,
            environmentCrudService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            applicationCertificatesUpdateDomainService
        );

        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        var pastStartDate = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));

        spiedService.initWith(
            List.of(
                cert(
                    "cert-fail",
                    "app1",
                    "env1",
                    "Cert whose update fails",
                    pastStartDate,
                    pastDate,
                    pastStartDate,
                    pastStartDate,
                    ClientCertificateStatus.ACTIVE_WITH_END
                ),
                cert(
                    "cert-succeed",
                    "app2",
                    "env2",
                    "Cert whose update succeeds",
                    pastDate,
                    null,
                    pastDate,
                    pastDate,
                    ClientCertificateStatus.SCHEDULED
                )
            )
        );

        doAnswer(invocation -> {
            String certId = invocation.getArgument(0);
            if ("cert-fail".equals(certId)) {
                throw new RuntimeException("Update failed");
            }
            return invocation.callRealMethod();
        })
            .when(spiedService)
            .update(anyString(), any());

        spiedUseCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        verify(applicationCertificatesUpdateDomainService, never()).updateActiveMTLSSubscriptions("app1");
        verify(applicationCertificatesUpdateDomainService).updateActiveMTLSSubscriptions("app2");
    }

    @Test
    void should_continue_processing_remaining_certificates_when_one_certificate_update_fails() {
        var spiedService = spy(clientCertificateCrudService);
        var spiedUseCase = new ProcessPendingCertificateTransitionsUseCase(
            spiedService,
            environmentCrudService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            applicationCertificatesUpdateDomainService
        );

        var pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        var pastStartDate = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));

        spiedService.initWith(
            List.of(
                cert(
                    "cert-fail",
                    "app1",
                    "env1",
                    "Cert that will fail update",
                    pastStartDate,
                    pastDate,
                    pastStartDate,
                    pastStartDate,
                    ClientCertificateStatus.ACTIVE_WITH_END
                ),
                cert(
                    "cert-succeed",
                    "app2",
                    "env2",
                    "Cert that will succeed",
                    pastDate,
                    null,
                    pastDate,
                    pastDate,
                    ClientCertificateStatus.SCHEDULED
                )
            )
        );

        doAnswer(invocation -> {
            String certId = invocation.getArgument(0);
            if ("cert-fail".equals(certId)) {
                throw new RuntimeException("Update failed");
            }
            return invocation.callRealMethod();
        })
            .when(spiedService)
            .update(anyString(), any());

        var result = spiedUseCase.execute(new ProcessPendingCertificateTransitionsUseCase.Input(AUDIT_ACTOR));

        assertThat(result.transitionedCertificates()).hasSize(1);
        assertThat(result.transitionedCertificates().get(0).id()).isEqualTo("cert-succeed");

        // The failed certificate should remain unchanged in storage
        assertThat(clientCertificateCrudService.storage())
            .filteredOn(c -> "cert-fail".equals(c.id()))
            .extracting(ClientCertificate::status)
            .containsExactly(ClientCertificateStatus.ACTIVE_WITH_END);
    }
}
