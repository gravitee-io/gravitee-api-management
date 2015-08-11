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

import io.gravitee.repository.model.ApiKey;
import io.gravitee.repository.model.Application;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApiKeyRepository {

	/**
	 * Generate an application Key
	 * 
	 * @param application {@link Application} name 
	 * @return ApiKey generated
	 */
	ApiKey generateKey(String application);

	/**
	 * Give the application Key detail for a given API
	 * 
	 * @param apiKey Application key 
	 * @param apiName Api name
	 * @return Full ApiKey
	 */
    ApiKey getApiKey(String apiKey, String apiName);

    /**
     * Invalidate a key 
     * 
     * @param applicationName Key owner Application 
     * @return true success, false otherwise
     */
	boolean invalidateKey(String applicationName);


}
