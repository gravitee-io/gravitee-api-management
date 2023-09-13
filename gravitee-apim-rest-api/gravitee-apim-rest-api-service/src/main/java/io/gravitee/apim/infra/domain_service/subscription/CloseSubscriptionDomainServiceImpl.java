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
package io.gravitee.apim.infra.domain_service.subscription;

import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApplicationHookContext;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CloseSubscriptionDomainServiceImpl implements CloseSubscriptionDomainService {

    private final SubscriptionRepository subscriptionRepository;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final AuditDomainService auditDomainService;
    private final ApplicationCrudService applicationCrudService;
    private final RevokeApiKeyDomainService revokeApiKeyDomainService;

    public CloseSubscriptionDomainServiceImpl(
        @Lazy SubscriptionRepository subscriptionRepository,
        TriggerNotificationDomainService triggerNotificationDomainService,
        AuditDomainService auditDomainService,
        ApplicationCrudService applicationCrudService,
        RevokeApiKeyDomainService revokeApiKeyDomainService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.triggerNotificationDomainService = triggerNotificationDomainService;
        this.auditDomainService = auditDomainService;
        this.applicationCrudService = applicationCrudService;
        this.revokeApiKeyDomainService = revokeApiKeyDomainService;
    }

    @Override
    public SubscriptionEntity closeSubscription(ExecutionContext executionContext, String subscriptionId, AuditActor currentUser) {
        log.debug("Close subscription {}", subscriptionId);
        Optional<Subscription> optSubscription;
        try {
            optSubscription = subscriptionRepository.findById(subscriptionId);
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to get subscription [subscriptionId={}]", subscriptionId, e);
            throw new TechnicalManagementException(e);
        }
        return optSubscription
            .map(SubscriptionAdapter.INSTANCE::toEntity)
            .map(subscriptionEntity -> {
                if (subscriptionEntity.isAccepted() || subscriptionEntity.isPaused()) {
                    return closeAcceptedOrPausedSubscription(executionContext, subscriptionId, subscriptionEntity, currentUser);
                }
                // TODO: create exception type
                throw new IllegalStateException("Cannot close subscription with status " + subscriptionEntity.getStatus().name());
            })
            .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
    }

    private SubscriptionEntity closeAcceptedOrPausedSubscription(
        ExecutionContext executionContext,
        String subscriptionId,
        SubscriptionEntity subscriptionEntity,
        AuditActor currentUser
    ) {
        var closedSubscriptionEntity = subscriptionEntity.close();
        var convertedSubscription = SubscriptionAdapter.INSTANCE.fromEntity(closedSubscriptionEntity);
        try {
            subscriptionRepository.update(convertedSubscription);

            var apiHook = new SubscriptionClosedApiHookContext(
                closedSubscriptionEntity.getApiId(),
                closedSubscriptionEntity.getApplicationId(),
                closedSubscriptionEntity.getPlanId()
            );
            triggerNotificationDomainService.triggerApiNotification(executionContext, apiHook);
            var applicationHook = new SubscriptionClosedApplicationHookContext(
                closedSubscriptionEntity.getApplicationId(),
                closedSubscriptionEntity.getApiId(),
                closedSubscriptionEntity.getPlanId()
            );
            triggerNotificationDomainService.triggerApplicationNotification(executionContext, applicationHook);

            auditDomainService.createApiAuditLog(
                ApiAuditLogEntity
                    .builder()
                    .environmentId(executionContext.getEnvironmentId())
                    .organizationId(executionContext.getOrganizationId())
                    .apiId(subscriptionEntity.getApiId())
                    .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                    .oldValue(subscriptionEntity)
                    .newValue(closedSubscriptionEntity)
                    .actor(currentUser)
                    .createdAt(closedSubscriptionEntity.getClosedAt())
                    .properties(Collections.singletonMap(AuditProperties.APPLICATION, subscriptionEntity.getApplicationId()))
                    .build()
            );
            auditDomainService.createApplicationAuditLog(
                ApplicationAuditLogEntity
                    .builder()
                    .environmentId(executionContext.getEnvironmentId())
                    .organizationId(executionContext.getOrganizationId())
                    .applicationId(subscriptionEntity.getApplicationId())
                    .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                    .oldValue(subscriptionEntity)
                    .newValue(closedSubscriptionEntity)
                    .actor(currentUser)
                    .createdAt(closedSubscriptionEntity.getClosedAt())
                    .properties(Collections.singletonMap(AuditProperties.API, subscriptionEntity.getApiId()))
                    .build()
            );

            revokeApiKeys(executionContext, subscriptionEntity, currentUser);
            return closedSubscriptionEntity;
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to save subscription [subscriptionId={}]", subscriptionId, e);
            throw new TechnicalManagementException(e);
        }
    }

    private void revokeApiKeys(ExecutionContext executionContext, SubscriptionEntity subscriptionEntity, AuditActor currentUser) {
        var application = applicationCrudService.findById(executionContext, subscriptionEntity.getApplicationId());
        if (!application.hasApiKeySharedMode()) {
            revokeApiKeyDomainService.revokeAllSubscriptionsApiKeys(
                executionContext,
                subscriptionEntity.getApiId(),
                subscriptionEntity.getId(),
                currentUser
            );
        }
    }
}
