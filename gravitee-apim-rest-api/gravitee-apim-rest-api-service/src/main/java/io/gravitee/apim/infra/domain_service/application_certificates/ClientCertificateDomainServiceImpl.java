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

    @Override
    public ClientCertificate create(String applicationId, ClientCertificate certificate) {
        log.debug("Creating client certificate for application [{}]", applicationId);
        var created = clientCertificateCrudService.create(applicationId, certificate);
        mtlsSubscriptionSyncDomainService.updateActiveMTLSSubscriptions(applicationId);
        return created;
    }

    @Override
    public ClientCertificate update(String certificateId, ClientCertificate certificate) {
        log.debug("Updating client certificate [{}]", certificateId);
        var updated = clientCertificateCrudService.update(certificateId, certificate);
        mtlsSubscriptionSyncDomainService.updateActiveMTLSSubscriptions(updated.applicationId());
        return updated;
    }

    @Override
    public void delete(String applicationId, String certificateId) {
        log.debug("Deleting client certificate [{}] for application [{}]", certificateId, applicationId);
        mtlsSubscriptionSyncDomainService.validateCertificateRemoval(applicationId, certificateId);
        clientCertificateCrudService.delete(certificateId);
        mtlsSubscriptionSyncDomainService.updateActiveMTLSSubscriptions(applicationId);
    }
}
