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
package io.gravitee.repository.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.ApiKey;
import io.gravitee.repository.model.Application;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApiKeyRepository {

	/**
	 * Give the API Key detail from the given key
	 * 
	 * @param apiKey API key
	 * @return API Key Details
	 */
    Optional<ApiKey> retrieve(String apiKey) throws TechnicalException;

	/**
	 * Create a new API Key for an {@link Application} and an {@link Api}
	 *
	 * @param applicationName Application name
	 * @param apiName Name of the Api to associate
	 * @return Newly created API Key
	 */
	ApiKey create(String applicationName, String apiName, ApiKey key) throws TechnicalException;

	/**
	 * Update an API Key
	 *
	 * @param key The API Key to update
	 * @return Updated API key
	 */
	ApiKey update(ApiKey key) throws TechnicalException;

	/**
	 * Provide an history of all API Keys generated for an {@link Application} and an {@link Api}
	 *
	 * @param applicationName Application name
	 * @param apiName Name of the Api
	 * @return List of generated keys for an {@link Application} and an {@link Api}
	 * @throws TechnicalException
	 */
	Set<ApiKey> findByApplicationAndApi(String applicationName, String apiName) throws TechnicalException;

	Set<ApiKey> findByApplication(String applicationName) throws TechnicalException;
}
