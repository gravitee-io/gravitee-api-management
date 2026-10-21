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
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class ApiKeySubscriptionsUpgrader implements Upgrader {

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
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
            for (ApiKey apiKey : apiKeyRepository.findAll()) {
                updateApiKeySubscriptions(apiKey);
            }
            return true;
        });
    }

    @SuppressWarnings("removal")
    private void updateApiKeySubscriptions(ApiKey apiKey) throws TechnicalException {
        try {
            log.debug("Updating subscriptions for API Key [{}]", apiKey);
            String legacySubscription = apiKey.getSubscription();
            List<String> subscriptions = apiKey.getSubscriptions() != null ? apiKey.getSubscriptions() : List.of();
            if (legacySubscription != null && !subscriptions.contains(legacySubscription)) {
                // ApiKeyRepository.update does not write the subscriptions: the legacy one is linked explicitly.
                apiKeyRepository.addSubscription(apiKey.getId(), legacySubscription);
            }
        } catch (TechnicalException e) {
            throw new TechnicalException("Failed to update subscriptions for API Key [" + apiKey + "]", e);
        }
    }
}
