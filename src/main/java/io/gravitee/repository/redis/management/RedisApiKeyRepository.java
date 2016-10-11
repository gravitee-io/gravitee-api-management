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
import io.gravitee.repository.redis.management.model.RedisApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisApiKeyRepository implements ApiKeyRepository {

    @Autowired
    private ApiKeyRedisRepository apiKeyRedisRepository;

    @Override
    public Optional<ApiKey> findById(String apiKey) throws TechnicalException {
        return Optional.ofNullable(convert(apiKeyRedisRepository.find(apiKey)));
    }

    @Override
    public ApiKey create(ApiKey apiKey) throws TechnicalException {
        RedisApiKey redisApiKey = apiKeyRedisRepository.saveOrUpdate(convert(apiKey));
        return convert(redisApiKey);
    }

    @Override
    public ApiKey update(ApiKey key) throws TechnicalException {
        RedisApiKey redisApiKey = apiKeyRedisRepository.saveOrUpdate(convert(key));
        return convert(redisApiKey);
    }

    @Override
    public Set<ApiKey> findBySubscription(String subscription) throws TechnicalException {
        return apiKeyRedisRepository.findBySubscription(subscription)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) throws TechnicalException {
        return apiKeyRedisRepository.findByPlan(plan)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    private ApiKey convert(RedisApiKey redisApiKey) {
        if (redisApiKey == null) {
            return null;
        }

        ApiKey apiKey = new ApiKey();

        apiKey.setKey(redisApiKey.getKey());
        apiKey.setApplication(redisApiKey.getApplication());
        apiKey.setSubscription(redisApiKey.getSubscription());
        apiKey.setPlan(redisApiKey.getPlan());
        if (redisApiKey.getCreatedAt() != 0) {
            apiKey.setCreatedAt(new Date(redisApiKey.getCreatedAt()));
        }
        if (redisApiKey.getUpdatedAt() != 0) {
            apiKey.setUpdatedAt(new Date(redisApiKey.getUpdatedAt()));
        }
        if (redisApiKey.getExpireAt() != 0) {
            apiKey.setExpireAt(new Date(redisApiKey.getExpireAt()));
        }
        if (redisApiKey.getRevokeAt() != 0) {
            apiKey.setRevokedAt(new Date(redisApiKey.getRevokeAt()));
        }
        apiKey.setRevoked(redisApiKey.isRevoked());

        return apiKey;
    }

    private RedisApiKey convert(ApiKey apiKey) {
        RedisApiKey redisApiKey = new RedisApiKey();

        redisApiKey.setKey(apiKey.getKey());
        redisApiKey.setApplication(apiKey.getApplication());
        redisApiKey.setSubscription(apiKey.getSubscription());
        redisApiKey.setPlan(apiKey.getPlan());

        if (apiKey.getCreatedAt() != null) {
            redisApiKey.setCreatedAt(apiKey.getCreatedAt().getTime());
        }
        if (apiKey.getUpdatedAt() != null) {
            redisApiKey.setUpdatedAt(apiKey.getUpdatedAt().getTime());
        }
        if (apiKey.getExpireAt() != null) {
            redisApiKey.setExpireAt(apiKey.getExpireAt().getTime());
        }
        if (apiKey.getRevokedAt() != null) {
            redisApiKey.setRevokeAt(apiKey.getRevokedAt().getTime());
        }
        redisApiKey.setRevoked(redisApiKey.isRevoked());

        return redisApiKey;
    }
}
