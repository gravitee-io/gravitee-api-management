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

import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    public Optional<ApiKeyEntity> findByKeyAndApiId(String key, String apiId) {
        if (subscriptionCrudService != null) {
            return findByKeyAndReferenceIdAndReferenceType(key, apiId, SubscriptionReferenceType.API.name());
        }
        return storage
            .stream()
            .filter(apiKey -> apiKey.getKey().equals(key))
            .findFirst();
    }

    @Override
    public Optional<ApiKeyEntity> findByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType) {
        if (subscriptionCrudService == null) {
            return storage
                .stream()
                .filter(apiKey -> apiKey.getKey().equals(key))
                .findFirst();
        }
        return storage
            .stream()
            .filter(apiKey -> apiKey.getKey().equals(key))
            .filter(apiKey ->
                apiKey
                    .getSubscriptions()
                    .stream()
                    .map(subId -> {
                        try {
                            return subscriptionCrudService.get(subId);
                        } catch (SubscriptionNotFoundException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .anyMatch(sub -> referenceId.equals(sub.getReferenceId()) && referenceType.equals(sub.getReferenceType().name()))
            )
            .findFirst();
    }

    @Override
    public Stream<ApiKeyEntity> findBySubscription(String subscriptionId) {
        return storage.stream().filter(apiKey -> apiKey.getSubscriptions().contains(subscriptionId));
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
