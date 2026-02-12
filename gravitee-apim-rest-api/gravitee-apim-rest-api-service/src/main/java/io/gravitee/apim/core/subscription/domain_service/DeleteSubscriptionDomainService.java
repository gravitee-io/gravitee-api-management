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
package io.gravitee.apim.core.subscription.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.common.utils.TimeProvider;
import java.util.Collections;

@DomainService
public class DeleteSubscriptionDomainService {

    SubscriptionCrudService subscriptionCrudService;
    AuditDomainService auditService;

    public DeleteSubscriptionDomainService(SubscriptionCrudService subscriptionCrudService, AuditDomainService auditService) {
        this.subscriptionCrudService = subscriptionCrudService;
        this.auditService = auditService;
    }

    public void delete(SubscriptionEntity subscriptionEntity, AuditInfo auditInfo) {
        subscriptionCrudService.delete(subscriptionEntity.getId());
        createAuditLog(subscriptionEntity, auditInfo);
    }

    private void createAuditLog(SubscriptionEntity subscriptionEntity, AuditInfo auditInfo) {
        String referenceId = subscriptionEntity.getReferenceId();
        boolean isApiProduct = SubscriptionReferenceType.API_PRODUCT == subscriptionEntity.getReferenceType();

        if (isApiProduct) {
            auditService.createApiProductAuditLog(
                ApiProductAuditLogEntity.builder()
                    .organizationId(auditInfo.organizationId())
                    .environmentId(auditInfo.environmentId())
                    .apiProductId(referenceId)
                    .event(SubscriptionAuditEvent.SUBSCRIPTION_DELETED)
                    .actor(auditInfo.actor())
                    .oldValue(subscriptionEntity)
                    .createdAt(TimeProvider.now())
                    .properties(Collections.emptyMap())
                    .build()
            );
        } else {
            auditService.createApiAuditLog(
                ApiAuditLogEntity.builder()
                    .organizationId(auditInfo.organizationId())
                    .environmentId(auditInfo.environmentId())
                    .apiId(referenceId)
                    .event(SubscriptionAuditEvent.SUBSCRIPTION_DELETED)
                    .actor(auditInfo.actor())
                    .oldValue(subscriptionEntity)
                    .createdAt(TimeProvider.now())
                    .properties(Collections.emptyMap())
                    .build()
            );
        }
    }
}
