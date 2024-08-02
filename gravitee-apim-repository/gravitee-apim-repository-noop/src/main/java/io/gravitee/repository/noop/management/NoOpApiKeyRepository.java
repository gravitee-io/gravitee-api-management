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
package io.gravitee.repository.noop.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpApiKeyRepository implements ApiKeyRepository {

    @Override
    public Optional<ApiKey> findById(String id) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public List<ApiKey> findByKey(String key) throws TechnicalException {
        return List.of();
    }

    @Override
    public Optional<ApiKey> findByKeyAndApi(String key, String api) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public ApiKey create(ApiKey apiKey) throws TechnicalException {
        return null;
    }

    @Override
    public ApiKey update(ApiKey key) throws TechnicalException {
        return null;
    }

    @Override
    public Set<ApiKey> findBySubscription(String subscription) throws TechnicalException {
        return Set.of();
    }

    @Override
    public List<ApiKey> findByApplication(String applicationId) throws TechnicalException {
        return List.of();
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) throws TechnicalException {
        return Set.of();
    }

    @Override
    public List<ApiKey> findByCriteria(ApiKeyCriteria filter) throws TechnicalException {
        return List.of();
    }

    @Override
    public List<ApiKey> findByCriteria(ApiKeyCriteria filter, Sortable sortable) throws TechnicalException {
        return List.of();
    }

    @Override
    public Optional<ApiKey> addSubscription(String id, String subscriptionId) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        return List.of();
    }

    @Override
    public Set<ApiKey> findAll() throws TechnicalException {
        return Set.of();
    }
}
