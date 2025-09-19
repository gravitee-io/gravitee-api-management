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
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Date;
import java.util.List;
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
}
