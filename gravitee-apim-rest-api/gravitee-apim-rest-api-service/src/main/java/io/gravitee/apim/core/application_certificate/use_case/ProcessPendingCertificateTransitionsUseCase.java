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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.domain_service.ApplicationCertificatesUpdateDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.CustomLog;

/**
 * Use case that processes pending certificate transitions.
 * <p>
 * Certificates with {@code ACTIVE_WITH_END} status whose {@code endsAt} has passed are revoked.
 * Certificates with {@code SCHEDULED} status whose {@code startsAt} has passed are activated.
 * For each affected application, mTLS subscriptions are updated.
 *
 * @author GraviteeSource Team
 */
@UseCase
@CustomLog
public class ProcessPendingCertificateTransitionsUseCase {

    private final ClientCertificateCrudService clientCertificateCrudService;
    private final EnvironmentCrudService environmentCrudService;
    private final AuditDomainService auditDomainService;
    private final ApplicationCertificatesUpdateDomainService applicationCertificatesUpdateDomainService;

    public ProcessPendingCertificateTransitionsUseCase(
        ClientCertificateCrudService clientCertificateCrudService,
        EnvironmentCrudService environmentCrudService,
        AuditDomainService auditDomainService,
        ApplicationCertificatesUpdateDomainService applicationCertificatesUpdateDomainService
    ) {
        this.clientCertificateCrudService = clientCertificateCrudService;
        this.environmentCrudService = environmentCrudService;
        this.auditDomainService = auditDomainService;
        this.applicationCertificatesUpdateDomainService = applicationCertificatesUpdateDomainService;
    }

    public Output execute(Input input) {
        log.info("Processing pending certificate transitions");

        var candidates = clientCertificateCrudService.findByStatuses(
            ClientCertificateStatus.ACTIVE_WITH_END,
            ClientCertificateStatus.SCHEDULED
        );

        log.info("Found {} candidate certificates to evaluate", candidates.size());

        List<ClientCertificate> transitioned = new ArrayList<>();
        Set<String> affectedApplicationIds = new HashSet<>();
        int failedCount = 0;

        for (var certificate : candidates) {
            try {
                if (shouldTransition(certificate)) {
                    applyTransition(certificate, input.auditActor, transitioned, affectedApplicationIds);
                }
            } catch (Exception e) {
                failedCount++;
                log.error(
                    "Failed to process certificate transition for [{}] on application [{}]: {}",
                    certificate.getId(),
                    certificate.getApplicationId(),
                    e.getMessage(),
                    e
                );
            }
        }

        int failedMtlsUpdateCount = 0;
        for (var applicationId : affectedApplicationIds) {
            try {
                applicationCertificatesUpdateDomainService.updateActiveMTLSSubscriptions(applicationId);
            } catch (Exception e) {
                failedMtlsUpdateCount++;
                log.error("Failed to update mTLS subscriptions for application [{}]: {}", applicationId, e.getMessage(), e);
            }
        }

        if (!candidates.isEmpty()) {
            int successfulMtlsUpdates = affectedApplicationIds.size() - failedMtlsUpdateCount;
            var message =
                "Completed certificate transitions: {} transitioned, {} failed | mTLS updates: {} updated, {} failed over {} applications";
            if (failedCount > 0 || failedMtlsUpdateCount > 0) {
                log.warn(
                    message,
                    transitioned.size(),
                    failedCount,
                    successfulMtlsUpdates,
                    failedMtlsUpdateCount,
                    affectedApplicationIds.size()
                );
            } else {
                log.info(
                    message,
                    transitioned.size(),
                    failedCount,
                    successfulMtlsUpdates,
                    failedMtlsUpdateCount,
                    affectedApplicationIds.size()
                );
            }
        }

        return new Output(transitioned);
    }

    private boolean shouldTransition(ClientCertificate certificate) {
        var computedStatus = ClientCertificateStatus.computeStatus(certificate.getStartsAt(), certificate.getEndsAt());
        return computedStatus != certificate.getStatus();
    }

    private void applyTransition(
        ClientCertificate certificate,
        AuditActor auditActor,
        List<ClientCertificate> transitioned,
        Set<String> affectedApplicationIds
    ) {
        var oldCertificate = certificate.toBuilder().build();
        var updated = clientCertificateCrudService.update(certificate.getId(), certificate);
        // Audit log failure must not block executing the application update.
        // Revocation/activation takes precedence over auditability -- a missing
        // audit record is preferable to a certificate stuck in a stale state.
        try {
            createAuditLog(oldCertificate, updated, auditActor);
        } catch (Exception e) {
            log.error(
                "Failed to create audit log for certificate [{}] on application [{}]: {}",
                certificate.getId(),
                certificate.getApplicationId(),
                e.getMessage(),
                e
            );
        }
        transitioned.add(updated);
        affectedApplicationIds.add(certificate.getApplicationId());
    }

    private void createAuditLog(ClientCertificate oldCertificate, ClientCertificate newCertificate, AuditActor actor) {
        var environment = environmentCrudService.get(oldCertificate.getEnvironmentId());

        auditDomainService.createApplicationAuditLog(
            ApplicationAuditLogEntity.builder()
                .organizationId(environment.getOrganizationId())
                .environmentId(environment.getId())
                .applicationId(oldCertificate.getApplicationId())
                .actor(actor)
                .event(newCertificate.getStatus().toAuditEvent())
                .oldValue(oldCertificate)
                .newValue(newCertificate)
                .createdAt(ZonedDateTime.now())
                .properties(Collections.singletonMap(AuditProperties.CLIENT_CERTIFICATE, oldCertificate.getId()))
                .build()
        );
    }

    public record Input(AuditActor auditActor) {
        public Input {
            Objects.requireNonNull(auditActor, "auditActor must not be null");
        }
    }

    public record Output(List<ClientCertificate> transitionedCertificates) {}
}
