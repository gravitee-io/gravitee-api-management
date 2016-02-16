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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.redis.management.internal.ApiKeyRedisRepository;
import io.gravitee.repository.redis.management.internal.ApiRedisRepository;
import io.gravitee.repository.redis.management.internal.ApplicationRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisApiKeyRepository implements ApiKeyRepository {

    @Autowired
    private ApiKeyRedisRepository apiKeyRedisRepository;

    @Autowired
    private ApplicationRedisRepository applicationRedisRepository;

    @Autowired
    private ApiRedisRepository apiRedisRepository;

    @Override
    public Optional<ApiKey> retrieve(String apiKey) throws TechnicalException {
        return Optional.ofNullable(convert(apiKeyRedisRepository.find(apiKey)));
    }

    @Override
    public ApiKey create(String applicationId, String apiId, ApiKey key) throws TechnicalException {
        RedisApiKey redisApiKey = apiKeyRedisRepository.saveOrUpdate(convert(key));
        return convert(redisApiKey);
    }

    @Override
    public ApiKey update(ApiKey key) throws TechnicalException {
        RedisApiKey redisApiKey = apiKeyRedisRepository.saveOrUpdate(convert(key));
        return convert(redisApiKey);
    }

    @Override
    public Set<ApiKey> findByApplicationAndApi(String applicationId, String apiId) throws TechnicalException {
        Set<ApiKey> apiKeysByApp = findByApplication(applicationId);
        Set<ApiKey> apiKeysByApi = findByApi(apiId);

        apiKeysByApp.retainAll(apiKeysByApi);

        return apiKeysByApp;
    }

    @Override
    public Set<ApiKey> findByApplication(String applicationId) throws TechnicalException {
        return apiKeyRedisRepository.findByApplication(applicationId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<ApiKey> findByApi(String apiId) throws TechnicalException {
        return apiKeyRedisRepository.findByApi(apiId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(String apiKey) throws TechnicalException {
        apiKeyRedisRepository.delete(apiKey);
    }

    private ApiKey convert(RedisApiKey redisApiKey) {
        if (redisApiKey == null) {
            return null;
        }

        ApiKey apiKey = new ApiKey();

        apiKey.setKey(redisApiKey.getKey());
        apiKey.setApi(redisApiKey.getApi());
        apiKey.setApplication(redisApiKey.getApplication());
        apiKey.setCreatedAt(new Date(redisApiKey.getCreatedAt()));
        if (redisApiKey.getExpiration() != 0) {
            apiKey.setExpiration(new Date(redisApiKey.getExpiration()));
        }
        if (redisApiKey.getRevokeAt() != 0) {
            apiKey.setRevokeAt(new Date(redisApiKey.getRevokeAt()));
        }
        apiKey.setRevoked(redisApiKey.isRevoked());

        return apiKey;
    }

    private RedisApiKey convert(ApiKey apiKey) {
        RedisApiKey redisApiKey = new RedisApiKey();

        redisApiKey.setKey(apiKey.getKey());
        redisApiKey.setApi(apiKey.getApi());
        redisApiKey.setApplication(apiKey.getApplication());
        redisApiKey.setCreatedAt(apiKey.getCreatedAt().getTime());
        if (apiKey.getExpiration() != null) {
            redisApiKey.setExpiration(apiKey.getExpiration().getTime());
        }
        if (apiKey.getRevokeAt() != null) {
            redisApiKey.setRevokeAt(apiKey.getRevokeAt().getTime());
        }
        redisApiKey.setRevoked(apiKey.isRevoked());

        return redisApiKey;
    }
}
