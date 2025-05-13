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
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ApiKeySubscriptionsUpgrader implements Upgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeySubscriptionsUpgrader.class);

    private final ApiKeyRepository apiKeyRepository;

    @Autowired
    public ApiKeySubscriptionsUpgrader(@Lazy ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.API_KEY_SUBSCRIPTIONS_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            apiKeyRepository.findAll().forEach(this::updateApiKeySubscriptions);
        } catch (Exception e) {
            log.error("Error applying upgrader", e);
            return false;
        }

        return true;
    }

    @SuppressWarnings("removal")
    private void updateApiKeySubscriptions(ApiKey apiKey) {
        try {
            LOGGER.debug("Updating subscriptions for API Key [{}]", apiKey);
            List<String> allSubscriptions = apiKey.getSubscriptions() != null ? apiKey.getSubscriptions() : new ArrayList<>();
            if (apiKey.getSubscription() != null && !allSubscriptions.contains(apiKey.getSubscription())) {
                allSubscriptions.add(apiKey.getSubscription());
            }
            apiKey.setSubscriptions(allSubscriptions);
            apiKeyRepository.update(apiKey);
        } catch (TechnicalException e) {
            LOGGER.error("Failed to update subscriptions for API Key [{}]", apiKey, e);
        }
    }
}
