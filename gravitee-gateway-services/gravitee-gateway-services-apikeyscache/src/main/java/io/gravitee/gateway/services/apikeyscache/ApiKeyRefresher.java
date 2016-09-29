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
package io.gravitee.gateway.services.apikeyscache;

import io.gravitee.definition.model.Api;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Plan;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyRefresher implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(ApiKeyRefresher.class);

    private ApiKeyRepository apiKeyRepository;

    private Cache cache;

    private final Api api;

    private Set<Plan> plans;

    public ApiKeyRefresher(final Api api) {
        this.api = api;
    }

    @Override
    public void run() {
        LOGGER.debug("Refresh API keys for API {}", api);

        plans.stream()
                .flatMap(new Function<Plan, Stream<ApiKey>>() {
                    @Override
                    public Stream<ApiKey> apply(Plan plan) {
                        try {
                            return apiKeyRepository.findByPlan(plan.getId()).stream();
                        } catch (TechnicalException e) {
                            LOGGER.warn("Not able to refresh API keys from repository: {}", e.getMessage());
                            return Stream.empty();
                        }
                    }
                })
                .forEach(apiKey -> cache.put(new Element(apiKey.getKey(), apiKey)));
    }

    public void setApiKeyRepository(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public void setPlans(Set<Plan> plans) {
        this.plans = plans;
    }
}
