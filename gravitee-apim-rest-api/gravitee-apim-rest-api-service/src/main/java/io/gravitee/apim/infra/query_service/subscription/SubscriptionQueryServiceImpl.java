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
package io.gravitee.apim.infra.query_service.subscription;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAdapter subscriptionAdapter;

    public SubscriptionQueryServiceImpl(@Lazy SubscriptionRepository subscriptionRepository, SubscriptionAdapter subscriptionAdapter) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionAdapter = subscriptionAdapter;
    }

    @Override
    public List<SubscriptionEntity> findExpiredSubscriptions() {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder()
            .statuses(List.of(SubscriptionStatus.ACCEPTED.name()))
            .endingAtBefore(new Date().getTime())
            .build();

        try {
            return subscriptionRepository.search(criteria).stream().map(subscriptionAdapter::toEntity).toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find expired subscription", e);
        }
    }

    @Override
    public List<SubscriptionEntity> findSubscriptionsByPlan(String planId) {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder().plans(List.of(planId)).build();

        try {
            return subscriptionRepository.search(criteria).stream().map(subscriptionAdapter::toEntity).toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find plan's subscription", e);
        }
    }

    @Override
    public List<SubscriptionEntity> findActiveSubscriptionsByPlan(String planId) {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder()
            .plans(List.of(planId))
            .statuses(List.of(SubscriptionStatus.ACCEPTED.name(), SubscriptionStatus.PENDING.name(), SubscriptionStatus.PAUSED.name()))
            .build();

        try {
            return subscriptionRepository.search(criteria).stream().map(subscriptionAdapter::toEntity).toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find active plan's subscription", e);
        }
    }

    @Override
    public List<SubscriptionEntity> findActiveByApplicationIdAndApiId(String applicationId, String apiId) {
        var criteria = SubscriptionCriteria.builder()
            .statuses(List.of(SubscriptionStatus.ACCEPTED.name(), SubscriptionStatus.PENDING.name(), SubscriptionStatus.PAUSED.name()))
            .apis(List.of(apiId))
            .applications(List.of(applicationId))
            .build();

        try {
            return subscriptionRepository.search(criteria).stream().map(subscriptionAdapter::toEntity).toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find subscriptions", e);
        }
    }

    @Override
    public List<SubscriptionEntity> findActiveByApplicationIdAndPlanSecurityTypes(
        String applicationId,
        Collection<String> planSecurityTypes
    ) {
        var criteria = SubscriptionCriteria.builder()
            .statuses(List.of(SubscriptionStatus.ACCEPTED.name(), SubscriptionStatus.PENDING.name(), SubscriptionStatus.PAUSED.name()))
            .applications(List.of(applicationId))
            .planSecurityTypes(planSecurityTypes)
            .build();

        try {
            return subscriptionRepository.search(criteria).stream().map(subscriptionAdapter::toEntity).toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find subscriptions by plan security types", e);
        }
    }

    @Override
    public List<SubscriptionEntity> findActiveByApplicationIdAndReferenceIdAndReferenceType(
        String applicationId,
        String referenceId,
        SubscriptionReferenceType referenceType
    ) {
        try {
            io.gravitee.repository.management.model.SubscriptionReferenceType repoReferenceType =
                io.gravitee.repository.management.model.SubscriptionReferenceType.valueOf(referenceType.name());
            var criteria = SubscriptionCriteria.builder()
                .statuses(List.of(SubscriptionStatus.ACCEPTED.name(), SubscriptionStatus.PENDING.name(), SubscriptionStatus.PAUSED.name()))
                .referenceIds(List.of(referenceId))
                .referenceType(repoReferenceType)
                .applications(List.of(applicationId))
                .build();

            return subscriptionRepository.search(criteria).stream().map(subscriptionAdapter::toEntity).toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find subscriptions", e);
        }
    }

    @Override
    public List<SubscriptionEntity> findAllByReferenceIdAndReferenceType(String referenceId, SubscriptionReferenceType referenceType) {
        try {
            // Convert core enum to repository enum
            io.gravitee.repository.management.model.SubscriptionReferenceType repoReferenceType =
                io.gravitee.repository.management.model.SubscriptionReferenceType.valueOf(referenceType.name());
            return subscriptionRepository
                .findByReferenceIdAndReferenceType(referenceId, repoReferenceType)
                .stream()
                .map(subscriptionAdapter::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find subscriptions by reference", e);
        }
    }

    @Override
    public Optional<SubscriptionEntity> findByIdAndReferenceIdAndReferenceType(
        String subscriptionId,
        String referenceId,
        SubscriptionReferenceType referenceType
    ) {
        try {
            io.gravitee.repository.management.model.SubscriptionReferenceType repoReferenceType =
                io.gravitee.repository.management.model.SubscriptionReferenceType.valueOf(referenceType.name());
            return subscriptionRepository
                .findByIdAndReferenceIdAndReferenceType(subscriptionId, referenceId, repoReferenceType)
                .map(subscriptionAdapter::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find subscription by id and reference", e);
        }
    }
}
