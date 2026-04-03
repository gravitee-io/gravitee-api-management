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

import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateDomainService;
import io.gravitee.apim.core.application_certificate.domain_service.MtlsSubscriptionSyncDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.service.exceptions.ClientCertificateLastRemovalException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementation of ClientCertificateDomainService.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
@Service
public class ClientCertificateDomainServiceImpl implements ClientCertificateDomainService {

    private final ClientCertificateCrudService clientCertificateCrudService;
    private final MtlsSubscriptionSyncDomainService mtlsSubscriptionSyncDomainService;
    private final SubscriptionQueryService subscriptionQueryService;

    @Override
    public ClientCertificate create(String applicationId, ClientCertificate certificate) {
        log.debug("Creating client certificate for application [{}]", applicationId);
        var created = clientCertificateCrudService.create(applicationId, certificate);
        mtlsSubscriptionSyncDomainService.updateActiveMTLSSubscriptions(applicationId);
        return created;
    }

    @Override
    public ClientCertificate update(String applicationId, String certificateId, ClientCertificate certificate) {
        log.debug("Updating client certificate [{}]", certificateId);
        resolveAndVerifyOwnership(certificateId, applicationId);
        var updated = clientCertificateCrudService.update(certificateId, certificate);
        mtlsSubscriptionSyncDomainService.updateActiveMTLSSubscriptions(updated.applicationId());
        return updated;
    }

    @Override
    public void delete(String applicationId, String certificateId) {
        log.debug("Deleting client certificate [{}] for application [{}]", certificateId, applicationId);
        var existing = resolveAndVerifyOwnership(certificateId, applicationId);
        var resolvedAppId = existing.applicationId();
        validateCertificateRemoval(resolvedAppId, certificateId);
        clientCertificateCrudService.delete(certificateId);
        mtlsSubscriptionSyncDomainService.updateActiveMTLSSubscriptions(resolvedAppId);
    }

    private ClientCertificate resolveAndVerifyOwnership(String certificateId, String applicationId) {
        var existing = clientCertificateCrudService.findById(certificateId);
        if (applicationId != null && !applicationId.equals(existing.applicationId())) {
            throw new ClientCertificateNotFoundException(certificateId);
        }
        return existing;
    }

    private void validateCertificateRemoval(String applicationId, String certificateId) {
        List<SubscriptionEntity> mtlsSubscriptions = subscriptionQueryService.findActiveByApplicationIdAndPlanSecurityTypes(
            applicationId,
            List.of(PlanSecurityType.MTLS.name())
        );

        if (mtlsSubscriptions.isEmpty()) {
            return;
        }

        var activeCertificates = clientCertificateCrudService.findByApplicationIdAndStatuses(
            applicationId,
            ClientCertificateStatus.ACTIVE,
            ClientCertificateStatus.ACTIVE_WITH_END
        );

        boolean noneRemaining = activeCertificates.stream().noneMatch(c -> !c.id().equals(certificateId));

        if (noneRemaining) {
            throw new ClientCertificateLastRemovalException(applicationId);
        }
    }
}
