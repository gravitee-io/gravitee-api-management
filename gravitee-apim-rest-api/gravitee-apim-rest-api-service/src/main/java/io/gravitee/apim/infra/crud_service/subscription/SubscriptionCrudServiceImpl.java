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
package io.gravitee.apim.infra.crud_service.subscription;

import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionCrudServiceImpl implements SubscriptionCrudService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAdapter subscriptionAdapter;

    public SubscriptionCrudServiceImpl(@Lazy SubscriptionRepository subscriptionRepository, SubscriptionAdapter subscriptionAdapter) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionAdapter = subscriptionAdapter;
    }

    @Override
    public SubscriptionEntity get(String subscriptionId) {
        try {
            return subscriptionRepository
                .findById(subscriptionId)
                .map(subscriptionAdapter::toEntity)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find a subscription by id: " + subscriptionId, e);
        }
    }

    @Override
    public SubscriptionEntity update(SubscriptionEntity subscriptionEntity) {
        try {
            var result = subscriptionRepository.update(subscriptionAdapter.fromEntity(subscriptionEntity));
            return subscriptionAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to update the subscription: " + subscriptionEntity.getId(),
                e
            );
        }
    }
}
