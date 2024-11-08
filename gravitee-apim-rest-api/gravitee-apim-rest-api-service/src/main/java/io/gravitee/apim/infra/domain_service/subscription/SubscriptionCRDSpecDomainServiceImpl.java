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

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.domain_service.AcceptSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.SubscriptionCRDSpecDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionCRDSpecDomainServiceImpl implements SubscriptionCRDSpecDomainService {

    private static final String ACCEPT_REASON = "Kubernetes subscriptions are always auto accepted";

    private final SubscriptionService subscriptionService;

    private final SubscriptionAdapter adapter;

    private final AcceptSubscriptionDomainService acceptService;

    private final CloseSubscriptionDomainService closeSubscriptionDomainService;

    @Override
    public SubscriptionEntity createOrUpdate(AuditInfo auditInfo, SubscriptionCRDSpec spec) {
        return find(spec.getId()).map((existing -> update(auditInfo, existing, spec))).orElseGet(() -> create(auditInfo, spec));
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

        var subscription = subscriptionService.create(toExecutionContext(auditInfo), adapter.fromCoreForCreate(entity), null, spec.getId());

        if (entity.getEndingAt() != null) {
            subscriptionService.update(toExecutionContext(auditInfo), adapter.fromCoreForUpdate(entity));
        }

        log.debug("Auto accepting subscription [{}]", spec.getId());
        return acceptService.autoAccept(subscription.getId(), ZonedDateTime.now(), spec.getEndingAt(), ACCEPT_REASON, "", auditInfo);
    }

    private SubscriptionEntity update(AuditInfo auditInfo, SubscriptionEntity existing, SubscriptionCRDSpec spec) {
        var update = existing.toBuilder().build();

        if (!Objects.equals(spec.getEndingAt(), existing.getEndingAt())) {
            log.debug("Updating expiry date to [{}] for subscription [{}]", spec.getEndingAt(), spec.getId());
            update.setEndingAt(spec.getEndingAt());
        }

        subscriptionService.update(toExecutionContext(auditInfo), adapter.fromCoreForUpdate(update));

        return update;
    }

    private Optional<SubscriptionEntity> find(String id) {
        try {
            return Optional.ofNullable(subscriptionService.findById(id)).map(adapter::toCore);
        } catch (SubscriptionNotFoundException e) {
            log.debug("Subscription [{}] not found", id);
            return Optional.empty();
        }
    }

    private static ExecutionContext toExecutionContext(AuditInfo auditInfo) {
        return new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
    }
}
