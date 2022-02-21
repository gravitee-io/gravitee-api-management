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
package io.gravitee.repository.mongodb.management;

import static java.util.stream.Collectors.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.mongodb.management.internal.key.ApiKeyMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoApiKeyRepository implements ApiKeyRepository {

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private ApiKeyMongoRepository internalApiKeyRepo;

    @Override
    public ApiKey create(ApiKey apiKey) throws TechnicalException {
        ApiKeyMongo apiKeyMongo = mapper.map(apiKey, ApiKeyMongo.class);
        apiKeyMongo = internalApiKeyRepo.insert(apiKeyMongo);
        return toApiKey(apiKeyMongo);
    }

    @Override
    public ApiKey update(ApiKey apiKey) throws TechnicalException {
        if (apiKey == null || apiKey.getId() == null) {
            throw new IllegalStateException("ApiKey to update must have an id");
        }

        ApiKeyMongo apiKeyMongo = internalApiKeyRepo.findById(apiKey.getId()).orElse(null);

        if (apiKeyMongo == null) {
            throw new IllegalStateException(String.format("No apiKey found with id [%s]", apiKey.getId()));
        }

        apiKeyMongo = internalApiKeyRepo.save(mapper.map(apiKey, ApiKeyMongo.class));
        return toApiKey(apiKeyMongo);
    }

    @Override
    public Set<ApiKey> findBySubscription(String subscription) {
        return internalApiKeyRepo.findBySubscription(subscription).stream().map(this::toApiKey).collect(toSet());
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) throws TechnicalException {
        return internalApiKeyRepo.findByPlan(plan).stream().map(this::toApiKey).collect(toSet());
    }

    @Override
    public List<ApiKey> findByCriteria(ApiKeyCriteria filter) {
        return mapper.collection2list(internalApiKeyRepo.search(filter), ApiKeyMongo.class, ApiKey.class);
    }

    @Override
    public Optional<ApiKey> findById(String id) throws TechnicalException {
        return internalApiKeyRepo.findById(id).map(this::toApiKey);
    }

    @Override
    public List<ApiKey> findByKey(String key) {
        return internalApiKeyRepo.findByKey(key).stream().map(this::toApiKey).collect(toList());
    }

    @Override
    public Optional<ApiKey> findByKeyAndApi(String key, String api) {
        return internalApiKeyRepo.findByKeyAndApi(key, api).stream().findFirst().map(this::toApiKey);
    }

    @Override
    public Set<ApiKey> findAll() throws TechnicalException {
        return internalApiKeyRepo.findAll().stream().map(this::toApiKey).collect(toSet());
    }

    @Override
    public List<ApiKey> findByApplication(String applicationId) throws TechnicalException {
        try {
            return internalApiKeyRepo.findByApplication(applicationId).stream().map(this::toApiKey).collect(toList());
        } catch (Exception e) {
            throw new TechnicalException("An error occurred trying to find api key by application", e);
        }
    }

    private ApiKey toApiKey(ApiKeyMongo apiKeyMongo) {
        return mapper.map(apiKeyMongo, ApiKey.class);
    }
}
