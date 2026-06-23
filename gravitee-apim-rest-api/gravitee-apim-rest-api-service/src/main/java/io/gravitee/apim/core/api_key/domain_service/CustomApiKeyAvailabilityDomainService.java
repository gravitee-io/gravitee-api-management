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
package io.gravitee.apim.core.api_key.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@DomainService
@CustomLog
@RequiredArgsConstructor
public class CustomApiKeyAvailabilityDomainService {

    private final ApiKeyQueryService apiKeyQueryService;
    private final SubscriptionCrudService subscriptionCrudService;

    public boolean canUseCustomKey(String keyValue, String referenceId, String referenceType, String applicationId, String environmentId) {
        log.debug(
            "Check if custom API Key can be used for reference {} (type {}) and application {}",
            referenceId,
            referenceType,
            applicationId
        );

        var existingKeys = apiKeyQueryService.findByKeyAndEnvironmentId(keyValue, environmentId);
        if (existingKeys.isEmpty()) {
            return true;
        }

        var subscriptionsById = loadSubscriptionsById(existingKeys);

        for (ApiKeyEntity existingKey : existingKeys) {
            if (!applicationId.equals(existingKey.getApplicationId())) {
                return false;
            }
            if (isActiveForReference(existingKey, referenceId, referenceType, subscriptionsById)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, SubscriptionEntity> loadSubscriptionsById(Collection<ApiKeyEntity> apiKeys) {
        Set<String> subscriptionIds = apiKeys
            .stream()
            .flatMap(apiKey -> apiKey.getSubscriptions().stream())
            .collect(Collectors.toSet());
        if (subscriptionIds.isEmpty()) {
            return Map.of();
        }
        return subscriptionCrudService
            .findByIdIn(subscriptionIds)
            .stream()
            .collect(Collectors.toMap(SubscriptionEntity::getId, Function.identity()));
    }

    private boolean isActiveForReference(
        ApiKeyEntity apiKey,
        String referenceId,
        String referenceType,
        Map<String, SubscriptionEntity> subscriptionsById
    ) {
        return apiKey
            .getSubscriptions()
            .stream()
            .map(subscriptionsById::get)
            .filter(Objects::nonNull)
            .filter(subscription -> ApiKeyAvailabilityHelper.matchesReference(subscription, referenceId, referenceType))
            .anyMatch(subscription -> ApiKeyAvailabilityHelper.isActiveForSubscription(apiKey, subscription));
    }
}
