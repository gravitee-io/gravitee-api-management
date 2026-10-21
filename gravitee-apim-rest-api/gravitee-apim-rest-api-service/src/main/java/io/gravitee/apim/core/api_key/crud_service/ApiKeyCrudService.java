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
package io.gravitee.apim.core.api_key.crud_service;

import io.gravitee.apim.core.api_key.exception.ApiKeyIdAlreadyExistsException;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import java.util.Optional;

public interface ApiKeyCrudService {
    /**
     * @throws ApiKeyIdAlreadyExistsException when a key with the same id already exists; nothing is created then
     */
    ApiKeyEntity create(ApiKeyEntity apiKey);

    /**
     * Updates every field of the key except its subscriptions, which only {@link #addSubscription(String, String)} changes.
     *
     * @return the key as stored after the update
     */
    ApiKeyEntity update(ApiKeyEntity apiKey);

    /**
     * Attaches a subscription to a key in one atomic operation, without rewriting the rest of the key, so that concurrent
     * writers (other Management API nodes, other requests) cannot lose each other's links. Attaching a subscription that is
     * already attached is a no-op for the links. The key's {@code updatedAt} moves in both cases.
     *
     * @return the key as stored after the change, or empty when no key has this id
     */
    Optional<ApiKeyEntity> addSubscription(String apiKeyId, String subscriptionId);
}
