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
package io.gravitee.gateway.services.sync.cache.task;

import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ApiKeyRefresher implements Callable<Result<Boolean>> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected ApiKeyRepository apiKeyRepository;

    private Map<String, ApiKey> cache;

    protected Result<Boolean> doRefresh(ApiKeyCriteria criteria) {
        logger.debug("Refresh api-keys");

        try {
            apiKeyRepository
                    .findByCriteria(criteria)
                    .forEach(this::saveOrUpdate);

            return Result.success(true);
        } catch (Exception ex) {
            return Result.failure(ex);
        }
    }

    protected void saveOrUpdate(ApiKey apiKey) {
        if (apiKey.isRevoked() || apiKey.isPaused()) {
            logger.debug("Remove a paused / revoked api-key from cache [key: {}] [plan: {}] [app: {}]", apiKey.getKey(), apiKey.getPlan(), apiKey.getApplication());
            cache.remove(apiKey.getKey());
        } else {
            logger.debug("Cache an api-key [key: {}] [plan: {}] [app: {}]", apiKey.getKey(), apiKey.getPlan(), apiKey.getApplication());
            cache.put(apiKey.getKey(), new ApiKey(apiKey));
        }
    }

    public void setCache(Map<String, ApiKey> cache) {
        this.cache = cache;
    }

    public void setApiKeyRepository(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }
}
