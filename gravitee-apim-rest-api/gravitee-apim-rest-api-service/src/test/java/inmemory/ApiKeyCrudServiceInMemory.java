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
import io.gravitee.apim.core.api_key.exception.ApiKeyIdAlreadyExistsException;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.common.utils.TimeProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class ApiKeyCrudServiceInMemory implements ApiKeyCrudService, InMemoryAlternative<ApiKeyEntity> {

    final ArrayList<ApiKeyEntity> storage = new ArrayList<>();

    private final boolean rejectsExistingIds;

    public ApiKeyCrudServiceInMemory() {
        this(false);
    }

    /**
     * @param rejectsExistingIds whether {@link #create(ApiKeyEntity)} rejects an existing id, like the repositories. Off by
     * default: some tests create several keys with the same generated id.
     */
    public ApiKeyCrudServiceInMemory(boolean rejectsExistingIds) {
        this.rejectsExistingIds = rejectsExistingIds;
    }

    @Override
    public ApiKeyEntity create(ApiKeyEntity apiKey) {
        synchronized (storage) {
            if (rejectsExistingIds && this.findIndex(this.storage, stored -> stored.getId().equals(apiKey.getId())).isPresent()) {
                throw new ApiKeyIdAlreadyExistsException(apiKey.getId(), null);
            }
            storage.add(apiKey);
            return apiKey;
        }
    }

    @Override
    public ApiKeyEntity update(ApiKeyEntity entity) {
        synchronized (storage) {
            OptionalInt index = this.findIndex(this.storage, apiKey -> apiKey.getId().equals(entity.getId()));
            if (index.isPresent()) {
                storage.set(index.getAsInt(), entity);
                return entity;
            }

            throw new IllegalStateException("ApiKey not found");
        }
    }

    @Override
    public Optional<ApiKeyEntity> addSubscription(String apiKeyId, String subscriptionId) {
        synchronized (storage) {
            OptionalInt index = this.findIndex(this.storage, apiKey -> apiKey.getId().equals(apiKeyId));
            if (index.isEmpty()) {
                return Optional.empty();
            }
            ApiKeyEntity current = storage.get(index.getAsInt());
            ApiKeyEntity updated = current.hasSubscription(subscriptionId)
                ? current.toBuilder().updatedAt(TimeProvider.now()).build()
                : current.addSubscription(subscriptionId);
            storage.set(index.getAsInt(), updated);
            return Optional.of(updated);
        }
    }

    @Override
    public void initWith(List<ApiKeyEntity> items) {
        synchronized (storage) {
            storage.clear();
            storage.addAll(items);
        }
    }

    @Override
    public void reset() {
        synchronized (storage) {
            storage.clear();
        }
    }

    @Override
    public List<ApiKeyEntity> storage() {
        synchronized (storage) {
            return List.copyOf(storage);
        }
    }
}
