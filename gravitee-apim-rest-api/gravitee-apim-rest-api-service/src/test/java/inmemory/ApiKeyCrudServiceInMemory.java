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

import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.common.utils.TimeProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class ApiKeyCrudServiceInMemory implements ApiKeyCrudService, InMemoryAlternative<ApiKeyEntity> {

    final ArrayList<ApiKeyEntity> storage = new ArrayList<>();

    @Override
    public ApiKeyEntity create(ApiKeyEntity apiKey) {
        storage.add(apiKey);
        return apiKey;
    }

    /** Like the repositories: every field but the subscriptions, which are kept as stored. */
    @Override
    public ApiKeyEntity update(ApiKeyEntity entity) {
        OptionalInt index = this.findIndex(this.storage, apiKey -> apiKey.getId().equals(entity.getId()));
        if (index.isPresent()) {
            var updated = entity.toBuilder().subscriptions(storage.get(index.getAsInt()).getSubscriptions()).build();
            storage.set(index.getAsInt(), updated);
            return updated;
        }

        throw new IllegalStateException("ApiKey not found");
    }

    @Override
    public Optional<ApiKeyEntity> addSubscription(String apiKeyId, String subscriptionId) {
        OptionalInt index = this.findIndex(this.storage, apiKey -> apiKey.getId().equals(apiKeyId));
        if (index.isEmpty()) {
            return Optional.empty();
        }
        var stored = storage.get(index.getAsInt());
        // Like the repositories, a key stored without a list of subscriptions has none
        var subscriptions = stored.getSubscriptions() == null ? new ArrayList<String>() : new ArrayList<>(stored.getSubscriptions());
        if (!subscriptions.contains(subscriptionId)) {
            subscriptions.add(subscriptionId);
        }
        var updated = stored.toBuilder().subscriptions(subscriptions).updatedAt(TimeProvider.now()).build();
        storage.set(index.getAsInt(), updated);
        return Optional.of(updated);
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
