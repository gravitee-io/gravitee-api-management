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
package io.gravitee.gateway.services.sync.cache.repository;

import io.gravitee.gateway.services.sync.cache.ApiKeysCache;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyRepositoryWrapper implements ApiKeyRepository {

    private final ApiKeyRepository wrapped;
    private final ApiKeysCache cache;

    public ApiKeyRepositoryWrapper(ApiKeyRepository wrapped, ApiKeysCache cache) {
        this.wrapped = wrapped;
        this.cache = cache;
    }

    @Override
    public Optional<ApiKey> findById(String id) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<ApiKey> findByKey(String apiKey) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<ApiKey> findByKeyAndApi(String apiKey, String api) throws TechnicalException {
        return Optional.ofNullable(cache.get(api, apiKey));
    }

    @Override
    public ApiKey create(ApiKey apiKey) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<ApiKey> findBySubscription(String subscription) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<ApiKey> findByCriteria(ApiKeyCriteria filter) throws TechnicalException {
        return wrapped.findByCriteria(filter);
    }

    @Override
    public ApiKey update(ApiKey apiKey) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<ApiKey> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<ApiKey> findByApplication(String applicationId) throws TechnicalException {
        throw new IllegalStateException();
    }
}
