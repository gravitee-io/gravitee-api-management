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
import io.gravitee.repository.management.api.search.ApiKeyCursor;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Collection;
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

    List<ApiKey> findByKeyAndEnvironmentId(String key, String environmentId) throws TechnicalException;

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
     * Update an API Key: every field except {@link ApiKey#getSubscriptions() subscriptions}, which is ignored. Links to
     * subscriptions are only changed by {@link #addSubscription(String, String)} and {@link #addSubscriptions(String, Collection)},
     * so that an update made from a copy of the key read earlier cannot drop links added in the meantime.
     *
     * @param key The API Key to update
     * @return Updated API Key, as stored (with its current subscriptions)
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
     * Same as {@link #findByCriteria(ApiKeyCriteria)} but without the implicit {@code updatedAt} sort.
     * Use when the caller doesn't need a stable order — allows the planner to pick an ESR-friendly index
     * (e.g. {@code {revoked:1, expireAt:1}}) instead of being constrained by an unwanted sort field.
     */
    List<ApiKey> findByCriteriaUnordered(ApiKeyCriteria filter) throws TechnicalException;

    /**
     * Keyset / seek pagination over api keys matching {@code criteria}. Returns up to
     * {@code pageSize} api keys positioned strictly after {@code after} (or starting from the
     * beginning when {@code after} is {@code null}). Exhaustion is signalled by either an empty
     * page OR a short page (size {@code < pageSize}); callers should stop iterating on either.
     *
     * <p>Sort and seek mode are controlled by {@code sortable.field()}:
     * <ul>
     *   <li>{@code "updatedAt"} (or {@code null}) — sort {@code (updatedAt ASC, id ASC)}, seek
     *       past {@code (after.updatedAt, after.id)}. Used by the gateway-sync delta loop.</li>
     *   <li>{@code "id"} — sort {@code id ASC}, seek past {@code after.id}. Used by the
     *       gateway-sync warmup load where a {@code subscriptions IN} filter dominates
     *       selectivity.</li>
     * </ul>
     * {@code sortable.order()} is <b>silently ignored</b> — ASC is enforced unconditionally;
     * passing DESC will <em>not</em> throw. Sort field other than the two documented values
     * <em>will</em> throw {@link TechnicalException}.
     *
     * @throws TechnicalException if {@code sortable.field()} is not one of the documented values.
     */
    List<ApiKey> searchAfter(ApiKeyCriteria criteria, Sortable sortable, ApiKeyCursor after, int pageSize) throws TechnicalException;

    /**
     * Atomically link a subscription to an API Key, without rewriting the rest of the key, and move its {@code updatedAt}
     * (gateways synchronise keys by {@code updatedAt}). Idempotent: linking a subscription that is already linked keeps a
     * single link and does not fail, nor does it abort a surrounding transaction.
     *
     * @param id apikey ID
     * @param subscriptionId subscription ID to add to this shared apikey
     * @return the updated apikey if found
     * @throws TechnicalException
     */
    Optional<ApiKey> addSubscription(String id, String subscriptionId) throws TechnicalException;

    /**
     * Same as {@link #addSubscription(String, String)} for several subscriptions.
     *
     * @param id apikey ID
     * @param subscriptionIds subscription IDs to add to this apikey
     * @return the updated apikey if found
     * @throws TechnicalException
     */
    default Optional<ApiKey> addSubscriptions(String id, Collection<String> subscriptionIds) throws TechnicalException {
        Optional<ApiKey> apiKey = findById(id);
        for (String subscriptionId : subscriptionIds) {
            if (apiKey.isEmpty()) {
                break;
            }
            apiKey = addSubscription(id, subscriptionId);
        }
        return apiKey;
    }

    /**
     * Delete api key by environment ID
     *
     * @param environmentId The environment ID
     * @return List of deleted IDs for api key
     * @throws TechnicalException
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;

    /**
     * Find an API Key by key, reference id, and reference type (API or API_PRODUCT).
     *
     * @param key API Key
     * @param referenceId API or API_PRODUCT id
     * @param referenceType "API" or "API_PRODUCT"
     * @return Optional API Key
     * @throws TechnicalException
     */
    Optional<ApiKey> findByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType)
        throws TechnicalException;
}
