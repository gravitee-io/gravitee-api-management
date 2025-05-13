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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Subscription;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ClientIdInApiKeySubscriptionsUpgrader implements Upgrader {

    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public ClientIdInApiKeySubscriptionsUpgrader(@Lazy SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.CLIENT_ID_IN_API_KEY_SUBSCRIPTIONS_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            SubscriptionCriteria.SubscriptionCriteriaBuilder criteriaBuilder = SubscriptionCriteria.builder();
            criteriaBuilder.planSecurityTypes(List.of(Plan.PlanSecurityType.API_KEY.name()));
            subscriptionRepository.search(criteriaBuilder.build()).forEach(this::updateApiKeySubscriptions);
        } catch (Exception e) {
            log.error("Error applying upgrader", e);
            return false;
        }

        return true;
    }

    @SuppressWarnings("removal")
    private void updateApiKeySubscriptions(Subscription subscription) {
        try {
            if (subscription.getClientId() != null) {
                log.debug("Removing clientId from API Key subscription [{}]", subscription);
                subscription.setClientId(null);
                subscriptionRepository.update(subscription);
            }
        } catch (TechnicalException e) {
            log.error("Failed to remove clientID from API Key subscriptions for API Key [{}]", subscription, e);
        }
    }
}
