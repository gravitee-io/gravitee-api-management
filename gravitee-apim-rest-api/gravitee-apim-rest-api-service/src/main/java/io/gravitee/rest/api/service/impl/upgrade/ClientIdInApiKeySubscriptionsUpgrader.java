/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.service.InstallationService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class ClientIdInApiKeySubscriptionsUpgrader extends OneShotUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientIdInApiKeySubscriptionsUpgrader.class);

    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public ClientIdInApiKeySubscriptionsUpgrader(@Lazy SubscriptionRepository subscriptionRepository) {
        super(InstallationService.CLIENT_ID_IN_API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS);
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public int getOrder() {
        return 502;
    }

    @Override
    protected void processOneShotUpgrade() throws TechnicalException {
        SubscriptionCriteria.Builder criteriaBuilder = new SubscriptionCriteria.Builder();
        criteriaBuilder.planSecurityTypes(List.of(Plan.PlanSecurityType.API_KEY));
        subscriptionRepository.search(criteriaBuilder.build()).forEach(this::updateApiKeySubscriptions);
    }

    @SuppressWarnings("removal")
    private void updateApiKeySubscriptions(Subscription subscription) {
        try {
            if (subscription.getClientId() != null) {
                LOGGER.debug("Removing clientId from API key subscription [{}]", subscription);
                subscription.setClientId(null);
                subscriptionRepository.update(subscription);
            }
        } catch (TechnicalException e) {
            LOGGER.error("Failed to remove clientID from API key subscriptions for API key [{}]", subscription, e);
        }
    }
}
