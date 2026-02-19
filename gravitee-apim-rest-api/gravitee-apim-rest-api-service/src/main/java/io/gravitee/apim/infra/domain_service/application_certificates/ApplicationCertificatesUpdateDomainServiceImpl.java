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
import io.gravitee.apim.core.application_certificate.domain_service.ApplicationCertificatesUpdateDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.common.security.PKCS7Utils;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementation of ApplicationCertificatesUpdateDomainService.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
@Service
public class ApplicationCertificatesUpdateDomainServiceImpl implements ApplicationCertificatesUpdateDomainService {

    private final SubscriptionQueryService subscriptionQueryService;
    private final SubscriptionCrudService subscriptionCrudService;
    private final ClientCertificateCrudService clientCertificateCrudService;

    @Override
    public void updateActiveMTLSSubscriptions(String applicationId) {
        log.debug("Updating client certificates for application: {}", applicationId);

        List<SubscriptionEntity> mtlsSubscriptions = subscriptionQueryService.findActiveByApplicationIdAndPlanSecurityTypes(
            applicationId,
            List.of(PlanSecurityType.MTLS.getLabel())
        );

        if (mtlsSubscriptions.isEmpty()) {
            log.debug("No active mTLS subscriptions found for application: {}", applicationId);
            return;
        }

        List<ClientCertificate> activeCertificates = clientCertificateCrudService
            .findByApplicationIdAndStatuses(applicationId, ClientCertificateStatus.ACTIVE, ClientCertificateStatus.ACTIVE_WITH_END)
            .stream()
            .sorted(Comparator.comparing(ClientCertificate::createdAt))
            .toList();

        if (activeCertificates.isEmpty()) {
            log.debug("No active certificates found for application: {}", applicationId);
            updateSubscriptionsWithCertificate(mtlsSubscriptions, null);
            return;
        }

        String encodedCertificate = createEncodedCertificate(activeCertificates);
        updateSubscriptionsWithCertificate(mtlsSubscriptions, encodedCertificate);
    }

    private String createEncodedCertificate(List<ClientCertificate> certificates) {
        if (certificates.size() == 1) {
            String pem = certificates.getFirst().certificate();
            return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
        } else {
            byte[] pkcs7Bundle = PKCS7Utils.createBundle(certificates.stream().map(ClientCertificate::certificate).toList());
            return Base64.getEncoder().encodeToString(pkcs7Bundle);
        }
    }

    private void updateSubscriptionsWithCertificate(List<SubscriptionEntity> subscriptions, String encodedCertificate) {
        for (SubscriptionEntity subscription : subscriptions) {
            if (!Objects.equals(subscription.getClientCertificate(), encodedCertificate)) {
                log.debug("Updating client certificate for subscription: {}", subscription.getId());
                subscription.setClientCertificate(encodedCertificate);
                subscriptionCrudService.update(subscription);
            }
        }
    }
}
