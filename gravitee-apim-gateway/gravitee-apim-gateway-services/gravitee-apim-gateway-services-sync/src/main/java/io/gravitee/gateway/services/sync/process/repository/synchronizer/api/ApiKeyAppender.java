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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import static java.util.stream.Collectors.groupingBy;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ApiKeyAppender {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;

    /**
     * Fetching API Keys for given deployables
     * @param deployables the deployables to update
     * @return the deployables updated with API Keys
     */
    public List<ApiReactorDeployable> appends(
        final boolean initialSync,
        final List<ApiReactorDeployable> deployables,
        final Set<String> environments
    ) {
        final Map<String, ApiReactorDeployable> deployableByApi = deployables
            .stream()
            .collect(Collectors.toMap(ApiReactorDeployable::apiId, d -> d));
        List<Subscription> allApiKeySubscriptions = deployableByApi
            .values()
            .stream()
            .filter(deployable -> deployable.subscriptions() != null && deployable.apiKeyPlans() != null)
            .flatMap(deployable ->
                deployable
                    .subscriptions()
                    .stream()
                    .filter(subscription -> subscription.getPlan() != null && deployable.apiKeyPlans().contains(subscription.getPlan()))
            )
            .collect(Collectors.toList());
        if (!allApiKeySubscriptions.isEmpty()) {
            Map<String, List<ApiKey>> apiKeyByApi = loadApiKey(initialSync, allApiKeySubscriptions, environments);
            apiKeyByApi.forEach((api, apiKeys) -> {
                ApiReactorDeployable deployable = deployableByApi.get(api);
                deployable.apiKeys(apiKeys);
            });
        }
        return deployables;
    }

    private Map<String, List<ApiKey>> loadApiKey(
        final boolean initialSync,
        final List<Subscription> subscriptions,
        final Set<String> environments
    ) {
        try {
            Map<String, Subscription> subscriptionsById = subscriptions.stream().collect(Collectors.toMap(Subscription::getId, s -> s));
            ApiKeyCriteria.ApiKeyCriteriaBuilder criteriaBuilder = ApiKeyCriteria.builder()
                .subscriptions(subscriptionsById.keySet())
                .environments(environments);
            if (initialSync) {
                criteriaBuilder.includeRevoked(false).expireAfter(Instant.now().toEpochMilli()).includeWithoutExpiration(true);
            } else {
                criteriaBuilder.includeRevoked(true);
            }
            List<io.gravitee.repository.management.model.ApiKey> bySubscriptions = apiKeyRepository.findByCriteria(
                criteriaBuilder.build(),
                new SortableBuilder().field("updatedAt").order(Order.ASC).build()
            );
            return bySubscriptions
                .stream()
                .flatMap(apiKey ->
                    apiKey
                        .getSubscriptions()
                        .stream()
                        .map(subscriptionsById::get)
                        .filter(Objects::nonNull)
                        .map(subscription -> apiKeyMapper.to(apiKey, subscription))
                )
                .collect(groupingBy(ApiKey::getApi));
        } catch (Exception ex) {
            throw new SyncException("Error occurred when retrieving API Keys", ex);
        }
    }
}
