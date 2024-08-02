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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiKeyRepository extends FindAllRepository<ApiKey> {
    /**
     * Give the API Key detail from the given id
     *
     * @param id API Key's unique id
     * @return API Key Details
     */
    Optional<ApiKey> findById(String id) throws TechnicalException;

    /**
     * Give the API Keys from the given key
     *
     * @param key API Key
     * @return API Keys
     */
    List<ApiKey> findByKey(String key) throws TechnicalException;

    /**
     * Give the API Key from the given key and api
     *
     * @param key API Key
     * @param api Key api
     * @return API Key
     */
    Optional<ApiKey> findByKeyAndApi(String key, String api) throws TechnicalException;

    /**
     * Create a new API Key
     *
     * @param apiKey API Key
     * @return Newly created API Key
     */
    ApiKey create(ApiKey apiKey) throws TechnicalException;

    /**
     * Update an API Key
     *
     * @param key The API Key to update
     * @return Updated API Key
     */
    ApiKey update(ApiKey key) throws TechnicalException;

    /**
     * List of {@link ApiKey} for a given {@link io.gravitee.repository.management.model.Subscription}
     *
     * @param subscription
     * @return
     * @throws TechnicalException
     */
    Set<ApiKey> findBySubscription(String subscription) throws TechnicalException;

    /**
     * List of {@link ApiKey} for a given {@link io.gravitee.repository.management.model.Application}
     *
     * @param applicationId application ID
     * @return API Keys issued for the given application
     * @throws TechnicalException
     */
    List<ApiKey> findByApplication(String applicationId) throws TechnicalException;

    /**
     * List of {@link ApiKey} for a given {@link io.gravitee.repository.management.model.Plan}
     */
    Set<ApiKey> findByPlan(String plan) throws TechnicalException;

    List<ApiKey> findByCriteria(ApiKeyCriteria filter) throws TechnicalException;

    List<ApiKey> findByCriteria(ApiKeyCriteria filter, Sortable sortable) throws TechnicalException;

    /**
     *
     * @param id apikey ID
     * @param subscriptionId subscription ID to add to this shared apikey
     * @return the updated apikey if found
     * @throws TechnicalException
     */
    Optional<ApiKey> addSubscription(String id, String subscriptionId) throws TechnicalException;

    /**
     * Delete api key by environment ID
     *
     * @param environmentId The environment ID
     * @return List of deleted IDs for api key
     * @throws TechnicalException
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
