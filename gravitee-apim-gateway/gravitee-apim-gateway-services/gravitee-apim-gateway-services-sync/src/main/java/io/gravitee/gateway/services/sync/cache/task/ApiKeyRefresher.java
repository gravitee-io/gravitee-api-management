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

import static java.util.stream.Collectors.*;

import io.gravitee.gateway.handlers.api.definition.ApiKey;
import io.gravitee.gateway.services.sync.cache.ApiKeysCache;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ApiKeyRefresher implements Callable<Result<Boolean>> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected ApiKeyRepository apiKeyRepository;

    protected SubscriptionRepository subscriptionRepository;

    private ApiKeysCache cache;

    protected Result<Boolean> doRefresh(ApiKeyCriteria criteria) {
        logger.debug("Refresh api-keys");

        try {
            findApiKeys(criteria).forEach(this::saveOrUpdate);
            return Result.success(true);
        } catch (Exception ex) {
            return Result.failure(ex);
        }
    }

    protected void saveOrUpdate(ApiKey apiKey) {
        if (apiKey.isRevoked() || apiKey.isPaused()) {
            cache.remove(apiKey);
        } else {
            cache.put(apiKey);
        }
    }

    public void setCache(ApiKeysCache cache) {
        this.cache = cache;
    }

    public void setApiKeyRepository(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public void setSubscriptionRepository(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    private List<ApiKey> findApiKeys(ApiKeyCriteria criteria) throws TechnicalException {
        List<io.gravitee.repository.management.model.ApiKey> apiKeys = apiKeyRepository.findByCriteria(criteria);
        Map<String, Subscription> subscriptionsById = findSubscriptions(apiKeys);

        return apiKeys
            .stream()
            .flatMap(
                apiKey ->
                    apiKey.getSubscriptions().stream().map(subscriptionId -> new ApiKey(apiKey, subscriptionsById.get(subscriptionId)))
            )
            .collect(Collectors.toList());
    }

    private Map<String, Subscription> findSubscriptions(List<io.gravitee.repository.management.model.ApiKey> apiKeys)
        throws TechnicalException {
        Set<String> subscriptionIds = apiKeys.stream().flatMap(key -> key.getSubscriptions().stream()).collect(toSet());
        return subscriptionRepository.findByIdIn(subscriptionIds).stream().collect(toMap(Subscription::getId, Function.identity()));
    }
}
