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
package inmemory;

import io.gravitee.apim.core.api_key.domain_service.ApiKeyAvailabilityHelper;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.model.ExpiringApiKey;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiKeyQueryServiceInMemory implements ApiKeyQueryService, InMemoryAlternative<ApiKeyEntity> {

    private final ArrayList<ApiKeyEntity> storage;
    private final SubscriptionCrudServiceInMemory subscriptionCrudService;

    public ApiKeyQueryServiceInMemory() {
        storage = new ArrayList<>();
        subscriptionCrudService = null;
    }

    public ApiKeyQueryServiceInMemory(ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory) {
        storage = apiKeyCrudServiceInMemory.storage;
        subscriptionCrudService = null;
    }

    public ApiKeyQueryServiceInMemory(
        ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory,
        SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory
    ) {
        storage = apiKeyCrudServiceInMemory.storage;
        subscriptionCrudService = subscriptionCrudServiceInMemory;
    }

    @Override
    public Optional<ApiKeyEntity> findById(String apiKeyId) {
        return storage
            .stream()
            .filter(apiKey -> apiKey.getId().equals(apiKeyId))
            .findFirst();
    }

    @Override
    public Stream<ApiKeyEntity> findByApplication(String applicationId) {
        return storage.stream().filter(apiKey -> apiKey.getApplicationId().equals(applicationId));
    }

    @Override
    public List<ApiKeyEntity> findByKeyAndEnvironmentId(String key, String environmentId) {
        return storage
            .stream()
            .filter(apiKey -> apiKey.getKey().equals(key))
            .filter(apiKey -> environmentId == null || environmentId.equals(apiKey.getEnvironmentId()))
            .toList();
    }

    @Override
    public Optional<ApiKeyEntity> findByKeyAndApiId(String key, String apiId) {
        return preferActiveKey(storage
            .stream()
            .filter(apiKey -> apiKey.getKey().equals(key))
            .toList(), subscription -> apiId.equals(subscription.getApiId()));
    }

    @Override
    public Optional<ApiKeyEntity> findByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType) {
        return preferActiveKey(storage
            .stream()
            .filter(apiKey -> apiKey.getKey().equals(key))
            .toList(), subscription -> ApiKeyAvailabilityHelper.matchesReference(subscription, referenceId, referenceType));
    }

    @Override
    public List<ApiKeyEntity> findAllByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType) {
        return storage
            .stream()
            .filter(apiKey -> apiKey.getKey().equals(key))
            .toList();
    }

    @Override
    public Stream<ApiKeyEntity> findBySubscription(String subscriptionId) {
        return storage.stream().filter(apiKey -> apiKey.getSubscriptions().contains(subscriptionId));
    }

    @Override
    public List<ExpiringApiKey> findExpiringApiKeys(Instant now, List<Integer> daysBuckets, long windowMs) {
        return List.of();
    }

    private Optional<ApiKeyEntity> preferActiveKey(List<ApiKeyEntity> candidates, Predicate<SubscriptionEntity> subscriptionFilter) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Map<String, SubscriptionEntity> subscriptionsById = loadSubscriptions(candidates);
        Comparator<ApiKeyEntity> byUpdatedAt = Comparator.comparing(
            ApiKeyEntity::getUpdatedAt,
            Comparator.nullsLast(Comparator.naturalOrder())
        );

        return candidates
            .stream()
            .filter(apiKey -> isActiveForAnyMatchingSubscription(apiKey, subscriptionsById, subscriptionFilter))
            .max(byUpdatedAt);
    }

    private Map<String, SubscriptionEntity> loadSubscriptions(List<ApiKeyEntity> candidates) {
        if (subscriptionCrudService == null) {
            return Map.of();
        }
        Set<String> subscriptionIds = candidates
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

    private boolean isActiveForAnyMatchingSubscription(
        ApiKeyEntity apiKey,
        Map<String, SubscriptionEntity> subscriptionsById,
        Predicate<SubscriptionEntity> subscriptionFilter
    ) {
        return apiKey
            .getSubscriptions()
            .stream()
            .map(subscriptionsById::get)
            .filter(Objects::nonNull)
            .filter(subscriptionFilter)
            .anyMatch(subscription -> ApiKeyAvailabilityHelper.isActiveForSubscription(apiKey, subscription));
    }

    @Override
    public void initWith(List<ApiKeyEntity> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ApiKeyEntity> storage() {
        return Collections.unmodifiableList(storage);
    }
}
