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

import io.gravitee.apim.core.api_key.domain_service.ReconcileApiKeysDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.AcceptSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.SubscriptionCRDDomainService;
import io.gravitee.apim.core.subscription.exception.SubscriptionApplicationImmutableException;
import io.gravitee.apim.core.subscription.exception.SubscriptionPlanImmutableException;
import io.gravitee.apim.core.subscription.model.SubscriptionConfiguration;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.crd.ApiKeyCRDSpec;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Service
@RequiredArgsConstructor
public class SubscriptionCRDDomainServiceImpl implements SubscriptionCRDDomainService {

    private static final String ACCEPT_REASON = "Kubernetes subscriptions are always auto accepted";

    private final SubscriptionService subscriptionService;

    private final SubscriptionAdapter adapter;

    private final AcceptSubscriptionDomainService acceptService;

    private final CloseSubscriptionDomainService closeSubscriptionDomainService;

    private final ReconcileApiKeysDomainService reconcileApiKeysDomainService;

    private final SubscriptionCrudService subscriptionCrudService;

    @Override
    public SubscriptionEntity createOrUpdate(AuditInfo auditInfo, SubscriptionCRDSpec spec) {
        return find(spec.getId())
            .map(existing -> update(auditInfo, existing, spec))
            .orElseGet(() -> create(auditInfo, spec));
    }

    @Override
    public void delete(AuditInfo auditInfo, String id) {
        closeSubscriptionDomainService.closeSubscription(id, auditInfo);
    }

    private SubscriptionEntity create(AuditInfo auditInfo, SubscriptionCRDSpec spec) {
        var entity = adapter.fromSpec(spec);

        var now = ZonedDateTime.now();

        entity.setEnvironmentId(auditInfo.environmentId());
        entity.setSubscribedBy(auditInfo.actor().userId());
        entity.setProcessedBy(auditInfo.actor().userId());
        entity.setCreatedAt(now);
        entity.setProcessedAt(now);
        entity.setStartingAt(now);
        entity.setReasonMessage(ACCEPT_REASON);
        entity.setEndingAt(spec.getEndingAt());

        String initialCustomKey = firstKeyOrNull(spec.getApiKeys());

        var subscription = subscriptionService.create(
            toExecutionContext(auditInfo),
            adapter.fromCoreForCreate(entity),
            initialCustomKey,
            spec.getId()
        );

        if (entity.getEndingAt() != null) {
            subscriptionService.update(toExecutionContext(auditInfo), adapter.fromCoreForUpdate(entity));
        }

        SubscriptionEntity accepted;
        if (subscription.getStatus() == SubscriptionStatus.ACCEPTED) {
            log.debug("Subscription [{}] already accepted, skipping auto accept", spec.getId());
            accepted = adapter.toCore(subscription);
        } else {
            log.debug("Auto accepting subscription [{}]", spec.getId());
            accepted = getAutoAccept(auditInfo, spec, subscription.getId());
        }

        reconcileApiKeys(accepted, auditInfo, spec);

        return accepted;
    }

    private SubscriptionEntity update(AuditInfo auditInfo, SubscriptionEntity existing, SubscriptionCRDSpec spec) {
        rejectImmutableFieldChanges(existing, spec);

        if (existing.getStatus() == SubscriptionEntity.Status.CLOSED) {
            var restored = restoreAsAccepted(auditInfo, existing, spec);
            return update(auditInfo, restored, spec);
        }

        var update = existing.toBuilder().build();

        if (!Objects.equals(spec.getEndingAt(), existing.getEndingAt())) {
            log.debug("Updating expiry date to [{}] for subscription [{}]", spec.getEndingAt(), spec.getId());
            update.setEndingAt(spec.getEndingAt());
        }

        if (!Objects.equals(spec.getMetadata(), existing.getMetadata())) {
            log.debug("Updating metadata for subscription [{}]", spec.getId());
            update.setMetadata(spec.getMetadata());
        }

        SubscriptionConfiguration subscriptionConfiguration = adapter.map(spec.getConsumerConfiguration());
        if (!Objects.equals(subscriptionConfiguration, existing.getConfiguration())) {
            log.debug("Updating consumer configuration for subscription [{}]", spec.getId());
            update.setConfiguration(subscriptionConfiguration);
        }

        subscriptionService.update(toExecutionContext(auditInfo), adapter.fromCoreForUpdate(update));

        reconcileApiKeys(update, auditInfo, spec);

        return update;
    }

    private SubscriptionEntity restoreAsAccepted(AuditInfo auditInfo, SubscriptionEntity existing, SubscriptionCRDSpec spec) {
        log.debug("Restoring closed subscription [{}]", spec.getId());
        subscriptionService.restore(toExecutionContext(auditInfo), existing.getId());
        if (hasApiKeys(spec)) {
            var pending = subscriptionCrudService.get(existing.getId());
            var accepted = pending.acceptBy(auditInfo.actor().userId(), ZonedDateTime.now(), spec.getEndingAt(), ACCEPT_REASON);
            return subscriptionCrudService.update(accepted);
        }
        return getAutoAccept(auditInfo, spec, existing.getId());
    }

    private static boolean hasApiKeys(SubscriptionCRDSpec spec) {
        return spec.getApiKeys() != null && !spec.getApiKeys().isEmpty();
    }

    private SubscriptionEntity getAutoAccept(AuditInfo auditInfo, SubscriptionCRDSpec spec, String id) {
        return acceptService.autoAccept(
            id,
            ZonedDateTime.now(),
            spec.getEndingAt(),
            ACCEPT_REASON,
            firstKeyOrNull(spec.getApiKeys()),
            auditInfo
        );
    }

    private static void rejectImmutableFieldChanges(SubscriptionEntity existing, SubscriptionCRDSpec spec) {
        if (!Objects.equals(spec.getPlanId(), existing.getPlanId())) {
            throw new SubscriptionPlanImmutableException(spec.getId());
        }
        if (!Objects.equals(spec.getApplicationId(), existing.getApplicationId())) {
            throw new SubscriptionApplicationImmutableException(spec.getId());
        }
    }

    private Optional<SubscriptionEntity> find(String id) {
        try {
            return Optional.ofNullable(subscriptionService.findById(id)).map(adapter::toCore);
        } catch (SubscriptionNotFoundException e) {
            log.debug("Subscription [{}] not found", id);
            return Optional.empty();
        }
    }

    private void reconcileApiKeys(SubscriptionEntity subscription, AuditInfo auditInfo, SubscriptionCRDSpec spec) {
        if (spec.getApiKeys() != null && !spec.getApiKeys().isEmpty()) {
            reconcileApiKeysDomainService.reconcile(subscription, spec.getApiKeys(), auditInfo);
        }
    }

    private static String firstKeyOrNull(List<ApiKeyCRDSpec> apiKeys) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            return null;
        }
        return apiKeys.getFirst().getKey();
    }

    private static ExecutionContext toExecutionContext(AuditInfo auditInfo) {
        return new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
    }
}
