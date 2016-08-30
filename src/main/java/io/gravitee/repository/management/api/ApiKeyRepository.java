/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.ApiKey;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiKeyRepository {

	/**
	 * Give the API Key detail from the given key
	 * 
	 * @param apiKey API key
	 * @return API Key Details
	 */
    Optional<ApiKey> findById(String apiKey) throws TechnicalException;

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
	 * @return Updated API key
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
	 * List of {@link ApiKey} for a given {@link io.gravitee.repository.management.model.Plan}
	 */
	Set<ApiKey> findByPlan(String plan) throws TechnicalException;
}
