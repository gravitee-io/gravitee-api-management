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

import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApplicationHookContext;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
public class RejectSubscriptionDomainServiceImpl implements RejectSubscriptionDomainService {

    private final AuditDomainService auditDomainService;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanCrudService planCrudService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final UserCrudService userCrudService;

    public RejectSubscriptionDomainServiceImpl(
        @Lazy SubscriptionRepository subscriptionRepository,
        PlanCrudService planCrudService,
        AuditDomainService auditDomainService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        UserCrudService userCrudService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planCrudService = planCrudService;
        this.triggerNotificationDomainService = triggerNotificationDomainService;
        this.auditDomainService = auditDomainService;
        this.userCrudService = userCrudService;
    }

    @Override
    public SubscriptionEntity rejectSubscription(ExecutionContext executionContext, String subscriptionId, AuditActor auditActor) {
        log.debug("Close subscription {}", subscriptionId);
        Optional<Subscription> optSubscription;
        try {
            optSubscription = subscriptionRepository.findById(subscriptionId);
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to get subscription [subscriptionId={}]", subscriptionId, e);
            throw new TechnicalManagementException(e);
        }

        final SubscriptionEntity subscriptionEntity = optSubscription
            .map(SubscriptionAdapter.INSTANCE::toEntity)
            .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        return rejectSubscription(executionContext, subscriptionEntity, auditActor);
    }

    @Override
    public SubscriptionEntity rejectSubscription(
        ExecutionContext executionContext,
        SubscriptionEntity subscriptionEntity,
        AuditActor auditActor
    ) {
        if (subscriptionEntity == null) {
            // TODO: find a better exception
            throw new IllegalArgumentException("Subscription should not be null");
        }
        checkSubscriptionStatus(subscriptionEntity);
        checkPlanStatus(subscriptionEntity);
        var rejectedSubscriptionEntity = subscriptionEntity.rejectBy(auditActor.userId());
        var subscription = SubscriptionAdapter.INSTANCE.fromEntity(rejectedSubscriptionEntity);

        try {
            subscriptionRepository.update(subscription);

            triggerNotifications(executionContext, rejectedSubscriptionEntity);
            createAudit(executionContext, subscriptionEntity, rejectedSubscriptionEntity, auditActor);
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to save subscription [subscriptionId={}]", subscription.getId(), e);
            throw new TechnicalManagementException(e);
        }

        return rejectedSubscriptionEntity;
    }

    private void triggerNotifications(ExecutionContext executionContext, SubscriptionEntity rejectedSubscriptionEntity) {
        var apiHook = new SubscriptionRejectedApiHookContext(
            rejectedSubscriptionEntity.getApiId(),
            rejectedSubscriptionEntity.getApplicationId(),
            rejectedSubscriptionEntity.getPlanId()
        );
        triggerNotificationDomainService.triggerApiNotification(executionContext, apiHook);
        var applicationHook = new SubscriptionRejectedApplicationHookContext(
            rejectedSubscriptionEntity.getApplicationId(),
            rejectedSubscriptionEntity.getApiId(),
            rejectedSubscriptionEntity.getPlanId()
        );
        final Map<String, Object> notificationParameters = triggerNotificationDomainService.prepareNotificationParameters(
            executionContext,
            applicationHook
        );
        triggerNotificationDomainService.triggerApplicationNotification(executionContext, applicationHook, notificationParameters);
        userCrudService
            .findBaseUserById(rejectedSubscriptionEntity.getSubscribedBy())
            .map(BaseUserEntity::getEmail)
            .filter(StringUtils::isNotEmpty)
            .ifPresentOrElse(
                recipient ->
                    triggerNotificationDomainService.triggerApplicationEmailNotification(
                        executionContext,
                        applicationHook,
                        notificationParameters,
                        recipient
                    ),
                () -> log.warn("Subscriber '{}' not found, unable to retrieve email", rejectedSubscriptionEntity.getSubscribedBy())
            );
    }

    private static void checkSubscriptionStatus(SubscriptionEntity subscriptionEntity) {
        if (!subscriptionEntity.isPending()) {
            throw new IllegalStateException("Cannot reject subscription");
        }
    }

    private void createAudit(
        ExecutionContext executionContext,
        SubscriptionEntity subscriptionEntity,
        SubscriptionEntity rejectedSubscriptionEntity,
        AuditActor auditActor
    ) {
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .actor(auditActor)
                .environmentId(executionContext.getEnvironmentId())
                .organizationId(executionContext.getOrganizationId())
                .apiId(subscriptionEntity.getApiId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .oldValue(subscriptionEntity)
                .newValue(rejectedSubscriptionEntity)
                .createdAt(rejectedSubscriptionEntity.getClosedAt())
                .properties(Collections.singletonMap(AuditProperties.APPLICATION, subscriptionEntity.getApplicationId()))
                .build()
        );
        auditDomainService.createApplicationAuditLog(
            ApplicationAuditLogEntity
                .builder()
                .actor(auditActor)
                .environmentId(executionContext.getEnvironmentId())
                .organizationId(executionContext.getOrganizationId())
                .applicationId(subscriptionEntity.getApplicationId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .oldValue(subscriptionEntity)
                .newValue(rejectedSubscriptionEntity)
                .createdAt(rejectedSubscriptionEntity.getClosedAt())
                .properties(Collections.singletonMap(AuditProperties.API, subscriptionEntity.getApiId()))
                .build()
        );
    }

    private void checkPlanStatus(SubscriptionEntity subscriptionEntity) {
        final GenericPlanEntity plan = planCrudService.findById(subscriptionEntity.getPlanId());
        if (plan.isClosed()) {
            throw new PlanAlreadyClosedException(plan.getId());
        }
    }
}
