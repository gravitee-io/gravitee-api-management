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
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.service.InstallationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class ApiKeySubscriptionsUpgrader extends OneShotUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeySubscriptionsUpgrader.class);

    private final ApiKeyRepository apiKeyRepository;

    @Autowired
    public ApiKeySubscriptionsUpgrader(ApiKeyRepository apiKeyRepository) {
        super(InstallationService.API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS);
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public int getOrder() {
        return 501;
    }

    @Override
    protected void processOneShotUpgrade() throws TechnicalException {
        apiKeyRepository.findAll().forEach(this::updateApiKeySubscriptions);
    }

    @SuppressWarnings("removal")
    private void updateApiKeySubscriptions(ApiKey apiKey) {
        try {
            LOGGER.debug("Updating subscriptions for API key [{}]", apiKey);
            apiKey.setSubscriptions(List.of(apiKey.getSubscription()));
            apiKeyRepository.update(apiKey);
        } catch (TechnicalException e) {
            LOGGER.error("Failed to update subscriptions for API key [{}]", apiKey, e);
        }
    }
}
